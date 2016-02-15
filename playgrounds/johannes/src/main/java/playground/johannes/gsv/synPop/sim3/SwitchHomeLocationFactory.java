/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.synPop.sim3;

import org.matsim.contrib.common.util.XORShiftRandom;

import java.util.Random;

public class SwitchHomeLocationFactory implements MutatorFactory {

	private final Random random;
	
	public SwitchHomeLocationFactory(Random random) {
		this.random = random;
	}
	
	@Override
	public Mutator newInstance() {
		return new SwitchOperator(new SwitchHomeLocation(), new XORShiftRandom(random.nextLong()));
	}

}
