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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import playground.johannes.gsv.synPop.ProxyPerson;

/**
 * @author johannes
 *
 */
public class RandomSelector implements Mutator {

	private final Random random;
	
	private final SingleMutator delegate;
	
	private final List<ProxyPerson> mutation;
	
	public RandomSelector(SingleMutator delegate, Random random) {
		this.delegate = delegate;
		this.random = random;
		mutation = new ArrayList<>(1);
		mutation.add(null);
	}
	
	@Override
	public List<ProxyPerson> select(List<ProxyPerson> persons) {
		mutation.set(0, persons.get(random.nextInt(persons.size())));
		return mutation;
	}

	@Override
	public boolean modify(List<ProxyPerson> persons) {
		return delegate.mutate(persons.get(0));
	}

	@Override
	public void revert(List<ProxyPerson> persons) {
		delegate.revert(persons.get(0));
		
	}

}
