package playground.tobiqui.master;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scenario.ScenarioUtils;

public class EventsComparator {
	static private int leaveCount = 0;
	static private int enterCount = 0;
	static private HashMap<Id<Person>, Double> activityEndCount = new HashMap<>();
	static private HashMap<Id<Person>, Double> activityStartCount = new HashMap<>();
	static private HashMap<Id<Person>, Double> travelTimeM = new HashMap<>();
	static private HashMap<Id<Person>, Double> travelTimeMCar = new HashMap<>();
	static private HashMap<Id<Person>, Double> travelTimeMBus = new HashMap<>();
	static private HashMap<Id<Person>, Double> travelTimeMWalk = new HashMap<>();
	static private HashMap<Id<Person>, Double> travelTimeS = new HashMap<>();
	static private HashMap<Id<Person>, Double> travelTimeSCar = new HashMap<>();
	static private HashMap<Id<Person>, Double> travelTimeSBus = new HashMap<>();
	static private HashMap<Id<Person>, Double> travelTimeSWalk = new HashMap<>();
	static private HashMap<Id<Person>, Double> travelTimeDif = new HashMap<>();
	static private HashMap<Id<Person>, Double> travelTimeDifCar = new HashMap<>();
	static private HashMap<Id<Person>, Double> travelTimeDifBus = new HashMap<>();
	static private HashMap<Id<Person>, Double> travelTimeDifWalk = new HashMap<>();

	public static void main(String[] args) {
		String inputFileM = "../../matsim/output/siouxfalls-2014_renamed/ITERS/it.10/10.events.xml.gz";
		String inputFileS = "../../matsim/output/siouxfalls-2014/events.xml";
		// Create an EventsManager instance. This is MATSim infrastructure.
		EventsManager eventsM = new EventsManagerImpl();
		EventsManager eventsS = new EventsManagerImpl();
		// Create an instance of the custom EventHandler which you just wrote.
		// Add it to the EventsManager.
		TravelTimeCalculator handlerM = new TravelTimeCalculator();
		eventsM.addHandler(handlerM);
		TravelTimeCalculator handlerS = new TravelTimeCalculator();
		eventsS.addHandler(handlerS);	
		// Connect a file reader to the EventsManager and read in the event file.
		MatsimEventsReader readerM = new MatsimEventsReader(eventsM);
		readerM.readFile(inputFileM);
		MatsimEventsReader readerS = new MatsimEventsReader(eventsS);
		readerS.readFile(inputFileS);
			
		System.out.println("Events file read!");
		
		travelTimeM = handlerM.getTravelTime();
		travelTimeMCar = handlerM.getTravelTimeCar();
		travelTimeMBus = handlerM.getTravelTimeBus();
		travelTimeMWalk = handlerM.getTravelTimeWalk();
		travelTimeS = handlerS.getTravelTime();
		travelTimeSCar = handlerS.getTravelTimeCar();
		travelTimeSBus = handlerS.getTravelTimeBus();
		travelTimeSWalk = handlerS.getTravelTimeWalk();		
		analysePlans();
	}
	
	//Analyse
	private static void analysePlans() {
		
		int avgTravelTimeDif = 0;
		int avgTravelTimeDifCar = 0;
		int avgTravelTimeDifBus = 0;
		int avgTravelTimeDifWalk = 0;

		//iteration over all agents of SUMO
		for(Id<Person> id : travelTimeS.keySet()) {
				//calculate difference between "SUMO" and "MATSim"
				Double timeDif = travelTimeM.get(id) - travelTimeS.get(id);
				avgTravelTimeDif += timeDif;
				travelTimeDif.put(id, timeDif); //travel-time difference of each agent
		}
		for(Id<Person> id : travelTimeSCar.keySet()) {
			//calculate difference between "SUMO" and "MATSim"
			Double timeDif = travelTimeMCar.get(id) - travelTimeSCar.get(id);
			avgTravelTimeDifCar += timeDif;
			travelTimeDifCar.put(id, timeDif); //travel-time difference of each agent
		}
		for(Id<Person> id : travelTimeSBus.keySet()) {
			//calculate difference between "SUMO" and "MATSim"
			Double timeDif = travelTimeMBus.get(id) - travelTimeSBus.get(id);
			avgTravelTimeDifBus += timeDif;
			travelTimeDifBus.put(id, timeDif); //travel-time difference of each agent
		}
		for(Id<Person> id : travelTimeSWalk.keySet()) {
			//calculate difference between "SUMO" and "MATSim"
			Double timeDif = travelTimeMWalk.get(id) - travelTimeSWalk.get(id);
			avgTravelTimeDifWalk += timeDif;
			travelTimeDifWalk.put(id, timeDif); //travel-time difference of each agent
		}
//		
		//average travel-time difference
		if (travelTimeDif.size() != 0)
			avgTravelTimeDif = Math.round(avgTravelTimeDif/travelTimeDif.size());
		else
			avgTravelTimeDif = 0;
		if (travelTimeDifCar.size() != 0)
			avgTravelTimeDifCar = Math.round(avgTravelTimeDifCar/travelTimeDifCar.size());
		else
			avgTravelTimeDifCar = 0;
		if (travelTimeDifBus.size() != 0)
			avgTravelTimeDifBus = Math.round(avgTravelTimeDifBus/travelTimeDifBus.size());
		else
			avgTravelTimeDifBus = 0;
		if (travelTimeDifWalk.size() != 0)
			avgTravelTimeDifWalk = Math.round(avgTravelTimeDifWalk/travelTimeDifWalk.size());
		else
			avgTravelTimeDifWalk = 0;

//		//gets the 10% of agents with the biggest difference of travel-time on average 
//		ValueSortedMap <Id, Integer> travelTimeDifSorted = new ValueSortedMap<Id, Integer>();	
//		travelTimeDifSorted.putAll(travelTimeDif);	//sorted map (lowest difference to biggest difference)
//		double n = 0.9*travelTimeDifSorted.size();	//iteration number from where the 10% with biggest difference begins
//		int i = 0;
//		for (Id id : travelTimeDifSorted.keySet()){
//			i++;
//			if(i>n) // begin after iteration n (worst 10%)
//				travelTimeDifWorst10.put(id, travelTimeDifSorted.get(id));
//		}

		//output
		System.out.println("Anzahl Agenten: " + travelTimeDif.size() + "\t Reisezeitunterschied jedes Agenten: " + travelTimeDif);
//		System.out.println("Anzahl Agenten Car: " + travelTimeDifCar.size() + "\t Reisezeitunterschied jedes Agenten: " + travelTimeDifCar);
//		System.out.println("Anzahl Agenten Bus: " + travelTimeDifBus.size() + "\t Reisezeitunterschied jedes Agenten: " + travelTimeDifBus);
//		System.out.println("Anzahl Agenten Walk: " + travelTimeDifWalk.size() + "\t Reisezeitunterschied jedes Agenten: " + travelTimeDifWalk);
//		System.out.println("Anzahl Agenten: " + travelTimeDifSorted.size() + "\t Reisezeitunterschied jedes Agenten sortiert: " + travelTimeDifSorted);
//		System.out.println("Anzahl Agenten: " + travelTimeDifWorst10.size() + "\t Reisezeitunterschied der schlechtesten 10 %: " + travelTimeDifWorst10);
		System.out.println("durchschnittlicher Reisezeitunterschied (MATSim - SUMO): " + avgTravelTimeDif);
		System.out.println("durchschnittlicher Reisezeitunterschied Car (MATSim - SUMO): " + avgTravelTimeDifCar);
		System.out.println("durchschnittlicher Reisezeitunterschied Bus (MATSim - SUMO): " + avgTravelTimeDifBus);
		System.out.println("durchschnittlicher Reisezeitunterschied Walk (MATSim - SUMO): " + avgTravelTimeDifWalk);
		
//		String configFileName = "../../matsim/examples/siouxfalls-2014/config_renamed.xml";
//		String populationInput = "../../matsim/output/siouxfalls-2014_renamed/output_plans.xml.gz";
//
//		Config config = ConfigUtils.loadConfig(configFileName);
//		Scenario scenario = ScenarioUtils.createScenario(config);
//		{new MatsimPopulationReader(scenario).readFile(populationInput);}
//		
//		int count = 0;
//		for (Id<Person> id : scenario.getPopulation().getPersons().keySet())
//			if(travelTimeS.containsKey(id) == false){
//				count++;
////				System.out.println(id);
//			}
//		System.out.println(count + " / " +scenario.getPopulation().getPersons().size());
	}

	static class TravelTimeCalculator implements PersonDepartureEventHandler, PersonArrivalEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler{
		private HashMap<Id<Person>, Double> travelTime = new HashMap<>();
		private HashMap<Id<Person>, Double> travelTimeCar = new HashMap<>();
		private HashMap<Id<Person>, Double> travelTimeBus = new HashMap<>();
		private HashMap<Id<Person>, Double> travelTimeWalk = new HashMap<>();
		
		public HashMap<Id<Person>, Double> getTravelTime() {
			return travelTime;
		}
		
		public HashMap<Id<Person>, Double> getTravelTimeCar() {
			return travelTimeCar;
		}
		public HashMap<Id<Person>, Double> getTravelTimeBus() {
			return travelTimeBus;
		}
		public HashMap<Id<Person>, Double> getTravelTimeWalk() {
			return travelTimeWalk;
		}
		
		@Override
		public void reset(int iteration) {
			travelTime.clear();
			travelTimeCar.clear();
			travelTimeBus.clear();
			travelTimeWalk.clear();
		}

		@Override
		public void handleEvent(PersonArrivalEvent event) {
			if(travelTime.containsKey(event.getPersonId())){
				Double travelTimeTemp = travelTime.get(event.getPersonId()) + event.getTime();
				travelTime.put(event.getPersonId(), travelTimeTemp);
				if (event.getLegMode().equals("car") && (event.getPersonId().toString().contains("pt") == false)) //exclude pt drivers
					travelTimeCar.put(event.getPersonId(), travelTimeTemp);
				if (event.getLegMode().equals("pt")) //only legs of pt passengers
					travelTimeBus.put(event.getPersonId(), travelTimeTemp);
				if (event.getLegMode().contains("walk"))
					travelTimeWalk.put(event.getPersonId(), travelTimeTemp);
			}else
				System.out.println(event.getPersonId() + ": first ActivityEndEvent is missing");
		}

		@Override
		public void handleEvent(PersonDepartureEvent event) {
			if (travelTime.containsKey(event.getPersonId())){
				Double travelTimeTemp = travelTime.get(event.getPersonId()) - event.getTime();
				travelTime.put(event.getPersonId(), travelTimeTemp);
				if (event.getLegMode().equals("car") && (event.getPersonId().toString().contains("pt") == false)) //exclude pt drivers
					travelTimeCar.put(event.getPersonId(), travelTimeTemp);
				if (event.getLegMode().equals("pt")) //only legs of pt passengers
					travelTimeBus.put(event.getPersonId(), travelTimeTemp);
				if (event.getLegMode().contains("walk"))
					travelTimeWalk.put(event.getPersonId(), travelTimeTemp);
			}else{
				travelTime.put(event.getPersonId(), event.getTime()*(-1));
				if (event.getLegMode().equals("car") && (event.getPersonId().toString().contains("pt") == false)) //exclude pt drivers
					travelTimeCar.put(event.getPersonId(), event.getTime()*(-1));
				if (event.getLegMode().equals("pt")) //only legs of pt passengers
					travelTimeBus.put(event.getPersonId(), event.getTime()*(-1));
				if (event.getLegMode().contains("walk"))
					travelTimeWalk.put(event.getPersonId(), event.getTime()*(-1));
			}
		}

		@Override
		public void handleEvent(PersonLeavesVehicleEvent event) {
//			if(travelTime.containsKey(event.getDriverId())){
//				Double travelTimeTemp = travelTime.get(event.getDriverId()) + event.getTime();
//				travelTime.put(event.getDriverId(), travelTimeTemp);
//				if (event.getVehicleId().toString().contains("car"))
//					travelTimeCar.put(event.getDriverId(), travelTimeTemp);
//				if (event.getVehicleId().toString().contains("bus"))
//					travelTimeBus.put(event.getDriverId(), travelTimeTemp);
//			}else
//				System.out.println(event.getDriverId() + ": first ActivityEndEvent is missing");
		}

		@Override
		public void handleEvent(PersonEntersVehicleEvent event) {
//			if (travelTime.containsKey(event.getDriverId())){
//				Double travelTimeTemp = travelTime.get(event.getDriverId()) - event.getTime();
//				travelTime.put(event.getDriverId(), travelTimeTemp);
//				if (event.getVehicleId().toString().contains("car"))
//					travelTimeCar.put(event.getDriverId(), travelTimeTemp);
//				if (event.getVehicleId().toString().contains("bus"))
//					travelTimeBus.put(event.getDriverId(), travelTimeTemp);
//			}else{
//				travelTime.put(event.getDriverId(), event.getTime()*(-1));
//				if (event.getVehicleId().toString().contains("car"))
//					travelTimeCar.put(event.getDriverId(), event.getTime()*(-1));
//				if (event.getVehicleId().toString().contains("bus"))
//					travelTimeBus.put(event.getDriverId(), event.getTime()*(-1));
//			}			
		}
	}
}
