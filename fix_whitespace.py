#!/usr/bin/env python3

f = open('other_simulation.txt')
s = f.read()
f.close()
print(s
  .replace('--------------------', '\n--------------------')
  .replace(': \t', ':\t')
  .replace('Registers\n\n', 'Registers\n')
  .replace('Data', '\nData')
  .strip()
)
