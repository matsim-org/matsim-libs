/* *********************************************************************** *
 * project: org.matsim.*
 * ConvertOsmToMatsim.java
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

package playground.jjoubert.projects.capeTownFreight;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.opengis.feature.simple.SimpleFeature;

import playground.southafrica.utilities.Header;
import playground.southafrica.utilities.analysis.VktEstimator;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;

/**
 * Class to estimate the vehicle kilometres travelled (vkt) within a given
 * {@link Geometry}.
 * 
 * @author jwjoubert
 */
public class EstimateVktFromPopulation {
	private final static Logger LOG = Logger.getLogger(EstimateVktFromPopulation.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(EstimateVktFromPopulation.class.toString(), args);
		
		String population = args[0];
		String shapefile = args[1];
		String output = args[2];
		
		/* Parse population */
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimPopulationReader(sc).parse(population);
		
		/* Parse shapefile */
		ShapeFileReader sfr = new ShapeFileReader();
		sfr.readFileAndInitialize(shapefile);
		Collection<SimpleFeature> features = sfr.getFeatureSet();
		if(features.size() > 1){
			LOG.warn("Shapefile contains multiple features. Only using the first!!");
		}
		SimpleFeature sf = features.iterator().next();
		MultiPolygon ct = null;
		if(sf.getDefaultGeometry() instanceof MultiPolygon){
			ct = (MultiPolygon)sf.getDefaultGeometry();
			LOG.info("Yes!! Cape Town has a MultiPolygon." );
		}
		
		/* Run the whole bunch. */
		Counter counter = new Counter("  plans # ");
		BufferedWriter bw = IOUtils.getBufferedWriter(output);
		try{
			bw.write("Id,vkt");
			bw.newLine();
			
			for(Person person : sc.getPopulation().getPersons().values()){
				Plan plan = person.getSelectedPlan();
				double vkt = VktEstimator.estimateVktFromPlan(plan, ct);
				bw.write(String.format("%s,%.0f\n", person.getId().toString(), vkt));
				counter.incCounter();
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write to " + output);
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + output);
			}
		}
		counter.printCounter();
		
		Header.printFooter();
	}
	
	
}
