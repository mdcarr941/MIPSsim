#!/bin/sh
# The submitted file should not be a java package, so we remove this from the source
# file and rename it with a .txt extension.

sed -e 's/package cda5155.MIPSsim;//' src/main/java/cda5155/MIPSsim/MIPSsim.java > MIPSsim.java.txt
cp MIPSsim.java.txt build/MIPSsim.java
cp proj2/sample.txt build/input.txt
cd build
javac MIPSsim.java
java MIPSsim input.txt
if test -z "$(diff -w -B disassembly.txt ../proj2/sample_disassembly.txt)"
  test -z "$(diff -w -B simulation.txt ../proj2/sample_simulation.txt)"; then
  echo "Success"
else
  echo "Failure"
fi
