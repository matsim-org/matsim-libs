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

import java.util.Collection;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.misc.Counter;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.opengis.feature.simple.SimpleFeature;

import playground.southafrica.utilities.Header;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 * Class to check if a commercial vehicle is an 'intra-provincial' vehicle in
 * the Gauteng province. The term 'intra-provincial' is covered in Joubert &
 * Axhausen (2011): if an activity chains performs at least 60% of it's 
 * activities inside the given area (described by a geographic polygon).
 * 
 * @author jwjoubert
 * @see Joubert, J.W., Axhausen, K.W. (2011). Inferring commercial vehicle
 * 		activities in Gauteng, South Africa. <a href=doi:10.1016/j.jtrangeo.2009.11.005>
 * 		Journal of Transport Geography</a>, 19(1), 115-124.
 */
public class IntraAreaIdentifier {
	private final static Logger LOG = Logger.getLogger(IntraAreaIdentifier.class.toString());
	private final static Double GAUTENG_INTRA_THRESHOLD = 0.60;
	
	/**
	 * Implementation of the intra-area static method. 
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(IntraAreaIdentifier.class.toString(), args);
		
		String populationFile = args[0];
		String attributesFile = args[1];
		String shapefile = args[2];
		String attributeName = args[3];
		
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimPopulationReader(sc).parse(populationFile);
		new ObjectAttributesXmlReader(sc.getPopulation().getPersonAttributes()).parse(attributesFile);
		
		IntraAreaIdentifier.run(sc, shapefile, false, attributeName);
		
		Header.printFooter();
	}

	/**
	 * Method to check if an activity chain ({@link Plan}) contain at least a 
	 * minimum threshold percentage of activities within an area. If so, a 
	 * boolean attribute is added to the person. Currently (2011) the threshold 
	 * is set to 60% of activities (study by Joubert & Axhausen, 2011, for 
	 * Gauteng, South Africa).
	 *  
	 * @param populationFile
	 * @param attributesFile
	 * @param shapefile
	 * @param newAttributesFile
	 *
	 * @see Joubert, J.W., Axhausen, K.W. (2011). Inferring commercial vehicle
	 * 		activities in Gauteng, South Africa. <a href=doi:10.1016/j.jtrangeo.2009.11.005>
	 * 		Journal of Transport Geography</a>, 19(1), 115-124.
	 */
	public static Scenario run(Scenario sc, String shapefile, 
			boolean strictlyInside, String attributeName) {
		
		/* Read shapefile. */
		ShapeFileReader sfr = new ShapeFileReader();
		sfr.readFileAndInitialize(shapefile);
		Collection<SimpleFeature> features = sfr.getFeatureSet();
		if(features.size() > 1){
			LOG.warn("There are multiple zones in shapefile. Only the first will be used!");
		}
		Geometry area = null;
		Object o = features.iterator().next().getDefaultGeometry();
		if(o instanceof Geometry){
			area = (Geometry) o;
			LOG.info("Shapefile's geomtery type: " + area.getClass().toString());
		}
		Geometry envelope = area.getEnvelope();
		
		/* Set up some counters for reporting. */
		double numberOfIntra = 0.0;
		
		/* Check percentage of activities (major and minor) that is within Gauteng. */
		LOG.info("Checking selected plans...");
		Counter counter = new Counter("  person # ");
		GeometryFactory gf = new GeometryFactory();
		for(Person p : sc.getPopulation().getPersons().values()){
			Plan plan = p.getSelectedPlan();
			double numberOfActivities = 0.0;
			double numberInside = 0.0;
			for(PlanElement pe : plan.getPlanElements()){
				if(pe instanceof Activity){
					Activity act = (Activity) pe;
					numberOfActivities++;
					
					/* Check if inside area. */
					Point point = gf.createPoint(new Coordinate(act.getCoord().getX(), act.getCoord().getY()));
					if(envelope.contains(point)){
						if(strictlyInside){
							if(area.covers(point)){
								
							}
						} else{
							numberInside++;
						}
					}
				}
			}
			double percentage = numberInside / numberOfActivities;
			
			/* Add id to list that should be removed. */
			if(percentage >= GAUTENG_INTRA_THRESHOLD){
				sc.getPopulation().getPersonAttributes().putAttribute(p.getId().toString(), attributeName, true);
				numberOfIntra++;
			}
			counter.incCounter();
		}
		counter.printCounter();
		
		/* Report. */
		LOG.info( String.format("Number of intra-area plans: %.0f (%.2f%%)", numberOfIntra, (numberOfIntra/(double)sc.getPopulation().getPersons().size())*100) );
		return sc;
	}
}
