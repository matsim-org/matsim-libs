/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.kai.usecases.freight;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierImpl;
import org.matsim.contrib.freight.carrier.CarrierPlanStrategyManagerFactory;
import org.matsim.contrib.freight.carrier.CarrierScoringFunctionFactory;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.controler.CarrierController;
import org.matsim.contrib.freight.mobsim.ScoringFunctionFactoryForTests;
import org.matsim.contrib.freight.mobsim.StrategyManagerFactoryForTests;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author nagel
 *
 */
final class Main {

	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig() ;
		
		Scenario sc = ScenarioUtils.createScenario(config) ;
		
		Carriers carriers = new Carriers() ;
		{
			Id id = sc.createId("testCarrier") ;
			Carrier carrier = CarrierImpl.newInstance(id) ; 
			carriers.getCarriers().put(id, carrier) ;
		}

		CarrierPlanStrategyManagerFactory strategyManagerFactory  = new StrategyManagerFactoryForTests() ;

		CarrierScoringFunctionFactory scoringFunctionFactory = new ScoringFunctionFactoryForTests(sc.getNetwork()) ;

		CarrierController listener = new CarrierController(carriers, strategyManagerFactory, scoringFunctionFactory ) ;
		
		Controler ctrl = new Controler( sc ) ;
		ctrl.addControlerListener(listener) ;
		ctrl.run();

	}

}
