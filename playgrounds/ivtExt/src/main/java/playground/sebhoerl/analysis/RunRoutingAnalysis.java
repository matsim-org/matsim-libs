package playground.sebhoerl.analysis;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import playground.sebhoerl.av.framework.AVConfigGroup;
import playground.sebhoerl.av.framework.AVModule;

public class RunRoutingAnalysis {
    static class WaitingEventHandler implements PersonDepartureEventHandler, PersonArrivalEventHandler, PersonEntersVehicleEventHandler {
        @Override
        public void reset(int iteration) {}
        
        private Map<Id<Person>, Double> ongoing = new HashMap<>();
        private Map<Id<Person>, Double> ongoingCar = new HashMap<>();
        
        public LinkedList<Double> waitingTimes = new LinkedList<>();
        public LinkedList<Double> travelTimes = new LinkedList<>();
        public LinkedList<Double> carTravelTimes = new LinkedList<>();
        public long stuck = 0;

        public void finish() {
            stuck = ongoing.size();
        }
        
        @Override
        public void handleEvent(PersonEntersVehicleEvent event) {
            if (event.getVehicleId().toString().contains("av")) {
                Double start = ongoing.get(event.getPersonId());
                
                if (start != null) {
                    waitingTimes.add(event.getTime() - start);
                    ongoing.put(event.getPersonId(), event.getTime());
                }
            }
        }

        @Override
        public void handleEvent(PersonDepartureEvent event) {
            if (event.getLegMode().equals("av")) {
                ongoing.put(event.getPersonId(), event.getTime());
            }
            
            if (event.getLegMode().equals("car") && !event.getPersonId().toString().contains("av")) {
                ongoingCar.put(event.getPersonId(), event.getTime());
            }
        }

        @Override
        public void handleEvent(PersonArrivalEvent event) {
            if (event.getLegMode().equals("av")) {
                Double start = ongoing.remove(event.getPersonId());
                
                if (start != null) {
                    travelTimes.add(event.getTime() - start);
                }
            } else if (event.getLegMode().equals("car")) {
                Double start = ongoingCar.remove(event.getPersonId());
                
                if (start != null) {
                    carTravelTimes.add(event.getTime() - start);
                }
            }
        }
    }
    
    static void modifyPopulation(Population population, double fraction, boolean carsOnly) {
        for (Person person : population.getPersons().values()) {
            if (Math.random() <= fraction) {
                Plan plan = person.getSelectedPlan();
                
                for (PlanElement element : plan.getPlanElements()) {
                    if (element instanceof Leg) {
                        Leg leg = (Leg) element;
                        
                        if (!carsOnly || leg.getMode().equals("car")) {
                            leg.setMode("av");
                        }
                    }
                }
            }
        }
    }
    
    public static void main(String[] args) throws JsonGenerationException, JsonMappingException, IOException {
        Config config = ConfigUtils.loadConfig(args[0], new AVConfigGroup());
        Scenario scenario = ScenarioUtils.loadScenario(config);
        
        boolean carsOnly = args[2].equals("cars");
        modifyPopulation(scenario.getPopulation(), Double.parseDouble(args[1]), carsOnly);
        
        Controler controler = new Controler(scenario);
        controler.addOverridingModule(new AVModule());
        
        final WaitingEventHandler handler = new WaitingEventHandler();
        
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                this.addEventHandlerBinding().toInstance(handler);
            }
        });
        
        controler.run();
        handler.finish();
        
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(new File(args[3]), handler);
    }
}
