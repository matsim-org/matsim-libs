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

/**
 * 
 */
package org.matsim.contrib.noise.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

/**
 * 
 * Extends the basic information of a receiver point towards data required for the computation of noise.
 * 
 * @author ikaddoura
 *
 */
public class NoiseReceiverPoint extends ReceiverPoint {
	
	public NoiseReceiverPoint(Id<ReceiverPoint> id, Coord coord) {
		super(id, coord);
	}

	private Map<Id<Person>, ArrayList<PersonActivityInfo>> personId2actInfos = new HashMap<Id<Person>, ArrayList<PersonActivityInfo>>();
	
	// initialization
	private Map<Id<Link>, Double> linkId2distanceCorrection = new HashMap<Id<Link>, Double>();
	private Map<Id<Link>, Double> linkId2angleCorrection = new HashMap<Id<Link>, Double>();
			
	// time-specific information
	private Map<Id<Link>, Double> linkId2IsolatedImmission = new HashMap<Id<Link>, Double>();
	private Map<Id<Link>, Double> linkId2IsolatedImmissionPlusOneCar = new HashMap<Id<Link>, Double>();
	private Map<Id<Link>, Double> linkId2IsolatedImmissionPlusOneHGV = new HashMap<Id<Link>, Double>(); 
	private double finalImmission = 0.;
	private double affectedAgentUnits = 0.;
	private double damageCosts;
	private double damageCostsPerAffectedAgentUnit;

	public Map<Id<Person>, ArrayList<PersonActivityInfo>> getPersonId2actInfos() {
		return personId2actInfos;
	}

	public void setPersonId2actInfos(Map<Id<Person>, ArrayList<PersonActivityInfo>> personId2actInfos) {
		this.personId2actInfos = personId2actInfos;
	}
	
	public Map<Id<Link>, Double> getLinkId2distanceCorrection() {
		return linkId2distanceCorrection;
	}

	public void setLinkId2distanceCorrection(
			Map<Id<Link>, Double> linkId2distanceCorrection) {
		this.linkId2distanceCorrection = linkId2distanceCorrection;
	}

	public Map<Id<Link>, Double> getLinkId2angleCorrection() {
		return linkId2angleCorrection;
	}

	public void setLinkId2angleCorrection(Map<Id<Link>, Double> linkId2angleCorrection) {
		this.linkId2angleCorrection = linkId2angleCorrection;
	}

	public Map<Id<Link>, Double> getLinkId2IsolatedImmission() {
		return linkId2IsolatedImmission;
	}

	public void setLinkId2IsolatedImmission(Map<Id<Link>, Double> linkId2IsolatedImmission) {
		this.linkId2IsolatedImmission = linkId2IsolatedImmission;
	}

	public double getFinalImmission() {
		return finalImmission;
	}

	public void setFinalImmission(double finalImmission) {
		this.finalImmission = finalImmission;
	}

	public double getDamageCosts() {
		return damageCosts;
	}

	public void setDamageCosts(double damageCosts) {
		this.damageCosts = damageCosts;
	}

	public double getDamageCostsPerAffectedAgentUnit() {
		return damageCostsPerAffectedAgentUnit;
	}

	public void setDamageCostsPerAffectedAgentUnit(
			double damageCostsPerAffectedAgentUnit) {
		this.damageCostsPerAffectedAgentUnit = damageCostsPerAffectedAgentUnit;
	}

	public double getAffectedAgentUnits() {
		return affectedAgentUnits;
	}

	public void setAffectedAgentUnits(double affectedAgentsUnits) {
		this.affectedAgentUnits = affectedAgentsUnits;
	}

	public Map<Id<Link>, Double> getLinkId2IsolatedImmissionPlusOneCar() {
		return linkId2IsolatedImmissionPlusOneCar;
	}

	public void setLinkId2IsolatedImmissionPlusOneCar(
			Map<Id<Link>, Double> linkId2IsolatedImmissionPlusOneCar) {
		this.linkId2IsolatedImmissionPlusOneCar = linkId2IsolatedImmissionPlusOneCar;
	}

	public Map<Id<Link>, Double> getLinkId2IsolatedImmissionPlusOneHGV() {
		return linkId2IsolatedImmissionPlusOneHGV;
	}

	public void setLinkId2IsolatedImmissionPlusOneHGV(
			Map<Id<Link>, Double> linkId2IsolatedImmissionPlusOneHGV) {
		this.linkId2IsolatedImmissionPlusOneHGV = linkId2IsolatedImmissionPlusOneHGV;
	}

	@Override
	public String toString() {
		return "NoiseReceiverPoint [personId2actInfos=" + personId2actInfos
				+ ", linkId2distanceCorrection=" + linkId2distanceCorrection
				+ ", linkId2angleCorrection=" + linkId2angleCorrection
				+ ", linkId2IsolatedImmission=" + linkId2IsolatedImmission
				+ ", linkId2IsolatedImmissionPlusOneCar="
				+ linkId2IsolatedImmissionPlusOneCar
				+ ", linkId2IsolatedImmissionPlusOneHGV="
				+ linkId2IsolatedImmissionPlusOneHGV + ", finalImmission="
				+ finalImmission + ", affectedAgentUnits=" + affectedAgentUnits
				+ ", damageCosts=" + damageCosts + ", damageCostsPerAffectedAgentUnit="
				+ damageCostsPerAffectedAgentUnit + "]";
	}
}
