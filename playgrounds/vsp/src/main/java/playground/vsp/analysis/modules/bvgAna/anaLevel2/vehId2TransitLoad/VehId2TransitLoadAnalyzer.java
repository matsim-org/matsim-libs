/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

/**
 * 
 * @author ikaddoura
 * 
 */
package playground.vsp.analysis.modules.bvgAna.anaLevel2.vehId2TransitLoad;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.vehicles.Vehicle;

import playground.vsp.analysis.modules.AbstractAnalyisModule;
import playground.vsp.analysis.modules.bvgAna.anaLevel1.TransitLoadByTimeHandler;

/**
 * 
 * @author ikaddoura, andreas
 *
 */
public class VehId2TransitLoadAnalyzer extends AbstractAnalyisModule{
	private final static Logger log = Logger.getLogger(VehId2TransitLoadAnalyzer.class);
	private ScenarioImpl scenario;
	private TransitLoadByTimeHandler loadHandler;
	private Map<Id, Vehicle> vehiclesMap;
	
	public VehId2TransitLoadAnalyzer(String ptDriverPrefix) {
		super(VehId2TransitLoadAnalyzer.class.getSimpleName(), ptDriverPrefix);
	}
	
	public void init(ScenarioImpl scenario) {
		this.scenario = scenario;
		this.loadHandler = new TransitLoadByTimeHandler();
	}
	
	@Override
	public List<EventHandler> getEventHandler() {
		List<EventHandler> handler = new LinkedList<EventHandler>();
		handler.add(this.loadHandler);		
		return handler;
	}

	@Override
	public void preProcessData() {
		
	}

	@Override
	public void postProcessData() {
		// ...	
	}

	@Override
	public void writeResults(String outputFolder) {
		// ...
	}
	
	/**
	 * @return Returns the load for a given vehicle id and time.
	 */
	public double getVehLoadByTime(Id vehId, double time){
		double occupancy = this.loadHandler.getVehicleLoad(vehId, time);
		double capacity = this.vehiclesMap.get(vehId).getType().getCapacity().getSeats().intValue()
						+ this.vehiclesMap.get(vehId).getType().getCapacity().getStandingRoom().intValue();
		double load = occupancy / capacity;
		this.log.debug("Occupancy " + occupancy + ", Capacity " + capacity + ", Load " + load);
		
		if(load > 1.0){
			this.log.warn("Load for vehicle " + vehId + " at " + time + " > 1. Better check this");
		}
		return load;
	}

}
