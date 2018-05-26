#!/bin/sh


python3.4 D4_packet_generator.py --remote-sff-ip 192.168.0.52 --local-port 6633 --remote-sff-port 6633 --sfp-id 4 --sfp-index 255 --inner-src-ip 192.168.0.51 --inner-dest-ip 192.168.0.51 --inner-src-port 6633 --inner-dest-port 6633 --ctx1 192.168.0.51 --encapsulate vxlan-nsh-ethernet-legacy
