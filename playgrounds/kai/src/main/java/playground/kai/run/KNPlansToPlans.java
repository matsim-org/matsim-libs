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

package playground.kai.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author kn
 */
class KNPlansToPlans {

	void run(final String[] args) {
		
		Config config = ConfigUtils.createConfig() ;
//		config.plans().setInputFile( "/Users/nagel/kairuns/new-gauteng/output_base_vot110_3/100.plans.xml.gz") ;
//		config.network().setInputFile( "/Users/nagel/kairuns/new-gauteng/3sep-wo-toll-vot110_3-output/output_network.xml.gz") ;
		
		
		config.network().setInputFile("/Users/nagel/git/matsim/matsim/src/test/resources/test/scenarios/berlin/network.xml.gz");
		config.plans().setInputFile("/Users/nagel/git/matsim/contribs/common/src/test/resources/test/input/org/matsim/integration/always/ReRoutingTest/testReRoutingFastAStarLandmarks/1.plans.xml.gz");

		Scenario sc = ScenarioUtils.loadScenario(config) ;
		
		Population pop = sc.getPopulation() ;


//		PlansFilterByLegMode pf = new PlansFilterByLegMode( TransportMode.pt, FilterType.keepAllPlansWithMode ) ;
//		pf.run(pop) ;

//		PlanMutateTimeAllocation pm = new PlanMutateTimeAllocation( 60, new Random() ) ;
//		for (Person person : pop.getPersons().values()) {
//			Plan plan = person.getPlans().iterator().next();
//			pm.run(plan);
//		}

//		Population newPop = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getPopulation() ;
//		for ( Person person : pop.getPersons().values() ) {
//			Plan plan = person.getSelectedPlan() ;
//			List<Leg> legs = PopulationUtils.getLegs(plan) ;
//			boolean accept = true ;
////			for ( Leg leg : legs ) {
////				if ( leg.getMode().equals( TransportMode.car ) ) {
////					accept = false ;
////				}
////			}
//			if ( Math.random() < 0.9 ) accept = false ;
//			if ( accept ) {
//				System.out.println("adding person...");
//				newPop.addPerson(person);
//			}
//		}
		

		PopulationWriter popwriter = new PopulationWriter(pop,sc.getNetwork()) ;
		popwriter.write("/Users/nagel/kw/pop.xml.gz") ;

		System.out.println("done.");
	}

	public static void main(final String[] args) {
		KNPlansToPlans app = new KNPlansToPlans();
		app.run(args);
	}

}
