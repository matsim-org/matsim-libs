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
package playground.jhackney.algorithms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.TreeMap;

import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.facilities.Facilities;
import org.matsim.gbl.Gbl;
import org.matsim.socialnetworks.algorithms.FacilitiesFindScenarioMinMaxCoords;
import org.matsim.utils.geometry.CoordI;
import org.matsim.utils.geometry.shared.Coord;
import org.matsim.world.AbstractLocation;
import org.matsim.world.MappingRule;
import org.matsim.world.World;
import org.matsim.world.Zone;
import org.matsim.world.ZoneLayer;
import org.matsim.world.algorithms.WorldAlgorithm;

/**
 * 
 * @author jhackney
 *
 * Makes a raster of cells in a world layer with a grid side length = cellsize.
 * The X,Y limits of the layer are based on the min and max coordinates of
 * a pre-existing facilities layer, because the facilities layer is the one that will be
 * mapped to this layer later in WorldBottom2TopCompletion.
 * 
 * See WorldCreateRasterLaser for a version which uses a pre-existing ZoneLayer to
 * establish the X,Y limits and to calculate a mapping rule.
 *
 */
public class WorldCreateRasterLayer2 extends WorldAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final Id layerid = new IdImpl("raster");
	private final int cellsize;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public WorldCreateRasterLayer2(final int cellsize) {
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
		if (nof_layers == 0) { Gbl.errorMsg("      The world must have a facilities layer first."); }

		if (!(world.getLayers().containsKey(Facilities.LAYER_TYPE))) { Gbl.errorMsg("      World must contain a Facilities layer."); }

		for (Id lid : world.getLayers().keySet()) {
			if (lid.toString().equals(layerid.toString())) { Gbl.errorMsg("      A layer with type " + layerid + " already exists."); }
		}

		System.out.println("      calculate extent...");
		CoordI min = new Coord(Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY);
		CoordI max = new Coord(Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY);

		Facilities facilities = (Facilities)Gbl.getWorld().getLayer(Facilities.LAYER_TYPE);
		FacilitiesFindScenarioMinMaxCoords fff= new FacilitiesFindScenarioMinMaxCoords();
		fff.run(facilities);
		min = fff.getMinCoord();
		max = fff.getMaxCoord();

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
		TreeMap<Id,Zone> zones = (TreeMap<Id,Zone>)layer.getLocations();
		for (int i=0; i<n ; i++) {
			for (int j=0; j<m; j++) {
				CoordI z_min = new Coord(min.getX()+j*cellsize,min.getY()+i*cellsize);
				CoordI z_max = new Coord(min.getX()+(j+1)*cellsize,min.getY()+(i+1)*cellsize);
				CoordI z_center = new Coord((z_min.getX()+z_max.getX())/2.0,(z_min.getY()+z_max.getY())/2.0);
				Zone zone = new Zone(layer,new IdImpl(j+i*m),z_center,z_min,z_max,cellsize*cellsize,"raster("+i+","+j+")");
				zones.put(zone.getId(),zone);
			}
		}
		
		// Initialize a mapping rule for the new layer
		// Is this only an *up* mapping rule?
//		Iterator<IdI> layerIt=world.getLayers().keySet().iterator();
//		while(layerIt.hasNext()){
//			IdI layerId=layerIt.next();
//			if (world.getLayer(layerId) instanceof ZoneLayer){
//				ZoneLayer zoneLayer= (ZoneLayer) world.getLayer(layerId);
//				MappingRule mappingrule = world.createMappingRule(layer.getType() + "[*]-[?]" + zoneLayer.getType());
//			}
//		}
		System.out.println("      done.");
	}
}
