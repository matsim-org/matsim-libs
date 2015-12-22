/* *********************************************************************** *
 * project: org.matsim.*
 * ConvertOsmToMatsim.java
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
package playground.jjoubert.projects.capeTownFreight;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import playground.southafrica.utilities.Header;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

/**
 * @author jwjoubert
 *
 */
public class LinkCounterByHour implements LinkEnterEventHandler, VehicleEntersTrafficEventHandler{
	final private static Logger LOG = Logger.getLogger(LinkCounterByHour.class);
	Map<Id<Link>, Map<String,Integer>> map = new HashMap<Id<Link>, Map<String,Integer>>();

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(LinkCounterByHour.class.toString(), args);
		
		String events = args[0];
		String network = args[1];
		String output = args[2];
		String shapefile = args[3];
		
		EventsManager manager = EventsUtils.createEventsManager();
		LinkCounterByHour linkcounter = new LinkCounterByHour();
		manager.addHandler(linkcounter );
		MatsimEventsReader mer = new MatsimEventsReader(manager );
		mer.readFile(events);
		
		linkcounter.writeHourlyLinkCounts(network, output);

		SimpleFeatureCollection collection = linkcounter.createFeatureCollection(network);
		try {
			writeShapefile(shapefile, collection);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write shapefile.");
		}

		Header.printFooter();
	}
	
	public LinkCounterByHour() {
	}
	
	public SimpleFeatureCollection createFeatureCollection(String network){
		LOG.info("Parsing network from " + network);
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario ).parse(network);
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84_SA_Albers", "WGS84");
		GeometryFactory gf = new GeometryFactory();

		LOG.info("Creating feature collection for shapefile...");
		DefaultFeatureCollection collection = new DefaultFeatureCollection();
		
		Counter counter = new Counter("   feature # ");
		for(Id<Link> linkId : map.keySet()){
			SimpleFeatureBuilder builder = new SimpleFeatureBuilder(createFeatureType());
			
			/* Build line segment. */
			Link link = scenario.getNetwork().getLinks().get(linkId);
			Coord o = link.getFromNode().getCoord();
			Coord d = link.getToNode().getCoord();
			Coord oWgs = ct.transform(o);
			Coord dWgs = ct.transform(d);
			Coordinate oo = new Coordinate(oWgs.getX(), oWgs.getY());
			Coordinate dd = new Coordinate(dWgs.getX(), dWgs.getY());
			Coordinate[] coordinates = {oo, dd};
			LineString ls = gf.createLineString(coordinates);
			builder.add(ls);
			builder.add(linkId.toString());
			
			/* Add hourly values. */
			int linkTotal = 0;
			Map<String,Integer> thisMap = map.get(linkId);
			for(int i = 0; i < 24; i++){
				String hour = String.format("%02d", i);
				int hourlyCount = thisMap.get(hour);
				linkTotal += hourlyCount;
				builder.add(hourlyCount);
			}
			builder.add(linkTotal);

			/* Build and add the feature. */
			SimpleFeature feature = builder.buildFeature(linkId.toString());
			collection.add(feature);
			counter.incCounter();
		}
		counter.printCounter();
		LOG.info("Done creating feature collection.");
		
		return collection;
	}
	
	private static SimpleFeatureType createFeatureType(){
		SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
		builder.setName("Link");
		builder.setCRS(DefaultGeographicCRS.WGS84);
		
		/* Add the attributes in order. */
//		builder.add("Link", LineString.class);
		builder.add("the_geom", LineString.class);
		builder.length(6).add("Id", String.class);
		builder.length(6).add("h00", Integer.class);
		builder.length(6).add("h01", Integer.class);
		builder.length(6).add("h02", Integer.class);
		builder.length(6).add("h03", Integer.class);
		builder.length(6).add("h04", Integer.class);
		builder.length(6).add("h05", Integer.class);
		builder.length(6).add("h06", Integer.class);
		builder.length(6).add("h07", Integer.class);
		builder.length(6).add("h08", Integer.class);
		builder.length(6).add("h09", Integer.class);
		builder.length(6).add("h10", Integer.class);
		builder.length(6).add("h11", Integer.class);
		builder.length(6).add("h12", Integer.class);
		builder.length(6).add("h13", Integer.class);
		builder.length(6).add("h14", Integer.class);
		builder.length(6).add("h15", Integer.class);
		builder.length(6).add("h16", Integer.class);
		builder.length(6).add("h17", Integer.class);
		builder.length(6).add("h18", Integer.class);
		builder.length(6).add("h19", Integer.class);
		builder.length(6).add("h20", Integer.class);
		builder.length(6).add("h21", Integer.class);
		builder.length(6).add("h22", Integer.class);
		builder.length(6).add("h23", Integer.class);
		builder.length(6).add("total", Integer.class);
		
		/* build the type. */
		final SimpleFeatureType ZONE = builder.buildFeatureType();
		return ZONE;
	}

	
	private static void writeShapefile(String filename, SimpleFeatureCollection collection) throws IOException{
		LOG.info("Writing shapefile to " + filename);
		File file = new File(filename);
		ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
		Map<String, Serializable> params = new HashMap<String, Serializable>();
		params.put("url", file.toURI().toURL());
		params.put("create spatial index", Boolean.TRUE);
		
		ShapefileDataStore dataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
		dataStore.createSchema(createFeatureType());
		
		Transaction transaction = new DefaultTransaction("create");
		
		String typeName = dataStore.getTypeNames()[0];
		SimpleFeatureSource featureSource = dataStore.getFeatureSource(typeName);
		if(featureSource instanceof SimpleFeatureSource){
			SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
			featureStore.setTransaction(transaction);
			try{
				featureStore.addFeatures(collection);
				transaction.commit();
			} finally{
				transaction.close();
			}
		} else{
			System.exit(1);
		}
	}
	
	
	public void writeHourlyLinkCounts(String network, String output){
		LOG.info("Parsing network from " + network);
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario ).parse(network);
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84_SA_Albers", "WGS84");
		LOG.info("Writing link counts to " + output);
		
		BufferedWriter bw = IOUtils.getBufferedWriter(output);
		try{
			/* Write header. */
			bw.write("linkId,oX,oY,oLat,oLon,dX,dY,dLat,dLon,LengthMeter");
			for(int i = 0; i < 24; i++){
				bw.write(String.format(",h%02d", i));
			}
			bw.newLine();
			
			/* Write the link counts */
			for(Id<Link> link : map.keySet()){
				Link theLink = scenario.getNetwork().getLinks().get(link);
				Coord o = theLink.getFromNode().getCoord();
				Coord d = theLink.getToNode().getCoord();
				Coord oWgs = ct.transform(o);
				Coord dWgs = ct.transform(d);

				bw.write(String.format("%s,%.0f,%.0f,%.6f,%.6f,%.0f,%.0f,%.6f,%.6f,%.2f", 
						link.toString(),
						o.getX(), o.getY(), oWgs.getX(), oWgs.getY(),
						d.getX(), d.getY(), dWgs.getX(), dWgs.getY(),
						theLink.getLength()));
				
				Map<String,Integer> thisMap = map.get(link);
				for(int i = 0; i < 24; i++){
					String hour = String.format("%02d", i);
					bw.write(String.format(",%d", thisMap.get(hour)));
				}
				bw.newLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Canot write to " + output);
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + output);
			}
		}
		
		LOG.info("Done writing link counts.");
	}

	@Override
	public void reset(int iteration) {
		map = new HashMap<>();
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		this.handleLinkAndTime(event.getLinkId(), event.getTime());
	}
	
	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		this.handleLinkAndTime(event.getLinkId(), event.getTime());
	}
	
	private void handleLinkAndTime(Id<Link> linkId, double time){
		while(time >= 24*60*60){
			time -= 24*60*60;
		}
		String hour = String.format("%02d", (int)Math.floor(time/3600.0));
		if(!map.containsKey(linkId)){
			Map<String,Integer> thisMap = new TreeMap<String, Integer>();
			for(int i = 0; i < 24; i++){
				String s = String.format("%02d", i);
				thisMap.put(s, 0);
			}
			map.put(linkId, thisMap);
		}
		
		/*FIXME Remove after debugging. */
		Map<String, Integer> aMap = map.get(linkId);
		if(aMap == null){
			LOG.debug("Oops... NULL Map for link " + linkId.toString());
		} else if(aMap.get(hour) == null){
			LOG.debug("Oops... NULL counter for link " + linkId.toString() + ", hour " + hour);
		}
		
		
		
		
		
		int oldValue = map.get(linkId).get(hour);
		map.get(linkId).put(hour, oldValue+1);
	}

}
