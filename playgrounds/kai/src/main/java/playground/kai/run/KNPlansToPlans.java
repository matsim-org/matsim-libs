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
import org.matsim.api.core.v01.population.Person;
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
		String inputPopFilename = null ;
		String outputPopFilename = null ;
		String netFilename = null ;
				
		if ( args!=null ) {
			if ( !(args.length==2 || args.length==3) ) {
				System.err.println( "Usage: cmd inputPop.xml.gz outputPop.xml.gz [network.xml.gz]");
			} else {
				inputPopFilename = args[0] ;
				outputPopFilename = args[1] ;
				if ( args.length==3 ) {
					netFilename = args[2] ;
				}
			}
		}
		
		
		Config config = ConfigUtils.createConfig() ;
		config.network().setInputFile( netFilename ) ;
		config.plans().setInputFile( inputPopFilename ) ;

		Scenario sc = ScenarioUtils.loadScenario(config) ;
		
		Population pop = sc.getPopulation() ;


//		PlansFilterByLegMode pf = new PlansFilterByLegMode( TransportMode.pt, FilterType.keepAllPlansWithMode ) ;
//		pf.run(pop) ;

//		PlanMutateTimeAllocation pm = new PlanMutateTimeAllocation( 60, new Random() ) ;
//		for (Person person : pop.getPersons().values()) {
//			Plan plan = person.getPlans().iterator().next();
//			pm.run(plan);
//		}

		Population newPop = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getPopulation() ;
		for ( Person person : pop.getPersons().values() ) {
//			Plan plan = person.getSelectedPlan() ;
//			List<Leg> legs = PopulationUtils.getLegs(plan) ;
//			boolean accept = true ;
////			for ( Leg leg : legs ) {
////				if ( leg.getMode().equals( TransportMode.car ) ) {
////					accept = false ;
////				}
////			}
//			if ( Math.random() < 0.9 ) accept = false ;
			if ( Math.random() < 0.1 ) {
				System.out.println("adding person...");
				newPop.addPerson(person);
			}
		}
		

		PopulationWriter popwriter = new PopulationWriter(newPop,sc.getNetwork()) ;
		popwriter.write( outputPopFilename ) ;

		System.out.println("done.");
	}

	public static void main(final String[] args) {
		KNPlansToPlans app = new KNPlansToPlans();
		app.run(args);
	}

}
