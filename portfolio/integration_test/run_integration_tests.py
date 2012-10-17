#!/usr/bin/python

################################
### 18-649 Fall 2012         ###
### Group 17                 ###
### Collin Buchan (cbuchan)  ###
### Rajeev Sharma (rdsharma) ###
### Jesse Salazar (jessesal) ###
### Jessica Tiu (jtiu)       ###
################################
### Author:                  ###
### Rajeev Sharma (rdsharma) ###
################################

# Loops through integration tests, running them and printing result summaries.

import os
import subprocess
import re


# CONFIGURATION
java_path = '/usr/bin/java'
code_path = '../../simulator/code'
file_path = 'integration_tests.txt'


# initialize reused variables
cwd = os.getcwd()
classpath = os.path.abspath(code_path)

# open file for input
f = open(file_path)

# keep track of line count inside loop
linecount = 0

# loop through lines
for line in f :
    linecount += 1

    # ignore empty lines
    result = re.match('\A\s*$', line)
    if (result != None) :
        continue
    # ignore comments
    result = re.match('\A\s*;', line)
    if (result != None) :
        continue

    # split into tokens
    tokens = re.split("\s+", line)
    # validate tokens
    result = re.match('.*\.cf$', tokens[1])
    if (result == None) :
        print "Line " + str(linecount) + " malformed"
        continue
    result = re.match('.*\.mf$', tokens[2])
    if (result == None) :
        print "Line " + str(linecount) + " malformed"
        continue

    # run java commmand and save output
    output = subprocess.Popen([java_path, '-cp', classpath,
            'simulator.framework.Elevator', '-head', 'headerfile', '-cf', 
            tokens[1], '-mf', tokens[2]], stdout=subprocess.PIPE, 
            stderr=subprocess.STDOUT).communicate()[0]

    # get passed count
    passed_m = re.search('Passed:\s*(\d+)', output)
    if (passed_m == None):
        print tokens[0] + ': Error running test'
        continue
    passed = passed_m.group(1)

    # get failed count
    failed_m = re.search('Failed:\s*(\d+)', output)
    if (failed_m == None):
        print tokens[0] + ': Error running test'
        continue
    failed = failed_m.group(1)

    # print result
    print tokens[0] + ": " + passed + " passed, " + failed + " failed"

# done, close file
f.close()
