/* *********************************************************************** *
 * project: org.matsim.*
 * ConvertMadeiraNetwork.java
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

/**
 * 
 */
package playground.southafrica.tmp;

import java.io.BufferedWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.NetworkReaderMatsimV1;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.OsmNetworkReader;
import org.matsim.core.utils.misc.Counter;
import org.matsim.utils.objectattributes.ObjectAttributes;

import playground.southafrica.utilities.Header;

/**
 * @author jwjoubert
 *
 */
public class ConvertMadeiraNetwork {
	final private static Logger LOG = Logger.getLogger(ConvertMadeiraNetwork.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(ConvertMadeiraNetwork.class.toString(), args);
		
		String osm = args[0];
		String network = args[1];
		String network3D = args[2];
		String gradeFile = args[3];
		
//		convertOsmToMatsim(osm, network);
		extractGradeFrom3D(network3D, gradeFile);
		
		Header.printFooter();
	}
	
	private static void convertOsmToMatsim(String osmFile, String networkFile){
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, "EPSG:3061");
		
		OsmNetworkReader onr = new OsmNetworkReader(sc.getNetwork(), ct);
		onr.setKeepPaths(true);
		onr.parse(osmFile);
		
		new NetworkWriter(sc.getNetwork()).write(networkFile);
		
		
	}
	
	private static void extractGradeFrom3D(String networkFile, String gradeFile){
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new NetworkReaderMatsimV1(sc.getNetwork()).readFile(networkFile);
		
		/* Extract the elevation and grade for each link. */
		BufferedWriter bw = IOUtils.getBufferedWriter(gradeFile);
		Counter counter = new Counter("  link # ");
		ObjectAttributes linkAttributes = new ObjectAttributes();
		try{
			bw.write("lid,fx,fy,fz,tx,ty,tz,length,grade");
			bw.newLine();
			for(Link l : sc.getNetwork().getLinks().values()){
				Coord cFrom = l.getFromNode().getCoord();
				Coord cTo = l.getToNode().getCoord();
				
				double grade = calculateGrade(l);
				linkAttributes.putAttribute(l.getId().toString(), "grade", grade);
				
				String line = String.format("%s,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.1f,%.6f\n", 
						l.getId().toString(),
						cFrom.getX(), cFrom.getY(), cFrom.getZ(),
						cTo.getX(), cTo.getY(), cTo.getZ(),
						l.getLength(),
						grade);
				bw.write(line);
				counter.incCounter();
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write to elevation file.");
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close elevation file.");
			}
		}
		counter.printCounter();
	}

	private static double calculateGrade(Link link){
		if(!link.getFromNode().getCoord().hasZ() || 
				!link.getToNode().getCoord().hasZ()){
			LOG.error("From node: " + link.getFromNode().getCoord().toString());
			LOG.error("To node: " + link.getToNode().getCoord().toString());
			throw new IllegalArgumentException("Cannot calculate grade if both nodes on the link do not have elevation (z) set.");
		}
		
		Coord c1 = link.getFromNode().getCoord();
		Coord c2 = link.getToNode().getCoord();
		
		double length = link.getLength();
		double grade = (c2.getZ()-c1.getZ())/length;
		
		return grade;
	}

	
	
}
