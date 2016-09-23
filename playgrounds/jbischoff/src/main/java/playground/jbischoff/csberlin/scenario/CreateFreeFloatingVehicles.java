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
package playground.jbischoff.csberlin.scenario;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.data.VehicleImpl;
import org.matsim.contrib.dvrp.data.file.VehicleWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

import playground.jbischoff.av.preparation.ScenarioPreparator;
import playground.jbischoff.av.preparation.TaxiVehicleCreator;
import playground.jbischoff.taxi.berlin.demand.TaxiDemandWriter;
import playground.jbischoff.utils.JbUtils;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class CreateFreeFloatingVehicles {

	private String networkFile = "C:/Users/Joschka/Documents/shared-svn/projects/bmw_carsharing/data/scenario/network-car.xml.gz";
	private String shapeFile = "C:/Users/Joschka/Documents/shared-svn/projects/bmw_carsharing/data/scenario/gis/ff_current_business_area.shp";
	private String vehiclesFilePrefix = "C:/Users/Joschka/Documents/shared-svn/projects/bmw_carsharing/data/scenario/ff_";
	
	private Scenario scenario ;
	private Geometry geometry;
	private Random random = MatsimRandom.getRandom();
    private List<Vehicle> vehicles = new ArrayList<>();

	
	public static void main(String[] args) {
		CreateFreeFloatingVehicles tvc = new CreateFreeFloatingVehicles();
		tvc.run(1000);
}

	public CreateFreeFloatingVehicles() {
				
		this.scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile);
		this.geometry = JbUtils.readShapeFileAndExtractGeometry(shapeFile, "ID").get("0");	
	}
	private void run(int amount) {
	    
		for (int i = 0 ; i< amount; i++){
		Point p = TaxiDemandWriter.getRandomPointInFeature(random, geometry);
		Link link = NetworkUtils.getNearestLinkExactly(((Network) scenario.getNetwork()),MGC.point2Coord(p));
        Vehicle v = new VehicleImpl(Id.create("ff"+i, Vehicle.class), link, 5, Math.round(1), Math.round(30*3600));
        vehicles.add(v);

		}
		new VehicleWriter(vehicles).write(vehiclesFilePrefix+amount+".xml");
	}

}
