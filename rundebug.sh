#!/bin/bash
mvn install -DskipTests
export MAVEN_OPTS="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,address=8002,suspend=n"
mvn hpi:run
