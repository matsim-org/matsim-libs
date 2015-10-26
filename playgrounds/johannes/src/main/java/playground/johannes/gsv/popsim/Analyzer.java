/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.popsim;

import org.apache.log4j.Logger;
import org.matsim.contrib.common.util.XORShiftRandom;
import playground.johannes.gsv.synPop.analysis.AnalyzerTaskComposite;
import playground.johannes.gsv.synPop.analysis.LegGeoDistanceTask;
import playground.johannes.gsv.synPop.analysis.ProxyAnalyzer;
import playground.johannes.gsv.synPop.mid.PersonCloner;
import playground.johannes.gsv.synPop.mid.Route2GeoDistance;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.data.PlainFactory;
import playground.johannes.synpop.data.PlainPerson;
import playground.johannes.synpop.data.io.XMLHandler;
import playground.johannes.synpop.processing.TaskRunner;

import java.io.IOException;
import java.util.Collection;
import java.util.Random;
import java.util.Set;

/**
 * @author johannes
 *
 */
public class Analyzer {

	private static final Logger logger = Logger.getLogger(Analyzer.class);
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
	
		String output = "/home/johannes/gsv/matrix2014/mid-fusion/";

		String personFile = "/home/johannes/gsv/germany-scenario/mid2008/pop/mid2008.merged.xml";
		
		XMLHandler parser = new XMLHandler(new PlainFactory());
		parser.setValidating(false);
		
		parser.parse(personFile);

		Set<? extends Person> persons = parser.getPersons();
		
		
		logger.info("Cloning persons...");
		Random random = new XORShiftRandom();
		persons = PersonCloner.weightedClones((Collection<PlainPerson>) persons, 200000, random);
//		new ApplySampleProbas(82000000).apply(persons);
		logger.info(String.format("Generated %s persons.", persons.size()));

		TaskRunner.run(new Route2GeoDistance(new Simulator.Route2GeoDistFunction()), persons);

		AnalyzerTaskComposite task = new AnalyzerTaskComposite();
//		task.setOutputDirectory(output);
//		task.addTask(new AgeIncomeCorrelation());
//		task.addTask(new ActTypeDistanceTask());
		task.addTask(new LegGeoDistanceTask("car"));
		task.setOutputDirectory(output);
		
		ProxyAnalyzer.analyze(persons, task);

	}

}
