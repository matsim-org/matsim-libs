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

import java.util.Collection;
import java.util.TreeMap;

import org.matsim.basic.v01.Id;
import org.matsim.gbl.Gbl;
import org.matsim.utils.geometry.CoordI;
import org.matsim.utils.geometry.shared.Coord;
import org.matsim.utils.identifiers.IdI;
import org.matsim.world.Location;
import org.matsim.world.MappingRule;
import org.matsim.world.World;
import org.matsim.world.Zone;
import org.matsim.world.ZoneLayer;

public class WorldCreateRasterLayer extends WorldAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final IdI layerid = new Id("raster");
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

	@Override
	public void run(final World world) {
		System.out.println("    running " + this.getClass().getName() + " module with cell size = " + cellsize + " x " + cellsize + " meters...");

		int nof_layers = world.getLayers().size();
		if (nof_layers == 0) { Gbl.errorMsg("      At least one zone layer must already exist."); }
		if (!(world.getBottomLayer() instanceof ZoneLayer)) { Gbl.errorMsg("      Bottom layer must be a zone layer."); }

		for (IdI lid : world.getLayers().keySet()) {
			if (lid.toString().equals(layerid.toString())) { Gbl.errorMsg("      A layer with type " + layerid + " already exists."); }
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

		int x_remain = ((int)Math.ceil(max.getX()-min.getX())) % cellsize;
		int m = ((int)Math.ceil(max.getX()-min.getX())) / cellsize;
		if (x_remain > 0) { m++; }

		int y_remain = ((int)Math.ceil(max.getY()-min.getY())) % cellsize;
		int n = ((int)Math.ceil(max.getY()-min.getY())) / cellsize;
		if (y_remain > 0) { n++; }

		System.out.println("      creating " + layerid + " layer...");
		ZoneLayer layer = (ZoneLayer)world.createLayer(layerid,"created by '" + this.getClass() + "'");
		System.out.println("      done.");

		System.out.println("      creating " + n + " x " + m + " cells...");
		TreeMap<IdI,Zone> zones = (TreeMap<IdI,Zone>)layer.getLocations();
		for (int i=0; i<n ; i++) {
			for (int j=0; j<m; j++) {
				CoordI z_min = new Coord(min.getX()+j*cellsize,min.getY()+i*cellsize);
				CoordI z_max = new Coord(min.getX()+(j+1)*cellsize,min.getY()+(i+1)*cellsize);
				CoordI z_center = new Coord((z_min.getX()+z_max.getX())/2.0,(z_min.getY()+z_max.getY())/2.0);
				Zone zone = new Zone(layer,new Id(j+i*m),z_center,z_min,z_max,cellsize*cellsize,"raster("+i+","+j+")");
				zones.put(zone.getId(),zone);
			}
		}
		System.out.println("      done.");

		System.out.println("      creating mapping rule...");
		MappingRule mappingrule = world.createMappingRule(layer.getType() + "[*]-[?]" + zonelayer.getType());
		world.complete();
		System.out.println("      done.");
		
		System.out.println("      setting up and down mappings...");
		for (Location upper : zonelayer.getLocations().values()) {
			System.out.println("        upper zone id: " + upper.getId());
			for (Location lower : layer.getLocations().values()) {
				if (upper.calcDistance(lower.getCenter()) == 0.0) {
					if (lower.getUpMapping().isEmpty()) {
						lower.addUpMapping(upper);
						upper.addDownMapping(lower);
					}
				}
			}
		}
		System.out.println("      done.");

		System.out.println("    done.");
	}
}
