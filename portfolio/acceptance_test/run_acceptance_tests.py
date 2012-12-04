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
### Collin Buchan (cbuchan)  ###
### Rajeev Sharma (rdsharma) ###
################################

# Loops through integration tests, running them and printing result summaries.

import os
import subprocess
import re
import sys

# CONFIGURATION
java_path = '/usr/bin/java'
code_path = '../../simulator/code'
file_path = 'acceptance_tests.txt'

# cmd line args
from optparse import OptionParser
parser = OptionParser()

parser.add_option("-v", action="store_true", dest="verbose", help="verbose output")
parser.add_option("-n", type="int", dest="num", help="Run tests n times")
parser.add_option("-s", type="int", dest="seed", help="Specify seed")

(options, args) = parser.parse_args(sys.argv)

if(options.verbose):
    print "Using verbose output"

if(options.num != None):
    print "Running tests " + str(options.num) + " times"
    runCount = options.num
else:
    runCount = 1

if(options.seed != None):
    print "Using seed " + str(options.seed)

# initialize reused variables
cwd = os.getcwd()
classpath = os.path.abspath(code_path)

time = [0] * runCount

for i in range(runCount):
    
    print "================================="
    print "Iteration: " + str(i)
    print "================================="
    
    # open file for input
    f = open(file_path)
    
    # keep track of line count inside loop
    linecount = 0
    time[i] = 0

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
        result = re.match('.*\.pass$', tokens[1])
        if (result == None) :
            print "Line " + str(linecount) + " malformed passfile"
            continue
        result = re.match('.*RuntimeMonitor$', tokens[2])
        if (result == None) :
            print "Line " + str(linecount) + " malformed monitor"
            continue

        if(options.seed != None):
            # run java commmand and save output
            output = subprocess.Popen([java_path, '-cp', classpath,
                    'simulator.framework.Elevator', '-head', 'headerfile', '-pf', tokens[1], 
                    '-monitor', tokens[2], '-fs', '5.0', '-b', '200', 
                    '-seed', str(options.seed)], 
                    stdout=subprocess.PIPE, stderr=subprocess.STDOUT).communicate()[0]
        else:
            # run java commmand and save output
            output = subprocess.Popen([java_path, '-cp', classpath,
                    'simulator.framework.Elevator', '-head', 'headerfile', '-pf', 
                    tokens[1], '-monitor', tokens[2], '-fs', '5.0', '-b', '200'], 
                    stdout=subprocess.PIPE, stderr=subprocess.STDOUT).communicate()[0]


        #print output
        
        ####################
        # EMERGENCY OUTPUTS
        ####################
        
        # get RandomSeed used
        randomseed_m = re.search('RandomSeed\s=\s*(\d+)', output)
        if (randomseed_m != None):
            randomseed = randomseed_m.group(1)
        else:
            print tokens[0] + ": " + "ERROR: malformed output (RandomSeed)"
            continue

        # get Emergency Brake if engaged
        e_break = re.search('\[Safety\]\s+@(\d+\.\d*):\s+(.*)', output)
        if (e_break != None):
            safety = e_break.group(1)
            e_message = e_break.group(2)
            print tokens[0] + "(" + randomseed + ")" + ": FAILED @ " + safety
            if(options.verbose):
                print "\t" + e_message
            continue
        
        ###################
        # STANDARD OUTPUTS
        ###################
            
        # get passenger delivered count
        delivered_m = re.search('Delivered:\s*(\d+)', output)
        if (delivered_m != None):
            delivered = delivered_m.group(1)
            
        # get total passenger count
        stranded_m = re.search('Stranded:\s*(\d+)', output)
        if (stranded_m != None):
            stranded = stranded_m.group(1)

        # get total passenger count
        total_m = re.search('Total:\s*(\d+)', output)
        if (total_m != None):
            total = total_m.group(1)
            
        ##################
        # VERBOSE OUTPUTS
        ##################
        
        # Delivery Performance
        avg_time_m = re.search('Average_delivery_time:\s*(\d+\.\d*)', output)
        if (avg_time_m != None):
            avg_time = avg_time_m.group(1)
            
        max_time_m = re.search('Maximum_delivery_time:\s*(\d+\.\d*)', output)
        if (max_time_m != None):
            max_time = max_time_m.group(1)
            
        del_score_m = re.search('Delivery_performance_score:\s*(\d+\.\d*)', output)
        if (del_score_m != None):
            del_score = del_score_m.group(1)
         
        # Satisfaction Performance
        avg_satis_m = re.search('Average_satisfaction_score:\s*(\d+\.\d*)', output)
        if (avg_satis_m != None):
            avg_satis = avg_satis_m.group(1)
            
        min_satis_m = re.search('Min_satisfaction_score:\s*(\d+\.\d*)', output)
        if (min_satis_m != None):
            min_satis = min_satis_m.group(1)
        
        satis_score_m = re.search('Satisfaction_performance_score:\s*(\d+\.\d*)', output)
        if (satis_score_m != None):
            satis_score = satis_score_m.group(1)
            
        # Time
        simtime_m = re.search('(\d+\.\d*s)\s*simulation seconds', output)
        if (simtime_m != None):
            simtime = simtime_m.group(1)
            
        realtime_m = re.search('(\d+\.\d*)\s*real seconds', output)
        if (realtime_m != None):
            realtime = realtime_m.group(1)
            
            
        ##################
        # RUNTIME MONITOR
        ##################
        
        # get total passenger count
        warnings_m = re.search('generated\s*(\d+)\s*warnings', output)
        if (warnings_m != None):
            warnings = warnings_m.group(1)
            
        stop_no_calls_m = re.search('Stopped at floor with no calls =\s*(\d+)', output)
        if (stop_no_calls_m != None):
            stop_no_calls = stop_no_calls_m.group(1)
                
        open_no_calls_m = re.search('Doors opened at floor with no calls =\s*(\d+)', output)
        if (open_no_calls_m != None):
            open_no_calls = open_no_calls_m.group(1)
            
        lantern_not_lit_m = re.search('another floor =\s*(\d+)', output)
        if (lantern_not_lit_m != None):
            lantern_not_lit = lantern_not_lit_m.group(1)
        
        lantern_change_m = re.search('doors open =\s*(\d+)', output)
        if (lantern_change_m != None):
            lantern_change = lantern_change_m.group(1)
            
        service_wrong_dir_m = re.search('than lantern =\s*(\d+)', output)
        if (service_wrong_dir_m != None):
            service_wrong_dir = service_wrong_dir_m.group(1)
                    
        no_fast_speed_m = re.search('fast speed =\s*(\d+)', output)
        if (no_fast_speed_m != None):
            no_fast_speed = no_fast_speed_m.group(1)
            
        nudge_reverse_m = re.search('before reversal =\s*(\d+)', output)
        if (nudge_reverse_m != None):
            nudge_reverse = nudge_reverse_m.group(1)   
            
        ################
        # PRINT RESULTS
        ################
        if(options.verbose):
            # print result
            print tokens[0] + "(" + randomseed + ")" + ": "
            print "  Delivery: " + delivered + " delivered, " + stranded + " stranded, " + total + " total"
            print "  Delivery Perf: " + avg_time + " avg, " + max_time + " max, " + del_score + " score"
            print "  Satisfaction: " + avg_satis + " avg, " + min_satis + " min, " + satis_score + " score"
            print "  Warnings: " + warnings + " w, " + stop_no_calls + " snc, " + open_no_calls + " onc, " \
                    + lantern_not_lit + " lantern no lit, " + lantern_change + " lantern change dir, " \
                    + service_wrong_dir + " service wrong dir, " + no_fast_speed + " no fast speed, " \
                    + nudge_reverse + " nr"
            print "  Time: " + simtime + " (" + realtime + "s real)" 

        else:
            # print result
            print ''.join([tokens[0], "(" + randomseed + ")", ": ", delivered, " delivered, ", 
                    stranded, " stranded, ", total, " total, ", warnings, " warnings"])
                    
        time[i] += float(realtime)

    print "\nIteration time: " + str(time[i]) + "s"
    
    # done, close file
    f.close()
    
#done running all iterations
print "================================="
print "Total Time: " + str(sum(time)) + "s"
print "Average Time: " + str(sum(time)/runCount) + "s"
print "================================="
