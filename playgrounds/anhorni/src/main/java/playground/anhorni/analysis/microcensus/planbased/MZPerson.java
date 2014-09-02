/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.anhorni.analysis.microcensus.planbased;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.PersonImpl;

public class MZPerson extends PersonImpl implements Person {	
	private double hhIncome;
	private int day;
	private int hh;
	private int hhSize;
	private int plz;
	private double weight;
	
	public MZPerson(Id<Person> id) {
		super(id);
	}	
	public Id<Person> getId() {
		return id;
	}
	public void setId(Id id) {
		this.id = id;
	}
	public double getHhIncome() {
		return hhIncome;
	}
	public void setHhIncome(double hhIncome) {
		this.hhIncome = hhIncome;
	}
	public int getDay() {
		return day;
	}
	public void setDay(int day) {
		this.day = day;
	}
	public int getHh() {
		return hh;
	}
	public void setHh(int hh) {
		this.hh = hh;
	}
	public int getHhSize() {
		return hhSize;
	}
	public void setHhSize(int hhSize) {
		this.hhSize = hhSize;
	}
	public int getPlz() {
		return plz;
	}
	public void setPlz(int plz) {
		this.plz = plz;
	}
	public double getWeight() {
		return weight;
	}
	public void setWeight(double weight) {
		this.weight = weight;
	}
}
