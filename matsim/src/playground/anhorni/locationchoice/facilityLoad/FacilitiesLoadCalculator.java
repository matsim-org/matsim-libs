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
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.Id;
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
	private TreeMap<Id, FacilityPenalty> facilityPenalties = null;
	private final static Logger log = Logger.getLogger(FacilitiesLoadCalculator.class);

	public FacilitiesLoadCalculator(TreeMap<Id, FacilityPenalty> facilityPenalties) {
		this.facilityPenalties = facilityPenalties;
	}
	
	// scales the load of the facilities (for e.g. 10 % runs)
	// assume that only integers can be used to scale a  x% scenario ((100 MOD x == 0) runs e.g. x=10%)
	// TODO: this has to be taken from the config.
	private int scaleNumberOfPersons = 10;

	public void notifyStartup(final StartupEvent event) {
		Controler controler = event.getControler();
		this.eventsToFacilityLoad = new EventsToFacilityLoad(controler.getFacilities(), this.scaleNumberOfPersons,
				this.facilityPenalties);
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
			this.printStatistics(facilities, controler.getIterationPath(), event.getIteration(), 
					this.eventsToFacilityLoad.getFacilityPenalties());
		}	
		this.eventsToFacilityLoad.resetAll(event.getIteration());
	}

	private void printStatistics(Facilities facilities, String iterationPath, int iteration, 
			TreeMap<Id, FacilityPenalty> facilityPenalties) {

		try {
				final String header="Facility_id\tx\ty\tNumberOfVisitorsPerDay\tAllVisitors\tCapacity\tsumPenaltyFactor";
				final BufferedWriter out = IOUtils.getBufferedWriter(iterationPath+"/"+iteration+".facFrequencies.txt");
				final BufferedWriter out_summary = IOUtils.getBufferedWriter(iterationPath+"/"+iteration+".facFrequencies_summary.txt");
	
				out.write(header);
				out.newLine();
							
				double loadPerHourSum[] = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
	
				Iterator<? extends Facility> iter = facilities.getFacilities().values().iterator();
				while (iter.hasNext()){
					Facility facility = iter.next();
					FacilityPenalty facilityPenalty = facilityPenalties.get(facility.getId());
													
					Iterator<Activity> act_it=facility.getActivities().values().iterator();
					while (act_it.hasNext()){
						Activity activity = act_it.next();
						out.write(facility.getId().toString()+"\t"+
								String.valueOf(facility.getCenter().getX())+"\t"+
								String.valueOf(facility.getCenter().getY())+"\t"+
								String.valueOf(facilityPenalty.getFacilityLoad().getNumberOfVisitorsPerDay())+"\t"+
								String.valueOf(facilityPenalty.getFacilityLoad().getAllVisitors())+"\t"+
								String.valueOf(facilityPenalty.getCapacity())+"\t"+
								String.valueOf(facilityPenalty.getSumCapacityPenaltyFactor()));
						out.newLine();
						
						for (int i = 0; i<24; i++) {
							loadPerHourSum[i] += facilityPenalty.getFacilityLoad().getLoadPerHour(i);
						}
						break;
					}
				}
				out.flush();
				out.close();
				
				out_summary.write("Hour\tLoad");
				out_summary.newLine();
				for (int i = 0; i<24; i++) {
					out_summary.write(String.valueOf(i)+"\t"+String.valueOf(loadPerHourSum[i]));
					out_summary.newLine();
					out_summary.flush();
				}
				out_summary.close();		
			} catch (final IOException e) {
				Gbl.errorMsg(e);
			}
	}
}
