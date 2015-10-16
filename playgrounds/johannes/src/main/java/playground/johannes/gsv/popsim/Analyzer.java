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
import playground.johannes.gsv.synPop.analysis.AnalyzerTaskComposite;
import playground.johannes.gsv.synPop.analysis.ProxyAnalyzer;
import playground.johannes.synpop.data.io.XMLHandler;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.data.PlainFactory;

import java.io.IOException;
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
	
		String output = "/home/johannes/gsv/germany-scenario/mid2008/analysis/pop/";

		String personFile = "/home/johannes/gsv/germany-scenario/mid2008/pop/pop.xml";
		
		XMLHandler parser = new XMLHandler(new PlainFactory());
		parser.setValidating(false);
		
		parser.parse(personFile);

		Set<? extends Person> persons = parser.getPersons();
		
		
//		logger.info("Cloning persons...");
//		Random random = new XORShiftRandom();
//		persons = PersonCloner.weightedClones(persons, 200000, random);
//		new ApplySampleProbas(82000000).apply(persons);
//		logger.info(String.format("Generated %s persons.", persons.size()));
	
		AnalyzerTaskComposite task = new AnalyzerTaskComposite();
		task.addTask(new AgeIncomeCorrelation());
		task.setOutputDirectory(output);
		
		ProxyAnalyzer.analyze(persons, task);

	}

}
