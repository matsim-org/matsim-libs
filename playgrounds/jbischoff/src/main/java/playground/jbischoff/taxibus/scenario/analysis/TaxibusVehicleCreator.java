/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.jbischoff.taxibus.scenario.analysis;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.data.VehicleImpl;
import org.matsim.contrib.dvrp.data.file.VehicleWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author  jbischoff
 *
 */
public class TaxibusVehicleCreator
	{


	private String networkFile = "C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/scenario/network/versions/network_nopt.xml";
	private String vehiclesFilePrefix = "C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/scenario/input/taxibus_vehicles_woblines";
	
	private Scenario scenario ;
	private int ii = 0;
	private Random random = MatsimRandom.getRandom();
    private List<Vehicle> vehicles = new ArrayList<>();

	
	public static void main(String[] args) {
		TaxibusVehicleCreator tvc = new TaxibusVehicleCreator();
		int i = 50;
		tvc.run(i, Id.createLinkId("vw222"));
		tvc.run(i, Id.createLinkId("vw12"));
		tvc.run(i, Id.createLinkId("vw14"));
		tvc.run(i, Id.createLinkId("64653"));
		tvc.run(i, Id.createLinkId("40590"));
		tvc.run(i, Id.createLinkId("6422"));
		tvc.run(i, Id.createLinkId("41185"));
		tvc.run(i, Id.createLinkId("41181"));
		
		

		new VehicleWriter(tvc.vehicles).write(tvc.vehiclesFilePrefix+i+".xml.gz");

//		for (int i = 10; i<150 ; i=i+10 ){
//			System.out.println(i);
//			tvc.run(i);
//		}
}

	public TaxibusVehicleCreator() {
				
		this.scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile);
	}
	private void run(int amount) {
	    
		for (int i = 0 ; i< amount; i++){
		double x = 593084 + random.nextDouble()*(629810-593084);
		double y = 5785583 + random.nextDouble()*(5817600-5785583);
		Coord c = new Coord(x,y);
		Link link = ((NetworkImpl) scenario.getNetwork()).getNearestLinkExactly(c);
        Vehicle v = new VehicleImpl(Id.create("tb"+i, Vehicle.class), link, 8, Math.round(1), Math.round(48*3600));
        vehicles.add(v);

		}
		new VehicleWriter(vehicles).write(vehiclesFilePrefix+amount+".xml");
	}
	
private void run(int amount, Id<Link> linkId) {
	    
		for (int i = 0 ; i< amount; i++){
	
		Link link = scenario.getNetwork().getLinks().get(linkId);
        Vehicle v = new VehicleImpl(Id.create("tb"+ii, Vehicle.class), link, 8, Math.round(1), Math.round(48*3600));
        ii++;
        vehicles.add(v);

		}
	}

	
}
