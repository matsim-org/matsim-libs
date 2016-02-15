package playground.tschlenther.analysis.modules.taxiTrips;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.taxi.TaxiUtils;

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
	        if (!event.getLegMode().equals(TaxiUtils.TAXI_MODE))
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
	    
	    public void writeCustomerStats(String fileDir){
	    	try {
	            BufferedWriter bw = new BufferedWriter(new FileWriter(new File(fileDir)));
	            bw.write("total number of customer's taxi trips: \t" + this.numberOfTrips);
	            bw.newLine();
	            bw.write("total waiting time (h) : \t" + (this.totalWaitingTime / 3600));
	            bw.newLine();
	            bw.write("average waiting time per trip (s) : \t " + getAvgWaitingTime());
	            bw.flush();
	            bw.close();
	        }
	        catch (IOException e) {
	            System.err.println("Could not create File" + fileDir);
	            e.printStackTrace();
	        }
	    }
	    
}
