/* *********************************************************************** *
 * project: org.matsim.*
 * CountsCompareToSHP.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2009 by the members listed in the COPYING,  *
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

package playground.telaviv.counts;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.utils.geometry.transformations.GeotoolsTransformation;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public class CountsCompareToSHP {

	final private static Logger log = Logger.getLogger(CountsCompareToSHP.class);
	
	private static String delimiter = "\t";
	private static Charset charset = Charset.forName("UTF-8");
	private static String ITM = "EPSG:2039";	// network coding String
	private static String countsCompareFile = "../../matsim/mysimulations/telaviv/output_JDEQSim/ITERS/it.100/100.countscompare.txt";
	private static String networkFile = "../../matsim/mysimulations/telaviv/input/network.xml";
	private static String shpFile = "../../matsim/mysimulations/telaviv/output_JDEQSim/ITERS/it.100/100.countscompare.shp";
	
	private Scenario scenario = new ScenarioImpl();
	private Map<String, List<Line>> counts;
	
	public static void main(String[] args) throws Exception {
		CountsCompareToSHP cctSHP = new CountsCompareToSHP();
		cctSHP.readNetwork();
		cctSHP.readCounts();
		cctSHP.writeSHPFile();
	}
	
	private void readNetwork() throws Exception {
		new MatsimNetworkReader(scenario).parse(networkFile);
	}
	
	private void readCounts() throws Exception {
		FileInputStream fis = null;
		InputStreamReader isr = null;
	    BufferedReader br = null;
	    
	    fis = new FileInputStream(countsCompareFile);
		isr = new InputStreamReader(fis, charset);
		br = new BufferedReader(isr);
		
		counts = new HashMap<String, List<Line>>();
		
		// skip first Line
		br.readLine();
		
		String textLine;
		while((textLine = br.readLine()) != null) {
			textLine = textLine.replace(",", "");
			String[] cols = textLine.split(delimiter);
			
			Line line = new Line();
			line.Link_Id = cols[0];
			line.Hour = cols[1];
			line.MATSIM_Volumes = cols[2];
			line.Count_Volumes = cols[3];
			line.Relative_Error = cols[4];
			
			List<Line> list = counts.get(line.Link_Id);
			if (list == null) {
				list = new ArrayList<Line>();
				counts.put(line.Link_Id, list);
			}
			list.add(line);
		}
		
		br.close();
		isr.close();
		fis.close();
		
		log.info("Read " + counts.size() + " counts.");
	}
	
	private void writeSHPFile() throws Exception {

		GeotoolsTransformation transformator = new GeotoolsTransformation(ITM, "WGS84");
		
		GeometryFactory geoFac = new GeometryFactory();
		Collection<Feature> features = new ArrayList<Feature>();
		CoordinateReferenceSystem crs = DefaultGeographicCRS.WGS84;
			
		AttributeType geom = DefaultAttributeTypeFactory.newAttributeType("Point", Point.class, true, null, null, crs);
		AttributeType id = AttributeTypeFactory.newAttributeType("Id", String.class);
		
		AttributeType hour6Count = AttributeTypeFactory.newAttributeType("Hour 6 Count Volume", Double.class);
		AttributeType hour7Count = AttributeTypeFactory.newAttributeType("Hour 7 Count Volume", Double.class);
		AttributeType hour8Count = AttributeTypeFactory.newAttributeType("Hour 8 Count Volume", Double.class);
		AttributeType hour9Count = AttributeTypeFactory.newAttributeType("Hour 9 Count Volume", Double.class);
		
		AttributeType hour6MATSim = AttributeTypeFactory.newAttributeType("Hour 6 MATSim Volume", Double.class);
		AttributeType hour7MATSim = AttributeTypeFactory.newAttributeType("Hour 7 MATSim Volume", Double.class);
		AttributeType hour8MATSim = AttributeTypeFactory.newAttributeType("Hour 8 MATSim Volume", Double.class);
		AttributeType hour9MATSim = AttributeTypeFactory.newAttributeType("Hour 9 MATSim Volume", Double.class);
		
		AttributeType hour6Error = AttributeTypeFactory.newAttributeType("Hour 6 Relative Error", Double.class);
		AttributeType hour7Error = AttributeTypeFactory.newAttributeType("Hour 7 Relative Error", Double.class);
		AttributeType hour8Error = AttributeTypeFactory.newAttributeType("Hour 8 Relative Error", Double.class);
		AttributeType hour9Error = AttributeTypeFactory.newAttributeType("Hour 9 Relative Error", Double.class);
		
		AttributeType[] array = new AttributeType[] {geom, id, hour6Count, hour7Count, hour8Count, hour9Count,
				hour6MATSim, hour7MATSim, hour8MATSim, hour9MATSim, hour6Error, hour7Error, hour8Error, hour9Error};
		
		FeatureType ftNode = FeatureTypeBuilder.newFeatureType(array, "node");
		
		
		for (List<Line> lines : counts.values()) {
			Id linkId = scenario.createId(lines.get(0).Link_Id);
			Link link = scenario.getNetwork().getLinks().get(linkId);
			Coord transformedCoord = transformator.transform(link.getCoord());
			
			Coordinate coord = new Coordinate(transformedCoord.getX(), transformedCoord.getY());
			Point point = geoFac.createPoint(coord);
			
			double[] countVolumes = new double[4];
			double[] MATSimVolumes = new double[4];
			double[] errors = new double[4];

			for (int i = 0; i < lines.size(); i++) {
				Line line = lines.get(i);
				countVolumes[i] = Double.valueOf(line.Count_Volumes);
				MATSimVolumes[i] = Double.valueOf(line.MATSIM_Volumes);
				errors[i] = Double.valueOf(line.Relative_Error);
			}
			
			Object[] object = new Object[] {point, linkId.toString(), countVolumes[0], countVolumes[1], countVolumes[2], 
					countVolumes[3], MATSimVolumes[0], MATSimVolumes[1], MATSimVolumes[2], MATSimVolumes[3],
					errors[0], errors[1], errors[2], errors[3]};
			
			Feature ft = ftNode.create(object, "nodes");
			features.add(ft);
		}
		
		ShapeFileWriter.writeGeometries(features, shpFile);
	}
	
	private static class Line {
//		Link Id	Hour	MATSIM volumes	Count volumes	Relative Error	
//		10005	6	71.6	5,521	-98.703
		String Link_Id;
		String Hour;
		String MATSIM_Volumes;
		String Count_Volumes;
		String Relative_Error;
	}
	
}
