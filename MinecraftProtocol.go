package MinecraftProxy

import (
	"io"
	"net"
	"bytes"
	"encoding/binary"
)

type MinecraftStream struct {
	*net.TCPConn
}

type BytesReader interface {
	io.Reader
	io.ByteReader
}

func (stream *MinecraftStream) ReadByte() (byte, error) {
	bytes, err := ReadBytes(stream, 1)
	return bytes[0], err
}
func ReadBytes(reader io.Reader, n int) (bytes []byte, err error) {
	bytes = make([]byte, n)
	_, err = reader.Read(bytes)
	return
}

func packetReader(reader BytesReader) (packetID int, packetReader BytesReader, err error) {
	length, err := readVarInt(reader); if err != nil { return }
	packet, err := ReadBytes(reader, length); if err != nil { return }
	packetReader = bytes.NewReader(packet)
	packetID, err = readVarInt(packetReader); return
}
func packetWriter(packetID int) (writer *bytes.Buffer, err error) {
	writer = new(bytes.Buffer)
	err = writeVarInt(writer, packetID); return
}

// VarInts (required for Minecraft 1.7)
func readVarInt(reader io.ByteReader) (num int, err error) {
	varint, err := binary.ReadUvarint(reader); if err != nil { return }
	return int(varint), err
}
func encodeVarInt(num int) []byte {
	buffer := make([]byte, binary.MaxVarintLen32)
	return buffer[:binary.PutUvarint(buffer, uint64(num))]
}
func  writeVarInt(writer io.Writer, num int) (err error) {
	_, err = writer.Write(encodeVarInt(num)); return
}

func readShort(reader io.Reader) (num uint16, err error) {
	// 2 bytes required for short
	buffer, err := ReadBytes(reader, 2); if err != nil { return }
	return binary.BigEndian.Uint16(buffer), err
}
func writeShort(writer io.Writer, num uint16) error {
	_, err := writer.Write(encodeShort(num)); return err
}
func encodeShort(num uint16) []byte {
	buffer := make([]byte, 2) // 2 bytes required for short
	binary.BigEndian.PutUint16(buffer, num); return buffer
}

func readString(reader BytesReader) (result string, err error) {
	length, err := readVarInt(reader); if err != nil { return }
	buffer, err := ReadBytes(reader, length); if err != nil { return }
	return string(buffer), err
}
func writeString(writer io.Writer, s string) (err error) {
	err = writeVarInt(writer, len(s)); if err != nil { return }
	_, err = io.WriteString(writer, s); return
}
