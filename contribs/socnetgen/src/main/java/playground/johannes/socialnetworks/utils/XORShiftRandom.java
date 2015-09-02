/* *********************************************************************** *
 * project: org.matsim.*
 * XORShiftRandom.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.utils;

import java.util.Random;

/**
 * @author illenberger
 * 
 */
public class XORShiftRandom extends Random {

	private static final long serialVersionUID = 1620841289225382129L;
	
	private long seed;

	public XORShiftRandom() {
		super();
	}

	public XORShiftRandom(long seed) {
		super(seed);
	}

	@Override
	public void setSeed(long seed) {
		if (seed == 0)
			throw new IllegalArgumentException("Zero is not allowd as seed number.");
		else
			this.seed = seed;

		super.setSeed(seed);
	}

	protected int next(int nbits) {
		long x = this.seed;
		x ^= (x << 21);
		x ^= (x >>> 35);
		x ^= (x << 4);
		this.seed = x;
		x &= ((1L << nbits) - 1);
		return (int) x;
	}
}
