/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.popsim;

import java.util.Random;

import playground.johannes.gsv.synPop.sim3.Mutator;
import playground.johannes.gsv.synPop.sim3.MutatorFactory;

/**
 * @author johannes
 *
 */
public class AgeMutatorFactory implements MutatorFactory {

	private final Random random;

	private final HistogramSync histSync;

	public AgeMutatorFactory(Random random, HistogramSync histSync) {
		this.random = random;
		this.histSync = histSync;
	}

	@Override
	public Mutator newInstance() {
		return new AgeMutator(random, histSync);
	}

}
