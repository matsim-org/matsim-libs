package playground.tschlenther.analysis.modules.taxiTrips;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.population.Person;

public class TaxiCustomerWaitHandler implements PersonDepartureEventHandler, PersonEntersVehicleEventHandler {

	private int numberOfTrips;
	private double totalWaitingTime;
	
	private Map<Id<Person>, Double> personsTaxiCallTime;

	
		public TaxiCustomerWaitHandler() {
			this.numberOfTrips = 0;
			this.totalWaitingTime = 0.0;
			this.personsTaxiCallTime = new HashMap<Id<Person>, Double>();
		}
	
	    @Override
	    public void reset(int iteration){
	    }
	    
	    @Override
	    public void handleEvent(PersonDepartureEvent event){
	        if (!event.getLegMode().equals(/*TaxiUtils.TAXI_MODE*/ "taxi"))
	            return;
	        this.personsTaxiCallTime.put(event.getPersonId(), event.getTime());
	    }

	    @Override
	    public void handleEvent(PersonEntersVehicleEvent event){
	        if (!this.personsTaxiCallTime.containsKey(event.getPersonId()))
	            return;
	        double waitingTime = event.getTime() - this.personsTaxiCallTime.get(event.getPersonId());
	        this.totalWaitingTime += waitingTime;
	        this.numberOfTrips += 1;
	        this.personsTaxiCallTime.remove(event.getPersonId());
	    }

	    public double getAvgWaitingTime(){
	    	return (this.totalWaitingTime)/(this.numberOfTrips);
	    }
}
