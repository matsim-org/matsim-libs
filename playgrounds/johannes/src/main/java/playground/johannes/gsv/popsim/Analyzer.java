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
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import playground.johannes.gsv.popsim.analysis.AnalyzerTaskRunner;
import playground.johannes.gsv.popsim.analysis.FileIOContext;
import playground.johannes.gsv.popsim.analysis.PersonLAU2Density;
import playground.johannes.gsv.synPop.mid.PersonCloner;
import playground.johannes.gsv.synPop.mid.Route2GeoDistance;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.data.PlainFactory;
import playground.johannes.synpop.data.PlainPerson;
import playground.johannes.synpop.data.io.XMLHandler;
import playground.johannes.synpop.gis.DataPool;
import playground.johannes.synpop.gis.ZoneData;
import playground.johannes.synpop.gis.ZoneDataLoader;
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

//		String personFile = "/home/johannes/gsv/germany-scenario/mid2008/pop/mid2008.merged.xml";
//		String personFile = "/home/johannes/gsv/germany-scenario/mid2008/pop/mid2008.midjourneys.validated.xml";
		String personFile = "/home/johannes/gsv/germany-scenario/mid2008/pop/mid2008.midtrips.validated.xml";
		
		XMLHandler parser = new XMLHandler(new PlainFactory());
		parser.setValidating(false);
		
		parser.parse(personFile);

		Set<? extends Person> persons = parser.getPersons();
		
		
		logger.info("Cloning persons...");
		Random random = new XORShiftRandom();
		persons = PersonCloner.weightedClones((Collection<PlainPerson>) persons, 100000, random);
//		new ApplySampleProbas(82000000).apply(persons);
		logger.info(String.format("Generated %s persons.", persons.size()));

		TaskRunner.run(new Route2GeoDistance(new Simulator.Route2GeoDistFunction()), persons);

//		AnalyzerTaskComposite task = new AnalyzerTaskComposite();
//		task.setOutputDirectory(output);
//		task.addTask(new AgeIncomeCorrelation());
//		task.addTask(new ActTypeDistanceTask());
//		task.addTask(new LegGeoDistanceTask("car"));
//		task.addTask(new DaySeasonTask());
//		task.setOutputDirectory(output);
		
//		ProxyAnalyzer.analyze(persons, task);
		final Config config = new Config();
		ConfigUtils.loadConfig(config, "/home/johannes/gsv/germany-scenario/sim/config/simulator.xml");
		DataPool dataPool = new DataPool();
		dataPool.register(new ZoneDataLoader(config.getModule("synPopSim")), ZoneDataLoader.KEY);
		ZoneData zoneData = (ZoneData) dataPool.get(ZoneDataLoader.KEY);
		FileIOContext ioContext = new FileIOContext(output);
		PersonLAU2Density task = new PersonLAU2Density(zoneData.getLayer("lau2"));
		task.setIoContext(ioContext);
		AnalyzerTaskRunner.run(persons, task, ioContext);
	}

}
