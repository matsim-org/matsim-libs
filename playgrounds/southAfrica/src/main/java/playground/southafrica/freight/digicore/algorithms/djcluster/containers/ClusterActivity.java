/* *********************************************************************** *
 * project: org.matsim.*
 * ClusterPoint.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.southafrica.freight.digicore.algorithms.djcluster.containers;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;


/**
 * A simple class linking a {@link DigicoreActivity} to a specific cluster.
 * 
 * @author jwjoubert
 */
public class ClusterActivity implements Identifiable{
	private Id id;
	private Coord coord;
	private DigicoreCluster cluster;
	
	public ClusterActivity(Id id, Coord coord, DigicoreCluster cluster){
		this.id = id;
		this.coord = coord;
		this.cluster = cluster;
	}

	public Coord getCoord() {
		return this.coord;
	}

	public DigicoreCluster getCluster() {
		return cluster;
	}
	
	public void setCluster(DigicoreCluster cluster){
		this.cluster = cluster;
	}

	@Override
	public Id getId() {
		return this.id;
	}
	

}
