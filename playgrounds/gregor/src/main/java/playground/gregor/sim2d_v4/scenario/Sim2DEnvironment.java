/* *********************************************************************** *
 * project: org.matsim.*
 * Environment.java
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

package playground.gregor.sim2d_v4.scenario;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Polygon;


public class Sim2DEnvironment implements Identifiable{

	private Envelope envelope;
	private CoordinateReferenceSystem crs;
	private final Map<Id, Section> sections = new HashMap<Id,Section>();
	private Network net;
	private  Id id = null;
	
	
	
	public void setEnvelope(Envelope e) {
		this.envelope = e;
	}

	public Section createAndAddSection(Id id, Polygon p, int[] openings,
			Id[] neighbors, int level) {
		Section s = new Section(id,p,openings,neighbors, level);
		this.sections.put(id, s);
		return s;
	}

	public void setCRS(CoordinateReferenceSystem crs) {
		this.crs = crs;
		
	}

	public Envelope getEnvelope() {
		return this.envelope;
	}

	public CoordinateReferenceSystem getCRS() {
		return this.crs;
	}

	public Map<Id,Section> getSections() {
		return this.sections ;
	}


	public void setNetwork(Network net) {
		this.net = net;
	}
	
	public Network getEnvironmentNetwork(){
		return this.net;
	}
	
	
	//TODO link --> section mapping (and link --> evironment mapping) [gl dec 2012]
	public Section getSection(Link link) {
		return null;
	}

	@Override
	public Id getId() {
		return this.id;
	}
	
	public void setId(Id id) {
		this.id = id;
	}



	

	
}
