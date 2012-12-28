/* *********************************************************************** *
 * project: org.matsim.*
 * GeospatialEventFilter
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
package playground.dgrether.events.filters;

import java.util.ArrayList;
import java.util.List;

import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.AgentWait2LinkEvent;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;


/**
 * TODO make class use GeospatialEventTools
 * @author dgrether
 *
 */
public class GeospatialEventFilter implements EventFilter {

	private Network network;
	private List<Tuple<CoordinateReferenceSystem, SimpleFeature>> featureTuples;
	private CoordinateReferenceSystem networkCrs;
	private List<Geometry> transformedFeatureGeometries;
	
	public GeospatialEventFilter(Network network){
		this.network = network;
		this.featureTuples = new ArrayList<Tuple<CoordinateReferenceSystem, SimpleFeature>>();
		this.transformedFeatureGeometries = new ArrayList<Geometry>();
	}
	
	public GeospatialEventFilter(Network network, CoordinateReferenceSystem networkCrs){
		this(network);
		this.networkCrs = networkCrs;
	}
	
	public void addCrsFeatureTuple(Tuple<CoordinateReferenceSystem, SimpleFeature> featureTuple) {
		this.featureTuples.add(featureTuple);
		if ( !(this.networkCrs == null)){
			MathTransform transformation;
			try {
				transformation = CRS.findMathTransform(featureTuple.getFirst(), this.networkCrs, true);
				Geometry transformedFeatureGeometry = JTS.transform((Geometry) featureTuple.getSecond().getDefaultGeometry(), transformation);
				this.transformedFeatureGeometries.add(transformedFeatureGeometry);
			} catch (FactoryException e) {
				e.printStackTrace();
			} catch (TransformException e) {
				e.printStackTrace();
			}
		}
		else {
			this.transformedFeatureGeometries.add((Geometry) featureTuple.getSecond().getDefaultGeometry());
		}
	}

	
	private boolean doFeaturesContainCoordinate(Coordinate coordinate) {
		Geometry linkPoint = MGC.coordinate2Point(coordinate);
		for (Geometry featureGeo : this.transformedFeatureGeometries){
				return featureGeo.contains(linkPoint);
		}
		return false;
	}
	
	@Override
	public boolean doProcessEvent(Event event) {
		if (event instanceof LinkEnterEvent) {
			LinkEnterEvent e = (LinkEnterEvent) event;
			Id linkId = e.getLinkId();
			return containsLink(linkId);
		} else if (event instanceof LinkLeaveEvent) {
			LinkLeaveEvent e = (LinkLeaveEvent) event;
			Id linkId = e.getLinkId();
			return containsLink(linkId);
		} else if (event instanceof AgentWait2LinkEvent) {
			AgentWait2LinkEvent e = (AgentWait2LinkEvent) event;
			Id linkId = e.getLinkId();
			return containsLink(linkId);
		} else if (event instanceof AgentDepartureEvent) {
			AgentDepartureEvent e = (AgentDepartureEvent) event;
			Id linkId = e.getLinkId();
			return containsLink(linkId);
		} else if (event instanceof AgentArrivalEvent) {
			AgentArrivalEvent e = (AgentArrivalEvent) event;
			Id linkId = e.getLinkId();
			return containsLink(linkId);
		} else if (event instanceof ActivityStartEvent) {
			ActivityStartEvent e = (ActivityStartEvent) event;
			Id linkId = e.getLinkId();
			return containsLink(linkId);
		} else if (event instanceof ActivityEndEvent) {
			ActivityEndEvent e = (ActivityEndEvent) event;
			Id linkId = e.getLinkId();
			return containsLink(linkId);
		} else {
			return false;
		}
	}

	private boolean containsLink(Id linkId) {
		Link link = this.network.getLinks().get(linkId);
		if (link != null){
			Coordinate coordinate = MGC.coord2Coordinate(link.getCoord());
			return this.doFeaturesContainCoordinate(coordinate);
		}
		return false;
	}


}
