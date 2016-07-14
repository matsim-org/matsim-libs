/* *********************************************************************** *
 * project: org.matsim.*
 * GridCell
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Implementation of a Zone as an origin of transport demand. 
 * 
 * Yes, indeed I should have protected my boundaries when writing this by wrapping Polygon (Martin Clean Code). 
 * But as geotools is very central within MATSim, I skipped this step, simply relying on the fact that 
 * something as Polygon should be stable over a long time. 
 * 
 * @author dgrether
 */
public class DgZone extends DgOriginImpl implements DgOrigin{

	private Polygon polygon;
	private Id<DgZone> id;
	private Map<Id, DgZoneFromLink> fromLinks = new HashMap<Id, DgZoneFromLink>();
	private Link zoneNetworkConnectionLink;
	
	public DgZone(Id<DgZone> id, Polygon p) {
		this.id = id;
		polygon = p;
	}
	
	public Id getId(){
		return this.id;
	}

	public Envelope getEnvelope() {
		return this.polygon.getEnvelope().getEnvelopeInternal();
	}

	
	public Polygon getPolygon() {
		return this.polygon;
	}
	
	@Override
	public Coordinate getCoordinate(){
		return this.getEnvelope().centre();
	}

	public Map<Id, DgZoneFromLink> getFromLinks(){
		return this.fromLinks;
	}
	
	public DgZoneFromLink getFromLink(Link startLink) {
		if (! fromLinks.containsKey(startLink.getId())){
			fromLinks.put(startLink.getId(), new DgZoneFromLink(startLink));
		}
		return fromLinks.get(startLink.getId());
	}

	
	public Link getZoneNetworkConnectionLink() {
		return zoneNetworkConnectionLink;
	}

	
	public void setZoneNetworkConnectionLink(Link zoneNetworkConnectionLink) {
		this.zoneNetworkConnectionLink = zoneNetworkConnectionLink;
	}
	
}
