package org.matsim.core.mobsim.jdeqsim.parallel;

import java.util.HashMap;

import org.apache.log4j.Logger;

import org.matsim.core.api.experimental.population.Activity;
import org.matsim.core.events.Events;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.jdeqsim.*;
import org.matsim.core.mobsim.jdeqsim.util.Timer;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.utils.misc.Time;
import org.matsim.core.population.ActivityImpl;

public class PJDEQSim extends JDEQSimulation {

	public PJDEQSim(NetworkLayer network, PopulationImpl population, Events events) {
		super(network, population, events);
		log = Logger.getLogger(JDEQSimulation.class);
	}
	
	public void run(){
		log = Logger.getLogger(JDEQSimulation.class);
		
		Timer t = new Timer();
		t.startTimer();

		Scheduler scheduler = new Scheduler();
		SimulationParameters.setAllRoads(new HashMap<String, Road>());

		

		// find out networkXMedian
		int numberOfLinks=0;
		double sumXCoord=0;
		for (PersonImpl person : this.population.getPersons().values()) {			
			// estimate, where to cut the map
			numberOfLinks++;
			//System.out.println(((Activity) (person.getSelectedPlan().getPlanElements().get(0))).getCoord().getX());
			sumXCoord+= ((Activity) (person.getSelectedPlan().getPlanElements().get(0))).getCoord().getX();
		}
		
		// estimate, where to cut the map
		double networkXMedian= sumXCoord / numberOfLinks;
		
		System.out.println();
		System.out.println("SimulationParameters.networkXMedian:" + networkXMedian);
		System.out.println();

		
		// initialize network
		ExtendedRoad road = null;
		for (LinkImpl link : this.network.getLinks().values()) {
			road = new ExtendedRoad(scheduler, link);
			
			if (link.getCoord().getX()<networkXMedian){
				road.setThreadZoneId(0);
			} else {
				road.setThreadZoneId(1);
			}
			
			SimulationParameters.getAllRoads().put(link.getId().toString(), road);
		}
		
		// define border roads
		// just one layer long
		ExtendedRoad outRoad=null;
		for (LinkImpl link : this.network.getLinks().values()) {
			road = (ExtendedRoad) Road.getRoad(link.getId().toString());
			
			// mark border roads (adjacent to road in different zone)
				for (LinkImpl inLink : road.getLink().getFromNode().getInLinks().values()){
					outRoad=(ExtendedRoad)Road.getRoad(inLink.getId().toString());
					if (road.getThreadZoneId() !=outRoad.getThreadZoneId()){
						road.setBorderZone(true);
						outRoad.setBorderZone(true);
					}
				}
				
			// mark roads, which go away from border roads	
				for (LinkImpl outLink : road.getLink().getToNode().getOutLinks().values()){
					outRoad=(ExtendedRoad)Road.getRoad(outLink.getId().toString());;
					if (road.isBorderZone()){
						outRoad.setBorderZone(true);
					}
				}
		}
		
		
		// initialize vehicles
		Vehicle vehicle = null;
		// the vehicle has registered itself to the scheduler
		for (PersonImpl person : this.population.getPersons().values()) {
			vehicle = new Vehicle(scheduler, person);
		}
		
		
		// just inserted to remove message in bug analysis, that vehicle
		// variable is never read
		vehicle.toString();

		scheduler.startSimulation();

		t.endTimer();
		log.info("Time needed for one iteration (only Parallel JDEQSimulation part): " + t.getMeasuredTime() + "[ms]");
	}

}
