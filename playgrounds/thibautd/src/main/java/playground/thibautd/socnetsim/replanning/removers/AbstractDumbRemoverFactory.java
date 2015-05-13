/* *********************************************************************** *
 * project: org.matsim.*
 * AbstractDumbRemoverFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.thibautd.socnetsim.replanning.removers;

import org.matsim.api.core.v01.Scenario;

import com.google.inject.Provider;

import playground.thibautd.socnetsim.replanning.GroupReplanningConfigGroup;
import playground.thibautd.socnetsim.framework.replanning.ExtraPlanRemover;
import playground.thibautd.socnetsim.replanning.selectors.GroupLevelPlanSelector;

/**
 * @author thibautd
 */
abstract class AbstractDumbRemoverFactory implements Provider<ExtraPlanRemover> {

	private final Scenario sc;
	
	public AbstractDumbRemoverFactory( Scenario sc ) {
		this.sc = sc;
	}

	@Override
	public ExtraPlanRemover get() {
		final GroupReplanningConfigGroup conf = (GroupReplanningConfigGroup)
				sc.getConfig().getModule( GroupReplanningConfigGroup.GROUP_NAME );
		return new DumbExtraPlanRemover(
				createSelector( ),
				conf.getMaxPlansPerAgent() );
	}

	protected abstract GroupLevelPlanSelector createSelector();
}

