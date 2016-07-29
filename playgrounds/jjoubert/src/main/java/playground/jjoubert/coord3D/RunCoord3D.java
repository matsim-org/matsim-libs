/* *********************************************************************** *
 * project: org.matsim.*
 * RunCoord3D.java                                                                        *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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
package playground.jjoubert.coord3D;

import java.io.File;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.utils.misc.Time;

import playground.southafrica.utilities.FileUtils;
import playground.southafrica.utilities.Header;

/**
 *
 * @author jwjoubert
 */
public class RunCoord3D {
	final private static Logger LOG = Logger.getLogger(RunCoord3D.class);
	private static String path;
	final private static int NUMBER_OF_PERSONS = 900;
	final private static int ITERS = 100;
	final private static int RUNS = 100;
	final private static long SEED = 20160728l;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(RunCoord3D.class.toString(), args);
		
		path = args[0];
		path += path.endsWith("/") ? "" : "/";
		
		for(int run = 1; run <= RUNS; run++){
			LOG.info("==================== RUN " + run + " ====================");
			Scenario sc = ScenarioBuilder3D.buildScenario(NUMBER_OF_PERSONS, SEED, run);
			sc = setupConfig(sc, path, run);
			new ConfigWriter(sc.getConfig()).write(path + "config.xml");
			
			Controler controler = setupControler(sc);
			
			controler.run();
			
			/* Remove all iteration folders except the first and the last. */
			for(int i = 1; i < ITERS; i++){
				File folder = new File(path + "output_" + run + "/ITERS/it." + i + "/");
				FileUtils.delete(folder);
			}
		}
		
		Header.printFooter();
	}
	
	
	/**
	 * Make all the required adjustments to the {@link Config}.
	 * @param sc
	 * @return
	 */
	private static Scenario setupConfig(Scenario sc, String path, int run){
		LOG.info("Setting up the config...");
		Config config = sc.getConfig();
		
		/* Fix the seed and threads. */
		config.global().setRandomSeed(SEED * run);
		config.global().setNumberOfThreads(1);
		config.qsim().setNumberOfThreads(1);
		
		/* Set the number of iterations and intervals. */
		config.controler().setLastIteration(ITERS);
		config.controler().setWriteEventsInterval(ITERS);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		
		/* Set the queue type. */
		config.qsim().setLinkDynamics("PassingQ");
		
		/* Set input/output files and paths. */
		config.plans().setInputFile(path + "population.xml.gz");
		config.plans().setInputPersonAttributeFile(path + "populationAttributes.xml.gz");
		config.network().setInputFile(path + "network.xml.gz");
		config.vehicles().setVehiclesFile(path + "vehicles.xml.gz");
		config.controler().setOutputDirectory(path + "output_" + run + "/");
		
		/* Set activity parameters. */
		ActivityParams a1 = new ActivityParams("a1");
		a1.setTypicalDuration(Time.parseTime("01:00:00"));
		config.planCalcScore().addActivityParams(a1);
		ActivityParams a2 = new ActivityParams("a2");
		a2.setTypicalDuration(Time.parseTime("01:00:00"));
		config.planCalcScore().addActivityParams(a2);
		ActivityParams a3 = new ActivityParams("a3");
		a3.setTypicalDuration(Time.parseTime("01:00:00"));
		config.planCalcScore().addActivityParams(a3);
		
		/* Set strategies. */
		StrategySettings best = new StrategySettings();
		best.setWeight(0.85);
		best.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta.toString());
		config.strategy().addStrategySettings(best);
		
		StrategySettings reroute = new StrategySettings();
		reroute.setWeight(0.15);
		reroute.setDisableAfter((int) (0.9*config.controler().getLastIteration()));
		reroute.setStrategyName("randomRerouter");
//		reroute.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute.toString());
		config.strategy().addStrategySettings(reroute);
		
		LOG.info("Done setting up the config.");
		return sc;
	}
	
	/**
	 * Make all the required adjustments to the {@link Controler}.
	 * @param sc
	 * @return
	 */
	private static Controler setupControler(Scenario sc){
		LOG.info("Setting up the controler...");
		Controler controler = new Controler(sc);
		
		controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
			@Override
			public ScoringFunction createNewScoringFunction(Person person) {
				return new ElevationScoringFunction(
						sc.getNetwork(), 
						sc.getPopulation().getPersonAttributes().getAttribute(person.getId().toString(), "vehicleType").toString());
			}
		});
		
		controler.addOverridingModule(new AbstractModule() {			
			@Override
			public void install() {
				this.addControlerListenerBinding().to(ElevationControlerListener.class);
				this.bind(ElevationEventHandler.class);
				this.addPlanStrategyBinding("randomRerouter").toProvider(EquilRandomRouterFactory.class);
			}
		});
		
		LOG.info("Done setting up the controler.");
		return controler;
	}
	
	
	
	
	
	

}
