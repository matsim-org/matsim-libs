#!/usr/bin/ruby

#require "prognose2025GvOneDay"
require "prognose2025Network"
#require "prognose2025Tools"
require "prognose2025Files"
require "network"

include Prognose2025Files


net_assumptions = NetworkAssumptions.new
#autobahn
net_assumptions.set_speed_for_category(1, 36.11)
#bundesstrasse
net_assumptions.set_speed_for_category(2, 22.22)
#landesstrasse
net_assumptions.set_speed_for_category(3, 22.22)

puts "starting network writer..."

network_out_file = BASE_DIRECTORY + "demand/network_pv.xml"

network = process_network(KNOTEN_FILE, STRECKEN_FILE, true, net_assumptions)
NetworkUtils.write_network(network_out_file, network)

puts "network written to #{network_out_file}"