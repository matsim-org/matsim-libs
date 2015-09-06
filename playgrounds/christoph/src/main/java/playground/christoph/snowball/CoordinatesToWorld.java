/* *********************************************************************** *
 * project: org.matsim.*
 * CoordinatesToWorld.java
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

package playground.christoph.snowball;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.opengis.kml._2.DocumentType;
import net.opengis.kml._2.KmlType;
import net.opengis.kml._2.ObjectFactory;

import org.apache.log4j.Logger;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.KmlNetworkWriter;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.core.utils.gis.PointFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.vis.kml.KMZWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public class CoordinatesToWorld {

	final private static Logger log = Logger.getLogger(CoordinatesToWorld.class);

	private static String delimiter = ";";
	private static Charset charset = Charset.forName("UTF-8");
	
	private String shapeFile = "../../matsim/mysimulations/snowball/world_countries_shp/world_countries_shp.shp";
	private String coordinatesFile = "../../matsim/mysimulations/snowball/SnowballAare.csv";
	private String kmzOutFile = "../../matsim/mysimulations/snowball/SnowballAare.kmz";
	private String shapeOutFile = "../../matsim/mysimulations/snowball/SnowballAare.shp";
	
	private Map<SimpleFeature, List<Line>> countries;
	private List<Line> lines;
	
	public static void main(String[] args) throws Exception {
		new CoordinatesToWorld();
	}
	
	public CoordinatesToWorld() throws Exception {
		readShapeFile();
		readCoordinates();
		
		assignCoordiantesToCountries();
		writeKMZFile();
		writeSHPFile();
	}
	
	private void readShapeFile() throws Exception {
		countries = new HashMap<SimpleFeature, List<Line>>();
		
		for (SimpleFeature country : ShapeFileReader.getAllFeatures(shapeFile)) {
			countries.put(country, new ArrayList<Line>());
		}
		log.info("Read " + countries.size() + " countries.");
	}
	
	private void readCoordinates() throws Exception {
		FileInputStream fis = null;
		InputStreamReader isr = null;
	    BufferedReader br = null;
	    
	    fis = new FileInputStream(coordinatesFile);
		isr = new InputStreamReader(fis, charset);
		br = new BufferedReader(isr);
		
		lines = new ArrayList<Line>();
		
		// skip first Line
		br.readLine();
		
		String textLine;
		while((textLine = br.readLine()) != null) {
			textLine = textLine.replaceAll(",", ".");
			String[] cols = textLine.split(delimiter);
			
			Line line = new Line();
			line.Ego_SQL_ID = cols[0];
			line.Alter_MergeParameter = cols[1];
			line.BreiteWGS84 = cols[2];
			line.LaengeWGS84 = cols[3];
			line.Ego_Origin = cols[4];
			line. Order = cols[5];
			line.BreiteCH1903 = cols[6];
			line.LaengeCH1903 = cols[7];
			
			lines.add(line);
		}
		
		br.close();
		isr.close();
		fis.close();
		
		log.info("Read " + lines.size() + " coordinates.");
	}
	
	private void assignCoordiantesToCountries() {
		
		GeometryFactory factory = new GeometryFactory();
		int assigned = 0;
		
		for (Line line : lines) {
			double x = Double.NaN;
			double y = Double.NaN;
			try {
				x = new Double(line.LaengeWGS84);
				y = new Double(line.BreiteWGS84);				
			} catch (NumberFormatException nfe) {
				continue;
			}
			Point point = factory.createPoint(new Coordinate(x, y));
			for (Entry<SimpleFeature, List<Line>> entry : countries.entrySet()) {
				SimpleFeature country = entry.getKey();
				
				Geometry polygon = (Geometry) country.getDefaultGeometry();
				if (polygon.contains(point)) {
					List<Line> list = entry.getValue();
					list.add(line);
					assigned++;
					break;
				}				
			}
		}
		log.info("Assigned " + assigned + " coordinates to countries.");
	}

	private void writeKMZFile() throws Exception {
		log.info("writing kmz network ...");
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network network = NetworkImpl.createNetwork();
		NetworkFactory factory = network.getFactory();
		
		for (Line line : lines) {
			double x = Double.NaN;
			double y = Double.NaN;
			try {
				x = new Double(line.LaengeWGS84);
				y = new Double(line.BreiteWGS84);
			} catch (NumberFormatException nfe) {
				continue;
			}
			Node node = factory.createNode(Id.create(line.Alter_MergeParameter, Node.class), new Coord(x, y));
			network.addNode(node);
		}
		
		ObjectFactory kmlObjectFactory = new ObjectFactory();
		KMZWriter kmzWriter = new KMZWriter(kmzOutFile);
		
		KmlType mainKml = kmlObjectFactory.createKmlType();
		DocumentType mainDoc = kmlObjectFactory.createDocumentType();
		mainKml.setAbstractFeatureGroup(kmlObjectFactory.createDocument(mainDoc));
		
		KmlNetworkWriter kmlNetworkWriter = new KmlNetworkWriter(network, new IdentityTransformation(), kmzWriter, mainDoc);
		
		mainDoc.getAbstractFeatureGroup().add(kmlObjectFactory.createFolder(kmlNetworkWriter.getNetworkFolder()));
		kmzWriter.writeMainKml(mainKml);
		kmzWriter.close();
		log.info("... done.");
	}
	
	private void writeSHPFile() throws Exception {

		GeometryFactory geoFac = new GeometryFactory();
		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
		CoordinateReferenceSystem crs = DefaultGeographicCRS.WGS84;
			
		PointFeatureFactory factory = new PointFeatureFactory.Builder().
				setCrs(crs)
				.setName("nodes")
				.addAttribute("ID", String.class)
				.create();
		
		for (Line line : lines) {
			double x = Double.NaN;
			double y = Double.NaN;
			try {
				x = new Double(line.LaengeWGS84);
				y = new Double(line.BreiteWGS84);
			} catch (NumberFormatException nfe) {
				continue;
			}
			
			Coordinate coord = new Coordinate(x, y);
			
			SimpleFeature ft = factory.createPoint(coord, new Object[] {line.Alter_MergeParameter}, null);
			features.add(ft);
		}
		
		ShapeFileWriter.writeGeometries(features, shapeOutFile);
	}
	
	private static class Line {		
//		"Ego_SQL_ID";"Alter_MergeParameter";"BreiteWGS84";"LaengeWGS84";"Ego_Origin";"Order";"BreiteCH1903";"LaengeCH1903"
//		194;"194_0";47,4207585;8,4130862;NA;0;699999,763621163;99999,9730950352
		String Ego_SQL_ID;
		String Alter_MergeParameter;
		String BreiteWGS84;
		String LaengeWGS84;
		String Ego_Origin;
		String Order;
		String BreiteCH1903;
		String LaengeCH1903;
	}
}
