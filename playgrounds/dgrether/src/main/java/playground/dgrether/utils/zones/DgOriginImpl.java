/* *********************************************************************** *
 * project: org.matsim.*
 * DgOriginImpl
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
package playground.dgrether.utils.zones;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.geometry.geotools.MGC;

import com.vividsolutions.jts.geom.Coordinate;


/**
 * @author dgrether
 *
 */
public abstract class DgOriginImpl implements DgOrigin {

	private Map<DgZone, Double> toZoneRelations = new HashMap<DgZone, Double>();
	private Map<Link, Double> toLinkRelations = new HashMap<Link, Double>();


	@Override
	public void incrementDestinationZoneTrips(DgZone toZone) {
		if (! toZoneRelations.containsKey(toZone)){
			this.toZoneRelations.put(toZone, 0.0);
		}
		Double count = this.toZoneRelations.get(toZone);
		this.toZoneRelations.put(toZone, (count + 1));
	}
	
	@Override
	public void incrementDestinationLinkTrips(Link endLink) {
		if (! toLinkRelations.containsKey(endLink)){
			this.toLinkRelations.put(endLink, 0.0);
		}
		Double count = this.toLinkRelations.get(endLink);
		this.toLinkRelations.put(endLink, (count + 1));
	}
	
	public Collection<DgDestination> getDestinations(){
		List<DgDestination> dests = new ArrayList<DgDestination>();
		for (Entry<DgZone, Double> entry : this.toZoneRelations.entrySet()){
			dests.add(new DgDestinationImpl(entry.getKey().getId().toString(), entry.getKey().getCoordinate(), entry.getValue()));
		}
		for (Entry<Link, Double> entry : this.toLinkRelations.entrySet()){
			Coordinate c = MGC.coord2Coordinate(entry.getKey().getCoord());
			dests.add(new DgDestinationImpl(entry.getKey().getId().toString(),  c, entry.getValue()));
		}
		return dests;
	}
	
	@Override
	public Map<DgZone, Double> getDestinationZoneTrips(){
		return this.toZoneRelations;
	}

	@Override
	public Map<Link, Double> getDestinationLinkTrips(){
		return this.toLinkRelations;
	}
	
}

class DgDestinationImpl implements DgDestination{

	private Coordinate coordinate;
	private Double noTrips;
	private String id;

	public DgDestinationImpl(String id, Coordinate c, Double noTrips){
		this.id = id;
		this.coordinate = c;
		this.noTrips = noTrips;
	}
	
	public String getId(){
		return this.id;
	}
	
	@Override
	public Coordinate getCoordinate() {
		return this.coordinate;
	}

	@Override
	public Double getNumberOfTrips() {
		return this.noTrips;
	}
}
