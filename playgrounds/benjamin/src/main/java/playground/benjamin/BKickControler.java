/* *********************************************************************** *
 * project: org.matsim.*
 * BKickControler
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.benjamin;

import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.population.algorithms.PlanCalcType;


/**
 * @author dgrether
 *
 */
public class BKickControler extends Controler {

	public BKickControler(String configFileName) {
		super(configFileName);
	}

	public BKickControler(Config conf){
		super(conf);
	}

	public BKickControler(String[] args) {
		super(args);
	}

	@Override
	protected void loadData() {
		super.loadData();
		new PlanCalcType().run(this.population);
	}
}
