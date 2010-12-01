/* *********************************************************************** *
 * project: org.matsim.*
 * CadytsExpBetaChangerChoice.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.yu.integration.cadyts.parameterCalibration.withCarCounts.mnlValidation;

import org.matsim.core.replanning.selectors.ExpBetaPlanChanger;

public interface CadytsExpBetaChangerChoice extends CadytsChoice {

	public abstract ExpBetaPlanChanger getCadytsExpBetaChanger();

}