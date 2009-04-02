/* *********************************************************************** *
 * project: org.matsim.*
 * ConfigConsistencyChecker
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
package org.matsim.core.config.consistency;

import org.matsim.core.config.Config;


/**
 * Implement this interface to implement consistency checks of the
 * MATSim config. Implementations of this class that are added 
 * to the Config instance used are triggered by the parser after
 * the config.xml file is read.
 * 
 * 
 * @author dgrether
 *
 */
public interface ConfigConsistencyChecker {

	public void checkConsistency(Config config);
	
}
