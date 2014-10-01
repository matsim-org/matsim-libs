/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,     *
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

package playground.southafrica.freight.digicore.analysis.activity;

import java.io.File;
import java.util.concurrent.Callable;

import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.Counter;
import org.matsim.vehicles.Vehicle;

import playground.southafrica.freight.digicore.containers.DigicoreActivity;
import playground.southafrica.freight.digicore.containers.DigicoreChain;
import playground.southafrica.freight.digicore.containers.DigicoreVehicle;
import playground.southafrica.freight.digicore.io.DigicoreVehicleReader_v1;

public class ActivityWithFacilityIdCallable implements Callable<Tuple<Id<Vehicle>, Double>> {
	final private File vehicleFile; 
	final private Counter counter;
	
	public ActivityWithFacilityIdCallable(File vehicleFile, Counter counter) {
		this.vehicleFile = vehicleFile;
		this.counter = counter;
	}
	
	@Override
	public Tuple<Id<Vehicle>, Double> call() throws Exception {
		/* Read the vehicle file. */
		DigicoreVehicleReader_v1 dvr = new DigicoreVehicleReader_v1();
		dvr.parse(this.vehicleFile.getAbsolutePath());
		DigicoreVehicle vehicle = dvr.getVehicle();
		
		/* Calculate the percentage of activities with a facility Id. */
		int activitiesWithId = 0;
		int activitiesTotal = 0;
		for(DigicoreChain chain : vehicle.getChains()){
			for(DigicoreActivity activity : chain.getAllActivities()){
				if(activity.getFacilityId() != null){
					activitiesWithId++;
				}
				activitiesTotal++;
			}
		}
		this.counter.incCounter();
		
		double percentage = ((double)activitiesWithId) / ((double)activitiesTotal);
		Tuple<Id<Vehicle>, Double> tuple = new Tuple<Id<Vehicle>, Double>(vehicle.getId(), percentage);
		return tuple;
	}

}
