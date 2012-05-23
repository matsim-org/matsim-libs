/* *********************************************************************** *
 * project: org.matsim.*
 * FacilitiesOccupancyCalculator.java
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

package occupancy;

//import java.io.BufferedWriter;
//import java.io.IOException;
import java.util.TreeMap;
import org.matsim.api.core.v01.Id;
//import org.matsim.core.api.experimental.facilities.ActivityFacilities;
//import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
//import org.matsim.core.facilities.ActivityOption;
//import org.matsim.core.gbl.Gbl;
//import org.matsim.core.utils.io.IOUtils;
//import org.matsim.locationchoice.facilityload.FacilityPenalty;

public class FacilitiesOccupancyCalculator implements StartupListener, BeforeMobsimListener, AfterMobsimListener, IterationEndsListener {

	private EventsToFacilityOccupancy eventsToFacilityOccupancy = null;
	final TreeMap<Id, FacilityOccupancy> facilityOccupancies;

	int numberOfTimeBins;
	double scaleNumberOfPersons;
	
	//--------------------------------------------------------------------------------------------------

	public FacilitiesOccupancyCalculator(TreeMap<Id, FacilityOccupancy> facilityOccupancies, int numberOfTimeBins, double scaleNumberOfPersons) {
		this.facilityOccupancies = facilityOccupancies;
		this.numberOfTimeBins = numberOfTimeBins;
		this.scaleNumberOfPersons = scaleNumberOfPersons;
	}


	@Override
	public void notifyStartup(final StartupEvent event) {
		this.eventsToFacilityOccupancy = new EventsToFacilityOccupancy(event.getControler().getFacilities(), this.numberOfTimeBins, this.scaleNumberOfPersons,
				facilityOccupancies, event.getControler().getConfig().locationchoice());
		event.getControler().getEvents().addHandler(this.eventsToFacilityOccupancy);
	}

	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		this.eventsToFacilityOccupancy.reset(event.getIteration());
	}



	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		// TODO Auto-generated method stub
		
	}
	/*
	 * Print daily load of every facility and aggregated hourly load
	 */
//	private void printStatistics(ActivityFacilities facilities, ActivityOption f, String iterationPath, int iteration,
//			TreeMap<Id, FacilityOccupancy> facilityOccupancies) {
//
//		try {
//				final String header="Facility_id\tx\ty\tNumberOfVisitorsPerDay\tAllVisitors\tCapacity\tsumPenaltyFactor\tis shopping facility";
//				final BufferedWriter out =
//					IOUtils.getBufferedWriter(iterationPath+"/"+iteration+".facFrequencies.txt");
//				final BufferedWriter out_summary =
//					IOUtils.getBufferedWriter(iterationPath+"/"+iteration+".facFrequencies_summary.txt");
//
//				out.write(header);
//				out.newLine();
//
//				double capacity = f.getCapacity();
//				double loadPerHourSum[] = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
//
//				for (ActivityFacility facility : facilities.getFacilities().values()) {
//					FacilityOccupancy facilityOccupancy = facilityOccupancies.get(facility.getId());
//					out.write(facility.getId().toString() + "\t"+
//							facility.getCoord().getX() + "\t"+
//							facility.getCoord().getY() + "\t"+
//							facilityOccupancy.getFacilityLoad().getNumberOfVisitorsPerDay() + "\t" +
//							facilityOccupancy.getFacilityLoad().getAllVisitors() + "\t" +
//							facilityOccupancy.getCapacity() + "\t" +
//							facilityOccupancy.getSumCapacityPenaltyFactor() + "\t");
//					if (facility.getActivityOptions().containsKey("shop")) {
//						out.write("shop");
//					}
//					else {
//						out.write("-");
//					}
//
//					out.newLine();
//
//					for (int i = 0; i < 24; i++) {
//						loadPerHourSum[i] += facilityPenalty.getFacilityLoad().getLoadPerHour(i);
//					}
//				}
//				out.flush();
//				out.close();
//
//				out_summary.write("Hour\tLoad");
//				out_summary.newLine();
//				for (int i = 0; i<24; i++) {
//					out_summary.write(i + "\t" + loadPerHourSum[i]);
//					out_summary.newLine();
//					out_summary.flush();
//				}
//				out_summary.close();
//			} catch (final IOException e) {
//				Gbl.errorMsg(e);
//			}
//	}

}