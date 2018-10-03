#!/bin/sh
# The submitted file should not be a java package, so we remove this from the source
# file and rename it with a .txt extension.

sed -e 's/package cda5155.proj1;//' src/main/java/cda5155/proj1/MIPSsim.java > MIPSsim.java.txt
