#!/bin/bash

name=$1
shift

rm -f net/granjow/acpr/*.class
rm -f net/granjow/bytedisplay/*.class
javac net/granjow/acpr/${name}.java && java  net.granjow.acpr.${name} "$@"