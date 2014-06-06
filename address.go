package main

import (
	"net"
	"strings"
	"strconv"
)

const (
	DEFAULT_PORT = 25565
	SRV_SERVICE = "minecraft"
)

type ServerAddress struct {
	Host string
	Port int
}

type ConnectAddress struct {
	Host string
	Port uint16
}

func (address *ServerAddress) Resolve() (tcp *net.TCPAddr, connect *ConnectAddress, err error) {
	if len(address.Host) > 0 && address.Port < 0 {
		_, records, err := net.LookupSRV(SRV_SERVICE, "tcp", address.Host)
		if err == nil && len(records) > 0 {
			ip, err := net.ResolveIPAddr("ip", records[0].Target)
			if err != nil { return nil, nil, err }
			return &net.TCPAddr { ip.IP, int(records[0].Port), ip.Zone }, &ConnectAddress { records[0].Target,
				records[0].Port }, nil
		}
	}

	port := address.Port
	if port < 0 { port = DEFAULT_PORT }

	ip, err := net.ResolveIPAddr("ip", address.Host)
	if err != nil { return }
	return &net.TCPAddr { ip.IP, port, ip.Zone }, &ConnectAddress { address.Host, uint16(port) }, nil
}

func createAddress(host string, port int) *ServerAddress {
	if len(host) == 0 || host == "*" { host = "" }
	return &ServerAddress { host, port }
}

func parseAddress(hostInput, portInput string) *ServerAddress {
	port := -1
	if len(portInput) > 0 {
		var err error
		port, err = strconv.Atoi(portInput)
		if err != nil { return nil }
	}

	return createAddress(hostInput, port)
}

func ParseAddress(input string) *ServerAddress {
	if len(input) == 0 { return nil }
	if input == ":" { return &ServerAddress {} }

	host := strings.SplitN(input, ":", 2)
	if len(host) == 0 { return nil }
	port := ""
	if len(host) > 1 { port = host[1] }
	return parseAddress(host[0], port)
}
