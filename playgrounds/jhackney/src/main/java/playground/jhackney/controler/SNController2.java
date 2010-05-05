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

package playground.jhackney.controler;

import org.apache.log4j.Logger;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.StrategyManager;

import playground.jhackney.SocNetConfigGroup;
import playground.jhackney.replanning.SocialStrategyManagerConfigLoader;

public class SNController2 extends Controler {

	private final Logger log = Logger.getLogger(SNController2.class);

	public SNController2(String args[]){
		super(args);
		this.config.addModule(SocNetConfigGroup.GROUP_NAME, new SocNetConfigGroup());
	}

	public static void main(final String[] args) {
		final Controler controler = new SNController2(args);
		controler.addControlerListener(new SNControllerListener2());
		controler.setOverwriteFiles(true);
		controler.run();
		System.exit(0);
	}

	/**
	 * @return A fully initialized StrategyManager for the plans replanning.
	 */
	@Override
	protected StrategyManager loadStrategyManager() {
		StrategyManager manager = new StrategyManager();
		SocialStrategyManagerConfigLoader.load(this, this.getConfig(), manager);
		return manager;
	}
}
