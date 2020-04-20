#!/bin/bash	

_package_main() {
	mvn package
}

_package_main ${*}
