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

package playground.anhorni.locationchoice.facilityLoad;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.matsim.controler.Controler;
import org.matsim.controler.events.AfterMobsimEvent;
import org.matsim.controler.events.IterationEndsEvent;
import org.matsim.controler.events.StartupEvent;
import org.matsim.controler.listener.AfterMobsimListener;
import org.matsim.controler.listener.IterationEndsListener;
import org.matsim.controler.listener.StartupListener;
import org.matsim.facilities.Activity;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.utils.io.IOUtils;
import org.matsim.world.algorithms.WorldBottom2TopCompletion;
import org.matsim.world.algorithms.WorldCheck;
import org.matsim.world.algorithms.WorldValidation;

/**
 *  Basically it integrates the
 * {@link org.matsim.scoring.EventsToFacilityLoad} with the
 * {@link org.matsim.controler.Controler}.
 *
 * @author anhorni
 */
public class FacilitiesLoadCalculator implements StartupListener, AfterMobsimListener, IterationEndsListener {

	private EventsToFacilityLoad eventsToFacilityLoad;

	private final static Logger log = Logger.getLogger(FacilitiesLoadCalculator.class);

	// scales the load of the facilities (for e.g. 10 % runs)
	// assume that only integers can be used to scale a  x% scenario ((100 MOD x == 0) runs e.g. x=10%)
	// TODO: this has to be taken from the config.
	private int scaleNumberOfPersons=10;

	public void notifyStartup(final StartupEvent event) {
		Controler controler = event.getControler();
		this.eventsToFacilityLoad = new EventsToFacilityLoad(controler.getFacilities(), this.scaleNumberOfPersons);
		event.getControler().getEvents().addHandler(this.eventsToFacilityLoad);

		// correctly initalize the world.
		//TODO: Move this to the controler
		controler.getWorld().complete();
		new WorldCheck().run(controler.getWorld());
		new WorldBottom2TopCompletion().run(controler.getWorld());
		new WorldValidation().run(controler.getWorld());
		new WorldCheck().run(controler.getWorld());
		log.info("world checking done.");
	}

	public void notifyAfterMobsim(final AfterMobsimEvent event) {	
		this.eventsToFacilityLoad.finish();		
	}
	
	public void notifyIterationEnds(IterationEndsEvent event) {
		Controler controler = event.getControler();
		Facilities facilities = controler.getFacilities();
		
		if (event.getIteration() % 10 == 0) {
			this.printStatistics(facilities, controler.getIterationPath(), event.getIteration());
		}	
		this.eventsToFacilityLoad.resetAll(event.getIteration());
	}

	private void printStatistics(Facilities facilities, String iterationPath, int iteration) {

		try {
			final String header="Facility_id\tx\ty\tNumberOfVisitorsPerDay\tCapacity\tAttrFactor\tsumPenaltyFactor";
			final BufferedWriter out_shop = IOUtils.getBufferedWriter(iterationPath+"/"+iteration+".facFrequencies_shop.txt");
			final BufferedWriter out_leisure = IOUtils.getBufferedWriter(iterationPath+"/"+iteration+".facFrequencies_leisure.txt");
			final BufferedWriter out_shop_summary = IOUtils.getBufferedWriter(iterationPath+"/"+iteration+".facFrequencies_summary.txt");

			out_shop.write(header);
			out_shop.newLine();
			
			out_leisure.write(header);
			out_leisure.newLine();
			
			double loadPerHourSum[] = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};

			Iterator<? extends Facility> iter = facilities.getFacilities().values().iterator();
			while (iter.hasNext()){
				Facility facility = iter.next();
							
				Iterator<Activity> act_it=facility.getActivities().values().iterator();
				while (act_it.hasNext()){
					Activity activity = act_it.next();
					// check if this is a shopping facility
					if (activity.getType().startsWith("s")) {
						out_shop.write(facility.getId().toString()+"\t"+
							String.valueOf(facility.getCenter().getX())+"\t"+
							String.valueOf(facility.getCenter().getY())+"\t"+
							String.valueOf(facility.getNumberOfVisitorsPerDay())+"\t"+
							String.valueOf(facility.getDailyCapacity())+"\t"+
							String.valueOf(facility.getAttrFactor()+"\t"+
							String.valueOf(facility.getSumCapacityPenaltyFactor())));
						out_shop.newLine();	
						
						for (int i = 0; i<24; i++) {
							loadPerHourSum[i] += facility.getLoadPerHour(i);
						}
						
						break;
					}
					//or a leisure facility
					if (activity.getType().startsWith("l")) {
						out_leisure.write(facility.getId().toString()+"\t"+
							String.valueOf(facility.getCenter().getX())+"\t"+
							String.valueOf(facility.getCenter().getY())+"\t"+
							String.valueOf(facility.getNumberOfVisitorsPerDay())+"\t"+
							String.valueOf(facility.getDailyCapacity())+"\t"+
							String.valueOf(1.0)+"\t"+
							String.valueOf(0.0));						
						out_leisure.newLine();							
						break;
					}
				}
			}
			out_shop.flush();
			out_shop.close();
			
			out_leisure.flush();
			out_leisure.close();
			
			out_shop_summary.write("Hour\tLoad");
			for (int i = 0; i<24; i++) {
				out_shop_summary.write(String.valueOf(i)+"\t"+String.valueOf(loadPerHourSum[i]));
				out_leisure.newLine();
				out_shop_summary.flush();
			}
			out_shop_summary.close();
			
		}
		catch (final IOException e) {
			Gbl.errorMsg(e);
		}
	}
}
