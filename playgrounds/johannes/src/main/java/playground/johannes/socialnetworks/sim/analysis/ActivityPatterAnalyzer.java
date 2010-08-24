/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityPatterAnalyzer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.sim.analysis;

import gnu.trove.TObjectIntHashMap;
import gnu.trove.TObjectIntIterator;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PopulationReaderMatsimV4;

import com.vividsolutions.jts.geom.GeometryFactory;

/**
 * @author illenberger
 *
 */
public class ActivityPatterAnalyzer {

	public TObjectIntHashMap<String> analyze(Population population) {
		TObjectIntHashMap<String> histogram = new TObjectIntHashMap<String>();
		
		for(Person person : population.getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			
			StringBuilder builder = new StringBuilder(300);
			
			for(int i = 0; i < plan.getPlanElements().size(); i += 2) {
				builder.append(((Activity)plan.getPlanElements().get(i)).getType());
				builder.append(" ");
			}
			
			histogram.adjustOrPutValue(builder.toString(), 1, 1);
		}
		
		return histogram;
	}
	
	public static void writeHistorgram(TObjectIntHashMap<String> histogram, String filename) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
		
		writer.write("pattern\t count");
		writer.newLine();
		
		TObjectIntIterator<String> it = histogram.iterator();
		for(int i = 0; i < histogram.size(); i++) {
			it.advance();
			writer.write(it.key());
			writer.write("\t");
			writer.write(String.valueOf(it.value()));
			writer.newLine();
		}
		writer.close();
	}
	
	public static void main(String args[]) throws IOException {
//		String netFile = args[0];
//		String facFile = args[1];
		String popFile ="/Users/jillenberger/Work/shared-svn/studies/schweiz-ivtch/baseCase/plans_v2/census2000v2_dilZh30km_10pct/plans.xml.gz";
//		String graphFile = args[3];
//		double proba = Double.parseDouble(args[4]);

		GeometryFactory geoFactory = new GeometryFactory();

		Scenario scenario = new ScenarioImpl();

//		NetworkReaderMatsimV1 netReader = new NetworkReaderMatsimV1(scenario);
//		netReader.parse(netFile);
//
//		FacilitiesReaderMatsimV1 facReader = new FacilitiesReaderMatsimV1((ScenarioImpl) scenario);
//		facReader.parse(facFile);

		PopulationReaderMatsimV4 reader = new PopulationReaderMatsimV4(scenario);
		reader.readFile(popFile);
		
		ActivityPatterAnalyzer analyzer = new ActivityPatterAnalyzer();
		TObjectIntHashMap<String> histogram = analyzer.analyze(scenario.getPopulation());
		ActivityPatterAnalyzer.writeHistorgram(histogram, "/Users/jillenberger/Work/work/socialnets/sim/pattern.txt");
	}
}
