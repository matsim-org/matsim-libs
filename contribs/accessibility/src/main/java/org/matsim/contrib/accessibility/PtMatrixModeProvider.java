/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package org.matsim.contrib.accessibility;

import javax.inject.Inject;
import javax.inject.Provider;

import org.matsim.contrib.matrixbasedptrouter.PtMatrix;
import org.matsim.core.config.Config;

/**
 * @author nagel
 *
 */
public class PtMatrixModeProvider implements Provider<AccessibilityContributionCalculator>{
	private final PtMatrix ptMatrix ;
	@Inject Config config ;
	public PtMatrixModeProvider(PtMatrix ptMatrix2) {
		this.ptMatrix = ptMatrix2;
	}
	@Override public AccessibilityContributionCalculator get() {
		return PtMatrixAccessibilityContributionCalculator.create(ptMatrix, config) ;
	}

}
