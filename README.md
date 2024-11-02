# hubby

The goal was to create a cross-platform tool with minimum dependencies that would allow establishing a data link between
two devices on separate local networks.

## Usage

`java -jar hubby.jar ENDPOINT ENDPOINT`

where `ENDPOINT` is `{ local | remote }:HOST:PORT` and `HOST` is a hostname or an IP address.

### Examples

`java -jar hubby.jar local:192.168.1.100:8080 remote:google.com:80`

- Connects to TCP port 80 on a host with hostname google.com.
- Binds a socket to TCP port 8080 on a network interface with IP address 192.168.1.100 and accepts a single client
  connection.
- Proxies all reads and writes from one endpoint to another.
- Reopens both endpoints in case of failure with exponential backoff.

### Limitations

- Single client per local endpoint.
- TCP only.
- No authentication.

## Installation

Java runtime environment version 21 or greater is required.

Download a JAR file from the latest release on GitHub.
