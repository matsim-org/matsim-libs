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

package playground.johannes.gsv.synPop.sim2;

import java.util.Random;

import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.sna.util.Composite;

/**
 * @author johannes
 *
 */
public class MutatorComposite extends Composite<Mutator> implements Mutator {

	private final Random random;
	
	private Mutator active;
	
	public MutatorComposite(Random random) {
		this.random = random;
	}
	
	@Override
	public boolean modify(ProxyPerson person) {
		active = components.get(random.nextInt(components.size()));
		return active.modify(person);
	}

	@Override
	public void revert(ProxyPerson person) {
		active.revert(person);
		active = null;
	}

}
