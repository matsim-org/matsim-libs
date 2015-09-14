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

package playground.jbischoff.av.preparation;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
import org.matsim.core.utils.geometry.geotools.MGC;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

import playground.jbischoff.taxi.berlin.demand.TaxiDemandWriter;

/**
 * @author  jbischoff
 *
 */
public class TaxiVehicleCreator
	{


	private String networkFile = "C:/Users/Joschka/Documents/shared-svn/projects/audi_av/scenario/network.xml.gz";
	private String shapeFile = "C:/Users/Joschka/Documents/shared-svn/projects/audi_av/shp/Untersuchungsraum.shp";
	private String vehiclesFilePrefix = "C:/Users/Joschka/Documents/shared-svn/projects/audi_av/scenario/taxi_vehicles_";
	
	private Scenario scenario ;
	private Geometry geometry;
	private Random random = MatsimRandom.getRandom();
    private List<Vehicle> vehicles = new ArrayList<>();

	
	public static void main(String[] args) {
		TaxiVehicleCreator tvc = new TaxiVehicleCreator();
		tvc.run(50000);
}

	public TaxiVehicleCreator() {
				
		this.scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile(networkFile);
		this.geometry = ScenarioPreparator.readShapeFileAndExtractGeometry(shapeFile);	
	}
	private void run(int amount) {
	    
		for (int i = 0 ; i< amount; i++){
		Point p = TaxiDemandWriter.getRandomPointInFeature(random, geometry);
		Link link = ((NetworkImpl) scenario.getNetwork()).getNearestLinkExactly(MGC.point2Coord(p));
        Vehicle v = new VehicleImpl(Id.create(i, Vehicle.class), link, 5, Math.round(1), Math.round(30*3600));
        vehicles.add(v);

		}
		new VehicleWriter(vehicles).write(vehiclesFilePrefix+amount+".xml.gz");
	}

	
}
