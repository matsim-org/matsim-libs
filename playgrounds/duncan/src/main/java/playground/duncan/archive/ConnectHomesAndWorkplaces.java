/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.duncan.archive;
/*
 * $Id: MyControler1.java,v 1.1 2007/11/14 12:00:28 nagel Exp $
 */

import java.io.PrintWriter;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.controler.Controler;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.knowledges.Knowledges;
import org.matsim.locationchoice.LocationMutator;
import org.matsim.locationchoice.RandomLocationMutator;

public class ConnectHomesAndWorkplaces {

	Config config ;

	public void run(final String[] args) {

		String configFile;
		if ( args.length==0 ) {
			configFile = "./src/playground/duncan/h2w-config.xml";
		} else {
			configFile = args[0] ;
		}
		ScenarioImpl scenario = ScenarioLoaderImpl.createScenarioLoaderImplAndResetRandomSeed(configFile).getScenario();

		this.config = scenario.getConfig();
		ConfigWriter configwriter = new ConfigWriter(this.config);
		configwriter.writeStream(new PrintWriter(System.out));

		// create the control(l)er:
		final Controler controler = new Controler(scenario);
//		controler.loadData() ;
		// (I think that the control(l)er is only needed to make the locationchoice module happy;
		// there is no logical reason why it is necessary. Kai)

		// create/read the network:
		new MatsimNetworkReader(scenario).readFile(this.config.network().getInputFile());

		// create/read the world (probably empty input file)
//		final World world = scenario.getWorld();
//		if (this.config.world().getInputFile() != null) {
//			final MatsimWorldReader worldReader = new MatsimWorldReader(scenario);
//			worldReader.readFile(this.config.world().getInputFile());
//		}
//		world.complete(config);

		MatsimFacilitiesReader fr = new MatsimFacilitiesReader(scenario) ;
		fr.readFile( this.config.facilities().getInputFile() ) ;

		// create the locachoice object:
		Knowledges knowledges = scenario.getKnowledges() ;
		LocationMutator locachoice = new RandomLocationMutator(controler.getNetwork(), controler, knowledges, MatsimRandom.getRandom()) ;

		final PopulationImpl plans = (PopulationImpl) scenario.getPopulation() ;
		plans.setIsStreaming(true);
		final PopulationReader plansReader = new MatsimPopulationReader(scenario);
		final PopulationWriter plansWriter = new PopulationWriter(plans, scenario.getNetwork(), knowledges);
		plansWriter.startStreaming(null);//config.plans().getOutputFile());
		plans.addAlgorithm(locachoice);
		plans.addAlgorithm(plansWriter); // planswriter must be the last algorithm added

		// I don't know why this works:
		plansReader.readFile(this.config.plans().getInputFile());
		plans.printPlansCount();
		plansWriter.closeStreaming();

		System.out.println("done.");
	}

	public static void main(final String[] args) {
		ConnectHomesAndWorkplaces app = new ConnectHomesAndWorkplaces();
		app.run(args);
	}

}
