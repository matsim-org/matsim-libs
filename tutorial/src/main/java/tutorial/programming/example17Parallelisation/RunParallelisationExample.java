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

package tutorial.programming.example17Parallelisation;

import java.io.File;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

/**
 * Demonstrates how to use parallelisation in MATSim using the Sioux Falls 
 * example. It is assumed the machine you are running this on actually 
 * accommodates multiple threads.
 * 
 * @author jwjoubert
 */
public class RunParallelisationExample {
	final private static Logger LOG = Logger.getLogger(Math.class);

	/**
	 * Running the Sioux-Falls scenario, first with single thread, and then in
	 * parallel (for all modules allowing parallelisation: mobsim, replanning 
	 * and events handling).<br>
	 * 
	 * Note: the purpose of the example is to show <i>how</i> multi-threaded
	 * behaviour is switched on. It may be, given your hardware setup, that this
	 * example runs longer under the multi-threaded setup ;-) 
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		/* Running in single thread. */
		Gbl.startMeasurement();
		Config config = ConfigUtils.createConfig();
		String configFilename = "examples/siouxfalls-2014/config_default.xml";
		ConfigUtils.loadConfig(config, configFilename);
		config.controler().setLastIteration(10);
		/*====================================================================*/
		/* Setting parallelisation: */
		config.qsim().setNumberOfThreads(1);					/* Mobility simulation */
		config.global().setNumberOfThreads(1); 					/* Replanning */
		config.parallelEventHandling().setNumberOfThreads(1);	/* Events handling. */
		/*====================================================================*/
		Scenario sc = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(sc);
		controler.run();
		LOG.info("Single thread time:");
		Gbl.printElapsedTime();
		
		/* Running multi-threaded. First delete the output directory. */
		IOUtils.deleteDirectory(new File(config.controler().getOutputDirectory()));
		Gbl.startMeasurement();
		config = ConfigUtils.createConfig();
		ConfigUtils.loadConfig(config, configFilename);
		config.controler().setLastIteration(10);
		/*====================================================================*/
		/* Setting parallelisation: */
		config.qsim().setNumberOfThreads(2);					/* Mobility simulation */
		config.global().setNumberOfThreads(2); 					/* Replanning */
		config.parallelEventHandling().setNumberOfThreads(2);	/* Events handling. */
		/*====================================================================*/
		sc = ScenarioUtils.loadScenario(config);
		controler = new Controler(sc);
		controler.run();
		LOG.info("Multi-threaded time:");
		Gbl.printElapsedTime();
	}

}
