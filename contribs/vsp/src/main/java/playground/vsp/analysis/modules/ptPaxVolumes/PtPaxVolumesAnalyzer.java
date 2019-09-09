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

package playground.vsp.analysis.modules.ptPaxVolumes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ServiceConfigurationError;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.opengis.feature.simple.SimpleFeature;

import playground.vsp.analysis.modules.AbstractAnalysisModule;

/**
 * @author droeder
 */
public class PtPaxVolumesAnalyzer extends AbstractAnalysisModule{

	private PtPaxVolumesHandler handler;
	private Scenario sc;
	private Map<Id<TransitLine>, Collection<SimpleFeature>> lineId2features;
	private final String targetCoordinateSystem;

	public PtPaxVolumesAnalyzer(Scenario sc, Double interval, String targetCoordinateSystem) {
		super(PtPaxVolumesAnalyzer.class.getSimpleName());
		this.handler =  new PtPaxVolumesHandler(interval);
		this.sc =  sc;
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
		this.lineId2features = new HashMap<>();
		for(TransitLine l: this.sc.getTransitSchedule().getTransitLines().values()){
			this.lineId2features.put(l.getId(), getTransitLineFeatures(l, this.targetCoordinateSystem));
		}
		this.lineId2features.put(Id.create("all", TransitLine.class), getAll(this.targetCoordinateSystem));
	}
	
	private Collection<SimpleFeature> getAll(String targetCoordinateSystem) {
		SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
		b.setCRS(MGC.getCRS(targetCoordinateSystem));
		b.setName("all");
		b.add("the_geom", LineString.class);
		b.add("line", String.class);
		b.add("linkId", String.class);
		b.add("paxTotal", Double.class);
		for(int i = 0; i < this.handler.getMaxInterval(); i++){
			b.add("pax_" + String.valueOf(i), Double.class);
		}
		SimpleFeatureBuilder builder = new SimpleFeatureBuilder(b.buildFeatureType());
		
		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
		
		Object[] featureAttribs;
		for(Link link: this.sc.getNetwork().getLinks().values()){
			featureAttribs = getFeatureAll(link,new Object[4 + this.handler.getMaxInterval()]);
			// skip links without passengers
			if(featureAttribs == null) continue;
			try {
				features.add(builder.buildFeature(null, featureAttribs));
			} catch (IllegalArgumentException e1) {
				e1.printStackTrace();
			}
		}
		return features;
	}

	private Object[] getFeatureAll(Link link, Object[] objects) {
		Double paxValue = this.handler.getPaxCountForLinkId(link.getId());
		if(paxValue <= 0) return null;
		Coordinate[] coord =  new Coordinate[2];
		coord[0] = new Coordinate(link.getFromNode().getCoord().getX(), link.getFromNode().getCoord().getY(), 0.);
		coord[1] = new Coordinate(link.getToNode().getCoord().getX(), link.getToNode().getCoord().getY(), 0.);
		
		objects[0] = new GeometryFactory().createLineString(coord);
		objects[1] = "all";
		objects[2] = link.getId().toString();
		objects[3] = paxValue;
		for(int i = 0; i < this.handler.getMaxInterval(); i++){
			objects[4 + i] = this.handler.getPaxCountForLinkId(link.getId(), i+1);
		}
		return objects;
	}

	private Collection<SimpleFeature> getTransitLineFeatures(TransitLine l, String targetCoordinateSystem) {
		SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
		b.setCRS(MGC.getCRS(targetCoordinateSystem));
		b.setName("the_geom");
		b.add("location", LineString.class);
		b.add("line", String.class);
		b.add("linkId", String.class);
		b.add("paxTotal", Double.class);
		for (int i = 0; i < this.handler.getMaxInterval(); i++){
			b.add("pax_" + String.valueOf(i), Double.class);
		}
		
		SimpleFeatureBuilder builder = new SimpleFeatureBuilder(b.buildFeatureType());
		
		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
		
		Object[] featureAttribs;
		for(Link link: this.sc.getNetwork().getLinks().values()){
			featureAttribs = getLinkFeatureAttribs(link,l.getId(),  new Object[4 + this.handler.getMaxInterval()]);
			// skip links without passengers
			if (featureAttribs == null) continue;
			try {
				features.add(builder.buildFeature(link.getId().toString(), featureAttribs));
			} catch (IllegalArgumentException e1) {
				e1.printStackTrace();
			}
		}
		 return features;
	}

	/**
	 * @param link
	 * @param lineId
	 * @param objects
	 * @return
	 */
	private Object[] getLinkFeatureAttribs(Link link, Id<TransitLine> lineId, Object[] objects) {
		Double paxValue = this.handler.getPaxCountForLinkId(link.getId(), lineId);
		if(paxValue <= 0) return null;
		Coordinate[] coord =  new Coordinate[2];
		coord[0] = new Coordinate(link.getFromNode().getCoord().getX(), link.getFromNode().getCoord().getY(), 0.);
		coord[1] = new Coordinate(link.getToNode().getCoord().getX(), link.getToNode().getCoord().getY(), 0.);
		
		objects[0] = new GeometryFactory().createLineString(coord);
		objects[1] = lineId.toString();
		objects[2] = link.getId().toString();
		objects[3] = paxValue;
		for(int i = 0; i < this.handler.getMaxInterval(); i++){
			objects[4 + i] = this.handler.getPaxCountForLinkId(link.getId(), lineId, i+1);
		}
		return objects;
	}

	@Override
	public void writeResults(String outputFolder) {
		for(Entry<Id<TransitLine>, Collection<SimpleFeature>> ee: this.lineId2features.entrySet()){
			try{
				if(ee.getValue().size() <= 0) continue;
				ShapeFileWriter.writeGeometries(ee.getValue(), outputFolder + ee.getKey().toString()+ ".shp");
			}catch(ServiceConfigurationError e){
				e.printStackTrace();
			}
		}
	}
}

