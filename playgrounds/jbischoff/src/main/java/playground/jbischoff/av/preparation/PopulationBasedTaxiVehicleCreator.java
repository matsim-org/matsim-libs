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
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.data.VehicleImpl;
import org.matsim.contrib.dvrp.data.file.VehicleWriter;
import org.matsim.contrib.util.random.WeightedRandomSelection;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

import playground.jbischoff.taxi.berlin.demand.TaxiDemandWriter;
import playground.jbischoff.utils.JbUtils;

/**
 * @author  jbischoff
 *
 */
public class PopulationBasedTaxiVehicleCreator
	{


	private String networkFile = "C:/Users/Joschka/Documents/shared-svn/projects/audi_av/scenario/networkc.xml.gz";
	private String shapeFile = "C:/Users/Joschka/Documents/shared-svn/projects/audi_av/shp/Planungsraum.shp";
	private String vehiclesFilePrefix = "C:/Users/Joschka/Documents/shared-svn/projects/audi_av/scenario/subscenarios/mobhubs/taxi_vehicles_";
	private String populationData = "C:/Users/Joschka/Documents/shared-svn/projects/audi_av/shp/bevoelkerungInnenring.txt";
	
	private Scenario scenario ;
	Map<String,Geometry> geometry;
	private Random random = MatsimRandom.getRandom();
    private List<Vehicle> vehicles = new ArrayList<>();
    private final WeightedRandomSelection<String> wrs;

	
	public static void main(String[] args) {
		for (int i = 4000; i<8001 ; i=i+500 ){
			PopulationBasedTaxiVehicleCreator tvc = new PopulationBasedTaxiVehicleCreator();
			System.out.println(i);
			tvc.run(i);
		}
}

	public PopulationBasedTaxiVehicleCreator() {
				
		this.scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile);
		this.geometry = JbUtils.readShapeFileAndExtractGeometry(shapeFile);	
		this.wrs = new WeightedRandomSelection<>();
        readPopulationData();
	}
	
	private void readPopulationData() {
		
		TabularFileParserConfig config = new TabularFileParserConfig();
        config.setDelimiterTags(new String[] {"\t"});
        config.setFileName(populationData);
        config.setCommentTags(new String[] { "#" });
        new TabularFileParser().parse(config, new TabularFileHandler() {
			
			@Override
			public void startRow(String[] row) {

				wrs.add(row[0], Double.parseDouble(row[2]));
			}
		});
        
		
	}

	private void run(int amount) {
	    
		for (int i = 0 ; i< amount; i++){
		Point p = TaxiDemandWriter.getRandomPointInFeature(random, geometry.get(wrs.select()));
		Link link = ((NetworkImpl) scenario.getNetwork()).getNearestLinkExactly(MGC.point2Coord(p));
        Vehicle v = new VehicleImpl(Id.create("rt"+i, Vehicle.class), link, 5, Math.round(1), Math.round(25*3600));
        vehicles.add(v);

		}
		new VehicleWriter(vehicles).write(vehiclesFilePrefix+amount+".xml.gz");
	}
	


	
}
