/* *********************************************************************** *
 * project: analysis
 * package-info.java
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

/**
 * Container to collect matsim analysis tools.  They should fulfill the following conditions:<ul>
 * <li> They should not maven-depend on anything besides the core (everything inside the matsim maven module).
 * <ul>
 * <p></p>
 * In order to not too much blow up the maven repository, it is assumed that inside this analysis directory there are multiple 
 * packages maintained by different persons each.
 * 
 * @author nagel
 *
 */
package org.matsim.contrib.analysis;