/* *********************************************************************** *
 * project: org.matsim.*
 * FacilitiesLoadCalculator.java.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.contrib.locationchoice.facilityload;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.locationchoice.DestinationChoiceConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.TreeMap;

/**
 * Prints statistics of facility load
 *
 * @author anhorni
 */
public class FacilitiesLoadCalculator implements StartupListener, BeforeMobsimListener, AfterMobsimListener, IterationEndsListener {

	private EventsToFacilityLoad eventsToFacilityLoad = null;
	private final TreeMap<Id, FacilityPenalty> facilityPenalties;

	//--------------------------------------------------------------------------------------------------

	public FacilitiesLoadCalculator(TreeMap<Id, FacilityPenalty> facilityPenalties) {
		this.facilityPenalties = facilityPenalties;
	}


	@Override
	public void notifyStartup(final StartupEvent event) {
		Controler controler = event.getControler();
		/*
		 * Scales the load of the facilities (for e.g. 10 % runs), assuming that only integers
		 * can be used to scale a  x% scenario ((100 MOD x == 0) runs e.g. x=10%)
		 */
		double scaleNumberOfPersons = Double.parseDouble(controler.getConfig().findParam("locationchoice", "scaleFactor"));
        this.eventsToFacilityLoad = new EventsToFacilityLoad(
        		controler.getScenario().getActivityFacilities(), 
        		scaleNumberOfPersons,
				this.facilityPenalties, 
				((DestinationChoiceConfigGroup)controler.getConfig().getModule("locationchoice"))
				);
		event.getControler().getEvents().addHandler(this.eventsToFacilityLoad);
	}

	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		this.eventsToFacilityLoad.resetAll(event.getIteration());
	}

	@Override
	public void notifyAfterMobsim(final AfterMobsimEvent event) {
		this.eventsToFacilityLoad.finish();
	}

	/*
	 * At the end of an iteration the statistics of the facility load are printed and
	 * the load values are set to zero afterwards.
	 */
	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		Controler controler = event.getControler();
        ActivityFacilities facilities = controler.getScenario().getActivityFacilities();

		if (event.getIteration() % 2 == 0) {
			this.printStatistics(facilities, event.getControler().getControlerIO().getIterationPath(event.getIteration()), event.getIteration(),
					this.eventsToFacilityLoad.getFacilityPenalties());
		}
	}

	/*
	 * Print daily load of every facility and aggregated hourly load
	 */
	private void printStatistics(ActivityFacilities facilities, String iterationPath, int iteration,
			TreeMap<Id, FacilityPenalty> facilityPenalties) {

		try {
				final String header="Facility_id\tx\ty\tNumberOfVisitorsPerDay\tAllVisitors\tCapacity\tsumPenaltyFactor\tis shopping facility";
				final BufferedWriter out =
					IOUtils.getBufferedWriter(iterationPath+"/"+iteration+".facFrequencies.txt");
				final BufferedWriter out_summary =
					IOUtils.getBufferedWriter(iterationPath+"/"+iteration+".facFrequencies_summary.txt");

				out.write(header);
				out.newLine();

				double loadPerHourSum[] = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};

				for (ActivityFacility facility : facilities.getFacilities().values()) {
					FacilityPenalty facilityPenalty = facilityPenalties.get(facility.getId());
					out.write(facility.getId().toString() + "\t"+
							facility.getCoord().getX() + "\t"+
							facility.getCoord().getY() + "\t"+
							facilityPenalty.getFacilityLoad().getNumberOfVisitorsPerDay() + "\t" +
							facilityPenalty.getFacilityLoad().getAllVisitors() + "\t" +
							facilityPenalty.getCapacity() + "\t" +
							facilityPenalty.getSumCapacityPenaltyFactor() + "\t");
					if (facility.getActivityOptions().containsKey("shop")) {
						out.write("shop");
					}
					else {
						out.write("-");
					}

					out.newLine();

					for (int i = 0; i < 24; i++) {
						loadPerHourSum[i] += facilityPenalty.getFacilityLoad().getLoadPerHour(i);
					}
				}
				out.flush();
				out.close();

				out_summary.write("Hour\tLoad");
				out_summary.newLine();
				for (int i = 0; i<24; i++) {
					out_summary.write(i + "\t" + loadPerHourSum[i]);
					out_summary.newLine();
					out_summary.flush();
				}
				out_summary.close();
			} catch (final IOException e) {
				throw new RuntimeException(e);
			}
	}

}
