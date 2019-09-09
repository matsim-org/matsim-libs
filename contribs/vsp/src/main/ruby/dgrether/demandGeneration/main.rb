#!/usr/bin/ruby

require "prognose2025GvOneDay"
require "prognose2025Network"
require "prognose2025Tools"
require "prognose2025Files"

include Prognose2025Files

#main code

network_out_file = BASE_DIRECTORY + "demand/network_bs.xml"

pop_out_file = BASE_DIRECTORY + "demand/population_gv_0.1pct_raw.xml"

network = process_network(KNOTEN_FILE, STRECKEN_FILE, false)
#NetworkUtils.write_network(network_out_file, network)

zones = process_anbindung(ANBINDUNGS_FILE)

entfernung_1steller_gewicht_map = load_lkw_auslastung(LKW_AUSLASTUNGS_FILE)

population = process_demand(GV_FILE, network, zones, entfernung_1steller_gewicht_map, 0.01)
#
#pop = PopulationUtils.create_sample(0.01, population)
#
PopulationUtils.write_population(File.new(pop_out_file, "w"), population)

#write_guetergruppe_tonnen(gueter_tonnen_file, pop)

puts "done"


