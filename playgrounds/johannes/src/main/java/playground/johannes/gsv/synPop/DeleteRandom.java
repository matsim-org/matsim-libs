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

package playground.johannes.gsv.synPop;

import org.matsim.contrib.common.util.XORShiftRandom;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.processing.PersonTask;

import java.util.Random;

/**
 * @author johannes
 *
 */
public class DeleteRandom implements PersonTask {

	private final Random random;
	
	private final double proba;
	
	public DeleteRandom(double proba) {
		this.random = new XORShiftRandom();
		this.proba = proba;
	}
	
	public DeleteRandom(double proba, Random random) {
		this.random = random;
		this.proba = proba;
	}
	@Override
	public void apply(Person person) {
		if(proba > random.nextDouble()) {
			person.setAttribute(CommonKeys.DELETE, "true");
		}

	}

}
