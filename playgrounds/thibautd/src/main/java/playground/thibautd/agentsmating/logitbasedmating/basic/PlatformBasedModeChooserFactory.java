/* *********************************************************************** *
 * project: org.matsim.*
 * PlatformBasedModeChooserFactory.java
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
package playground.thibautd.agentsmating.logitbasedmating.basic;

import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.internal.MatsimFactory;
import org.matsim.core.scenario.ScenarioImpl;

import playground.thibautd.agentsmating.logitbasedmating.framework.ChoiceModel;
import playground.thibautd.agentsmating.logitbasedmating.framework.CliquesConstructor;
import playground.thibautd.agentsmating.logitbasedmating.framework.MatingPlatform;
import playground.thibautd.agentsmating.logitbasedmating.framework.PlatformBasedModeChooser;

/**
 * @author thibautd
 */
public class PlatformBasedModeChooserFactory implements MatsimFactory {
	public PlatformBasedModeChooser createModeChooser(
			final Population population,
			final ChoiceModel choiceModel,
			final MatingPlatform platform,
			final CliquesConstructor cliquesConstructor) {
		PlatformBasedModeChooser chooser = new PlatformBasedModeChooser();

		chooser.setPopulation( population );
		chooser.setChoiceModel( choiceModel );
		chooser.setPlatform( platform );
		chooser.setCliquesConstructor( cliquesConstructor );

		return chooser;
	}

	public PlatformBasedModeChooser createModeChooser(
			final ScenarioImpl scenario,
			final ChoiceModel choiceModel) {
		return createModeChooser(
				scenario.getPopulation(),
				choiceModel,
				defaultPlatform( choiceModel ),
				defaultCliquesConstructor( scenario ));
	}

	private MatingPlatform defaultPlatform( final ChoiceModel model ) {
		return new MatingPlatformImpl( model, new MateProposerImpl() );
	}

	private CliquesConstructor defaultCliquesConstructor( final ScenarioImpl scenario ) {
		return new CliquesConstructorImpl( scenario );
	}
}

