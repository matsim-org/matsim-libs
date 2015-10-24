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

import org.matsim.contrib.socnetgen.socialnetworks.utils.XORShiftRandom;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author johannes
 *
 */
public class MutatorCompositeFactory implements MutatorFactory {

	private final List<MutatorFactory> factories;
	
	private final Random random;
	
	public MutatorCompositeFactory(Random random) {
		this.random = random;
		this.factories = new ArrayList<>();
	}
	
	public void addFactory(MutatorFactory factory) {
		factories.add(factory);
	}
	
	@Override
	public Mutator newInstance() {
		MutatorComposite composite = new MutatorComposite(new XORShiftRandom(random.nextLong()));
		for(MutatorFactory factory : factories) {
			composite.addMutator(factory.newInstance());
		}
		
		return composite;
	}

}
