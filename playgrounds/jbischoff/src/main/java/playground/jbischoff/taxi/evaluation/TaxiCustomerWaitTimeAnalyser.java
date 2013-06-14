package playground.jbischoff.taxi.evaluation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.PersonEntersVehicleEvent;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;

import playground.michalm.vrp.taxi.TaxiModeDepartureHandler;

public class TaxiCustomerWaitTimeAnalyser implements
		AgentDepartureEventHandler, PersonEntersVehicleEventHandler {

	private Map<Id,Double> taxicalltime;
	private List<Double> totalWaitTime;
	public TaxiCustomerWaitTimeAnalyser() {

			this.taxicalltime = new HashMap<Id,Double>();
			this.totalWaitTime=new ArrayList<Double>();
			}

	@Override
	public void reset(int iteration) {

	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if (!this.taxicalltime.containsKey(event.getPersonId())) return;
		double waitingtime = event.getTime() - this.taxicalltime.get(event.getPersonId()) ;
		this.totalWaitTime.add(waitingtime);
		this.taxicalltime.remove(event.getPersonId());
		
	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		if (!event.getLegMode().equals(TaxiModeDepartureHandler.TAXI_MODE)) return;
		this.taxicalltime.put(event.getPersonId(),event.getTime());
				
	}
	public double calculateAverageWaitTime(){
		double totalWt = 0;
		for (Double d : this.totalWaitTime){
			totalWt = totalWt + d;
		}
		
		double averageWt = totalWt / this.totalWaitTime.size();
		return averageWt;
		
	}
	
	public double returnMaxWaitTime(){
		return Collections.max(this.totalWaitTime);
		
	}
	public double returnMinWaitTime(){
		return Collections.min(this.totalWaitTime);
		
	}


	public void writeCustomerWaitStats(String waitstatsFile){
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(waitstatsFile)));
			bw.write("total taxi trips\taverage wait Time\tMinimum Wait Time\tMaximum Wait Time\n");
			bw.write(this.totalWaitTime.size()+"\t"+Math.round(this.calculateAverageWaitTime())+"\t"+Math.round(this.returnMinWaitTime())+"\t"+Math.round(this.returnMaxWaitTime()));
			bw.flush();
			bw.close();
		}
		 catch (IOException e) {
				System.err.println("Could not create File" + waitstatsFile);
				e.printStackTrace();
			}
		
	}
	
	public void printTaxiCustomerWaitStatistics() {
		System.out.println("Number of Taxi Trips "+ this.totalWaitTime.size());
		System.out.println("Average Waiting Time "+ this.calculateAverageWaitTime());
		System.out.println("Maximum Waiting Time "+ this.returnMaxWaitTime());


	}

}
