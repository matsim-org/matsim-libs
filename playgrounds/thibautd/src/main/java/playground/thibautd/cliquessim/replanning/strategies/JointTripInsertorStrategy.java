/* *********************************************************************** *
 * project: org.matsim.*
 * JointTripInsertorStrategy.java
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
package playground.thibautd.cliquessim.replanning.strategies;

import org.matsim.core.controler.Controler;

import playground.thibautd.cliquessim.replanning.JointPlanStrategy;
import playground.thibautd.cliquessim.replanning.modules.jointtimemodechooser.JointTimeModeChooserModule;
import playground.thibautd.cliquessim.replanning.modules.jointtripinsertor.JointTripInsertorModule;
import playground.thibautd.cliquessim.replanning.modules.reroute.JointReRouteModule;
import playground.thibautd.cliquessim.replanning.selectors.RandomPlanSelectorWithoutCasts;

/**
 * @author thibautd
 */
public class JointTripInsertorStrategy extends JointPlanStrategy {

	public JointTripInsertorStrategy(final Controler controler) {
		super( new RandomPlanSelectorWithoutCasts() );

		addStrategyModule( new JointTripInsertorModule( controler ) );
		addStrategyModule( new JointReRouteModule( controler ) );
		addStrategyModule( new JointTimeModeChooserModule( controler ) );
	}
}

