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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeBuilder;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileWriter;

import com.vividsolutions.jts.geom.Point;

public class PrintShopAndLeisureLocations implements StartupListener, IterationEndsListener, ShutdownListener {

	private FeatureType featureType;
	
	public void notifyStartup(final StartupEvent event) {	
	}

	public void notifyIterationEnds(final IterationEndsEvent event) {
		
		if (event.getIteration() % 10 != 0) return;
		
		this.initGeometries();
		String shopFile = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), "shopLocations.shp");
		String leisureFile = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), "leisureLocations.shp");
		
		ArrayList<Feature> featuresShop = new ArrayList<Feature>();
		ArrayList<Feature> featuresLeisure = new ArrayList<Feature>();
		
		Population plans = event.getControler().getPopulation();		
		for (Person person : plans.getPersons().values()) {
			
			Plan plan = person.getSelectedPlan();
			
			final List<?> actslegs = plan.getPlanElements();
			for (int j = 0; j < actslegs.size(); j=j+2) {			
				ActivityImpl act = (ActivityImpl)actslegs.get(j);
				
				Coord coord = act.getCoord();
				Feature feature = this.createFeature(coord, (IdImpl)person.getId());
				
				if (act.getType().startsWith("shop")) {
					featuresShop.add(feature);
				}
				else if (act.getType().startsWith("leisure")) {
					featuresLeisure.add(feature);
				}
			}
		}
		try {
			ShapeFileWriter.writeGeometries(featuresShop, shopFile);
			ShapeFileWriter.writeGeometries(featuresLeisure, leisureFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void notifyShutdown(final ShutdownEvent controlerShudownEvent) {
	}
		
	private void initGeometries() {
		AttributeType [] attr = new AttributeType[2];
		attr[0] = AttributeTypeFactory.newAttributeType("Point", Point.class);
		attr[1] = AttributeTypeFactory.newAttributeType("ID", String.class);
		
		try {
			this.featureType = FeatureTypeBuilder.newFeatureType(attr, "point");
		} catch (SchemaException e) {
			e.printStackTrace();
		}
	}
	
	private Feature createFeature(Coord coord, IdImpl id) {		
		Feature feature = null;
		try {
			feature = this.featureType.create(new Object [] {MGC.coord2Point(coord), id.toString()});
		} catch (IllegalAttributeException e) {
			e.printStackTrace();
		}
		return feature;
	}
}
