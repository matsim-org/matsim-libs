/* *********************************************************************** *
 * project: org.matsim.*
 * PrintShopAndLeisureLocations.java
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

package playground.anhorni.locationchoice.analysis;

import com.vividsolutions.jts.geom.Point;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;

import java.util.ArrayList;
import java.util.List;

public class PrintShopAndLeisureLocations implements StartupListener, IterationEndsListener, ShutdownListener {

	private SimpleFeatureBuilder builder;
	
	@Override
	public void notifyStartup(final StartupEvent event) {	
	}

	@Override
	public void notifyIterationEnds(final IterationEndsEvent event) {
		
		if (event.getIteration() % 10 != 0) return;
		
		this.initGeometries();
		String shopFile = event.getServices().getControlerIO().getIterationFilename(event.getIteration(), "shopLocations.shp");
		String leisureFile = event.getServices().getControlerIO().getIterationFilename(event.getIteration(), "leisureLocations.shp");
		
		ArrayList<SimpleFeature> featuresShop = new ArrayList<SimpleFeature>();
		ArrayList<SimpleFeature> featuresLeisure = new ArrayList<SimpleFeature>();

        Population plans = event.getServices().getScenario().getPopulation();
		for (Person person : plans.getPersons().values()) {
			
			Plan plan = person.getSelectedPlan();
			
			final List<?> actslegs = plan.getPlanElements();
			for (int j = 0; j < actslegs.size(); j=j+2) {			
				ActivityImpl act = (ActivityImpl)actslegs.get(j);
				
				Coord coord = act.getCoord();
				SimpleFeature feature = this.createFeature(coord, person.getId().toString());
				
				if (act.getType().startsWith("shop")) {
					featuresShop.add(feature);
				}
				else if (act.getType().startsWith("leisure")) {
					featuresLeisure.add(feature);
				}
			}
		}
		ShapeFileWriter.writeGeometries(featuresShop, shopFile);
		ShapeFileWriter.writeGeometries(featuresLeisure, leisureFile);
	}

	@Override
	public void notifyShutdown(final ShutdownEvent controlerShudownEvent) {
	}
		
	private void initGeometries() {
		SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
		b.setName("point");
		b.add("location", Point.class);
		b.add("ID", String.class);
		builder = new SimpleFeatureBuilder(b.buildFeatureType());
	}
	
	private SimpleFeature createFeature(Coord coord, String id) {
		return this.builder.buildFeature(id, new Object [] {MGC.coord2Point(coord), id.toString()});
	}
}
