/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.vehicles;


import org.matsim.utils.objectattributes.attributable.Attributes;

/**
 * @author dgrether
 */
public class FreightCapacityImpl implements FreightCapacity {

	private double volume;
	private double weight;
	private int unit;
	private Attributes attributes = new Attributes() ;

	public FreightCapacityImpl(){}
	
	@Override
	public void setVolume(double cubicMeters) {
		this.volume = cubicMeters;
	}

	@Override
	public double getVolume() {
		return this.volume;
	}

	@Override
	public void setWeight(double tons) { this.weight = tons; }

	@Override
	public double getWeight() { return this.weight; }

	@Override
	public void setUnits(int units) {	this.unit = units; }

	@Override
	public int getUnits() {return this.unit; }

	@Override public Attributes getAttributes(){
		return this.attributes ;
	}
}
