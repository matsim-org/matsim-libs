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

import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.population.algorithms.PlanCalcType;
import org.matsim.ptproject.controller.PtController;


/**
 * @author dgrether
 *
 */
public class BKickControler extends PtController {

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
	protected Population loadPopulation() {
		Population pop = super.loadPopulation();
		new PlanCalcType().run(pop);
		return pop;
	}
}
