package org.matsim.contrib.minibus.stats.abtractPAnalysisModules;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.vehicles.Vehicles;

import java.util.HashMap;

interface PAnalysisModule extends TransitDriverStartsEventHandler {

	/**
	 * 
	 * @return The name of that module
	 */
	public abstract String getName();

	/**
	 * 
	 * @param lineIds2ptModeMap Is called at the beginning of each iteration. Contains one public transport mode for each line in the schedule. 
	 */
	public abstract void setLineId2ptModeMap(HashMap<Id<TransitLine>, String> lineIds2ptModeMap);

	/**
	 * 
	 * @return The header of the information collected by the module. The header must not change from one iteration to the next one.
	 */
	public abstract String getHeader();

	/**
	 * 
	 * @return The results collected by the module. Must be in the same order as the header.
	 */
	public abstract String getResult();

	/**
	 * This is called before a new iteration starts. Update everything needed.
	 * 
	 * @param vehicles The vehicles used in the current iteration.
	 */
	public abstract void updateVehicles(Vehicles vehicles);

}