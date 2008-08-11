/* *********************************************************************** *
 * project: org.matsim.*
 * PlanExcluder.java
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

package playground.gregor.gis.excluder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureIterator;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.Person;
import org.matsim.population.Population;
import org.matsim.population.PopulationWriter;
import org.matsim.utils.geometry.Coord;
import org.matsim.utils.geometry.geotools.MGC;
import org.matsim.utils.gis.ShapeFileReader;

import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class PlanExcluder {

	private static final Logger log = Logger.getLogger(PlanExcluder.class);
	private final NetworkLayer network;
	private final Population plans;
	private final Collection<Polygon> ps;

	
	
	
	
	
	
	
	public PlanExcluder(NetworkLayer network, Population population,
			Collection<Polygon> ps) {
		this.network = network;
		this.plans = population;
		this.ps = ps;
	}



	public Population run() {
		
		Population plans = new Population();
		
		for (Person person : this.plans.getPersons().values()) {
			
			Coord c = person.getSelectedPlan().getFirstActivity().getCoord();
			Point p  = MGC.coord2Point(c);
			
			boolean include = true;
			for (Polygon po : this.ps) {
				if (po.contains(p)) {
					include = false;
					break;
				}
			}
			if (include) {
				try {
					plans.addPerson(person);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		return plans;
		
	}
	

	public static void main(String [] args) {
		

		final String links = "./output/analysis/excludes.shp";
		final String config = "./configs/timeVariantEvac.xml";
		
		FeatureSource l = null;
		try {
			l = ShapeFileReader.readDataFile(links);
		} catch (final Exception e) {
			e.printStackTrace();
		}
		final Collection<Polygon> ls = getPolygons(l);
		
		Gbl.createConfig(new String [] {config});
		Gbl.createWorld();
		
		
		final NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(Gbl.getConfig().network().getInputFile());
		Gbl.getWorld().setNetworkLayer(network);
		
		
		final Population population = new Population();
		new MatsimPopulationReader(population).readFile(Gbl.getConfig().plans().getInputFile());
		

		Population toSave = new PlanExcluder(network,population,ls).run();
		new PopulationWriter(toSave,"./output/analysis/padang_plans_v20080618_reduced.xml.gz", "v4").write();

	}
	
	
	
	
	private static Collection<Polygon> getPolygons(final FeatureSource n) {
		final Collection<Polygon> polygons = new ArrayList<Polygon>();
		FeatureIterator it = null;
		try {
			it = n.getFeatures().features();
		} catch (final IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		while (it.hasNext()) {
			final Feature feature = it.next();
//			int id = (Integer) feature.getAttribute(1);
			MultiPolygon multiPolygon = (MultiPolygon) feature.getDefaultGeometry();
			if (multiPolygon.getNumGeometries() > 1) {
				log.warn("MultiPolygons with more then 1 Geometry ignored!");
				continue;
			}
			Polygon polygon = (Polygon) multiPolygon.getGeometryN(0);
			polygons.add(polygon);
	}
	
		return polygons;
	}
}
