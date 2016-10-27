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

package playground.dziemke.cemdapMatsimCadyts.mmoyo.analysis.stopZoneOccupancyAnalysis;

		import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.cadyts.pt.CadytsPtOccupancyAnalyzerI;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.api.experimental.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.matsim.core.utils.misc.Time;
import org.matsim.counts.Counts;
import org.matsim.pt.counts.OccupancyAnalyzer;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.Collection;
import java.util.Set;

class ConfigurableOccupancyAnalyzer implements 	PersonEntersVehicleEventHandler,
		PersonLeavesVehicleEventHandler,
		VehicleArrivesAtFacilityEventHandler,
		VehicleDepartsAtFacilityEventHandler,
		TransitDriverStartsEventHandler , CadytsPtOccupancyAnalyzerI{

	private static final Logger log = Logger.getLogger(ConfigurableOccupancyAnalyzer.class);
	private OccupancyAnalyzer delegOccuAnalyzer;
	private int maxTime = (int)Time.MIDNIGHT-1;
	private final Set<Id<TransitLine>> calibratedLines;
	private boolean stopZoneConversion;

	/**
	 * Pt-occupancy analysis is configurable with StopZone conversion,  selected lines and time bin size.
	 */

	public ConfigurableOccupancyAnalyzer(Set<Id<TransitLine>> calibratedLines, int timeBinSize_s) {
		this.calibratedLines = calibratedLines;
		delegOccuAnalyzer = new OccupancyAnalyzer(timeBinSize_s, 	maxTime);

		log.info("time bin size set to: " + timeBinSize_s);
		if (this.calibratedLines.size()==0){
			log.warn("number of calibrated lines= 0!");
		}
	}

	protected void setStopZoneConversion(boolean stopZoneConversion){
		this.stopZoneConversion = stopZoneConversion;
	}

	@Override
	public void handleEvent(final VehicleDepartsAtFacilityEvent event) {
		if (stopZoneConversion){
			Id stopId =FacilityUtils.convertFacilitytoZoneId(event.getFacilityId());
			VehicleDepartsAtFacilityEvent localEvent = new VehicleDepartsAtFacilityEvent(event.getTime(), event.getVehicleId(), stopId, event.getDelay());
			delegOccuAnalyzer.handleEvent(localEvent);
			localEvent = null;  //try this for optimization
		}else{
			delegOccuAnalyzer.handleEvent(event);
		}

	}

	@Override
	public void handleEvent(final TransitDriverStartsEvent event) {
		if (this.calibratedLines.contains(event.getTransitLineId())) {
			delegOccuAnalyzer.handleEvent(event);
		}
	}

	protected OccupancyAnalyzer getOccuAnalyzer(){
		return delegOccuAnalyzer;
	}

	// the rest of the event handling is delegated
	@Override
	public void reset(int iteration) {delegOccuAnalyzer.reset(iteration);	}

	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {delegOccuAnalyzer.handleEvent(event);}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {delegOccuAnalyzer.handleEvent(event);}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {delegOccuAnalyzer.handleEvent(event);}

	@Override
	public int getOccupancyVolumeForStopAndTime(Id<TransitStopFacility> stopId, int time_s) {
		return this.delegOccuAnalyzer.getOccupancyVolumesForStop(stopId)[this.delegOccuAnalyzer.getTimeSlotIndex(time_s)] ;
	}

	@Override
	public void writeResultsForSelectedStopIds(String filename, Counts<Link> occupCounts,
			Collection<Id<TransitStopFacility>> stopIds) {
		log.warn("not implemented");
	}

	@Override
	public int[] getOccupancyVolumesForStop(Id<TransitStopFacility> stopId) {
		return this.getOccupancyVolumesForStop(stopId) ;
	}

}