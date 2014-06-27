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

package playground.johannes.gsv.synPop.sim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;

import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.sna.util.Composite;

/**
 * @author johannes
 *
 */
public class CompositeHamiltonian extends Composite<Hamiltonian> implements Hamiltonian {
	
	private static final Logger logger = Logger.getLogger(CompositeHamiltonian.class);

	private List<Double> thetas = new ArrayList<Double>();
	
	public void addComponent(Hamiltonian h) {
		super.addComponent(h);
		thetas.add(1.0);
	}
	
	public void addComponent(Hamiltonian h, double theta) {
		super.addComponent(h);
		thetas.add(theta);
	}
	
	public void removeComponent(Hamiltonian h) {
		int idx = components.indexOf(h);
		super.removeComponent(h);
		thetas.remove(idx);
	}
	
	/* (non-Javadoc)
	 * @see playground.johannes.gsv.synPop.sim.Hamiltonian#delta(playground.johannes.gsv.synPop.ProxyPerson, playground.johannes.gsv.synPop.ProxyPerson)
	 */
	@Override
	public double evaluate(ProxyPerson original, ProxyPerson modified) {
		double sum = 0;
		
		for(Hamiltonian component : components) {
			sum += component.evaluate(original, modified);
		}
		
		return sum;
	}

	/* (non-Javadoc)
	 * @see playground.johannes.gsv.synPop.sim.Hamiltonian#evaluate(java.util.Collection)
	 */
	@Override
	public double evaluate(Collection<ProxyPerson> persons) {
		double sum = 0;
		
		StringBuilder builder = new StringBuilder();
		builder.append("Score for hamiltonian terms:");
		for(Hamiltonian component : components) {
			double val = component.evaluate(persons);
			sum += val;
			builder.append(String.format("\n\t%s: %s", component.getClass().getCanonicalName(), val));
		}
		logger.info(builder.toString());
		return sum;
	}

}
