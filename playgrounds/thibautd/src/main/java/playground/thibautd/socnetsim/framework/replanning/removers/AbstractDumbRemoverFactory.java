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
package playground.thibautd.socnetsim.framework.replanning.removers;

import com.google.inject.Provider;
import playground.thibautd.socnetsim.framework.replanning.ExtraPlanRemover;
import playground.thibautd.socnetsim.framework.replanning.selectors.GroupLevelPlanSelector;

/**
 * @author thibautd
 */
public abstract class AbstractDumbRemoverFactory implements Provider<ExtraPlanRemover> {

	private final int maxPlansPerAgent;

	public AbstractDumbRemoverFactory(
			final int maxPlansPerAgent) {
		this.maxPlansPerAgent = maxPlansPerAgent;
	}

	@Override
	public ExtraPlanRemover get() {
		return new DumbExtraPlanRemover(
				createSelector( ),
				maxPlansPerAgent );
	}

	protected abstract GroupLevelPlanSelector createSelector();
}

