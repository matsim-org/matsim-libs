/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.julia.toi;

import java.util.Collection;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.geotools.feature.simple.SimpleFeatureTypeImpl;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

public class ShapeConverterNetwork {

	static String shapeFile = "input/oslo/Matsim_files_1/trondheim_med_omland_4.shp";
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		Logger logger = Logger.getLogger(ShapeConverterNetwork.class);
		
		Config config = new Config();
		config.addCoreModules();
		Controler controler = new Controler(config);
		Scenario scenario = controler.getScenario();

		ShapeFileReader sfr = new ShapeFileReader();
		Collection<SimpleFeature> features = sfr.readFileAndInitialize(shapeFile);
		
		NetworkImpl network = (NetworkImpl) scenario.getNetwork();

		//Node node1 = network.createAndAddNode(scenario.createId("1"), scenario.createCoord(0.0, 10000.0));
		
		for(SimpleFeature sf: features){
			if(sf.getFeatureType() instanceof SimpleFeatureTypeImpl){
				//SimpleFeatureTypeImpl http://www.opengis.net/gml:trondheim_med_omland_4 identified extends lineFeature(the_geom:MultiLineString,FNODE_:FNODE_,TNODE_:TNODE_,LPOLY_:LPOLY_,RPOLY_:RPOLY_,LENGTH:LENGTH,VEGNETT_:VEGNETT_,VEGNETT_ID:VEGNETT_ID,OPPR:OPPR,KOORDH:KOORDH,LTEMA:LTEMA,ADRS1:ADRS1,ADRS1F:ADRS1F,ADRS1T:ADRS1T,ADRS2:ADRS2,ADRS2F:ADRS2F,ADRS2T:ADRS2T,MEDIUM:MEDIUM,KOMM:KOMM,TRANSID:TRANSID,GATE:GATE,MAALEMETOD:MAALEMETOD,NOEYAKTIGH:NOEYAKTIGH,GATENAVN:GATENAVN,VEGTYPE:VEGTYPE,VEGSTATUS:VEGSTATUS,VEGNUMMER:VEGNUMMER,HOVEDPARSE:HOVEDPARSE,METER_FRA:METER_FRA,METER_TIL:METER_TIL,VKJORFLT:VKJORFLT,VFRADATO:VFRADATO,DATO:DATO,AKSEL_SO:AKSEL_SO,AKSEL_VI:AKSEL_VI,AKSEL_TEL:AKSEL_TEL,LENGDE:LENGDE,TOTVEKT:TOTVEKT,FARTSGRENS:FARTSGRENS,HOYDE:HOYDE,ONEWAY:ONEWAY,SPERRING:SPERRING,DRIVETIME:DRIVETIME,KOMMTRANSI:KOMMTRANSI,FYLKE:FYLKE,KOMMUNE:KOMMUNE,X_fra:X_fra,Y_fra:Y_fra,X_til:X_til,Y_til:Y_til)
//				System.out.println(sf.getAttribute("FNODE_"));
//				System.out.println(sf.getAttribute("KOORDH"));
				
				// create from node (if it doesnt exist yet)
				Double fromNodeX = (Double) sf.getAttribute("X_fra");
				Double fromNodeY = (Double) sf.getAttribute("Y_fra");
				Coord fromCoord = scenario.createCoord(fromNodeX, fromNodeY);
				Long fromNodeLong = (Long) sf.getAttribute("FNODE_");
				String fromNode = Long.toString(fromNodeLong);
				Node node1;
				
				if(!network.getNodes().containsKey(new IdImpl(fromNode))){
					node1 = network.createAndAddNode(scenario.createId(fromNode), fromCoord);
				}else{
					node1=network.getNodes().get(new IdImpl(fromNode));
					
				}
				
				// create to node (if it does not exist yet)
				Double toNodeX = (Double) sf.getAttribute("X_til");
				Double toNodeY = (Double) sf.getAttribute("Y_til");
				Coord toCoord = scenario.createCoord(toNodeX, toNodeY);
				Long toNodeLong = (Long) sf.getAttribute("TNODE_");
				String toNode = Long.toString(toNodeLong);
				Node node2;
				
				if(!network.getNodes().containsKey(new IdImpl(toNode))){
					node2 = network.createAndAddNode(scenario.createId(toNode), toCoord);
				}else{
					node2 = network.getNodes().get(new IdImpl(toNode));
					
				}
				
				// create link
				//network.createAndAddLink(scenario.createId("12"), node1, node2, 1000, 30.00, 3600, 1, null, "22");
				//network.createAndAddLink(id, fromNode, toNode, length, freespeed, capacity, numLanes);
				//network.createAndAddLink(id, fromNode, toNode, length, freespeed, capacity, numLanes, origId, type);
				Id linkId1 = new IdImpl(fromCoord+"_"+toCoord);
				Id linkId2 = new IdImpl(toCoord+"_"+fromCoord);
				if(node1.equals(node2)){
					logger.warn("nodes equal");
				}
				
				Double linkLength = (Double) sf.getAttribute("LENGDE");
				Integer freeSpeedkmh = (Integer) sf.getAttribute("FARTSGRENS"); // tempo limit //TODO change to m/sec?
				Double freeSpeed = freeSpeedkmh.doubleValue(); 
				Double capacity = 3600.;
				Double numLanes = 2.0;
					
				if (!network.getLinks().containsKey(linkId1)) {
					network.createAndAddLink(linkId1, node1, node2, linkLength,freeSpeed, capacity, numLanes);
				}
				if(!network.getLinks().containsKey(linkId2)){
					network.createAndAddLink(linkId2, node2, node1, linkLength,freeSpeed, capacity, numLanes);
				}
		}
		
		}
		
		 new NetworkCleaner().run(network);
		NetworkWriter nw = new NetworkWriter(network);
		nw.write("input/oslo/trondheim_network.xml");
		
		
	
	}
}
