/* *********************************************************************** *
 * project: org.matsim.*
 * ZoneMapping.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.telaviv.zones;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 * @author cdobler
 * 
 * Links and Nodes Coding in the CSV File: @see NetworkEmme2Matsim
 * 
 * Link Type Codes:
 * type / 	description
 * 1 	/ 	freeway
 * 2 	/ 	main road -> use only if necessary
 * 3 	/ 	arterial road -> use only if necessary
 * 4 	/	regional road -> use
 * 5 	/ 	collector street -> use
 * 6 	/ 	local street -> use
 * 7 	/ 	multilane highway
 * 9 	/ 	centroid connector -> use
 * 
 * 
 * Activity Types:
 * MATSim	/	Emme2 Demand	/	opening times
 * home	/	no (Activity)	/	0 .. 24
 * work	/	work	/	8 .. 18
 * education	/	study	/	8 .. 18
 * shopping	/	shopping	/	9 .. 19
 * leisure	/	other	6 .. 22
 * 
 */
public class ZoneMapping {

	private static final Logger log = Logger.getLogger(ZoneMapping.class);
	
	private String networkFile = "../../matsim/mysimulations/telaviv/network/network_WGS84.xml";
	private String shapeFile = "../../matsim/mysimulations/telaviv/zones/taz_new.shp";
	private String zonesFile = "../../matsim/mysimulations/telaviv/zones/zonal2007.txt";

	private GeometryFactory factory;
	private Set<Feature> zones;
	private Scenario scenario;
	private Network network;
	
	private Map<Id, Feature> linkMapping;	// linkId, Zone
	private Set<Id> externalNodes;	// nodeId
	private Map<Integer, Feature> zonesMap;	// TAZ, Feature
	private Map<Integer, Emme2Zone> parsedZones;	// TAZ, Emme2Zone
		
	/**
	 * @param args
	 * @throws IOException
	 * @throws FactoryException
	 * @throws TransformException
	 */
	public static void main(String[] args) 
	{
		new ZoneMapping();
	}
	
	public ZoneMapping() {
		
		try
		{
			this.scenario = new ScenarioImpl();
			new MatsimNetworkReader(scenario).readFile(networkFile);
			CoordinateTransformation coordinateTransformation = new IdentityTransformation();
			
//			new MatsimNetworkReader(scenario).readFile("../../matsim/mysimulations/telaviv/network/network.xml");
//			CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation("EPSG:2039", "WGS84");	
			
			this.createMapping(coordinateTransformation);
		} catch (IOException e) { 
			e.printStackTrace();
		} catch (FactoryException e) {
			e.printStackTrace();
		} catch (TransformException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * The Zones Shape file uses WGS84 Coordinates but the MATSim network uses ITM Coordinates.
	 * Therefore we have to transform the Coordinates.
	 */
	public ZoneMapping(Scenario scenario, CoordinateTransformation coordinateTransformation)
	{
		this.scenario = scenario;
		
		try
		{		
			this.createMapping(coordinateTransformation);
		} catch (IOException e) { 
			e.printStackTrace();
		} catch (FactoryException e) {
			e.printStackTrace();
		} catch (TransformException e) {
			e.printStackTrace();
		}
	}
	
	public void createMapping(CoordinateTransformation coordinateTransformation) throws IOException, FactoryException, TransformException {
		factory = new GeometryFactory();

		network = scenario.getNetwork();

		log.info("Loading Network ... done");
		log.info("Nodes: " + network.getNodes().size());
		log.info("Links: " + network.getLinks().size());

		/*
		 * read zones shape file
		 */
		zones = new HashSet<Feature>();

		FeatureSource featureSource = ShapeFileReader.readDataFile(shapeFile);
		for (Object o : featureSource.getFeatures()) {
			zones.add((Feature) o);
		}
	
		zonesMap = new TreeMap<Integer, Feature>();
		for (Feature zone : zones)
		{
			zonesMap.put((Integer)zone.getAttribute(3), zone);
		}
		
		/*
		 * print zones
		 */
		log.info("Using " + zones.size() + " zones.");

		/*
		 * iterate over all links
		 */
		linkMapping = new TreeMap<Id, Feature>();
		int outsideLinks = 0;

		for (Link link : network.getLinks().values()) {
			/*
			 * transform coordinates
			 */
			// Coord coord = link.getCoord();
			// double[] points = new double[] { coord.getX(), coord.getY() };
			// transform.transform(points, 0, points, 0, 1);
			// Point point = factory.createPoint(new Coordinate(points[0],
			// points[1]));
			Feature startZone = null;
			Feature centerZone = null;
			Feature endZone = null;

			Coord fromCoord = coordinateTransformation.transform(link.getFromNode().getCoord());
			Coord linkCoord = coordinateTransformation.transform(link.getCoord());
			Coord toCoord = coordinateTransformation.transform(link.getToNode().getCoord());

			Point startPoint = factory.createPoint(new Coordinate(fromCoord.getX(), fromCoord.getY()));
			Point centerPoint = factory.createPoint(new Coordinate(linkCoord.getX(), linkCoord.getY()));
			Point endPoint = factory.createPoint(new Coordinate(toCoord.getX(), toCoord.getY()));

//			Point startPoint = factory.createPoint(new Coordinate(link.getFromNode().getCoord().getX(), link.getFromNode().getCoord().getY()));
//			Point centerPoint = factory.createPoint(new Coordinate(link.getCoord().getX(), link.getCoord().getY()));
//			Point endPoint = factory.createPoint(new Coordinate(link.getToNode().getCoord().getX(), link.getToNode().getCoord().getY()));

			for (Feature zone : zones)
			{
				Geometry polygon = zone.getDefaultGeometry();
				if (polygon.contains(startPoint)) startZone = zone;
				if (polygon.contains(centerPoint)) centerZone = zone;
				if (polygon.contains(endPoint)) endZone = zone;
			}

			/*
			 * Count point that lie outside the specified zones. Links where at
			 * least 2 points lie outside the area are not assigned to a zone.
			 */
			int nullCounter = 0;
			if (startZone == null) nullCounter++;
			if (centerZone == null) nullCounter++;
			if (endZone == null) nullCounter++;
			if (nullCounter > 1)
			{
				outsideLinks++;
				continue;
			}

			if (startZone == endZone) linkMapping.put(link.getId(), startZone);
			else if (startZone == centerZone) linkMapping.put(link.getId(), startZone);
			else if (centerZone == endZone) linkMapping.put(link.getId(), endZone);
			else linkMapping.put(link.getId(), getLinkMapping(link, 5, coordinateTransformation));
		}
		
		log.info("Found " + outsideLinks + " links outside the mapped area.");
		log.info("Found " + linkMapping.size() + " link mappings.");
		
		/*
		 * iterate over all nodes to find all external nodes
		 */
		externalNodes = new TreeSet<Id>();
		for (Node node : network.getNodes().values())
		{
			Coord pointCoord = coordinateTransformation.transform(node.getCoord());
			Point point = factory.createPoint(new Coordinate(pointCoord.getX(), pointCoord.getY()));
			
//			Point point = factory.createPoint(new Coordinate(node.getCoord().getX(), node.getCoord().getY()));
			
			Feature pointZone = null;
			for (Feature zone : zones)
			{
				Geometry polygon = zone.getDefaultGeometry();
				if (polygon.contains(point)) pointZone = zone;
			}
			
			// if the point is not contained in any Zone it is an external node.
			if (pointZone == null) externalNodes.add(node.getId());
		}
		log.info("Found " + externalNodes.size() + " nodes outside the mapped area.");
		
		/*
		 * Find Connector Links and ensure that they are mapped to their zones.
		 * 
		 * Each Zone Connector Node has the same Id as the TAZ of its zone!
		 */
		for (Entry<Integer, Feature> entry : zonesMap.entrySet())
		{
			
			Node node = network.getNodes().get(scenario.createId(entry.getKey().toString()));
			
			if (node != null)
			{
				for (Link link : node.getInLinks().values())
				{
					linkMapping.put(link.getId(), entry.getValue());
				}
				for (Link link : node.getOutLinks().values())
				{
					linkMapping.put(link.getId(), entry.getValue());
				}
			}
		}
		
		/*
		 * Parse Zones file
		 */
		log.info("Parsing zones file...");
		parsedZones = new Emme2ZonesFileParser(zonesFile).readFile();
		log.info("done.");
		
		/*
		 *  Add found mappings to the zones (fill LinkId Lists)
		 */
		log.info("Adding Links to the zones...");
		for (Entry <Id, Feature> entry : linkMapping.entrySet())
		{
			Feature zone = entry.getValue();
			int TAZ = (Integer) zone.getAttribute(3);
			
			Emme2Zone emme2Zone = parsedZones.get(TAZ);
			emme2Zone.linkIds.add(entry.getKey());
		}
		log.info("done.");
		
		/*
		 * Searching for Zones without a mapped Link
		 */
		for (Emme2Zone emme2Zone : parsedZones.values())
		{
			if (emme2Zone.linkIds.size() == 0)
			{
				String infoString = "Warning: found Zone without mapped Link! TAZ: " + emme2Zone.TAZ;
				infoString = infoString + ", Population: " + emme2Zone.POPULATION;
				infoString = infoString + ", Students: " + emme2Zone.STUDENTS;
				infoString = infoString + ", Total Employment: " + emme2Zone.EMPL_TOT;
				infoString = infoString + ", Workers: " + emme2Zone.WORKERS;
				log.info(infoString);
				
			}
		}
		Set<Feature> zonesWithLinks = new HashSet<Feature>();
		zonesWithLinks.addAll(zones);
		Iterator<Feature> iter2 = zonesWithLinks.iterator();
		while (iter2.hasNext())
		{
			Feature zone = iter2.next();
			int TAZ = (Integer) zone.getAttribute(3);
			
			Emme2Zone emme2Zone = parsedZones.get(TAZ);
			if (emme2Zone.linkIds.size() == 0) iter2.remove();
		}
		ShapeFileWriter.writeGeometries(zonesWithLinks, "../../matsim/mysimulations/telaviv/network/zonesWithLinks.shp");
	}
		
	/*
	 * Find a unique mapping for the given Link. The link is separated in the
	 * number of parts that is defined by sections. For the coordinates at the
	 * intersection points the corresponding Zone is searched. The link is
	 * mapped to the zone which contains most of the points. If no unique
	 * mapping is found (some zones contain the same amount of points) the
	 * method is called recursively with the doubled number of sections until a
	 * clear mapping is found.
	 */
	private Feature getLinkMapping(Link link, int sections, CoordinateTransformation coordinateTransformation) {
		double fromX = link.getFromNode().getCoord().getX();
		double fromY = link.getFromNode().getCoord().getY();
		double toX = link.getToNode().getCoord().getX();
		double toY = link.getToNode().getCoord().getY();

		double dX = toX - fromX;
		double dY = toY - fromY;

		double dXSection = dX / sections;
		double dYSection = dY / sections;

		Map<Feature, Integer> mapping = new TreeMap<Feature, Integer>(new FeatureComparator());

		for (int i = 0; i <= sections; i++)
		{
			double x = fromX + i * dXSection;
			double y = fromY + i * dYSection;

			Coord pointCoord = coordinateTransformation.transform(new CoordImpl(x, y));
			Point point = factory.createPoint(new Coordinate(pointCoord.getX(), pointCoord.getY()));
			
//			Point point = factory.createPoint(new Coordinate(x, y));

			for (Feature zone : zones)
			{
				Geometry polygon = zone.getDefaultGeometry();
				if (polygon.contains(point))
				{
					if (mapping.containsKey(zone))
					{
						int count = mapping.get(zone);
						mapping.put(zone, count + 1);
					} 
					else
					{
						mapping.put(zone, 1);
					}
				}
			}
		}

		Feature mappedFeature = null;
		int maxCount = 0;
		boolean unclearMapping = false;
		for (Entry<Feature, Integer> entry : mapping.entrySet())
		{
			if (entry.getValue() >= maxCount)
			{
				if (entry.getValue() == maxCount) unclearMapping = true;
				else unclearMapping = false;

				maxCount = entry.getValue();

				mappedFeature = entry.getKey();
			}
		}

		/*
		 * If we could not find a clear mapping we call the method recursive and
		 * double the number of sections. This is done until a clear mapping is
		 * found.
		 */
		if (unclearMapping)
		{
			mappedFeature = getLinkMapping(link, 2 * sections, coordinateTransformation);
		}

		return mappedFeature;
	}
	
	public boolean zoneExists(int TAZ)
	{
		return parsedZones.containsKey(TAZ);
	}
		
	public Network getNetwork()
	{
		return this.network;
	}
	
	public Map<Id, Feature> getLinkMapping()
	{
		return linkMapping;
	}
	
	public int getLinkTAZ(Id linkId)
	{
		Feature zone = linkMapping.get(linkId);
		int TAZ = (Integer) zone.getAttribute(3);
		
		return TAZ;
	}
	
	public Set<Id> getExternalNodes()
	{
		return externalNodes;
	}
	
	public Emme2Zone getParsedZone(int id)
	{
		return parsedZones.get(id);
	}
	
	public Map<Integer, Emme2Zone> getParsedZones()
	{
		return parsedZones;
	}
	
	private static class FeatureComparator implements Comparator<Feature> {
		
		@Override
		public int compare(Feature f1, Feature f2)
		{
			if (f1.getID().equals(f2.getID())) return 0;
			else return (f1.getID().compareTo(f2.getID()));
		}
	}

}
