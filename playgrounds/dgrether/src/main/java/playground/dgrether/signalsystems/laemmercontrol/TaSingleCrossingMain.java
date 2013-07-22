/* *********************************************************************** *
 * project: org.matsim.*
 * TaSingleCrossingMain
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.dgrether.signalsystems.laemmercontrol;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.Controler;

import playground.dgrether.signalsystems.laemmer.controler.DgTaControlerListenerFactory;
import playground.dgrether.signalsystems.laemmer.testisolatedcrossing.SingleCrossingScenario;


/**
 * @author dgrether
 *
 */
public class TaSingleCrossingMain {

	private final static double lambdaMax = 0.1;
	
	public static void main(String[] args) {
		
		for (double lambdaWestEast = 0.0; lambdaWestEast <= lambdaMax; lambdaWestEast += 0.1){
			
		}
		double lambdaWestEast = 0.5;
		Scenario scenario = new SingleCrossingScenario().createScenario(lambdaWestEast);
		
		Controler controler = new Controler(scenario);
		controler.setSignalsControllerListenerFactory(new DgTaControlerListenerFactory());
		controler.setOverwriteFiles(true);
		controler.run();

	}

}
