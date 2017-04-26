/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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
 */
package playground.jbischoff.taxi.inclusion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.data.VehicleImpl;
import org.matsim.contrib.dvrp.data.FleetImpl;
import org.matsim.contrib.dvrp.data.file.VehicleReader;
import org.matsim.contrib.dvrp.data.file.VehicleWriter;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class CreateInclusionVehicles {
	private static final String DIR = "C:/Users/Joschka/Documents/shared-svn/projects/sustainability-w-michal-and-dlr/data/scenarios/inclusion/";

	public static void main(String[] args) {
		new CreateInclusionVehicles().run();
	}
	private void run(){
	    FleetImpl data = new FleetImpl();
		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(DIR+"berlin_brb.xml.gz"); 
		new VehicleReader(network,data).readFile(DIR+"orig_supply/taxis4to4_EV0.0.xml");
		Random random = MatsimRandom.getRandom();
		for (int i = 50; i<=1000; i=i+50 ){
			ArrayList<Vehicle> allVehicles = new ArrayList<>();
			ArrayList<Vehicle> newVehicles = new ArrayList<>();
			allVehicles.addAll(data.getVehicles().values());
			Collections.shuffle(allVehicles,random);
			for (int z = 0; z<i;z++){
				
				Vehicle v = allVehicles.remove(z);
				Vehicle nv = new VehicleImpl(Id.create("hc_"+v.getId().toString(), Vehicle.class), v.getStartLink(), v.getCapacity(), v.getServiceBeginTime(), v.getServiceEndTime());
				newVehicles.add(nv);
			}
			newVehicles.addAll(allVehicles);
			new VehicleWriter(newVehicles).write(DIR+"hc_vehicles"+i+".xml.gz");
		}
	}
}


