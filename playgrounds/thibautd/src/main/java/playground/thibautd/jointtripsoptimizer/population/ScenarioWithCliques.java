/* *********************************************************************** *
 * project: org.matsim.*
 * ScenarioWithCliques.java
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
package playground.thibautd.jointtripsoptimizer.population;

import org.apache.log4j.Logger;


import org.matsim.core.config.Config;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.misc.ConfigUtils;

/**
 * @author thibautd
 */
public class ScenarioWithCliques extends ScenarioImpl {
	private static final Logger log =
		Logger.getLogger(ScenarioWithCliques.class);

	private PopulationOfCliques cliques=null;

	/*
	 * =========================================================================
	 * Constructors
	 * =========================================================================
	 */
	public ScenarioWithCliques() {
		super(ConfigUtils.createConfig());
		super.setPopulation(new PopulationWithCliques(this));
		this.cliques = new PopulationOfCliques(this);
	}

	public ScenarioWithCliques(Config config) {
		super(config);
		super.setPopulation(new PopulationWithCliques(this));
		this.cliques =  new PopulationOfCliques(this);
		log.debug("populations initialized");
	}

	/*
	 * =========================================================================
	 * other
	 * =========================================================================
	 */
	public PopulationOfCliques getCliques() {
		return this.cliques;
	}
}

