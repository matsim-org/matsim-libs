#!/usr/bin/ruby


class Zone
  attr_reader :id, :knoten_distance_map
#  attr_writer :knoten_distance_map
  
  def initialize(id)
    @id = id
    @knoten_distance_map = Hash.new
  end
end

def convert_to_matsim_time_format(time_sec)
  h = (time_sec / 3600).to_int
  s = time_sec % 3600
  m = (s / 60).to_int
  s = (s % 60).round
  time_string = String.new
  tmp = h.to_s
  time_string << "0" if (tmp.length < 2)
  time_string << tmp
  time_string << ":"
  tmp = m.to_s
  time_string << "0" if (tmp.length < 2)
  time_string << tmp
  time_string << ":"
  tmp = s.to_s
  time_string << "0" if (tmp.length < 2)
  time_string << tmp
  return time_string
end

def get_closest_node(zone, network)
  nodeid = nil
  distance = 1.0/0.0 # infinity
  zone.knoten_distance_map().each_pair { |kn, dist| 
    if (dist < distance)
      nodeid = kn
      distance = dist
    end
  }
  node = network.nodes[nodeid]
  if (!node)
    $log.error "network doesn't contain a node with id #{nodeid}"
  end
  return node
end


def process_anbindung(anbindungs_file)
  zones = Hash.new
  File.open(anbindungs_file, "r") do |file|
    file.each{ |line| 
      sp = line.chomp.split(/;/)    
      if (sp[1] == "2")
        zone = Zone.new(sp[0].to_i)
        zones[zone.id] = zone
        n = sp[2].to_i
#        print "zone ", zone.id, " "
        1.upto(n) { |i|
#          print sp[(2 + i)], " ", sp[(3 + n + i)], " "
          zone.knoten_distance_map[sp[(2 + i)]] = sp[(2 + (2*i))].to_f
            #(feels wrong to me. in my intuition, both indices would need to 
            # have a "2*i" somwhere. kai, jul'13)
        }
#        puts
      end
    }    
  end
  return zones
end

def load_lkw_auslastung(lkw_auslastungs_file)
  entfernung_1steller_gewicht_map = Hash.new
  entfernung = nil
  einsteller = nil
  gewicht = nil
  headers = ""
  einsteller_gewicht_map = nil
  File.open(lkw_auslastungs_file, "r") do |file|
    1.times{headers << file.gets}
    file.each { |line| 
      sp = line.chomp.split(/;/)    
      entfernung = sp[0].to_i
      einsteller = sp[1].to_i
      gewicht = sp[2].to_f
      einsteller_gewicht_map = entfernung_1steller_gewicht_map[entfernung]
      if (!einsteller_gewicht_map)
        einsteller_gewicht_map = Hash.new
        entfernung_1steller_gewicht_map[entfernung] = einsteller_gewicht_map
      end
      puts "entfernung #{entfernung} einsteller #{einsteller} gewicht #{gewicht}"
      einsteller_gewicht_map[einsteller] = gewicht
    }    
  end
  return entfernung_1steller_gewicht_map
end