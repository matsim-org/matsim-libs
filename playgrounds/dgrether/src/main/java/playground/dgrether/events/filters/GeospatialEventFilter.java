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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.collections.Tuple;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.dgrether.events.GeospatialEventTools;

import com.vividsolutions.jts.geom.Geometry;


/**
 * @deprecated It is not a good idea to filter events directly by geospatial location. 
 * @author dgrether
 *
 */
@Deprecated
public class GeospatialEventFilter implements EventFilter {

	private Network network;
	private List<Tuple<CoordinateReferenceSystem, SimpleFeature>> featureTuples;
	private CoordinateReferenceSystem networkCrs;
	private List<Geometry> transformedFeatureGeometries;
	private GeospatialEventTools geospatialTools;
	
	public GeospatialEventFilter(Network network){
		this.geospatialTools = new GeospatialEventTools(network, networkCrs);
		this.featureTuples = new ArrayList<Tuple<CoordinateReferenceSystem, SimpleFeature>>();
		this.transformedFeatureGeometries = new ArrayList<Geometry>();
	}
	
	public GeospatialEventFilter(Network network, CoordinateReferenceSystem networkCrs){
		this(network);
		this.networkCrs = networkCrs;
	}
	
	public void addCrsFeatureTuple(Tuple<CoordinateReferenceSystem, SimpleFeature> featureTuple) {
		this.geospatialTools.addCrsFeatureTuple(featureTuple);
	}

	private boolean containsLink(Id linkId) {
		return this.geospatialTools.doNetworkAndFeaturesContainLink(linkId);
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
		} else if (event instanceof VehicleEntersTrafficEvent) {
			VehicleEntersTrafficEvent e = (VehicleEntersTrafficEvent) event;
			Id linkId = e.getLinkId();
			return containsLink(linkId);
		} else if (event instanceof PersonDepartureEvent) {
			PersonDepartureEvent e = (PersonDepartureEvent) event;
			Id linkId = e.getLinkId();
			return containsLink(linkId);
		} else if (event instanceof PersonArrivalEvent) {
			PersonArrivalEvent e = (PersonArrivalEvent) event;
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




}
