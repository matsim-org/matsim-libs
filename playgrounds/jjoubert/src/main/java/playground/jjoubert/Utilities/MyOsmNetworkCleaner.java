/* *********************************************************************** *
 * project: org.matsim.*
 * MyOsmNetworkCleaner.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.jjoubert.Utilities;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.utils.gis.matsim2esri.network.CapacityBasedWidthCalculator;
import org.matsim.utils.gis.matsim2esri.network.FeatureGeneratorBuilderImpl;
import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;
import org.matsim.utils.gis.matsim2esri.network.PolygonFeatureGenerator;
import org.xml.sax.SAXException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class MyOsmNetworkCleaner {
	private static Logger log = Logger.getLogger(MyOsmNetworkCleaner.class);
	private Scenario sc;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		MyOsmNetworkCleaner monc = null;
		if(args.length != 4){
			throw new IllegalArgumentException("Incorrect number of arguments.");
		} else{
			monc = new MyOsmNetworkCleaner();
		}
		Scenario runSc = new ScenarioImpl();
		MatsimNetworkReader mnr = new MatsimNetworkReader(runSc);
		try {
			mnr.parse(args[0]);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// For now, use the envelope of the multipolygon.
		MyShapefileReader msr = new MyShapefileReader(args[1]);
		Polygon envelope = (Polygon) msr.readMultiPolygon().getEnvelope();
		GeometryFactory gf = new GeometryFactory();
		Polygon[] pa = {envelope};
		MultiPolygon mp = gf.createMultiPolygon(pa);
		monc.cleanNetwork(runSc.getNetwork(), mp);
		
		monc.writeNewNetwork(args[2]);
		
		runSc.getConfig().global().setCoordinateSystem("WGS84_UTM35S");
		FeatureGeneratorBuilderImpl builder = new FeatureGeneratorBuilderImpl(monc.getNewNetwork(), "WGS84_UTM35S");
		builder.setWidthCoefficient(-0.01);
		builder.setFeatureGeneratorPrototype(PolygonFeatureGenerator.class);
		builder.setWidthCalculatorPrototype(CapacityBasedWidthCalculator.class);
		
		new Links2ESRIShape(monc.getNewNetwork(), args[3], builder).write();

	}
	
	public MyOsmNetworkCleaner() {

		
	}
	
	/**
	 * Returns the output network (if cleaned). 
	 * @return cleaned {@link Network}.
	 */
	public Network getNewNetwork(){
		Network nw = null;
		if(sc == null){
			log.warn("No cleaned network exist.");
		} else{
			nw = sc.getNetwork();
		}
		return nw;
	}

	/**
	 * Checks each link in a given network. If either the {@link Link#getFromNode()}
	 * or the {@link Link#getToNode()} falls within the given {@link MultiPolygon}
	 * the link is duplicated in the output network. 
	 * @param network a given {@link Network}.
	 * @param mp a given {@link MultiPolygon} with a single feature, such as an
	 * 		entire province, as opposed to multiple features such as transportation
	 * 		zones in the province.
	 */
	public void cleanNetwork(Network network, MultiPolygon mp) {
		log.info("Removing all links that falls outside the given polygon.");
		log.info("Original network has " + network.getNodes().size() + " nodes and " + network.getLinks().size() + " links.");
		sc = new ScenarioImpl();
		NetworkFactory nf = sc.getNetwork().getFactory();
		GeometryFactory gf = new GeometryFactory();
		for(Link l : network.getLinks().values()){
			Point p1 = gf.createPoint(new Coordinate(l.getFromNode().getCoord().getX(), l.getFromNode().getCoord().getY()));
			Point p2 = gf.createPoint(new Coordinate(l.getToNode().getCoord().getX(), l.getToNode().getCoord().getY()));
			if(mp.contains(p1) || mp.contains(p2)){
//				sc.getNetwork().addLink(l);
				Node fNode = l.getFromNode();
				if(!sc.getNetwork().getNodes().containsKey(fNode.getId())){
					sc.getNetwork().addNode(nf.createNode(fNode.getId(), fNode.getCoord()));
				}
				Node tNode = l.getToNode();
				if(!sc.getNetwork().getNodes().containsKey(tNode.getId())){
					sc.getNetwork().addNode(nf.createNode(tNode.getId(), tNode.getCoord()));
				}
				
				Link lNew = nf.createLink(l.getId(), fNode.getId(), tNode.getId());
				lNew.setCapacity(l.getCapacity());
				lNew.setFreespeed(l.getFreespeed());
				lNew.setLength(l.getLength());
				lNew.setNumberOfLanes(l.getNumberOfLanes());
				
				sc.getNetwork().addLink(lNew);				
			}
		}
		log.info("Done.");
		log.info("New network has " + sc.getNetwork().getNodes().size() + " nodes and " + sc.getNetwork().getLinks().size() + " links.");
		
		log.info("Removing unconnected links.");
		NetworkCleaner nc = new NetworkCleaner();
		nc.run(sc.getNetwork());
		log.info("Done.");
		log.info("Final network has " + sc.getNetwork().getNodes().size() + " nodes and " + sc.getNetwork().getLinks().size() + " links.");
	}
	
	public void writeNewNetwork(String filename){
		if(sc != null){
			NetworkWriter nw = new NetworkWriter(sc.getNetwork());
			nw.write(filename);			
		} else{
			log.warn("Cannot write a NULL network.");
		}
	}

}
