/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.thibautd.maxess.nestedlogitaccessibility.scripts;

import playground.thibautd.maxess.nestedlogitaccessibility.framework.AccessibilityComputationResult;
import playground.thibautd.maxess.nestedlogitaccessibility.writers.BasicPersonAccessibilityWriter;

/**
 * @author thibautd
 */
public class AdvantageColumnCalculator implements BasicPersonAccessibilityWriter.ColumnCalculator {
	private final String name;
	private final String better;
	private final String worse;

	public AdvantageColumnCalculator(
			final String name,
			final String better,
			final String worse ) {
		this.name = name;
		this.better = better;
		this.worse = worse;
	}

	@Override
	public String getColumnName() {
		return name;
	}

	@Override
	public double computeValue( AccessibilityComputationResult.PersonAccessibilityComputationResult personResults ) {
		final double noCar = personResults.getAccessibilities().get( worse );
		final double all = personResults.getAccessibilities().get( better );
		return all - noCar;
	}
}
