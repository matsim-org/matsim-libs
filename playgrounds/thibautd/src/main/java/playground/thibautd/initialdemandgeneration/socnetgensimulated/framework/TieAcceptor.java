/* *********************************************************************** *
 * project: org.matsim.*
 * TieAcceptor.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
package playground.thibautd.initialdemandgeneration.socnetgensimulated.framework;

import playground.thibautd.initialdemandgeneration.socnetgen.framework.Agent;

/**
 * @author thibautd
 */
public class TieAcceptor<T extends Agent> {
	private final TieUtility<T> utility;

	private final double thresholdPrimary;
	private final double thresholdSecondary;

	public TieAcceptor(
			final TieUtility<T> utility,
			final double thresholdPrimary,
			final double thresholdSecondary ) {
		this.utility = utility;
		this.thresholdPrimary = thresholdPrimary;
		this.thresholdSecondary = thresholdSecondary;
	}

	public boolean acceptPrimary( T ego , T alter ) {
		return utility.getTieUtility( ego , alter ) > thresholdPrimary;
	}

	public boolean acceptSecondary( T ego , T alter ) {
		return utility.getTieUtility( ego , alter ) > thresholdSecondary;
	}
}

