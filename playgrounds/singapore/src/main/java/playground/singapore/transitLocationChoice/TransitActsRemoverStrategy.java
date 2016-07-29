/* *********************************************************************** *
 * project: org.matsim.*
 * TransitActsRemoverStrategy.java
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

package playground.singapore.transitLocationChoice;

import org.matsim.core.config.Config;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;

public class TransitActsRemoverStrategy extends AbstractMultithreadedModule {

	public TransitActsRemoverStrategy(Config config) {
		super(config.global());
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		return new TransitActsRemover();
	}

}
