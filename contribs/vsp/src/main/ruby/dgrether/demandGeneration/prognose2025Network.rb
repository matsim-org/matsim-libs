#!/usr/bin/ruby
require "lib/network"
require "logger"

$no_node_output = 0;
$no_link_output = 0;
$no_malformed_lane_links = 0;
$no_malformed_fs_links = 0;

class Node
  attr_writer :zone_id
  attr_reader :zone_id
end

class Network
  
  attr_reader :node_of_zone
  
  def initialize()
    @nodes = Hash.new
    @links = Hash.new
    @node_of_zone = Hash.new
  end
  
  def add_node(n)
    @nodes[n.id()] = n
    @node_of_zone[n.zone_id()] = n
  end
end

class NetworkAssumptions
  def initialize()
    @speed = Hash.new
    @default_speed = 13.89
  end
  
  def set_speed_default(speed_m_s)
    @default_speed = speed_m_s
  end
  
  def set_speed_for_category(category, speed_m_s)
    @speed[category] = speed_m_s
  end
  
  def get_speed_for_category(category)
    if (@speed[category] == nil)
      return @default_speed
    end
    @speed[category]
  end
end

def set_number_of_lanes(link, number_lanes, category)
  lanes = number_lanes.to_f
  if (lanes == 0.0)
    $no_malformed_lane_links = $no_malformed_lane_links + 1
    if (category == 1) #autobahn
      lanes = 2.0
    elsif category == 2 #bundesstrasse
      lanes = 1.0
    elsif category == 3 #landesstrasse
      lanes = 1.0
    elsif category == 4 #sonstige strasse
      lanes = 1.0
    elsif category == 5 #fähre
      lanes = 1.0
    else #whatever
      lanes = 1.0
    end
  end
  link.number_of_lanes = lanes
end

def set_link_freespeed(link, fs, category, network_assumptions)
  freespeed = fs.to_f
  if (freespeed == 0.0)
    $no_malformed_fs_links = $no_malformed_fs_links + 1
    freespeed = network_assumptions.get_speed_for_category(category)
  end
  link.vfreespeed = freespeed
end



#$f = File.new('testdata.txt','w')
def create_node(line, network)
  nummer, name, coord1, coord2, typ, zone, orig_coord_x, orig_coord_y = line.chomp.split(/;/)
  node = Node.new(nummer)
  node.coordinate.x = coord1.to_f
  node.coordinate.y = coord2.to_f
#  node.coordinate.x = orig_coord_x
#  node.coordinate.y = orig_coord_y
  node.zone_id = zone
  network.add_node(node)
  $no_node_output = $no_node_output + 1
  if ($no_node_output < 7)
    print node.id, " ", node.coordinate.y, " ", node.coordinate.x, " ", node.zone_id
 #   $f.write(line)
    puts
  end
  node
end

def set_from_to_nodes(link, from, to)
  link.from_node_id = from
  link.to_node_id = to
end

def create_link_from_line(line, network, network_assumptions)
  sl = line.chomp.split(/;/)
  id1 = sl[0] + "-" + sl[1]
  id2 = sl[1] + "-" + sl[0]
  link = Link.new(id1)
  link2 = Link.new(id2)
  set_from_to_nodes(link, sl[0], sl[1])
  set_from_to_nodes(link2, sl[1], sl[0])
  
  # calculate length
  # multiply with 1000 for conversion km -> m
  l = sl[2].to_f * 1000
#  l = sl[2].split(/\./)

  #as there are links with length 0.000 in the file set them to length = 1.0 m
  if (l == 0.0 )
    l = 1.0
  end
  link.length = l
  link2.length = l 


  #get category and repair fields that
  #have no meaningful entry by category
  category = sl[4].to_i
  # calculate lanes
  set_number_of_lanes(link, sl[7], category)
  set_number_of_lanes(link2, sl[8], category)

  # calculate freespeed
  set_link_freespeed(link, sl[12], category, network_assumptions)
  set_link_freespeed(link2, sl[13], category, network_assumptions)
  
  #set capacity from category
  #autobahn
  if (category == 1)
    link.capacity = link.number_of_lanes * 2000.0
    link2.capacity = link2.number_of_lanes * 2000.0
  #bundesstrasse
  elsif category == 2
    link.capacity = link.number_of_lanes * 1200.0
    link2.capacity = link2.number_of_lanes * 1200.0
  #landesstrasse
  elsif category == 3
    link.capacity = link.number_of_lanes * 1000.0
    link2.capacity = link2.number_of_lanes * 1000.0
  #sonstige strasse
  elsif category == 4
    link.capacity = link.number_of_lanes * 800.0
    link2.capacity = link2.number_of_lanes * 800.0
  #fähre
  elsif category == 5
    link.capacity = link.number_of_lanes * 500.0
    link2.capacity = link2.number_of_lanes * 500.0
  else
    link.capacity = link.number_of_lanes * 500.0
    link2.capacity = link2.number_of_lanes * 500.0
  end
  #only add specific links of network
#  if (category == 1)
    network.add_link(link)
    network.add_link(link2)
#  end
  $no_link_output = $no_link_output + 1
  if ($no_link_output < 20)
    print link.inspect
    puts
    print line
  end
  link
end

def process_network(knoten_file, strecken_file, do_process_strecken, network_assumptions)
  log = Logger.new(STDOUT)
  network = Network.new

  headers = []
  File.open(knoten_file, "r") do |file|
    1.times {headers << file.gets}
    file.each {|line| create_node(line, network) }
  end
  if (do_process_strecken)
    File.open(strecken_file, "r") do |file|
      1.times {headers << file.gets}
      file.each { |line| create_link_from_line(line, network, network_assumptions)}
    end
  end

  log.info "created network with #{network.nodes.size} nodes and #{network.links.size} links"
  log.info "  number of links with malformed freespeed entry: #{$no_malformed_fs_links} "
  log.info "  number of links with malformed lanes entry: #{$no_malformed_lane_links} "
  return network
end