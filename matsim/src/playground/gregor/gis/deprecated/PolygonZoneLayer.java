/* *********************************************************************** *
 * project: org.matsim.*
 * PolygonZoneLayer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.gregor.gis.deprecated;

import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.facilities.Facilities;
import org.matsim.gbl.Gbl;
import org.matsim.network.NetworkLayer;
import org.matsim.utils.geometry.Coord;
import org.matsim.world.Layer;

/**
 * The collection of polygon zone objects in MATSim.
 * @see Layer
 * @see NetworkLayer
 * @see Facilities
 * @author laemmel
  */
public class PolygonZoneLayer extends Layer{
	
	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////
	public static final Id LAYER_TYPE = new IdImpl("polyzone");
	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	protected PolygonZoneLayer(final String type, final String name) {
		super(new IdImpl(type),name);
	}

	protected PolygonZoneLayer(final Id type, final String name) {
		super(type,name);
	}
	
	public final PolygonZone createPolygonZone( final Id id, final Coord center,
            final Coord[] shell, final double area, final String name){
//		IdI i = new Id(id);
		if (this.locations.containsKey(id)) { Gbl.errorMsg(this.toString() + "[zone id=" + id + " already exists]"); }
		
		PolygonZone zone = new PolygonZone(this,id,center,shell,area,name);
		this.locations.put(id, zone);
		
		return zone;
	}

}
