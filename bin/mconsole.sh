#!/bin/bash	

_console_main() {
	mvn compile exec:java -Dexec.mainClass="lulz.WebSocketConsole" 
}

_console_main ${*}
