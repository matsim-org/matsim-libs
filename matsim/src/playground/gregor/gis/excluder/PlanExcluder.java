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
import org.matsim.api.basic.v01.Coord;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.world.World;

import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class PlanExcluder {

	private static final Logger log = Logger.getLogger(PlanExcluder.class);
//	private final NetworkLayer network;
	private final PopulationImpl plans;
	private final Collection<Polygon> ps;








	public PlanExcluder(final NetworkLayer network, final PopulationImpl population,
			final Collection<Polygon> ps) {
//		this.network = network;
		this.plans = population;
		this.ps = ps;
	}



	public PopulationImpl run() {

		PopulationImpl plans = new PopulationImpl();

		for (PersonImpl person : this.plans.getPersons().values()) {

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


	public static void main(final String [] args) {


		final String links = "./output/analysis/excludes.shp";
		final String configFile = "./configs/timeVariantEvac.xml";

		FeatureSource l = null;
		try {
			l = ShapeFileReader.readDataFile(links);
		} catch (final Exception e) {
			e.printStackTrace();
		}
		final Collection<Polygon> ls = getPolygons(l);

		Config config = Gbl.createConfig(new String [] {configFile});
		World world = Gbl.createWorld();


		final NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(config.network().getInputFile());
		world.setNetworkLayer(network);
		world.complete();


		final PopulationImpl population = new PopulationImpl();
		new MatsimPopulationReader(population, network).readFile(config.plans().getInputFile());


		PopulationImpl toSave = new PlanExcluder(network,population,ls).run();
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
