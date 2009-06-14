/* *********************************************************************** *
 * project: org.matsim.*
 * ScenarioConsistencyChecker
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
package org.matsim.core.api.consistency;

import org.matsim.api.core.v01.Scenario;

/**
 * Implementations of this interface should only check consistency 
 * between scenario elements modules that can't be resolved by a
 * more specific and modular consistency checker.
 * @author dgrether
 * @deprecated just a draft to be discussed, don't implement this interface yet.
 */
@Deprecated
public interface ScenarioConsistencyChecker {
	
	public boolean checkConsistency(Scenario scenario);

}
