/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.ikaddoura.router;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;

import playground.ikaddoura.analysis.vtts.VTTSHandler;
import playground.ikaddoura.analysis.vtts.VTTScomputation;

/**
* @author ikaddoura
*/

public class RunVTTSRouter {

	public static void main(String[] args) {

		Config config = ConfigUtils.createConfig();
		Controler controler = new Controler(config);

		final VTTSHandler vttsHandler = new VTTSHandler(controler.getScenario());
		final VTTSTimeDistanceTravelDisutilityFactory factory = new VTTSTimeDistanceTravelDisutilityFactory(vttsHandler, config.planCalcScore());
		factory.setSigma(0.);
		
		controler.addOverridingModule(new AbstractModule(){
			@Override
			public void install() {
				this.bindCarTravelDisutilityFactory().toInstance( factory );
			}
		}); 
		
		controler.addControlerListener(new VTTScomputation(vttsHandler));
		controler.run();
	}

}

