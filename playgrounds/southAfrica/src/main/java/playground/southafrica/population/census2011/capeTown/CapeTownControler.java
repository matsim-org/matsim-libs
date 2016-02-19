/* *********************************************************************** *
 * project: org.matsim.*
 * CapeTownControler.java                                                                        *
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
package playground.southafrica.population.census2011.capeTown;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.utils.misc.Time;

import playground.southafrica.utilities.Header;

/**
 * Class to execute the City of Cape Town simulation run that consists of 
 * both private individuals and commercial vehicles. The basic scenario was
 * set up using {@link CapeTownScenarioCleaner}. This particular run executes
 * a 10% sample that was generated through {@link CapeTownPopulationSampler}.
 * 
 * @author jwjoubert
 * 
 * @see {@link CapeTownScenarioCleaner}
 */
public class CapeTownControler {
	private final static Logger LOG = Logger.getLogger(CapeTownControler.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(CapeTownControler.class.toString(), args);
		
		String folder = args[0];
		folder += folder.endsWith("/") ? "" : "/";
		Machine machine = Machine.valueOf(args[1]);
		double fraction = Double.parseDouble(args[2]);
		
		Config config = setupConfig(folder, machine, fraction);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		
		Controler controler = new Controler(config);
		
		/* Bind the travel time and disutility functions to all modes that will
		 * assume network routes. */
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addTravelTimeBinding("ride").to(networkTravelTime());
				addTravelDisutilityFactoryBinding("ride").to(carTravelDisutilityFactoryKey());

				addTravelTimeBinding("commercial").to(networkTravelTime());
				addTravelDisutilityFactoryBinding("commercial").to(carTravelDisutilityFactoryKey());
			}
		});
		
		
		controler.run();
		
		Header.printFooter();
	}
	
	
	private static Config setupConfig(String folder, Machine machine, double fraction){
		Config config = ConfigUtils.createConfig();
		ConfigUtils.loadConfig(config, folder + "config.xml");
		
		/* Fix the seed. */
		config.global().setRandomSeed(201602151600l);

		/* Set the number of threads. */
		config.global().setNumberOfThreads(machine.getThreads());
		config.qsim().setNumberOfThreads(Math.min(machine.getThreads(), 8));
		config.parallelEventHandling().setOneThreadPerHandler(true);

		/* Depending on the sample size we throttle the network. */
		if(fraction == 1.0){
			/* Full model. */
			config.qsim().setFlowCapFactor(1.0);
			config.qsim().setStorageCapFactor(1.0);
		} else if(fraction == 0.1){
			config.qsim().setFlowCapFactor(0.1);
			config.qsim().setStorageCapFactor(0.3);
		} else if(fraction == 0.01){
			config.qsim().setFlowCapFactor(0.01);
			config.qsim().setStorageCapFactor(0.05);
		} else{
			throw new RuntimeException("Don't know how to adjust config for sample size " + fraction);
		}
		
		config.controler().setLastIteration(100);
		config.qsim().setEndTime(Time.parseTime("36:00:00"));
		config.controler().setOutputDirectory(folder + "output/");
		
		/* Set up the input files. */
		config.plans().setInputFile(folder + "population.xml.gz");
		config.plans().setInputPersonAttributeFile(folder + "populationAttributes.xml.gz");
		config.facilities().setInputFile(folder + "facilities.xml.gz");
		config.network().setInputFile(folder + "network.xml.gz");
		
		/* Set overall strategy. */
		StrategySettings expBeta = new StrategySettings();
		expBeta.setWeight(1.0);
		expBeta.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta.toString());
		config.strategy().addStrategySettings(expBeta);
		
		/* Set up replanning strategies for the different subpopulations. */
		/* Private. */
		StrategySettings pExpBeta = new StrategySettings();
		pExpBeta.setWeight(0.70);
		pExpBeta.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta.toString());
		pExpBeta.setSubpopulation("private");
		config.strategy().addStrategySettings(pExpBeta);
		
		StrategySettings pReRoute = new StrategySettings();
		pReRoute.setWeight(0.15);
		pReRoute.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute.toString());
		pReRoute.setSubpopulation("private");
		pReRoute.setDisableAfter(80);
		config.strategy().addStrategySettings(pReRoute);

		StrategySettings pTime = new StrategySettings();
		pTime.setWeight(0.15);
		pTime.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.TimeAllocationMutator.toString());
		pTime.setSubpopulation("private");
		pTime.setDisableAfter(80);
		config.strategy().addStrategySettings(pTime);
		
		/* Commercial. */
		StrategySettings cExpBeta = new StrategySettings();
		cExpBeta.setWeight(0.80);
		cExpBeta.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta.toString());
		cExpBeta.setSubpopulation("commercial");
		config.strategy().addStrategySettings(cExpBeta);
		
		StrategySettings cReRoute = new StrategySettings();
		cReRoute.setWeight(0.20);
		cReRoute.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute.toString());
		cReRoute.setSubpopulation("commercial");
		cReRoute.setDisableAfter(80);
		config.strategy().addStrategySettings(cReRoute);
		
		return config;
	}
	
	
	/**
	 * Setting up the default values for known machines on which simualtions
	 * are run.
	 *
	 * @author jwjoubert
	 */
	private enum Machine{
		HOBBES(40),
		MAC_MINI(4),
		MACBOOK_PRO(4);

		private final int threads;

		Machine(int threads){
			this.threads = threads;
		}

		public int getThreads(){
			return this.threads;
		}
	}
}
