#!/bin/bash
export JAVA_HOME=/home/utilizador/.jdk/jdk-17.0.11+9
export PATH=$JAVA_HOME/bin:$PATH

cd "$(dirname "$0")"
mvn javafx:run
