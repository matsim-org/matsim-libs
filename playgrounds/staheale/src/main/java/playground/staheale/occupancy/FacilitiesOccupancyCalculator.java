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

package playground.staheale.occupancy;

import org.matsim.api.core.v01.Id;
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
import org.matsim.contrib.locationchoice.DestinationChoiceConfigGroup;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.TreeMap;
//import org.matsim.core.api.experimental.facilities.ActivityFacilities;
//import org.matsim.core.api.experimental.facilities.ActivityFacility;

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
        this.eventsToFacilityOccupancy = new EventsToFacilityOccupancy(
        		event.getControler().getScenario().getActivityFacilities(), 
        		this.numberOfTimeBins, this.scaleNumberOfPersons,
				facilityOccupancies, 
				((DestinationChoiceConfigGroup)event.getControler().getConfig().getModule("locationchoice"))
				);
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
        ActivityFacilities facilities = controler.getScenario().getActivityFacilities();

		if (event.getIteration() % 10 == 0) {
			this.printStatistics(facilities, event.getControler().getControlerIO().getIterationPath(event.getIteration()), event.getIteration(),
					facilityOccupancies);
		}

	}
	/*
	 * Print daily occupancy of every facility and aggregated hourly occupancy
	 */
	private void printStatistics(ActivityFacilities facilities, String iterationPath, int iteration,
			TreeMap<Id, FacilityOccupancy> facilityOccupancies) {

		try {
			final String header="Facility_id\tx\ty\tVisitorsPerDay\tAllVisitors\tis shopping retail facility\tis shopping service facility\tis leisure_sports fun facility\tis leisure_gastro culture facility";
			final BufferedWriter out =
					IOUtils.getBufferedWriter(iterationPath+"/"+iteration+".facFrequencies.txt");
			final BufferedWriter out_summary =
					IOUtils.getBufferedWriter(iterationPath+"/"+iteration+".facFrequencies_summary.txt");

			out.write(header);
			out.newLine();

			//				double occupancyPerHourSum[] = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};

			for (ActivityFacility facility : facilities.getFacilities().values()) {
				if (facility.getActivityOptions().containsKey("shop_retail") || facility.getActivityOptions().containsKey("shop_service")
						|| facility.getActivityOptions().containsKey("leisure_sports_fun") || facility.getActivityOptions().containsKey("leisure_gastro_culture")) {
					FacilityOccupancy facilityOccupancy = facilityOccupancies.get(facility.getId());
					out.write(facility.getId().toString() + "\t"+
							facility.getCoord().getX() + "\t"+
							facility.getCoord().getY() + "\t"+
							facilityOccupancy.getNumberOfVisitorsPerDay() + "\t" +
							facilityOccupancy.getAllVisitors()
							+ "\t");
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
					if (facility.getActivityOptions().containsKey("leisure_sports_fun")) {
						out.write("leisure_sports_fun");
					}
					else {
						out.write("-");
					}
					if (facility.getActivityOptions().containsKey("leisure_gastro_culture")) {
						out.write("leisure_gastro_culture");
					}
					else {
						out.write("-");
					}

					out.newLine();

					//						for (int i = 0; i < 24; i++) {
					//						occupancyPerHourSum[i] += facilityOccupancy.getOccupancyPerHour(i);
					//						
					//						}
				}
			}
			out.flush();
			out.close();

			out_summary.write("Facility_id\t0\t1\t2\t3\t4\t5\t6\t7\t8\t9\t10\t11\t12\t13\t14\t15\t16\t17\t18\t19\t20\t21\t22\t23\tshop_retail\tshop_service\tleisure_sports_fun\tleisure_gastro_culture");
			out_summary.newLine();
			for (ActivityFacility facility : facilities.getFacilities().values()) {
				if (facility.getActivityOptions().containsKey("shop_retail") || facility.getActivityOptions().containsKey("shop_service")
						|| facility.getActivityOptions().containsKey("leisure_sports_fun") || facility.getActivityOptions().containsKey("leisure_gastro_culture")) {
					FacilityOccupancy facilityOccupancy = facilityOccupancies.get(facility.getId());
					double capacity = 1.0;
					if (facility.getActivityOptions().get("shop_retail")!=null) {capacity = facility.getActivityOptions().get("shop_retail").getCapacity();}
					if (facility.getActivityOptions().get("shop_service")!=null) {capacity = facility.getActivityOptions().get("shop_service").getCapacity();}
					if (facility.getActivityOptions().get("leisure_sports_fun")!=null) {capacity= facility.getActivityOptions().get("leisure_sports_fun").getCapacity();}
					if (facility.getActivityOptions().get("leisure_gastro_culture")!=null) {capacity= facility.getActivityOptions().get("leisure_gastro_culture").getCapacity();}

					out_summary.write(facility.getId().toString() + "\t"+
							(double)Math.round(facilityOccupancy.getCurrentOccupancy(0)/capacity*1000)/1000 + "\t"+
							(double)Math.round(facilityOccupancy.getCurrentOccupancy(4)/capacity*1000)/1000 + "\t"+
							(double)Math.round(facilityOccupancy.getCurrentOccupancy(8)/capacity*1000)/1000 + "\t"+
							(double)Math.round(facilityOccupancy.getCurrentOccupancy(12)/capacity*1000)/1000 + "\t"+
							(double)Math.round(facilityOccupancy.getCurrentOccupancy(16)/capacity*1000)/1000 + "\t"+
							(double)Math.round(facilityOccupancy.getCurrentOccupancy(20)/capacity*1000)/1000 + "\t"+
							(double)Math.round(facilityOccupancy.getCurrentOccupancy(24)/capacity*1000)/1000 + "\t"+
							(double)Math.round(facilityOccupancy.getCurrentOccupancy(28)/capacity*1000)/1000 + "\t"+
							(double)Math.round(facilityOccupancy.getCurrentOccupancy(32)/capacity*1000)/1000 + "\t"+
							(double)Math.round(facilityOccupancy.getCurrentOccupancy(36)/capacity*1000)/1000 + "\t"+
							(double)Math.round(facilityOccupancy.getCurrentOccupancy(40)/capacity*1000)/1000 + "\t"+
							(double)Math.round(facilityOccupancy.getCurrentOccupancy(44)/capacity*1000)/1000 + "\t"+
							(double)Math.round(facilityOccupancy.getCurrentOccupancy(48)/capacity*1000)/1000 + "\t"+
							(double)Math.round(facilityOccupancy.getCurrentOccupancy(52)/capacity*1000)/1000 + "\t"+
							(double)Math.round(facilityOccupancy.getCurrentOccupancy(56)/capacity*1000)/1000 + "\t"+
							(double)Math.round(facilityOccupancy.getCurrentOccupancy(60)/capacity*1000)/1000 + "\t"+
							(double)Math.round(facilityOccupancy.getCurrentOccupancy(64)/capacity*1000)/1000 + "\t"+
							(double)Math.round(facilityOccupancy.getCurrentOccupancy(68)/capacity*1000)/1000 + "\t"+
							(double)Math.round(facilityOccupancy.getCurrentOccupancy(72)/capacity*1000)/1000 + "\t"+
							(double)Math.round(facilityOccupancy.getCurrentOccupancy(76)/capacity*1000)/1000 + "\t"+
							(double)Math.round(facilityOccupancy.getCurrentOccupancy(80)/capacity*1000)/1000 + "\t"+
							(double)Math.round(facilityOccupancy.getCurrentOccupancy(84)/capacity*1000)/1000 + "\t"+
							(double)Math.round(facilityOccupancy.getCurrentOccupancy(88)/capacity*1000)/1000 + "\t"+
							(double)Math.round(facilityOccupancy.getCurrentOccupancy(92)/capacity*1000)/1000 + "\t"
							+ "\t");

					if (facility.getActivityOptions().containsKey("shop_retail")) {
						out_summary.write("1\t");
					}
					else {
						out_summary.write("0\t");
					}
					if (facility.getActivityOptions().containsKey("shop_service")) {
						out_summary.write("1\t");
					}
					else {
						out_summary.write("0\t");
					}
					if (facility.getActivityOptions().containsKey("leisure_sports_fun")) {
						out_summary.write("1\t");
					}
					else {
						out_summary.write("0\t");
					}
					if (facility.getActivityOptions().containsKey("leisure_gastro_culture")) {
						out_summary.write("1\t");
					}
					else {
						out_summary.write("0\t");
					}
					out_summary.newLine();
				}
			}

			//				for (int i = 0; i<24; i++) {
			//					out_summary.write(i + "\t" + occupancyPerHourSum[i]);
			out_summary.flush();
			//				}
			out_summary.close();
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

}