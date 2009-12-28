/* *********************************************************************** *
 * project: org.matsim.*
 * ControlerForTests.java
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
package playground.mfeil.FilesForTests;


import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.scoring.ScoringFunctionFactory;


/**
 * @author Matthias Feil
 * Adjusting the Controler in order to call the PlanomatX. Replaces also the StrategyManagerConfigLoader.
 */
public class ControlerForTests extends Controler {
	
	
	public ControlerForTests (final Config config) {
		super(config);
	}
	
	
	@Override
	protected ScoringFunctionFactory loadScoringFunctionFactory() {
		return new JohScoringTestFunctionFactory();
	}
	
}
