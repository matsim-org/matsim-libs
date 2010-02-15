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

package tutorial.programming.example9PlansHandling;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.api.experimental.ScenarioLoader;
import org.matsim.core.api.experimental.ScenarioLoaderFactoryImpl;
import org.matsim.core.config.Config;
import org.matsim.population.algorithms.PlansFilterByLegMode;
import org.matsim.population.algorithms.PlansFilterByLegMode.FilterType;

/**
 * @author kn after mrieser
 */
public class MyPlansToPlans {

	private Config config;

	public void run(final String[] args) {
		ScenarioLoader sl = (new ScenarioLoaderFactoryImpl()).createScenarioLoader(
				"examples/tutorial/multipleIterations.xml") ;
		Scenario sc = sl.loadScenario() ;
		Population pop = sc.getPopulation();

		PlansFilterByLegMode pf = new PlansFilterByLegMode( TransportMode.car, FilterType.keepAllPlansWithMode ) ;
		pf.run(pop) ;

		PopulationWriter popwriter = new PopulationWriter(pop,sc.getNetwork()) ;
		popwriter.write("output/pop.xml.gz") ;

		System.out.println("done.");
	}

	public static void main(final String[] args) {
		MyPlansToPlans app = new MyPlansToPlans();
		app.run(args);
	}

}
