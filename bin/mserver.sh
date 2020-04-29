#!/bin/bash	

_server_main() {
	mvn compile exec:java -Dexec.mainClass="lulz.WebSocketChat" -Dmip
}

_server_main ${*}
