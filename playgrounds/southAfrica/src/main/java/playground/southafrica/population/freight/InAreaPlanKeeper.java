/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,     *
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

/**
 * 
 */
package playground.southafrica.population.freight;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.misc.Counter;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;
import org.opengis.feature.simple.SimpleFeature;

import playground.southafrica.utilities.Header;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

/**
 * Class to remove all persons (typically Digicore vehicles) who do not 
 * perform any activities inside a given study area. 
 * 
 * @author jwjoubert
 */
public class InAreaPlanKeeper {
	final private static Logger LOG = Logger.getLogger(InAreaPlanKeeper.class);

	/**
	 * Implementing the in-area plan keeper.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(InAreaPlanKeeper.class.toString(), args);
		
		String inputPlansFile = args[0];
		String inputAttributesFile = args[1];
		String shapefile = args[2];
		
		String outputPlansFile = args[3];
		String outputAttributesFile = args[4];
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimPopulationReader(sc).parse(inputPlansFile);
		new ObjectAttributesXmlReader(sc.getPopulation().getPersonAttributes()).parse(inputAttributesFile);

		/* Checking inside envelope is good enough, setting 'strictlyInside to false */
		Scenario cleanScenario = InAreaPlanKeeper.run(sc, shapefile, false);
		
		/* Write the output to files. */
		new PopulationWriter(cleanScenario.getPopulation()).write(outputPlansFile);
		new ObjectAttributesXmlWriter(cleanScenario.getPopulation().getPersonAttributes()).writeFile(outputAttributesFile);
		
		Header.printFooter();
	}
	
	
	/**
	 * The method parses a geographic area from a given shapefile, and checks
	 * if a person performs at least one activity inside the area. The definition
	 * of 'inside' is flexible, as it can be set to be strictly within the area,
	 * or inside the rectangular envelope covering the area (the latter being
	 * computationally more efficient).
	 * 
	 * @param sc the {@link Scenario} whose {@link Person}s will be considered;
	 * @param shapefile the filename of the shapefile used. Only the first
	 * 		geometry is used, so it is recommended that the shapefile is a large
	 * 		area, and not multiple subzones of the study area. 
	 * @param strictlyInside a boolean variable controlling the accuracy of the 
	 * 		spatial check. A value of <code>true</code> means the actual
	 * 		{@link Geometry} is used, while <code>false</code> means than 
	 * 		the {@link Geometry#getEnvelope()} is used.
	 * @return a cleaned up scenario with all persons not performing at least one
	 * 		activity in the area, and their associated attributes, being removed. 
	 */
	public static Scenario run(Scenario sc, String shapefile, boolean strictlyInside){	
		LOG.info("Checking scenario for persons in " + shapefile);
		
		ShapeFileReader sfr = new ShapeFileReader();
		sfr.readFileAndInitialize(shapefile);
		Collection<SimpleFeature> features = sfr.getFeatureSet();
		if(features.size() > 1){
			LOG.warn("Multiple features in " + shapefile);
			LOG.warn("Only using the first feature.");
		}
		Geometry geomtery = null;
		Object o = features.iterator().next().getDefaultGeometry();
		if(o instanceof Geometry){
			geomtery = (Geometry) o;
			LOG.info("Shapefile's geomtery type: " + geomtery.getClass().toString());
		}
		Geometry envelope = geomtery.getEnvelope();
		
		List<Id> listToRemove = new ArrayList<Id>();
		
		int numberRemoved = 0;
		Counter counter = new Counter("  checked # ");
		
		LOG.info("Checking " + sc.getPopulation().getPersons().size() + " persons in population...");
		for(Id id : sc.getPopulation().getPersons().keySet()){
			Person person = sc.getPopulation().getPersons().get(id);
			boolean inArea = false;
			
			Iterator<? extends Plan> planIterator = person.getPlans().iterator();
			while(!inArea && planIterator.hasNext()){
				Plan plan = planIterator.next();
				Iterator<PlanElement> peIterator = plan.getPlanElements().iterator();
				while(!inArea && peIterator.hasNext()){
					PlanElement pe = peIterator.next();
					if(pe instanceof Activity){
						Activity act = (Activity)pe;
						Point p = geomtery.getFactory().createPoint(new Coordinate(act.getCoord().getX(), act.getCoord().getY()));
						
						/* First filter on envelope only. */
						if(envelope.covers(p)){
							/* If required, check geomtery in detail. */
							if(strictlyInside){
								if(geomtery.covers(p)){
									inArea = true;
								}
							} else{
								inArea = true;
							}
						}
					}
				}
			}
			
			if(!inArea){
				listToRemove.add(id);
			}
			counter.incCounter();
		}
		counter.printCounter();
		
		/* Remove the person and all its associated attributes. */
		for(Id id : listToRemove){
			sc.getPopulation().getPersons().remove(id);
			sc.getPopulation().getPersonAttributes().removeAllAttributes(id.toString());
			numberRemoved++;
		}
		LOG.info("Number of persons removed: " + numberRemoved);
		LOG.info("Number of persons remaining: " + sc.getPopulation().getPersons().size());
		
		return sc;
	}
	
}
