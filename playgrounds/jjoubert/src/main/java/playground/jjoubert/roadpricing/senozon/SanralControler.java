/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.jjoubert.roadpricing.senozon;

import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;

public class SanralControler extends Controler {

	public SanralControler(String configFileName) {
		super(configFileName);
	}

	public SanralControler(Config config) {
		super(config);
	}

	@Override
	protected void loadCoreListeners() {
		super.loadCoreListeners();
		// add custom road pricing listener after all others, so it will be loaded first!
		this.addControlerListener(new SanralRoadPricing());
	}

}
