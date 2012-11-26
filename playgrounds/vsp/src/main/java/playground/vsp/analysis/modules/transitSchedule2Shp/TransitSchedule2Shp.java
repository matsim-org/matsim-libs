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
package playground.vsp.analysis.modules.transitSchedule2Shp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ServiceConfigurationError;

import org.apache.log4j.Logger;
import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeBuilder;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import playground.vsp.analysis.modules.AbstractAnalyisModule;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;

/**
 * @author droeder
 *
 */
public class TransitSchedule2Shp extends AbstractAnalyisModule{

	@SuppressWarnings("unused")
	private static final Logger log = Logger
			.getLogger(TransitSchedule2Shp.class);
	private Network network;
	private TransitSchedule schedule;

	public TransitSchedule2Shp(Scenario sc) {
		super(TransitSchedule2Shp.class.getSimpleName());
		this.schedule =  sc.getTransitSchedule();
		this.network =  sc.getNetwork();
	}

	@Override
	public List<EventHandler> getEventHandler() {
		
		return new ArrayList<EventHandler>();
	}

	@Override
	public void preProcessData() {
		
	}

	@Override
	public void postProcessData() {
		
	}

	@Override
	public void writeResults(String outputFolder) {
		Collection<Feature> features = new ArrayList<Feature>();
		// write a shape per line
		for(TransitLine l: this.schedule.getTransitLines().values()){
			Collection<Feature> temp = getTransitLineFeatures(l);
			features.addAll(temp);
			try{
				ShapeFileWriter.writeGeometries(temp, outputFolder + l.getId().toString() + ".shp");
			}catch(ServiceConfigurationError e){
				e.printStackTrace();
			}
		}
		// write a complete shape 
		try{
			ShapeFileWriter.writeGeometries(features, outputFolder + "allLines.shp");
		}catch(ServiceConfigurationError e){
			e.printStackTrace();
		}
	}
	
	
	private Collection<Feature> getTransitLineFeatures(TransitLine l){
		AttributeType[] attribs = new AttributeType[4];
		attribs[0] = AttributeTypeFactory.newAttributeType("LineString", LineString.class, true, null, null, MGC.getCRS(TransformationFactory.WGS84_UTM35S));
		attribs[1] = AttributeTypeFactory.newAttributeType("line", String.class);
		attribs[2] = AttributeTypeFactory.newAttributeType("route", String.class);
		attribs[3] =  AttributeTypeFactory.newAttributeType("mode", String.class);
		
		FeatureType featureType = null ;
		try {
			featureType = FeatureTypeBuilder.newFeatureType(attribs, l.getId().toString());
		} catch (FactoryRegistryException e) {
			e.printStackTrace();
		} catch (SchemaException e) {
			e.printStackTrace();
		}
		
		Collection<Feature> features = new ArrayList<Feature>();
		
		Object[] featureAttribs;
		for(TransitRoute r: l.getRoutes().values()){
			featureAttribs = getRouteFeatureAttribs(r,l.getId(),  new Object[4]);
			try {
				features.add(featureType.create(featureAttribs));
			} catch (IllegalAttributeException e1) {
				e1.printStackTrace();
			}
		}
		 return features;
	}
	/**
	 * @param r
	 * @return
	 */
	private Object[] getRouteFeatureAttribs(TransitRoute r, Id lineId, Object[] o ) {
		List<Coordinate> coords = new ArrayList<Coordinate>();

		// #### create the lineString
		// add the startLink
		Coord toNode = this.network.getLinks().get(r.getRoute().getStartLinkId()).getToNode().getCoord();
		coords.add(new Coordinate(toNode.getX(), toNode.getY(), 0.));
		// add the routeLinks
		for(Id linkId : r.getRoute().getLinkIds()){
			toNode = this.network.getLinks().get(linkId).getToNode().getCoord();
			coords.add(new Coordinate(toNode.getX(), toNode.getY(), 0.));
		}
		//add the endlink
		toNode = this.network.getLinks().get(r.getRoute().getEndLinkId()).getToNode().getCoord();
		coords.add(new Coordinate(toNode.getX(), toNode.getY(), 0.));
		// create an array
		Coordinate[] coord = new Coordinate[coords.size()];
		coord = coords.toArray(coord);
		LineString ls = new GeometryFactory().createLineString(new CoordinateArraySequence(coord));
		o[0] = ls;
		o[1] = lineId.toString();
		o[2] = r.getId();
		o[3] = r.getTransportMode();
		return o;
	}
	
}

