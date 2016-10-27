/* *********************************************************************** *
 * project: org.matsim.*
 * PolygonBuilder.java                                                                        *
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
package playground.jjoubert.projects.wasteCollection.polygons;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

import playground.southafrica.utilities.Header;

/**
 *
 * @author jwjoubert
 */
public class PolygonBuilder {
	final private static Logger LOG = Logger.getLogger(PolygonBuilder.class);
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(PolygonBuilder.class.toString(), args);
		
		String network = args[0];
		String polygon = args[1];
		Double buffer = Double.parseDouble(args[2]);
		
		PolygonBuilder.buildPolygons(network, polygon, buffer);
		
		Header.printFooter();
	}
	
	
	private static void buildPolygons(String network, String output, double buffer){
		LOG.info("Parsing network...");
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(sc.getNetwork()).readFile(network);
		LOG.info("Done parsing network.");
		
		LOG.info("Build polygons... (" + sc.getNetwork().getLinks().size() + " links)");
		Counter counter = new Counter("  links # ");
		
		List<Link> links = new ArrayList<>();
		Map<Id<Link>, Geometry> polygonMap = new HashMap<>();
		
		GeometryFactory gf = new GeometryFactory();
		for(Link link : sc.getNetwork().getLinks().values()){
			if(!links.contains(link)){
				/* Build its polygon. */
				Coordinate c1 = new Coordinate(link.getFromNode().getCoord().getX(), link.getFromNode().getCoord().getY());
				Coordinate c2 = new Coordinate(link.getToNode().getCoord().getX(), link.getToNode().getCoord().getY());
				Coordinate[] ca = {c1, c2};
				LineString ls = gf.createLineString(ca);
				Geometry polygon = ls.buffer(buffer);
				polygonMap.put(link.getId(), polygon);
				
				/* Add it's opposing link to the list, if it exists. */
				Iterator<? extends Link> possibleLinks = link.getToNode().getOutLinks().values().iterator();
				boolean found = false;
				while(!found && possibleLinks.hasNext()){
					Link thisLink = possibleLinks.next();
					if(thisLink.getToNode().equals(link.getFromNode())){
						found = true;
						links.add(thisLink);
					}
				}
				
				counter.incCounter();
			} else{
				/* Ignore it. */
			}
		}
		counter.printCounter();
		LOG.info("Done building polygons.");
		
		LOG.info("Write buffers to file...");
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84_SA_Albers", "WGS84");
		BufferedWriter bw = IOUtils.getBufferedWriter(output);
		try{
			bw.write("link,radius,polygon,point,x,y,lon,lat");
			bw.newLine();
			
			int poly = 1;
			for(Id<Link> lid : polygonMap.keySet()){
				Geometry g = polygonMap.get(lid);
				int coord = 1;
				for(Coordinate c : g.getCoordinates()){
					Coord cf = ct.transform(CoordUtils.createCoord(c.x, c.y));
					String line = String.format("%s,%.0f,%d,%d,%.0f,%.0f,%.8f,%.8f\n", 
							lid.toString(), buffer, poly, coord, c.x, c.y, cf.getX(), cf.getY());
					bw.write(line);
					coord++;
				}
				
				poly++;
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write to " + output);
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + output);
			}
		}
		
		LOG.info("Done writing buffers.");
		
	}

}
