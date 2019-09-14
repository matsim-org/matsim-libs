#!/usr/bin/ruby
require "network"
require "logger"
require "population"

require "prognose2025Tools"

$log = Logger.new(STDOUT)
$total = 0
$total_gv = 0
$valid_gv_trips = 0
$errors = 0
$person_id = 0
$tons_lkw_total = 0

class Person
  attr_writer :guetergruppe, :tonnen
  attr_reader :guetergruppe, :tonnen
end




def create_random_time_sec()
  #distributed equally over the day 
  start_time = 12 * 3600.0
  time_window = 6 * 3600.0
  daytimerand = rand
  if (daytimerand < 0.20) #drive at night
    start_time = 24.0 * 3600.0
    time_window = 10 * 3600.0
  elsif (daytimerand < 0.25) # no break at 4 in the morning
    start_time = 4 * 3600.0
    time_window = 1 * 3600.0
  elsif (daytimerand < 0.5) # peek at 8 in the morning
    start_time = 5.5 * 3600.0
    time_window = 2 * 3600.0
  elsif (daytimerand < 0.65) # another peek around 11
    start_time = 9.5 * 3600.0
    time_window = 2 * 3600.0
  elsif (daytimerand < 0.75) # another peek around 15
    start_time = 13.5 * 3600.0
    time_window = 2 * 3600.0
  end
  time = (rand * time_window).round
  if (rand < 0.5)
    time = start_time + time
  else
    time = start_time - time
  end
  if (time >= 24.0 * 3600.0)
    time = time - (24.0 * 3600.0)
  elsif (time < 0.0)
    time = time.abs
  end
  return time
end



def retrieve_closest_key(distance_km, entfernung_1steller_gewicht_map)
  sorted_keys = entfernung_1steller_gewicht_map.keys
  sorted_keys = sorted_keys.sort
  sorted_keys.each { |key|
    if (key >= distance_km)
      return key
    end
  }  
end

def calculate_trucks_count(guetergruppe, tonnen, transportleistung, entfernung_1steller_gewicht_map) 
#  puts
  distance_km = transportleistung / tonnen
  key = retrieve_closest_key(distance_km, entfernung_1steller_gewicht_map)
#  puts "found key #{key} for guetergruppe #{guetergruppe} distance #{distance_km} tonnen #{tonnen} transleistung #{transportleistung}"
  mittel_gewicht_kg = nil
  begin
    einsteller_gewicht_map = entfernung_1steller_gewicht_map[key]
    mittel_gewicht_kg = einsteller_gewicht_map[guetergruppe] if (einsteller_gewicht_map != nil && (!einsteller_gewicht_map.empty?))
#    puts "key #{key} einsteller_gewicht_map #{einsteller_gewicht_map} mittel_gewicht_kg #{mittel_gewicht_kg}"
    if (key >= 5)
      key = key - 5
    else
      break
    end
  end while (mittel_gewicht_kg == nil)
#  puts "mittel_gewicht_kg #{mittel_gewicht_kg} closest_key #{key}"
  no_trucks = (tonnen * 1000.0 / 300.0) / mittel_gewicht_kg
#  puts "no_trucks #{no_trucks} round #{no_trucks.round}"
  return no_trucks.round
end


def create_person(line, network, zones, population, entfernung_1steller_gewicht_map, sample_size)
#  puts "processing line #{line}"
  sp = line.chomp.split(/;/)
  quellzone = sp[0].to_i
  zielzone = sp[1].to_i
  mode = sp[2]
  guetergruppe = sp[3].to_i
  tonnen = sp[5].to_f
  transportleistung = sp[6].to_f
  $total = $total + 1
#  person_id = quellzone.to_s + zielzone.to_s + guetergruppe
#  if (population.persons[person_id])
#    $log.error "Id #{person_id} already exists in population"
#  else
    if (mode == "3")
      $tons_lkw_total = $tons_lkw_total + tonnen
      number_of_trips = calculate_trucks_count(guetergruppe, tonnen, transportleistung, entfernung_1steller_gewicht_map)
      number_of_trips.times {
        if (rand < sample_size) 
          $person_id = $person_id + 1
          person = Person.new($person_id)
          population.add_person(person)
          start_node = nil
          end_node = nil
          $total_gv = $total_gv + 1
          if (network.node_of_zone[quellzone] && network.node_of_zone[zielzone])
            $valid_gv_trips = $valid_gv_trips + 1
            start_node = network.node_of_zone[quellzone]
            end_node = network.node_of_zone[zielzone]
          elsif (zones[quellzone] && zones[zielzone])
            $valid_gv_trips = $valid_gv_trips + 1
            #here we have to get the closest node to this zone
            start_node = get_closest_node(zones[quellzone], network)
            end_node = get_closest_node(zones[zielzone], network)
          elsif (zones[quellzone] && network.node_of_zone[zielzone])
            $valid_gv_trips = $valid_gv_trips + 1
            start_node = get_closest_node(zones[quellzone], network)
            end_node = network.node_of_zone[zielzone]
          elsif (network.node_of_zone[quellzone] && zones[zielzone])
            $valid_gv_trips = $valid_gv_trips + 1
            start_node = network.node_of_zone[quellzone]
            end_node = get_closest_node(zones[zielzone], network)
          end
          if ((start_node) && (end_node))
            plan = Plan.new
            plan.selected = true
            person.plans << plan
            act1 = Activity.new
            plan.add_plan_element(act1)
            leg = Leg.new
            plan.add_plan_element(leg)
            act2 = Activity.new
            plan.add_plan_element(act2)
            act1.coordinate = start_node.coordinate
            act1.type = "h"
            end_time_sec = create_random_time_sec()
            end_time_matsim = convert_to_matsim_time_format(end_time_sec)
            act1.end_time = end_time_matsim
            leg.mode = "car"
            act2.coordinate = end_node.coordinate
            act2.type = "h"
          else
            if ($errors < 5000)
              $log.error "#{quellzone} -> #{zielzone} no node: #{start_node} to #{end_node}"
            end
            $errors = $errors + 1
          end
        end
      }
    end
#  end
end



def process_demand(gv_file, network, zones, entfernung_1steller_gewicht_map, sample_size)
  $log.info "processing demand with file #{gv_file}"
  population = Population.new
  File.open(gv_file, "r") do |file|
    file.each{ |line| 
      create_person(line, network, zones, population, entfernung_1steller_gewicht_map, sample_size)
    }    
  end
  $log.info "found #{$valid_gv_trips} valid gv trips of #{$total_gv} total gv trips of #{$total} trips, create #{$person_id} persons"
  $log.info "total tons of gueterverkehr #{$tons_lkw_total} t"
  population
end





