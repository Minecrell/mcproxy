package MinecraftProxy

import (
	"os"
	"log"
	"net"
	"io"
	"fmt"
	"errors"
	"strconv"
)

func badUsage() {
	fmt.Fprintln(os.Stderr, "Usage: MinecraftProxy <Host:Port> <RemoteHost:RemotePort>"); os.Exit(1)
}

func Create() {
	if len(os.Args) < 2  || len(os.Args) > 3 { badUsage() }

	localHost := ParseAddress(os.Args[1])
	if localHost == nil { badUsage() }
	localAddress, _, err := localHost.Resolve()
	if err != nil { fmt.Fprintln(os.Stderr, err); os.Exit(1) }

	remoteHost := ParseAddress(os.Args[2])
	if remoteHost == nil { badUsage() }
	_, _, err = remoteHost.Resolve()
	if err != nil { fmt.Fprintln(os.Stderr, err); os.Exit(1) }

	fmt.Println("Listening to:", os.Args[1])
	fmt.Println("Proxying to:", os.Args[2])
	fmt.Println()

	log.SetPrefix("[MinecraftProxy] ")
	log.SetOutput(os.Stdout)
	Start(localAddress, remoteHost)
}

func Start(localAddress *net.TCPAddr, remoteAddress *ServerAddress) (err error) {
	local, err := net.ListenTCP("tcp", localAddress)
	if err != nil { return }
	defer local.Close()

	for {
		conn, err := local.AcceptTCP()
		if err == nil {
			go proxy(conn, remoteAddress)
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

func proxy(local *net.TCPConn, remoteHost *ServerAddress) {
	defer local.Close()
	remoteAddress, connectAddress, err := remoteHost.Resolve()
	if err != nil { log.Println("Cannot resolve address:", err.Error()); return }

	log.Println("Proxying", local.RemoteAddr(), "to", remoteAddress)

	remote, err := net.DialTCP("tcp", nil, remoteAddress)
	if err != nil { log.Println("Unable to connect:", err); return }
	defer remote.Close()

	err = transformHandshake(local, remote, connectAddress)
	if err != nil { log.Println("Unable to transform handshake:", err)}

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
