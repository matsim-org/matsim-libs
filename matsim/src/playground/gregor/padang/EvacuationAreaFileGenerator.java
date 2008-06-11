/* *********************************************************************** *
 * project: org.matsim.*
 * EvacuationAreaFileGenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.gregor.padang;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureIterator;
import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.evacuation.EvacuationAreaFileWriter;
import org.matsim.evacuation.EvacuationAreaLink;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkFactory;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.network.TimeVariantLinkImpl;
import org.matsim.utils.geometry.CoordI;
import org.matsim.utils.geometry.geotools.MGC;
import org.matsim.utils.geometry.shared.Coord;
import org.matsim.world.World;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

import playground.gregor.gis.utils.ShapeFileReader;

public class EvacuationAreaFileGenerator {
	
	private static final Logger log = Logger.getLogger(EvacuationAreaFileGenerator.class);
	private NetworkLayer network;
	private Collection<Polygon> evacZone;
	private HashMap<Id, EvacuationAreaLink> evacLinks;
	private GeometryFactory geofac;

	public EvacuationAreaFileGenerator(NetworkLayer network,
			Collection<Polygon> evacZone) {
		this.network = network;
		this.network.connect();
		this.evacZone = evacZone;
		this.evacLinks = new HashMap<Id, EvacuationAreaLink>();
		this.geofac = new GeometryFactory();
		findEvacuationLinks();
		try {
			new EvacuationAreaFileWriter(this.evacLinks).writeFile("evac.xml.gz");
		} catch (IOException e) {
			e.printStackTrace();
		}
				
		
	}


	
	
	
	private void findEvacuationLinks() {
		
		for (Polygon p : this.evacZone) {
			
			
			double distance = Math.max(p.getExteriorRing().getEnvelopeInternal().getHeight(),p.getExteriorRing().getEnvelopeInternal().getWidth());
			Coord coord = (Coord) MGC.point2Coord(p.getCentroid());
			Collection<Node> nodes = this.network.getNearestNodes(coord, distance * 2);
			handleNodes(nodes,p);
			
			
		}
		
	}





	private void handleNodes(Collection<Node> nodes, Polygon p) {
		for (Node node : nodes) {
			for (Link l : node.getOutLinks().values()) {
				if (this.evacLinks.containsKey(l.getId())) {
					continue;
				}
				LineString ls = getLineString(l);
				
			
				
				if (ls.intersects(p)){
					this.evacLinks.put(l.getId(), new EvacuationAreaLink((IdImpl) l.getId(),3 * 3600));
				}
				
				
			}
			
			for (Link l : node.getInLinks().values()) {
				if (this.evacLinks.containsKey(l.getId())) {
					continue;
				}
				LineString ls = getLineString(l);
				
			
				
				if (ls.intersects(p)){
					this.evacLinks.put(l.getId(), new EvacuationAreaLink((IdImpl) l.getId(),3 * 3600));
				}
				
				
			}
			
			
		}
		
	}





	private LineString getLineString(Link l) {
		
		Coordinate [] coords = new Coordinate [2];
		coords[0] = MGC.coord2Coordinate(l.getFromNode().getCoord());
		coords[1] = MGC.coord2Coordinate(l.getToNode().getCoord());
		
		return this.geofac.createLineString(coords);
	}





	public static void main(String [] args) {
		
		

		String zone_0_5 = "./padang/zona_0_5.shp";
		String zone_5_10 = "./padang/zona_5_1.shp";
		


		if (args.length != 1) {
			throw new RuntimeException("wrong number of arguments! Pleas run EvacuationAreaFileGenerator config.xml" );
		} else {
			Gbl.createConfig(new String[]{args[0], "config_v1.dtd"});
		}

		World world = Gbl.createWorld();

		log.info("loading network from " + Gbl.getConfig().network().getInputFile());
		NetworkFactory fc = new NetworkFactory();
		fc.setLinkPrototype(TimeVariantLinkImpl.class);
		
		NetworkLayer network = new NetworkLayer(fc);
		new MatsimNetworkReader(network).readFile(Gbl.getConfig().network().getInputFile());
		world.setNetworkLayer(network);
		world.complete();
		log.info("done.");
		

		

		log.info("loading shape file from " + zone_0_5);
		FeatureSource fz_0_5 = null;
		try {
			fz_0_5 = ShapeFileReader.readDataFile(zone_0_5);
		} catch (Exception e) {
			e.printStackTrace();
		}
		log.info("done");
		
		log.info("loading shape file from " + zone_5_10);
		FeatureSource fz_5_10 = null;
		try {
			fz_5_10 = ShapeFileReader.readDataFile(zone_5_10);
		} catch (Exception e) {
			e.printStackTrace();
		}
		log.info("done");
		
		Collection<Polygon> evac_zone = new ArrayList<Polygon>();
		readPolygons(evac_zone,fz_0_5);
		readPolygons(evac_zone,fz_5_10);
		
		new EvacuationAreaFileGenerator(network,evac_zone);
		
		
		
	}


	private static void readPolygons(Collection<Polygon> evac_zone, FeatureSource fts) {
		
		
		FeatureIterator it = null;
		try {
			it = fts.getFeatures().features();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		while (it.hasNext()) {
			Feature feature = it.next();
			
			MultiPolygon multiPolygon = (MultiPolygon) feature.getDefaultGeometry();
			for (int i = 0; i < multiPolygon.getNumGeometries(); i++) {
				Polygon polygon = (Polygon) multiPolygon.getGeometryN(i);
				evac_zone.add(polygon);
			}

		}
		
	}

}
