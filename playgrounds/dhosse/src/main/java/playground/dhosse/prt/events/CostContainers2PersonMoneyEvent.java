package playground.dhosse.prt.events;

import java.util.*;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.AfterMobsimEvent;


public class CostContainers2PersonMoneyEvent {
	
	private static Logger log = Logger.getLogger(CostContainers2PersonMoneyEvent.class);
	
	private EventsManager events;
	private final double mobsimEndTime;
	private CostContainerHandler handler;
	
	public CostContainers2PersonMoneyEvent(MatsimServices controler, CostContainerHandler handler){
		
		this.events = controler.getEvents();
		this.mobsimEndTime = controler.getConfig().qsim().getEndTime();
		this.handler = handler;
		
	}
	
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		
		double amount = 0.;
		int tasksServed = 0;
		double meterTravelled = 0.;
		double personMetersTravelled = 0.;
		List<Id<Person>> agentsServed = new ArrayList<Id<Person>>();
		
		if(handler.getTaskContainersByVehicleId().size() > 0){
		
			for(CostContainer container : handler.getTaskContainersByVehicleId().values()){
				
				amount += container.getCummulatedCosts();
				tasksServed += container.getTasksServed();
				meterTravelled += container.getMeterTravelled();
				personMetersTravelled += container.getPersonMetersTravelled();
				agentsServed.addAll(container.getAgentsServed());
				
			}
			
			double farePerPassenger = amount / tasksServed;
			double farePerKm = 1000 * amount / personMetersTravelled;
		
			log.info("PRT system served " + tasksServed + " tasks (" + ( (double)( tasksServed / handler.getTaskContainersByVehicleId().size())) + " tasks per vehicle).");
			log.info("Total system costs: " + amount);
			log.info("Fare per passenger: " + farePerPassenger);
			log.info("Fare per kilometer: " + farePerKm);
			
			handler.setCostsPerDay(amount);
			handler.setMetersTravelled(meterTravelled);
			handler.setPersonMetersTravelled(personMetersTravelled);
			handler.setTasksServed(tasksServed);
			handler.setFarePerPassenger(farePerPassenger);
			handler.setFarePerKm(farePerKm);
			
			for(Id<Person> agentId : agentsServed){
				
				this.events.processEvent(new PersonMoneyEvent(this.mobsimEndTime,
						agentId, farePerPassenger));
				
			}
		
		}
		
	}

}
