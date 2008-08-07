/* *********************************************************************** *
 * project: org.matsim.*
 * MatsimRandom.java
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

package org.matsim.gbl;

import java.util.Random;

public class MatsimRandom {

	// the global random number generator
	// the seed is set by the config package (see matsim.gbl.Config)
	public static final Random random = new Random(MatsimRandom.DEFAULT_RANDOM_SEED);
	static final long DEFAULT_RANDOM_SEED = 4711;

}
