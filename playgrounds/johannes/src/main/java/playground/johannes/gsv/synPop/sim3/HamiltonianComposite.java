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

import playground.johannes.sna.util.Composite;
import playground.johannes.synpop.data.Person;

import java.util.ArrayList;
import java.util.List;

/**
 * @author johannes
 *
 */
public class HamiltonianComposite extends Composite<Hamiltonian> implements Hamiltonian {

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

	/*
	 * TODO: hide access?
	 */
	public List<Hamiltonian> getComponents() {
		return components;
	}
	
	@Override
	public double evaluate(Person person) {
		double sum = 0;
		
		for(int i = 0; i < components.size(); i++) {
			sum += thetas.get(i) * components.get(i).evaluate(person);
		}
		
		return sum;
	}

}
