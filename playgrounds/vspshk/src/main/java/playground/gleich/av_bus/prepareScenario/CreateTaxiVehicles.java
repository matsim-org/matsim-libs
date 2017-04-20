/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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
package playground.gleich.av_bus.prepareScenario;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.data.VehicleImpl;
import org.matsim.contrib.dvrp.data.file.VehicleWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;

import com.vividsolutions.jts.geom.Geometry;

import playground.gleich.av_bus.FilePaths;
import playground.jbischoff.utils.JbUtils;

/**
 * @author  vsp-gleich
 *
 */
public class CreateTaxiVehicles {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		int numberofVehicles = 100;
		double operationStartTime = 0.; //t0
		double operationEndTime = 24*3600.;	//t1
		int seats = 4;
		String networkfile = FilePaths.PATH_NETWORK_BERLIN__10PCT;
		String taxisFile = FilePaths.PATH_TAXI_VEHICLES_100_BERLIN__10PCT;
		List<Vehicle> vehicles = new ArrayList<>();
		Random random = MatsimRandom.getLocalInstance();
		Geometry geometryStudyArea = JbUtils.readShapeFileAndExtractGeometry(FilePaths.PATH_STUDY_AREA_SHP, FilePaths.STUDY_AREA_SHP_KEY).get(FilePaths.STUDY_AREA_SHP_KEY);
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkfile);
		List<Id<Link>> linksInArea = new ArrayList<>();
			for(Link link: scenario.getNetwork().getLinks().values()){
				if(geometryStudyArea.contains(MGC.coord2Point(link.getFromNode().getCoord())) &&
						geometryStudyArea.contains(MGC.coord2Point(link.getToNode().getCoord()))){
					linksInArea.add(link.getId());
				}
			}
		for (int i = 0; i< numberofVehicles;i++){
			Link startLink;
			do {
			Id<Link> linkId = linksInArea.get(random.nextInt(linksInArea.size()));
			startLink =  scenario.getNetwork().getLinks().get(linkId);
			}
			while (!startLink.getAllowedModes().contains(TransportMode.car));
			//for multi-modal networks: Only links where cars can ride should be used.
			Vehicle v = new VehicleImpl(Id.create("taxi"+i, Vehicle.class), startLink, seats, operationStartTime, operationEndTime);
		    vehicles.add(v);    
		}
		new VehicleWriter(vehicles).write(taxisFile);
	}

}
