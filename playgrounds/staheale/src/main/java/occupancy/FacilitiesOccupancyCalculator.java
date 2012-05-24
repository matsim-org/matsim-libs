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

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.TreeMap;
import org.matsim.api.core.v01.Id;
//import org.matsim.core.api.experimental.facilities.ActivityFacilities;
//import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.io.IOUtils;

public class FacilitiesOccupancyCalculator implements StartupListener, BeforeMobsimListener, AfterMobsimListener, IterationEndsListener {

	private EventsToFacilityOccupancy eventsToFacilityOccupancy = null;
	final TreeMap<Id, FacilityOccupancy> facilityOccupancies;

	int numberOfTimeBins;
	double scaleNumberOfPersons;
	
	private FacilityOccupancy facilityOccupancy;

	
	//--------------------------------------------------------------------------------------------------

	public FacilitiesOccupancyCalculator(TreeMap<Id, FacilityOccupancy> facilityOccupancies, int numberOfTimeBins, double scaleNumberOfPersons) {
		this.facilityOccupancies = facilityOccupancies;
		this.numberOfTimeBins = numberOfTimeBins;
		this.scaleNumberOfPersons = scaleNumberOfPersons;
		this.facilityOccupancy = new FacilityOccupancy(this.numberOfTimeBins, this.scaleNumberOfPersons);

	}

	private int calculateMeanOccupancy(int startTimeBinIndex, int endTimeBinIndex) {

		int [] facilityOccupancy = this.facilityOccupancy.getOccupancy();
		int meanOccupancy = 0;

		for (int i=startTimeBinIndex; i<endTimeBinIndex+1; i++) {
			
			meanOccupancy += (facilityOccupancy[i]);
		}
		meanOccupancy /= (endTimeBinIndex-startTimeBinIndex+1);
		return Math.round(meanOccupancy);
	}
	
	public double getMeanOccupancy(double startTime, double endTime) {

		if (startTime>24.0*3600.0 && endTime>24.0*3600.0) {
			return 0;
		}
		else if (endTime>24.0*3600.0) {
			endTime=24.0*3600.0;
		}
		int startTimeBinIndex = this.facilityOccupancy.timeBinIndex(startTime);
		int endTimeBinIndex = this.facilityOccupancy.timeBinIndex(endTime);
		return calculateMeanOccupancy(startTimeBinIndex, endTimeBinIndex);
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
		
	}


	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		Controler controler = event.getControler();
		ActivityFacilities facilities = controler.getFacilities();
		
		if (event.getIteration() % 10 == 0) {
			this.printStatistics(facilities, event.getControler().getControlerIO().getIterationPath(event.getControler().getIterationNumber()), event.getIteration(),
					facilityOccupancies);
		}
		
	}
	/*
	 * Print daily occupancy of every facility and aggregated hourly occupancy
	 */
	private void printStatistics(ActivityFacilities facilities, String iterationPath, int iteration,
			TreeMap<Id, FacilityOccupancy> facilityOccupancies) {

		try {
				final String header="Facility_id\tx\ty\tNumberOfVisitorsPerDay\tAllVisitors\tOccupancy\tis shopping retail facility\tis shopping service facility\tis sports fun facility\tis gastro culture facility";
				final BufferedWriter out =
					IOUtils.getBufferedWriter(iterationPath+"/"+iteration+".facFrequencies.txt");
				final BufferedWriter out_summary =
					IOUtils.getBufferedWriter(iterationPath+"/"+iteration+".facFrequencies_summary.txt");

				out.write(header);
				out.newLine();

				double occupancyPerHourSum[] = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};

				for (ActivityFacility facility : facilities.getFacilities().values()) {
					FacilityOccupancy facilityOccupancy = facilityOccupancies.get(facility.getId());
					out.write(facility.getId().toString() + "\t"+
							facility.getCoord().getX() + "\t"+
							facility.getCoord().getY() + "\t"+
							facilityOccupancy.getNumberOfVisitorsPerDay() + "\t" +
							facilityOccupancy.getAllVisitors() + "\t" +
							facilityOccupancy.getOccupancy() + "\t");
					if (facility.getActivityOptions().containsKey("shop_retail")) {
						out.write("shop_retail");
					}
					else {
						out.write("-");
					}
					if (facility.getActivityOptions().containsKey("shop_service")) {
						out.write("shop_service");
					}
					else {
						out.write("-");
					}
					if (facility.getActivityOptions().containsKey("sports_fun")) {
						out.write("sports_fun");
					}
					else {
						out.write("-");
					}
					if (facility.getActivityOptions().containsKey("gastro_culture")) {
						out.write("gastro_culture");
					}
					else {
						out.write("-");
					}

					out.newLine();

					for (int i = 0; i < 24; i++) {
						occupancyPerHourSum[i] += facilityOccupancy.getOccupancyPerHour(i);
					}
				}
				out.flush();
				out.close();

				out_summary.write("Hour\tOccupancy");
				out_summary.newLine();
				for (int i = 0; i<24; i++) {
					out_summary.write(i + "\t" + occupancyPerHourSum[i]);
					out_summary.newLine();
					out_summary.flush();
				}
				out_summary.close();
			} catch (final IOException e) {
				Gbl.errorMsg(e);
			}
	}

}