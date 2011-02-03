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

package playground.kai.plansToPlans;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.api.experimental.ScenarioLoader;
import org.matsim.core.api.experimental.ScenarioLoaderFactoryImpl;
import org.matsim.core.config.Config;

/**
 * @author kn after mrieser
 */
public class MyPlansToPlans {

	private Config config;

	public void run(final String[] args) {
//		this.config = Gbl.createConfig(new String[]{"../padang/dlr-network/pconfig.xml"});
//		new ConfigWriter(this.config).writeStream(new PrintWriter(System.out));

//		ScenarioImpl scenario = new ScenarioImpl(this.config);
//
////		final World world = Gbl.getWorld();
////
////		if (this.config.world().getInputFile() != null) {
////			final MatsimWorldReader worldReader = new MatsimWorldReader(world);
////			worldReader.readFile(this.config.world().getInputFile());
////		}
//
//		NetworkLayer network = scenario.getNetwork();
//		new MatsimNetworkReader(network).readFile(this.config.network().getInputFile());
//
//		final PopulationImpl plans = scenario.getPopulation();
//		plans.setIsStreaming(true);
//		final PopulationReader plansReader = new MatsimPopulationReader(scenario);
//		final PopulationWriter plansWriter = new PopulationWriter(plans);
//		plansWriter.startStreaming(this.config.plans().getOutputFile());
////		plans.addAlgorithm(new org.matsim.population.algorithms.XY2Links(network));
//		plans.addAlgorithm(new org.matsim.population.algorithms.PlansFilterByLegMode(TransportMode.pt,false));
//		plans.addAlgorithm(plansWriter); // planswriter must be the last algorithm added
//		plansReader.readFile(this.config.plans().getInputFile());
//		plans.printPlansCount();
//		plansWriter.closeStreaming();

		ScenarioLoader sl = (new ScenarioLoaderFactoryImpl()).createScenarioLoader(
//		"/Users/nagel/shared-svn/projects/detailedEval/teststrecke/sim/output/20090707/output_config.xml" ) ;
				"otfvis-config.xml" ) ;
		Scenario sc = sl.loadScenario() ;
		Population pop = sc.getPopulation();

		
//		PlansFilterByLegMode pf = new PlansFilterByLegMode( TransportMode.pt, FilterType.keepAllPlansWithMode ) ;
//		pf.run(pop) ;

		
		
		
//		PlanMutateTimeAllocation pm = new PlanMutateTimeAllocation( 60, new Random() ) ;
//		for (Person person : pop.getPersons().values()) {
//			Plan plan = person.getPlans().iterator().next();
//			pm.run(plan);
//		}

		
		
		Scenario newScen = new ScenarioImpl() ;
		Population newPop = newScen.getPopulation() ;
		for ( Person person : pop.getPersons().values() ) {
			if ( Math.random() < 0.1 ) 
				newPop.addPerson( person ); 
		}
		

		PopulationWriter popwriter = new PopulationWriter(newPop,sc.getNetwork()) ;
		popwriter.write("/Users/nagel/kw/pop.xml.gz") ;

		System.out.println("done.");
	}

	public static void main(final String[] args) {
		MyPlansToPlans app = new MyPlansToPlans();
		app.run(args);
	}

}
