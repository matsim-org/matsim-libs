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
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
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
import playground.southafrica.utilities.containers.MyZone;
import playground.southafrica.utilities.gis.MyMultiFeatureReader;

/**
 * Class to check if a commercial vehicle is an 'intra-provincial' vehicle in
 * the Gauteng province.
 * 
 * @author jwjoubert
 */
public class AddGautengIntraAttribute {
	private final static Logger LOG = Logger.getLogger(AddGautengIntraAttribute.class.toString());
	private final static Double GAUTENG_INTRA_THRESHOLD = 0.60;
	
	private static Scenario sc;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(AddGautengIntraAttribute.class.toString(), args);
		
		String populationFile = args[0];
		String attributesFile = args[1];
		String shapefile = args[2];
		String newAttributesFile = args[3];
		
		run(populationFile, attributesFile, shapefile, newAttributesFile);
		
		Header.printFooter();
	}

	/**
	 * Method to check if an activity chain (Plan) contain at least a minimum
	 * threshold of activities within Gauteng. If so, a boolean attribute is
	 * added to the person. Currently the threshold is set to 60% of activities
	 * within Gauteng. This is based on the work by
	 * <a href="http://www.sciencedirect.com/science/article/pii/S0966692309001781">Joubert 
	 * & Axhausen (2011)</a>
	 *  
	 * @param populationFile
	 * @param attributesFile
	 * @param shapefile
	 * @param newAttributesFile
	 */
	public static void run(String populationFile, String attributesFile,
			String shapefile, String newAttributesFile) {
		/* Read population and population attributes. */
		sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimPopulationReader(sc).parse(populationFile);
		new ObjectAttributesXmlReader(sc.getPopulation().getPersonAttributes()).parse(attributesFile);
		
		/* Read shapefile. */
		MyMultiFeatureReader mfr = new MyMultiFeatureReader();
		try {
			mfr.readMultizoneShapefile(shapefile, 3);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Could not read Gauteng shapefile from " + shapefile);
		}
		List<MyZone> zones = mfr.getAllZones();
		if(zones.size() > 1){
			LOG.warn("There are multiple zones in shapefile. Only the first will be used!");
		}
		MyZone gauteng = zones.get(0);
		Geometry envelope = gauteng.getEnvelope();

		/* Set up some counters for reporting. */
		double numberOfIntra = 0.0;
		
		/* Check percentage of activities (major and minor) that is within Gauteng. */
		LOG.info("Checking vehicles...");
		Counter counter = new Counter("  vehicles # ");
		GeometryFactory gf = new GeometryFactory();
		for(Person p : sc.getPopulation().getPersons().values()){
			Plan plan = p.getSelectedPlan();
			double numberOfActivities = 0.0;
			double numberInGauteng = 0.0;
			for(PlanElement pe : plan.getPlanElements()){
				if(pe instanceof Activity){
					Activity act = (Activity) pe;
					numberOfActivities++;
					
					/* Check if in Gauteng. */
					Point point = gf.createPoint(new Coordinate(act.getCoord().getX(), act.getCoord().getY()));
					if(envelope.contains(point)){
						if(gauteng.contains(point)){
							numberInGauteng++;
						}
					}
				}
			}
			double percentage = numberInGauteng / numberOfActivities;
			
			/* Add attribute if intra-Gauteng commercial vehicle. */
			if(percentage >= GAUTENG_INTRA_THRESHOLD){
				sc.getPopulation().getPersonAttributes().putAttribute(p.getId().toString(), "intraGauteng", true);
				numberOfIntra++;
			}
			counter.incCounter();
		}
		counter.printCounter();
		
		/* Write the population attributes. */
		new ObjectAttributesXmlWriter(sc.getPopulation().getPersonAttributes()).writeFile(newAttributesFile);
		
		/* Report. */
		LOG.info( String.format("Number of intra-Gauteng commercial vehicles: %.0f (%.2f%%)", numberOfIntra, (numberOfIntra/(double)sc.getPopulation().getPersons().size())*100) );
	}
}
