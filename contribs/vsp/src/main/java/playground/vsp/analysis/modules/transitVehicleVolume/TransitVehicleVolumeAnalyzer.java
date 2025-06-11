/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.vsp.analysis.modules.transitVehicleVolume;

import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ServiceConfigurationError;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.impl.CoordinateArraySequence;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.GeoFileWriter;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.Volume;

import playground.vsp.analysis.modules.AbstractAnalysisModule;

/**
 * @author droeder
 *
 */
public class TransitVehicleVolumeAnalyzer extends AbstractAnalysisModule {

	private static final Logger log = LogManager.getLogger(TransitVehicleVolumeAnalyzer.class);
	private Scenario sc;
	private TransitVehicleVolumeHandler handler;
	private HashMap<String, Map<Id, Double>> mode2Link2Total;
	private final String targetCoordinateSystem;

	/**
	 * creates a shapefiles (per pt-mode) containing the links used by pt-vehicles.
	 * Every single Geometry provides fields containing informations about the number of vehicles
	 * per interval and in total.
	 * @param sc, the scenario containing the links
	 * @param interval, the interval (normally in seconds)
	 */
	public TransitVehicleVolumeAnalyzer(Scenario sc, Double interval, String targetCoordinateSystem) {
		super(TransitVehicleVolumeAnalyzer.class.getSimpleName());
		this.sc = sc;
		this.handler = new TransitVehicleVolumeHandler(sc.getTransitSchedule(), interval);
		this.targetCoordinateSystem = targetCoordinateSystem;
	}

	@Override
	public List<EventHandler> getEventHandler() {
		List<EventHandler> handler = new ArrayList<EventHandler>();
		handler.add(this.handler);
		return handler;
	}

	@Override
	public void preProcessData() {
		//do nothing
	}

	@Override
	public void postProcessData() {
		this.createTotals();
	}

	private void createTotals() {
		// count totals
		this.mode2Link2Total = new HashMap<String, Map<Id, Double>>();
		Double total;
		for(Entry<String, Counts<Link>> e: this.handler.getMode2Counts().entrySet()){
			Map<Id, Double> temp = new HashMap<Id, Double>();
			for(Count<Link> c: e.getValue().getCounts().values()){
				total = new Double(0.);
				for(Volume v: c.getVolumes().values()){
					total += v.getValue();
				}
				temp.put(c.getId(), total);
			}
			this.mode2Link2Total.put(e.getKey(), temp);
		}
	}

	@Override
	public void writeResults(String outputFolder) {
		for(Entry<String, Counts<Link>> e: this.handler.getMode2Counts().entrySet()){
			writeModeShape(e.getKey(), e.getValue(), this.mode2Link2Total.get(e.getKey()), outputFolder + e.getKey() + ".shp", this.targetCoordinateSystem);
		}
	}

	private void writeModeShape(String name, Counts<Link> counts, Map<Id, Double> mode2Total, String file, String targetCoordinateSystem){
		SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
		b.setCRS(MGC.getCRS(targetCoordinateSystem));
		b.setName(name);
		b.add("location", LineString.class);
		b.add("name", String.class);
		b.add("total", Double.class);
		for(int  i = 0 ; i< this.handler.getMaxTimeSlice(); i++){
			b.add(String.valueOf(i), Double.class);
		}
		SimpleFeatureBuilder builder = new SimpleFeatureBuilder(b.buildFeatureType());

		Collection<SimpleFeature> features = new ArrayList<>();

		Object[] featureAttribs;
		for(Count c: counts.getCounts().values()){
			featureAttribs = new Object[2 + this.handler.getMaxTimeSlice() + 1];
			//create linestring from link
			Link l = this.sc.getNetwork().getLinks().get(Id.create(c.getCsLabel(), Link.class));
			if(l == null){
				log.debug("can not find link " + c.getId());
				log.debug("links #" + this.sc.getNetwork().getLinks().size());
				continue;
			}
			Coordinate[] coord = new Coordinate[2];
			coord[0] = new Coordinate(l.getFromNode().getCoord().getX(), l.getFromNode().getCoord().getY(), 0.);
			coord[1] = new Coordinate(l.getToNode().getCoord().getX(), l.getToNode().getCoord().getY(), 0.);
			LineString ls = new GeometryFactory().createLineString(new CoordinateArraySequence(coord));
			//###
			featureAttribs[0] = ls;
			featureAttribs[1] = c.getId().toString();
			featureAttribs[2] = mode2Total.get(c.getId());
			for(int i = 0; i < this.handler.getMaxTimeSlice() ; i++){
				if(c.getVolume(i+1) == null){
					featureAttribs[3 + i] = 0.;
				}else{
					featureAttribs[3 + i] = c.getVolume(i+1).getValue();
				}
			}
			try {
				features.add(builder.buildFeature(l.getId().toString(), featureAttribs));
			} catch (IllegalArgumentException e1) {
				e1.printStackTrace();
			}
		}
		try{
			GeoFileWriter.writeGeometries(features, file);
		}catch(ServiceConfigurationError e){
			e.printStackTrace();
		} catch (UncheckedIOException e) {
			e.printStackTrace();
			log.info("No entries for " + name + ". Thus, no file written to " + file);
		}
	}
}

