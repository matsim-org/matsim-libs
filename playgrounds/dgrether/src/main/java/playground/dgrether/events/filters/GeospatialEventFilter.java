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

import org.geotools.feature.Feature;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.AgentEvent;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.spatialschema.geometry.MismatchedDimensionException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;


/**
 * @author dgrether
 *
 */
public class GeospatialEventFilter implements EventFilter {

	private Network network;
	private List<Tuple<CoordinateReferenceSystem, Feature>> featureTuples;
	private CoordinateReferenceSystem networkCrs;

	public GeospatialEventFilter(Network network){
		this.network = network;
		this.featureTuples = new ArrayList<Tuple<CoordinateReferenceSystem, Feature>>();
	}
	
	public GeospatialEventFilter(Network network, CoordinateReferenceSystem networkCrs){
		this(network);
		this.networkCrs = networkCrs;
	}
	
	private boolean isCoordInFeatures(Coordinate coordinate) {
		for (Tuple<CoordinateReferenceSystem, Feature> t : this.featureTuples){
			Geometry geometry = MGC.coordinate2Point(coordinate);
			if (!(this.networkCrs == null)) {
				MathTransform transformation;
				try {
					transformation = CRS.findMathTransform(t.getFirst(), this.networkCrs, true);
					geometry = JTS.transform(geometry, transformation);
					
				} catch (FactoryException e) {
					e.printStackTrace();
				} catch (MismatchedDimensionException e) {
					e.printStackTrace();
				} catch (TransformException e) {
					e.printStackTrace();
				}
				return t.getSecond().getDefaultGeometry().contains(geometry);
			}
		}
		return false;
	}
	
	@Override
	public boolean doProcessEvent(Event event) {
		if (event instanceof AgentEvent) {
			AgentEvent e = (AgentEvent) event;
			Link link = this.network.getLinks().get(e.getLinkId());
			Coordinate coordinate = MGC.coord2Coordinate(link.getCoord());
			return this.isCoordInFeatures(coordinate);
		}
		return false;
	}

	public void addCrsFeatureTuple(Tuple<CoordinateReferenceSystem, Feature> featureTuple) {
		this.featureTuples.add(featureTuple);
	}

}
