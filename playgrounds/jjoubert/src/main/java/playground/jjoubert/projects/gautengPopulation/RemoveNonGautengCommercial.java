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
package playground.jjoubert.projects.gautengPopulation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

import playground.southafrica.utilities.Header;
import playground.southafrica.utilities.gis.MyMultiFeatureReader;

/**
 * @author jwjoubert
 *
 */
public class RemoveNonGautengCommercial {
	final private static Logger LOG = Logger.getLogger(RemoveNonGautengCommercial.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(RemoveNonGautengCommercial.class.toString(), args);
		
		String inputFilename = args[0];
		String inputAttributes = args[1];
		String shapefile = args[2];
		String outputFilename = args[3];
		String outputAttributes = args[4];
		RemoveNonGautengCommercial.Run(inputFilename, inputAttributes, shapefile, outputFilename, outputAttributes);
		
		Header.printFooter();
	}
	
	
	public static void Run(String inputFilename, String inputAttributes, String shapefile, String outputFilename, String outputAttributes){
		LOG.info("Checking person from " + inputFilename);
		LOG.info("  :--> do they have any activities in " + shapefile + "?");
		
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimPopulationReader(sc).parse(inputFilename);
		new ObjectAttributesXmlReader(sc.getPopulation().getPersonAttributes()).parse(inputAttributes);
		
		MyMultiFeatureReader mfr = new MyMultiFeatureReader();
		try {
			mfr.readMultizoneShapefile(shapefile, 8);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot read shapefile " + shapefile);
		}
		Geometry g = mfr.getAllZones().get(0); /* Assuming we only work with the first (True for Gauteng...) */
		Geometry envelope = g.getEnvelope();
		GeometryFactory gf = new GeometryFactory();
		
		List<Id> listToRemove = new ArrayList<Id>();
		
		int numberRemoved = 0;
		Counter counter = new Counter("  checked # ");
		
		LOG.info("Checking " + sc.getPopulation().getPersons().size() + " persons in population...");
		for(Id id : sc.getPopulation().getPersons().keySet()){
			Person person = sc.getPopulation().getPersons().get(id);
			boolean inGauteng = false;

			Iterator<? extends Plan> planIterator = person.getPlans().iterator();
			while(!inGauteng && planIterator.hasNext()){
				Plan plan = planIterator.next();
				Iterator<PlanElement> peIterator = plan.getPlanElements().iterator();
				while(!inGauteng && peIterator.hasNext()){
					PlanElement pe = peIterator.next();
					if(pe instanceof Activity){
						Activity act = (Activity)pe;
						Point p = gf.createPoint(new Coordinate(act.getCoord().getX(), act.getCoord().getY()));

						/* Envelope of Gauteng is good enough. */
						if(envelope.contains(p)){
							inGauteng = true;
						}
					}
				}
			}

			if(!inGauteng){
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
		
		/* Write the remaining population. */
		new PopulationWriter(sc.getPopulation(), sc.getNetwork()).write(outputFilename);
		new ObjectAttributesXmlWriter(sc.getPopulation().getPersonAttributes()).writeFile(outputAttributes);	
		LOG.info("Number of persons removed: " + numberRemoved);
	}

}
