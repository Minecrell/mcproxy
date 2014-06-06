package main

import (
	"fmt"
	"os"
	"log"
	"net"
	"errors"
	"strconv"
	"io"
)

const (
	USAGE = "Usage: mcproxy <Host:Port> <RemoteHost:RemotePort> [Version|1.7/-]"
)

func printError(a... interface{}) int {
	fmt.Fprintln(os.Stderr, a)
	return 1
}

func printUsage() int {
	return printError(USAGE)
}

func main() {
	os.Exit(Create())
}

func Create() int {
	if len(os.Args) < 2 || len(os.Args) > 4 { return printUsage() }

	localHost := ParseAddress(os.Args[1])
	if localHost == nil { return printUsage() }
	localAddress, _, err := localHost.Resolve()
	if err != nil { return printError(err) }

	remoteHost := ParseAddress(os.Args[2])
	if remoteHost == nil { return printUsage() }
	_, _, err = remoteHost.Resolve()
	if err != nil { return printError(err) }

	version := "1.7"
	if len(os.Args) > 3 { version = os.Args[3] }

	fmt.Println("Listening to:", os.Args[1])
	fmt.Println("Proxying to:", os.Args[2])
	fmt.Println()

	log.SetPrefix("[mcproxy] ")
	log.SetOutput(os.Stdout)
	err = Start(localAddress, remoteHost, version)
	if err != nil {
		log.Fatalln("Unable to start proxy:", err.Error())
	}

	return 0
}

func Start(localAddress *net.TCPAddr, remoteAddress *ServerAddress, version string) (err error) {
	local, err := net.ListenTCP("tcp", localAddress)
	if err != nil { return }
	defer local.Close()

	log.Println("Network listener started and is waiting for requests.")
	if version == "1.7" { log.Println("Transforming handshakes for version 1.7") }

	for {
		conn, err := local.AcceptTCP()
		if err == nil {
			go proxy(conn, remoteAddress, version)
		} else {
			log.Println("Connection failed:", err)
		}
	}
}

func transformHandshake(local *net.TCPConn, remote *net.TCPConn, address *ConnectAddress) (err error) {
	localMinecraft := &MinecraftStream { local }
	packetID, reader, err := packetReader(localMinecraft)
	if packetID != 0x00 { return errors.New("Invalid handshake packet: " + strconv.Itoa(packetID)) }

	version, err := readVarInt(reader); if err != nil { return }
	clientAddress, err := readString(reader); if err != nil { return }
	clientPort, err := readShort(reader); if err != nil { return }
	request, err := readVarInt(reader); if err != nil { return }

	log.Println("Client connected with", clientAddress, clientPort, "Version", version, "Request", request)
	log.Println("Connecting to server with", address.Host, address.Port, "Version", version, "Request", request)

	writer, err := packetWriter(0x00); if err != nil { return }
	err = writeVarInt(writer, version); if err != nil { return }
	err = writeString(writer, address.Host); if err != nil { return }
	err = writeShort(writer, address.Port); if err != nil { return }
	err = writeVarInt(writer, request); if err != nil { return }

	err = writeVarInt(remote, writer.Len()); if err != nil { return }
	_, err = remote.Write(writer.Bytes()); return
}

func proxy(local *net.TCPConn, remoteHost *ServerAddress, version string) {
	defer local.Close()
	remoteAddress, connectAddress, err := remoteHost.Resolve()
	if err != nil { log.Println("Cannot resolve address:", err.Error()); return }

	log.Println("Proxying", local.RemoteAddr(), "to", remoteAddress)

	remote, err := net.DialTCP("tcp", nil, remoteAddress)
	if err != nil { log.Println("Unable to connect:", err); return }
	defer remote.Close()

	if version == "1.7" {
		err = transformHandshake(local, remote, connectAddress)
		if err != nil { log.Println("Unable to transform handshake:", err)}
	}

	copyError := make(chan string)
	go copyStream(local, remote, copyError, true)
	go copyStream(remote, local, copyError, false)

	copyErr := <- copyError
	if len(copyErr) > 0 {
		log.Println(copyErr)
	}

	log.Println("Disconnected", local.RemoteAddr(), "from", remoteAddress)
}

func copyStream(from, to net.Conn, out chan<- string, client bool) {
	_, err := io.Copy(to, from)
	if err != nil {
		name := "SERVER"; if client { name = "CLIENT" }
		out <- "An error occurred with [" + name + "|" + from.RemoteAddr().String() + "]\n" + err.Error()
	} else {
		out <- ""
	}
}



