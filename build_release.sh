#!/bin/sh
# The submitted file should not be a java package, so we remove this from the source
# file and rename it with a .txt extension.

sed -e 's/package cda5155.proj1;//' src/main/java/cda5155/proj1/MIPSsim.java > MIPSsim.java.txt
cp MIPSsim.java.txt build/MIPSsim.java
cp sample.txt build/input.txt
cd build
javac MIPSsim.java
java MIPSsim input.txt
if test -z $(diff -w -B disassembly.txt ../sample_disassembly.txt)
  test -z $(diff -w -B simulation.txt ../sample_simulation.txt); then
  echo "Success"
else
  echo "Failure"
fi
