package saleem.ptoptimisation.optimisationintegration;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicles;

import saleem.ptoptimisation.utils.ScenarioHelper;
import floetteroed.opdyts.DecisionVariable;
import floetteroed.opdyts.SimulatorState;
/**
 * A class representing a variation of the transit schedule and vehicles, and also keeping track of the orignal pre-changed transit schedule and vehicles.
 * 
 * @author Mohammad Saleem
 * 
 */
public class PTSchedule implements DecisionVariable{
	private TransitSchedule schedule;
	private Vehicles vehicles;
	private TransitSchedule preschedule;
	private Vehicles prevehicles;
	private Scenario scenario;
	static int iteration=0;
	public PTSchedule(Scenario scenario, TransitSchedule schedule, Vehicles vehicles) {
		ScenarioHelper helper = new ScenarioHelper();
		this.schedule = schedule;
		this.vehicles=vehicles;
		this.scenario=scenario;
		this.preschedule=helper.deepCopyTransitSchedule(scenario.getTransitSchedule());//Pre-changed transit schedule
		this.prevehicles=helper.deepCopyVehicles(scenario.getTransitVehicles());
		this.iteration=0;
				
	}
	public TransitSchedule getPreSchedule(){
		return this.preschedule;
	}
	public Vehicles getPreVehicles(){
		return this.prevehicles;
	}
	public TransitSchedule getSchedule(){
		return this.schedule;
	}
	public Vehicles getVehicles(){
		return this.vehicles;
	}
	/*
	 * The implementInSimulation function updates the transit schedule and vehicles objects associated with the current main scenario. 
	 * Empty the transit schedule and vehicles objects by removing all existing vehicle types, vehicles, stop facilities and transit lines, 
	 * and add back from an updated vehicles object and transit schedule object into the transit schedule and vehicles object associated with the scenario.
	 * The updated vehicles object and transit schedule object are the ones that are produced by the PTScheduleRandomiser by adding and deleting 
	 * vehicles and departures to the selected transit schedule.
	*/
	@Override
	public void implementInSimulation() {
		ScenarioHelper helper = new ScenarioHelper();
		TransitSchedule copiedschedule = helper.deepCopyTransitSchedule(schedule);
		Vehicles copiedvehicles = helper.deepCopyVehicles(vehicles);
		helper.writePlausibilityStatistics(scenario, iteration);
		if(this.iteration>0){
			helper.removeEntireScheduleAndVehicles(scenario);//Removes all vehicle types, vehicles, stop facilities and transit lines from a transit schedule
			helper.addVehicles(scenario, copiedvehicles);//Adds all vehicle types and vehicles from an updated stand alone vehicles object into the current scenario vehicles object
			helper.addTransitSchedule(scenario, copiedschedule);//Add all stop facilities and transit lines from a stand alone updated transit schedule into the current scenario transit schedule
		}
		this.iteration++;
		
	
	}

}
