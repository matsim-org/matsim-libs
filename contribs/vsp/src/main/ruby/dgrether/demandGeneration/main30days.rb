#!/usr/bin/ruby

require "logger"

require "prognose2025Files"
require "prognose2025Network"
require "prognose2025Tools"
require "prognose2025Gv30Days"

include Prognose2025Files

log = Logger.new(STDOUT)

log.info "loading network ..."
network = process_network(KNOTEN_FILE, STRECKEN_FILE, false)
log.info "network loaded"

log.info "loading anbindung..."
zones = process_anbindung(ANBINDUNGS_FILE)
log.info "anbindung loaded"

log.info "loading lkw auslastung..."
entfernung_1steller_gewicht_map = load_lkw_auslastung(LKW_AUSLASTUNGS_FILE)
log.info "lkw auslastung loaded"



pop_out_file = BASE_DIRECTORY + "demand/population_gv_30_days_10lkw_1.0.xml"

log.info "processing demand..."
population = process_demand_30_days(GV_FILE, network, zones)
log.info "created population"

log.info "writing population"
PopulationUtils.write_population(File.new(pop_out_file, "w"), population)
log.info "population written"

log.info "done"
