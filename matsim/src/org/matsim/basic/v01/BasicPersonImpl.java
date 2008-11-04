/* *********************************************************************** *
 * project: org.matsim.*
 * BasicPerson.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.basic.v01;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.population.Desires;
import org.matsim.population.Knowledge;
import org.matsim.utils.customize.CustomizableImpl;

// TODO [balmermi]: need discussion about 'extends CustomizableImpl'
public class BasicPersonImpl<T extends BasicPlan> extends CustomizableImpl implements BasicPerson<T> {

	private static final Logger log = Logger.getLogger(BasicPersonImpl.class);
	
	protected List<T> plans = new ArrayList<T>(6);
	protected T selectedPlan = null;
	protected Id id;
	private String sex;
	private int age = Integer.MIN_VALUE;
	private String hasLicense;
	private String carAvail;
	private String isEmployed;

	private TreeSet<String> travelcards = null; 
	private Knowledge knowledge = null;
	private Desires desires = null;

	public BasicPersonImpl(final Id id) {
		this.id = id;
	}

	/* (non-Javadoc)
	 * @see org.matsim.basic.v01.BasicPerson#addPlan(T)
	 */
	public void addPlan(final T plan) {
		this.plans.add(plan);
		// Make sure there is a selected plan if there is at least one plan
		if (this.selectedPlan == null) this.selectedPlan = plan;
	}

	/* (non-Javadoc)
	 * @see org.matsim.basic.v01.BasicPerson#getSelectedPlan()
	 */
	public T getSelectedPlan() {
		return this.selectedPlan;
	}

	/* (non-Javadoc)
	 * @see org.matsim.basic.v01.BasicPerson#setSelectedPlan(T)
	 */
	public void setSelectedPlan(final T selectedPlan) {
		if (this.plans.contains(selectedPlan)) {
			this.selectedPlan = selectedPlan;
		}
	}

	/* (non-Javadoc)
	 * @see org.matsim.basic.v01.BasicPerson#getPlans()
	 */
	public List<T> getPlans() {
		return this.plans;
	}

	/* (non-Javadoc)
	 * @see org.matsim.basic.v01.BasicPerson#getId()
	 */
	public Id getId() {
		return this.id;
	}

	/* (non-Javadoc)
	 * @see org.matsim.basic.v01.BasicPerson#setId(org.matsim.utils.identifiers.IdI)
	 */
	public void setId(final Id id) {
		this.id = id;
	}

	/* (non-Javadoc)
	 * @see org.matsim.basic.v01.BasicPerson#setId(java.lang.String)
	 */
	public void setId(final String idstring) {
		this.id = new IdImpl(idstring);
	}

	public final String getSex() {
		return this.sex;
	}

	public final int getAge() {
		return this.age;
	}

	public final String getLicense() {
		return this.hasLicense;
	}

	public final boolean hasLicense() {
		return ("yes".equals(this.hasLicense));
	}

	public final String getCarAvail() {
		return this.carAvail;
	}

	public final String getEmployed() {
		return this.isEmployed;
	}

	public final boolean isEmpoyed() {
		return ("yes".equals(this.isEmployed));
	}

	public void setAge(final int age) {
		if ((age < 0) && (age != Integer.MIN_VALUE)) {
			throw new NumberFormatException("A person's age has to be an integer >= 0.");
		}
		this.age = age;
	}

	public final void setSex(final String sex) {
		this.sex = (sex == null) ? null : sex.intern();
	}

	public final void setLicence(final String licence) {
		this.hasLicense = (licence == null) ? null : licence.intern();
	}

	public final void setCarAvail(final String carAvail) {
		this.carAvail = (carAvail == null) ? null : carAvail.intern();
	}

	public final void setEmployed(final String employed) {
		this.isEmployed = (employed == null) ? null : employed.intern();
		// FIXME: maybe I am getting this wrong, but it seems to me that this is a bit weird:
		// - it accepts a String, implying that you can put in whatever you want
		// - it also writes it without problems in the population writer
		// - however, when reading it back in it complains that it wants "yes" or "no"
		// Maybe use a "boolean" instead of a "String"?  kai, nov08
	}

	public final Knowledge createKnowledge(final String desc) {
		if (this.knowledge == null) {
			this.knowledge = new Knowledge(desc);
		}
		return this.knowledge;
	}
	
	public final Desires createDesires(final String desc) {
		if (this.desires == null) {
			this.desires = new Desires(desc);
		}
		return this.desires;
	}
	

	public final void addTravelcard(final String type) {
		if (this.travelcards == null) {
			this.travelcards = new TreeSet<String>();
		}
		if (this.travelcards.contains(type)) {
			log.info(this + "[type=" + type + " already exists]");
		} else {
			this.travelcards.add(type.intern());
		}
	}


	public final TreeSet<String> getTravelcards() {
		return this.travelcards;
	}

	public final Knowledge getKnowledge() {
		return this.knowledge;
	}

	public final Desires getDesires() {
		return this.desires;
	}
	
}
