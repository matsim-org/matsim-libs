/* *********************************************************************** *
 * project: kai
 * PopulationReduce.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.kai.analysis;

import java.io.IOException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.config.Config;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

/**
 * @author nagel
 *
 */
public class PopulationReduce {

	private void run() throws IOException {
		Population popIn = null ;
		Population popOut = null ; 
		
		{
			Config config1 = ConfigUtils.createConfig() ;
			config1.network().setInputFile("/Users/nagel/kairuns/19jun-w-ba16ext/kairun5-incl-ba16ext.output_network.xml.gz") ;
//			config1.plans().setInputFile("/Users/nagel/kairuns/18jun-base/kairun3-incl-ba16.reduced_plans.xml.gz") ;
			config1.plans().setInputFile("/Users/nagel/kairuns/19jun-w-ba16ext/kairun5-incl-ba16ext.output_plans.xml.gz") ;
			Scenario sc1 = ScenarioUtils.loadScenario(config1) ;
			popIn = sc1.getPopulation() ;
		}
		{
			Config config2 = ConfigUtils.createConfig() ;
			Scenario sc2 = ScenarioUtils.createScenario(config2) ;
			popOut = sc2.getPopulation() ;
		}
		for ( Person personIn : popIn.getPersons().values() ) {
			Person personOut = popOut.getFactory().createPerson( personIn.getId() ) ;
			popOut.addPerson(personOut) ;
			
			for ( Plan planIn : personIn.getPlans() ) {
				Plan planOut = popOut.getFactory().createPlan() ;
				planOut.setScore(planIn.getScore()) ;
				Activity actIn = (Activity) planIn.getPlanElements().get(0) ;
				planOut.addActivity(actIn) ;
				personOut.addPlan(planOut);
			}
			
		}
		MatsimWriter popWriter = new PopulationWriter(popOut,null) ;
		popWriter.write("/Users/nagel/kairuns/pop.xml.gz") ;
		
		
	}
	
	public static void main( String[] args ) throws IOException {
		new PopulationReduce().run() ;
	}
}
