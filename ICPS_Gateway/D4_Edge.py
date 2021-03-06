import subprocess
import struct
import time

C_END     = "\033[0m"
C_BOLD    = "\033[1m"
C_INVERSE = "\033[7m"
 
C_BLACK  = "\033[30m"
C_RED    = "\033[31m"
C_GREEN  = "\033[32m"
C_YELLOW = "\033[33m"
C_BLUE   = "\033[34m"
C_PURPLE = "\033[35m"
C_CYAN   = "\033[36m"
C_WHITE  = "\033[37m"
 
C_BGBLACK  = "\033[40m"
C_BGRED    = "\033[41m"
C_BGGREEN  = "\033[42m"
C_BGYELLOW = "\033[43m"
C_BGBLUE   = "\033[44m"
C_BGPURPLE = "\033[45m"
C_BGCYAN   = "\033[46m"
C_BGWHITE  = "\033[47m"


threshold = 0.0180


result_central_ping = subprocess.check_output('./D4_Central_ping.sh',shell=True)
result_central_delay = result_central_ping.decode()
#print("iCPS Service delay from GW to iCPS Edge Server:", result_edge_delay, "Delay from GW to iCPS Central Cloud:", result_central_delay)
print(C_BOLD + C_RED + "Central Cloud :  " ,result_central_delay + C_END)

result_edge_data = subprocess.check_output('./D4_Edge_Flow.sh',shell=True)
result_edge_delay = result_edge_data.decode()
print( "Edge Cloud :  " ,result_edge_delay )

if result_central_delay > str(threshold) :
   print ('-----------------------------------------------------------------------------') 
   print (C_BOLD + C_RED +'Forwarding packet to Edge server\n'+ C_END) 
   subprocess.call ('python3.4 D4_Edge.py',shell=True)
   time.sleep (1)
else :
   print ('-----------------------------------------------------------------------------') 
   print (C_BOLD + C_GREEN + 'Forwarding packet to Central server\n' + C_END) 
   subprocess.call ('python3.4 D4_Central.py',shell=True)
   time.sleep (1)
