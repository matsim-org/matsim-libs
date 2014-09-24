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

package playground.johannes.gsv.synPop.invermo.sim;

import java.util.Random;

import playground.johannes.gsv.synPop.sim.RandomFacilities;
import playground.johannes.gsv.synPop.sim2.Mutator;
import playground.johannes.gsv.synPop.sim2.MutatorFactory;

/**
 * @author johannes
 *
 */
public class StartLocationMutatorFactory implements MutatorFactory {

	private final RandomFacilities facilities;
	
	private final Random random;
	
	public StartLocationMutatorFactory(RandomFacilities facilities, Random random) {
		this.facilities = facilities;
		this.random = random;
	}
	
	@Override
	public Mutator newInstance() {
		return new MutateStartLocation(facilities, random);
	}

}
