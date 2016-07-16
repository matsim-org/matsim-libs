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
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.opengis.feature.simple.SimpleFeature;

import playground.vsp.analysis.modules.AbstractAnalysisModule;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;

/**
 * @author aneumann, droeder
 */
public class TransitSchedule2Shp extends AbstractAnalysisModule{
	
	private static final Logger log = Logger.getLogger(TransitSchedule2Shp.class);
	
	private Network network;
	private TransitSchedule schedule;
	private final String targetCoordinateSystem;

	public TransitSchedule2Shp(Scenario sc, String targetCoordinateSystem) {
		super(TransitSchedule2Shp.class.getSimpleName());
		this.schedule =  sc.getTransitSchedule();
		this.network =  sc.getNetwork();
		this.targetCoordinateSystem = targetCoordinateSystem;
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
		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
		// write a shape per line
		for(TransitLine transitLine: this.schedule.getTransitLines().values()){
			if(transitLine.getRoutes().isEmpty()){
				log.warn("can not create a shapefile for transitline " + transitLine.getId() + ", because the line contains no routes...");
				continue;
			}
			Collection<SimpleFeature> temp = getTransitLineFeatures(transitLine, this.targetCoordinateSystem);
			features.addAll(temp);
			try{
				ShapeFileWriter.writeGeometries(temp, outputFolder + transitLine.getId().toString() + ".shp");
			}catch(ServiceConfigurationError e){
				e.printStackTrace();
			}
		}
		// write a complete shape 
		if(features.isEmpty()){
			log.error("the transitschedule seems to be empty. No features are created, thus no shapefile will be written...");
			return;
		}
		try{
			ShapeFileWriter.writeGeometries(features, outputFolder + "allLines.shp");
		}catch(ServiceConfigurationError e){
			e.printStackTrace();
		}
	}
	
	private Collection<SimpleFeature> getTransitLineFeatures(TransitLine transitLine, String targetCoordinateSystem) {
		SimpleFeatureTypeBuilder simpleFeatureBuilder = new SimpleFeatureTypeBuilder();
		simpleFeatureBuilder.setCRS(MGC.getCRS(targetCoordinateSystem));
		simpleFeatureBuilder.setName("transitLineFeature");
		simpleFeatureBuilder.add("the_geom", LineString.class);
		simpleFeatureBuilder.add("line", String.class);
		simpleFeatureBuilder.add("route", String.class);
		simpleFeatureBuilder.add("from", String.class);
		simpleFeatureBuilder.add("via", String.class);
		simpleFeatureBuilder.add("to", String.class);
		simpleFeatureBuilder.add("mode", String.class);
		simpleFeatureBuilder.add("tourLength", Double.class);
		simpleFeatureBuilder.add("tourTime", String.class);
		simpleFeatureBuilder.add("nVeh", Integer.class);
		simpleFeatureBuilder.add("headway", String.class);
		simpleFeatureBuilder.add("nDeparture", Integer.class);
		simpleFeatureBuilder.add("avgSpeed", Double.class);
		simpleFeatureBuilder.add("fsFactor", Double.class);
		simpleFeatureBuilder.add("firstDep", String.class);
		simpleFeatureBuilder.add("lastDep", String.class);
		simpleFeatureBuilder.add("timeOper", String.class);
		SimpleFeatureBuilder builder = new SimpleFeatureBuilder(simpleFeatureBuilder.buildFeatureType());
		
		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
		
		Object[] routeFeatureAttributes;
		for(TransitRoute transitRoute: transitLine.getRoutes().values()){
			routeFeatureAttributes = getRouteFeatureAttribs(transitRoute, transitLine.getId(), new Object[17]);
			try {
				features.add(builder.buildFeature(transitRoute.getId().toString(), routeFeatureAttributes));
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			}
		}
		 return features;
	}

	private Object[] getRouteFeatureAttribs(TransitRoute transitRoute, Id<TransitLine> lineId, Object[] routeFeatureAttributes ) {
		
		List<Coordinate> coords = new ArrayList<Coordinate>();

		// Create the polyline (lineString)
		
		// add the startLink
		Coord toNode = this.network.getLinks().get(transitRoute.getRoute().getStartLinkId()).getToNode().getCoord();
		coords.add(new Coordinate(toNode.getX(), toNode.getY(), 0.));
		
		// add the routeLinks
		for(Id<Link> linkId : transitRoute.getRoute().getLinkIds()){
			toNode = this.network.getLinks().get(linkId).getToNode().getCoord();
			coords.add(new Coordinate(toNode.getX(), toNode.getY(), 0.));
		}
		
		//add the endlink
		toNode = this.network.getLinks().get(transitRoute.getRoute().getEndLinkId()).getToNode().getCoord();
		coords.add(new Coordinate(toNode.getX(), toNode.getY(), 0.));
		
		// create an array
		Coordinate[] coord = new Coordinate[coords.size()];
		coord = coords.toArray(coord);
		LineString lineString = new GeometryFactory().createLineString(new CoordinateArraySequence(coord));

		// get the content
		TransitRouteData transitRouteData = new TransitRouteData(this.network, transitRoute);
		
		routeFeatureAttributes[0] = lineString;
		routeFeatureAttributes[1] = lineId.toString();
		routeFeatureAttributes[2] = transitRoute.getId();
		routeFeatureAttributes[3] = transitRouteData.getFirstStopName();
		routeFeatureAttributes[4] = transitRouteData.getViaStopName();
		routeFeatureAttributes[5] = transitRouteData.getLastStopName();
		routeFeatureAttributes[6] = transitRoute.getTransportMode();
		routeFeatureAttributes[7] = transitRouteData.getDistance();
		routeFeatureAttributes[8] = Time.writeTime(transitRouteData.getTravelTime(), Time.TIMEFORMAT_HHMMSS);
		routeFeatureAttributes[9] = transitRouteData.getNVehicles();
		routeFeatureAttributes[10] = Time.writeTime(transitRouteData.getHeadway(), Time.TIMEFORMAT_HHMMSS);
		routeFeatureAttributes[11] = transitRouteData.getNDepartures();
		routeFeatureAttributes[12] = transitRouteData.getAvgSpeed() * 3.6;
		routeFeatureAttributes[13] = transitRouteData.getFreeSpeedFactor();
		routeFeatureAttributes[14] = Time.writeTime(transitRouteData.getFirstDeparture(), Time.TIMEFORMAT_HHMMSS);
		routeFeatureAttributes[15] = Time.writeTime(transitRouteData.getLastDeparture(), Time.TIMEFORMAT_HHMMSS);
		routeFeatureAttributes[16] = Time.writeTime(transitRouteData.getOperatingDuration(), Time.TIMEFORMAT_HHMMSS);
		return routeFeatureAttributes;
	}
}
