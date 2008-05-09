/* *********************************************************************** *
 * project: org.matsim.*
 * WorldValidation.java
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

package org.matsim.world.algorithms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.TreeMap;

import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.gbl.Gbl;
import org.matsim.utils.geometry.CoordI;
import org.matsim.utils.geometry.shared.Coord;
import org.matsim.world.Location;
import org.matsim.world.World;
import org.matsim.world.Zone;
import org.matsim.world.ZoneLayer;

public class WorldCreateRasterLayer {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final Id layerid = new IdImpl("raster");
	private final int cellsize;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public WorldCreateRasterLayer(final int cellsize) {
		super();
		this.cellsize = cellsize;
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	public void run(final World world) {
		System.out.println("    running " + this.getClass().getName() + " module with cell size = " + this.cellsize + " x " + this.cellsize + " meters...");

		int nof_layers = world.getLayers().size();
		if (nof_layers == 0) { Gbl.errorMsg("      At least one zone layer must already exist."); }
		if (!(world.getBottomLayer() instanceof ZoneLayer)) { Gbl.errorMsg("      Bottom layer must be a zone layer."); }

		for (Id lid : world.getLayers().keySet()) {
			if (lid.toString().equals(this.layerid.toString())) { Gbl.errorMsg("      A layer with type " + this.layerid + " already exists."); }
		}

		System.out.println("      calculate extent...");
		CoordI min = new Coord(Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY);
		CoordI max = new Coord(Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY);
		ZoneLayer zonelayer = (ZoneLayer)world.getBottomLayer();
		for (Zone zone : (Collection<Zone>)zonelayer.getLocations().values()) {
			CoordI z_min = zone.getMin();
			CoordI z_max = zone.getMax();
			if (z_min.getX() < min.getX()) { min.setX(z_min.getX()); }
			if (z_min.getY() < min.getY()) { min.setY(z_min.getY()); }
			if (z_max.getX() > max.getX()) { max.setX(z_max.getX()); }
			if (z_max.getY() > max.getY()) { max.setY(z_max.getY()); }
		}
		System.out.println("        extent from min=" + min + " to max=" + max);
		System.out.println("      done.");

		int x_remain = ((int)Math.ceil(max.getX()-min.getX())) % this.cellsize;
		int m = ((int)Math.ceil(max.getX()-min.getX())) / this.cellsize;
		if (x_remain > 0) { m++; }

		int y_remain = ((int)Math.ceil(max.getY()-min.getY())) % this.cellsize;
		int n = ((int)Math.ceil(max.getY()-min.getY())) / this.cellsize;
		if (y_remain > 0) { n++; }

		System.out.println("      creating " + this.layerid + " layer...");
		ZoneLayer layer = (ZoneLayer)world.createLayer(this.layerid,"created by '" + this.getClass() + "'");
		System.out.println("      done.");

		System.out.println("      creating " + n + " x " + m + " cells...");
		TreeMap<Id, Zone> zones = (TreeMap<Id, Zone>) layer.getLocations();
		for (int i=0; i<n ; i++) {
			for (int j=0; j<m; j++) {
				CoordI z_min = new Coord(min.getX()+j*this.cellsize,min.getY()+i*this.cellsize);
				CoordI z_max = new Coord(min.getX()+(j+1)*this.cellsize,min.getY()+(i+1)*this.cellsize);
				CoordI z_center = new Coord((z_min.getX()+z_max.getX())/2.0,(z_min.getY()+z_max.getY())/2.0);
				Zone zone = new Zone(layer,new IdImpl(j+i*m),z_center,z_min,z_max,this.cellsize*this.cellsize,"raster("+i+","+j+")");
				zones.put(zone.getId(), zone);
			}
		}
		System.out.println("      done.");

		System.out.println("      creating mapping rule...");
		/*MappingRule mappingrule = */world.createMappingRule(layer.getType() + "[*]-[?]" + zonelayer.getType());
		world.complete();
		System.out.println("      done.");

		System.out.println("      setting up and down mappings...");
		for (Location upper : zonelayer.getLocations().values()) {
			ArrayList<Location> lowers = new ArrayList<Location>();
			boolean found = false;
			for (Location lower : layer.getLocations().values()) {
				if (upper.calcDistance(lower.getCenter()) == 0.0) {
					lowers.add(lower);
					if (lower.getUpMapping().isEmpty()) {
						lower.addUpMapping(upper);
						upper.addDownMapping(lower);
						found = true;
					}
				}
			}
			if (!found) {
				System.out.println("        upper zone id: " + upper.getId() + " no down_zone found yet. Try to steal from another up_zone...");
				for (Location lower : lowers) {
					Location other_upper = lower.getUpMapping().get(lower.getUpMapping().firstKey());
					if (other_upper.getDownMapping().size() > 1) {
						other_upper.getDownMapping().remove(lower.getId());
						lower.getUpMapping().remove(other_upper.getId());
						lower.addUpMapping(upper);
						upper.addDownMapping(lower);
						System.out.println("        stole down_zone id=" + lower.getId() + " from up_zone id=" + other_upper.getId());
						found = true;
						break;
					}
				}
			}
			if (!found) {
				System.out.println("        Nothing to steal!");
			}
		}
		System.out.println("      done.");

		System.out.println("    done.");
	}
}
