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
package scenarios.illustrative.singleCrossing.laemmer;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;

import signals.CombinedSignalsModule;
import signals.laemmer.model.LaemmerSignalsModule;


/**
 * @author dgrether
 * @author tthunig
 *
 */
public class SingleCrossingLaemmerMain {

	private final static double lambdaMax = 0.1;
	
	public static void main(String[] args) {
		
//		for (double lambdaWestEast = 0.0; lambdaWestEast <= lambdaMax; lambdaWestEast += 0.1){
//			
//		}
		double lambdaWestEast = 0.5;
		Scenario scenario = new SingleCrossingScenario().createScenario(lambdaWestEast, false);
		
		Controler controler = new Controler(scenario);
//		controler.addOverridingModule(new LaemmerSignalsModule());
		controler.addOverridingModule(new CombinedSignalsModule());
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		controler.run();

	}

}
