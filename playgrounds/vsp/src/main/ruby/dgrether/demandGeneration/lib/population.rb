require "scenario"
require "logger"
$log = Logger.new(STDOUT)

class Person
  attr_reader :id, :plans
  def initialize(id)
    @id = id
    @plans = Array.new
  end
  
  def to_s
    "Person id: #{@id} nr plans: #{@plans.size()}"
  end
end

class Population
  
  attr_reader :persons 
  
  def initialize()
    @persons = Hash.new
  end

  def add_person(p)
    @persons[p.id] = p
  end
end

class Plan
  
  attr_reader :plan_elements, :selected, :score
  attr_writer :selected, :score
  
  def initialize()
    @plan_elements = Array.new
  end
  
  def add_plan_element(pe)
    @plan_elements << pe
  end
end

class Activity
  attr_reader :end_time, :type, :coordinate, :link_id, :duration
  attr_writer :end_time, :type, :coordinate, :link_id, :duration
end

class Leg
  attr_reader :mode, :travel_time, :route
  attr_writer :mode, :travel_time, :route
end

class Route
  attr_reader :nodes
  attr_writer :nodes
end

module PopulationUtils
  
  require 'rubygems'
  require 'builder'
  
  def PopulationUtils.create_sample(sample_size, population)
    pop = Population.new
    population.persons().each_value { |p|
      pop.add_person(p) if rand < sample_size
    }
    return pop
  end
  
  def PopulationUtils.write_population(target, population)
#    f = File.new(target, "w")
#    x = Builder::XmlMarkup.new(:target => $stdout, :indent => 2)
    selected_string = nil
    x = Builder::XmlMarkup.new(:target => target, :indent => 2)
    x.instruct!
    x.declare!(:DOCTYPE, :plans, :SYSTEM, "http://www.matsim.org/files/dtd/plans_v4.dtd")
    x.plans {
      population.persons().each_value { |p| 
#        $log.error "person #{p}"
        x.person("id" => p.id) {
          p.plans().each { |plan|
            if (plan.selected == true)
              selected_string = 'yes'
            else
              selected_string = 'no'
            end
            x.plan("selected" => selected_string) {
              plan.plan_elements().each { |planElement|
                if (planElement.kind_of? Activity)
                  atts = Hash.new
                  #revise the order for Builder
                  if (planElement.link_id)
                    atts["link"] = planElement.link_id
                  end
                  if (planElement.coordinate)
                    atts["x"] = planElement.coordinate.x
                    atts["y"] = planElement.coordinate.y
                  end
                  if (planElement.type)
                    atts["type"] = planElement.type
                  end
                  if (planElement.end_time)
                    atts["end_time"] = planElement.end_time
                  end
                  if (planElement.duration)
                    atts["dur"] = planElement.duration
                  end
                  x.act(atts)
                elsif (planElement.kind_of? Leg)
                  atts = Hash.new
                  if (planElement.travel_time)
                    atts["trav_time"] = planElement.travel_time
                  end
                  if (planElement.mode)
                    atts["mode"] = planElement.mode
                  end
                  x.leg(atts) {
                    if (planElement.route)
                      x.route(planElement.route.nodes)   
                    end
                  }
                end
              }          
            }
          }
        }
      }    
    }
    if (target.kind_of? File)
      target.close
    end
  end
end #end module

def create_test_population()
  pop = Population.new
  person = Person.new(1)
  pop.add_person(person)
  plan = Plan.new
  person.plans << plan
  act = Activity.new
  act.type = "h"
  act.end_time = "08:00:00"
  c = Coordinate.new(2.3, 2.3)
  act.coordinate = c
  plan.plan_elements << act
  leg = Leg.new
  leg.mode = "car"
  plan.add_plan_element(leg)
  act2 = Activity.new
  c = Coordinate.new(4.2, 4.2)
  act2.coordinate = c
  act2.type = "h"
  plan.add_plan_element(act2)
  return pop
end

def write_test_population(target)
  pop = create_test_population()
  PopulationUtils.write_population(target, pop)
end

#write_test_population($stdout)
#write_test_population("test_population.xml")
