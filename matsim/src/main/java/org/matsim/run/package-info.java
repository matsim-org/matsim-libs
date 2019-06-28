
/* *********************************************************************** *
 * project: org.matsim.*
 * package-info.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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
 * This package contains classes that can directly be started from the command
 * line and that do something useful.
 * <br>
 * All the classes in this package should contain a main-method that
 * parses arguments, displays a short usage-note if arguments are missing or
 * make no sense, and otherwise do some (hopefully useful) work.
 * <br>
 * The code for the actual work these classes do should not be within this
 * package, but in other packages (e.g. {@link org.matsim.analysis},
 * {@link org.matsim.core.population.algorithms}, ...). In this package are really only
 * classes with main-methods that offer some functionality to the outside by
 * using code from other packages.
 * <br>
 * <b>Please contact Marcel Rieser before you put anything into this directory.</b>
 */
package org.matsim.run;
