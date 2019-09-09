require "lib/scenario"

class Node
  attr_reader :id, :coordinate
  attr_writer :coordinate
  def initialize(id)
    @id = id
    @coordinate = Coordinate.new(0,0)
  end
end

class Link 
  @capacity

  attr_reader :length, :vfreespeed, :capacity, :from_node_id  
  attr_reader :to_node_id, :number_of_lanes

  attr_writer :length, :vfreespeed, :capacity, :from_node_id  
  attr_writer :to_node_id, :number_of_lanes
  
  def initialize(id)
    @id = id
    @length = 1
    @vfreespeed = 1
  end
  
  def id
    @id
  end

  def to_s
    "Link length: #{@length} freespeed: #{@vfreespeed}" 
  end
  
end

class Network
  
  attr_reader :nodes, :links 
  
  def initialize()
    @nodes = Hash.new
    @links = Hash.new
  end

  def add_link(l)
    @links[l.id] = l
  end
  
  def add_node(n)
    @nodes[n.id()] = n
  end
  
end


module NetworkUtils
  
  require 'rubygems'
  require 'builder'
  
  def NetworkUtils.write_network(target, network)
    f = File.new(target, "w")
#    x = Builder::XmlMarkup.new(:target => $stdout, :indent => 2)
    x = Builder::XmlMarkup.new(:target => f, :indent => 2)
    x.instruct!
    x.declare!(:DOCTYPE, :network, :SYSTEM, "http://www.matsim.org/files/dtd/network_v1.dtd")
    x.network {
    x.nodes {
      network.nodes().each_value { |n| 
        x.node(nil, "id" => n.id, "x" => n.coordinate.x, "y" => n.coordinate.y)
      }    
    }
    x.links("capperiod" => "01:00:00") {
      network.links().each_value { |l| 
        x.link(nil, "id" => l.id, "from" => l.from_node_id, 
        "to" => l.to_node_id, "length" => l.length, "capacity" => l.capacity,
        "freespeed" => l.vfreespeed, "permlanes" => l.number_of_lanes)
      }
    }
    }
    f.close
  end
end
