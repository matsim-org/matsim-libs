#!/usr/bin/ruby

require "population"
require "logger"


class ToNodeTonnen
  
  attr_reader :to_node, :tonnen 
  
  def initialize(to_node, tonnen)
    @to_node = to_node
    @tonnen = tonnen
  end
end

class GuetergruppeArray < Array 
  attr_reader :tonnen_gesamt
  attr_writer :tonnen_gesamt
  
  def initialize()
    @tonnen_gesamt = 0.0
  end
end

class KnotenKnotenTonnen
  
  attr_reader :from_node_guetergruppe_map
  
  def initialize()
    @from_node_guetergruppe_map = Hash.new
  end

  def add_from_to_rel(from_node, to_node, guetergruppe, tons)
    guetergruppe_map  = @from_node_guetergruppe_map[from_node]
    if (guetergruppe_map == nil)
      guetergruppe_map = Hash.new
      @from_node_guetergruppe_map[from_node] = guetergruppe_map
    end
    list = guetergruppe_map[guetergruppe]
    if (list == nil)
      list = GuetergruppeArray.new
      guetergruppe_map[guetergruppe] = list
    end
    entry = ToNodeTonnen.new(to_node, tons)
    list.tonnen_gesamt = list.tonnen_gesamt + tons
    list << entry
    # sort the list ascending by tonnen attribute of entries
    # actually not needed
#    list.sort! { |a,b|
#      a.tonnen <=> b.tonnen
#    }
  end
  
  def get_random_from_node(person)
    keys = @from_node_guetergruppe_map.keys
    index = keys.size * rand
    from_node = keys[index.to_i]
    return get_weighted_random_to_node(from_node, person)
  end
   
  def get_weighted_random_to_node(from_node, person)
    guetergruppe = person.guetergruppe
    guetergruppe_map  = @from_node_guetergruppe_map[from_node]
    list = guetergruppe_map[guetergruppe]
    while (list == nil)
      g = guetergruppe - 1
      if (g < 0)
        g = 9
      end
      if (g == guetergruppe)
        raise "no guetergruppe found"
      end
      list = guetergruppe_map[g]
      person.guetergruppe = g
    end
    sum_tons = list.tonnen_gesamt
    selected_tons = 0.0
    random_tons_frac = rand * sum_tons    
    list.each { |to_node_tonnen|
      selected_tons = selected_tons + to_node_tonnen.tonnen
      if (selected_tons >= random_tons_frac)
        return to_node_tonnen.to_node   
      end
    }
  end
end

def process_line(line, kkt, valid_gv_trips, total_gv_trips, network, zones)
  sp = line.chomp.split(/;/)
  quellzone = sp[0].to_i
  zielzone = sp[1].to_i
  mode = sp[2]
  guetergruppe = sp[3].to_i
  tonnen = sp[5].to_f
  transportleistung = sp[6].to_f
  if (mode == "3")
    start_node = nil
    end_node = nil
    total_gv_trips = total_gv_trips + 1
    if (network.node_of_zone[quellzone] && network.node_of_zone[zielzone])
      start_node = network.node_of_zone[quellzone]
      end_node = network.node_of_zone[zielzone]
    elsif (zones[quellzone] && zones[zielzone])
      #here we have to get the closest node to this zone
      start_node = get_closest_node(zones[quellzone], network)
      end_node = get_closest_node(zones[zielzone], network)
    elsif (zones[quellzone] && network.node_of_zone[zielzone])
      start_node = get_closest_node(zones[quellzone], network)
      end_node = network.node_of_zone[zielzone]
    elsif (network.node_of_zone[quellzone] && zones[zielzone])
      start_node = network.node_of_zone[quellzone]
      end_node = get_closest_node(zones[zielzone], network)
    end
    if (start_node && end_node)
      valid_gv_trips = valid_gv_trips + 1
      kkt.add_from_to_rel(start_node, end_node, guetergruppe, tonnen)
    end
  end
end
    
class Person 
  attr_reader :guetergruppe
  attr_writer :guetergruppe
end


def process_demand_30_days(gv_file, network, zones)
  log = Logger.new(STDOUT)
  kkt = KnotenKnotenTonnen.new
  valid_gv_trips = 0
  total_gv_trips = 0
  File.open(gv_file, "r") do |file|
    file.each{ |line| 
      process_line(line, kkt, valid_gv_trips, total_gv_trips, network, zones)
#      log.info "size #{kkt.from_node_guetergruppe_map.size}"
    }    
  end
  log.info "file read"
  # warmup random number generator
  100000.times do
    rand
  end
  # create demand
  population = Population.new
  person = nil
  plan = nil
  id = 1
  10.times do
    person = Person.new(id)
    log.info "creating person id #{id}"
    id = id + 1
    person.guetergruppe = id % 10
    population.add_person(person)
    plan = Plan.new
    plan.selected = true
    person.plans << plan
    home_act = Activity.new
    plan.add_plan_element(home_act)
    home_act.type = "h"
    home_act.end_time = "07:00:00"

    start_node = kkt.get_random_from_node(person)
    end_node = kkt.get_weighted_random_to_node(start_node, person)
    home_act.coordinate = start_node.coordinate
    100.times do |i|
      if (i % 10)
        log.info "created #{i} legs for person #{id}"
      end
      leg = Leg.new
      plan.add_plan_element(leg)
      leg.mode = "car"
      act2 = Activity.new
      plan.add_plan_element(act2)
      act2.coordinate = end_node.coordinate
      act2.type = "h"
      act2.duration = "00:00:00"
      end_node = kkt.get_weighted_random_to_node(end_node, person)
    end
  end
  return population  
end

