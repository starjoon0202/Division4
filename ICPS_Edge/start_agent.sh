#!/bin/sh

# auto-sff-name means agent will try to discover its SFF name dynamically during
# start-up and later when it receives a RSP request
python3.4 D4_Service_Function/D4_Service_Function_agent.py --rest --odl-ip-port 192.168.0.49:8181 --auto-sff-name
