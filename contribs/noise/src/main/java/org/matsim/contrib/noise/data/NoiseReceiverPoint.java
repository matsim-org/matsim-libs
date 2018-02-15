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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

import gnu.trove.impl.unmodifiable.TUnmodifiableObjectDoubleMap;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;

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

	private Map<Id<Person>, List<PersonActivityInfo>> personId2actInfos = new HashMap<>(0);
	
	// initialization
	private Map<Id<Link>, Double> linkId2distanceCorrection = new HashMap<>(0);
	private Map<Id<Link>, Double> linkId2angleCorrection = new HashMap<>(0);
			
	// time-specific information
	private TObjectDoubleMap<Id<Link>> linkId2IsolatedImmission = new TObjectDoubleHashMap<>(8, 0.95f, 0);
	private TObjectDoubleMap<Id<Link>> linkId2IsolatedImmissionPlusOneCar = new TObjectDoubleHashMap<>(8, 0.95f, 0);
	private TObjectDoubleMap<Id<Link>> linkId2IsolatedImmissionPlusOneHGV = new TObjectDoubleHashMap<>(8, 0.95f, 0);
	private double finalImmission = 0.;
	private double affectedAgentUnits = 0.;
	private double damageCosts;
	private double damageCostsPerAffectedAgentUnit;

	public Map<Id<Person>, List<PersonActivityInfo>> getPersonId2actInfos() {
		return Collections.unmodifiableMap(personId2actInfos);
	}

	public void addPersonActInfo(Id<Person> person, PersonActivityInfo info) {
		List<PersonActivityInfo> infos = this.personId2actInfos.get(person);
		if(infos == null) {
			infos = new ArrayList<>();
			this.personId2actInfos.put(person, infos);
		}
		infos.add(info);
	}
	
	public Map<Id<Link>, Double> getLinkId2distanceCorrection() {
		return Collections.unmodifiableMap(linkId2distanceCorrection);
	}

	public void setDistanceCorrection(Id<Link> linkId,  Double distanceCorrection) {
		this.linkId2distanceCorrection.put(linkId, distanceCorrection);
	}

	public Map<Id<Link>, Double> getLinkId2angleCorrection() {
		return Collections.unmodifiableMap(linkId2angleCorrection);
	}

	public void setAngleCorrection(Id<Link> linkId, Double angleCorrection) {
		this.linkId2angleCorrection.put(linkId, angleCorrection);
	}

	public TObjectDoubleMap<Id<Link>>  getLinkId2IsolatedImmission() {
		return new TUnmodifiableObjectDoubleMap<>(linkId2IsolatedImmission);
	}

	public void setIsolatedImmission(Id<Link> linkId, Double isolatedImmission) {
		this.linkId2IsolatedImmission.put(linkId, isolatedImmission);
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

	public TObjectDoubleMap<Id<Link>> getLinkId2IsolatedImmissionPlusOneCar() {
		return new TUnmodifiableObjectDoubleMap<>(linkId2IsolatedImmissionPlusOneCar);
	}

	public void setIsolatedImmissionPlusOneCar(Id<Link> linkId, Double isolatedImmissionPlusOneCar) {
		this.linkId2IsolatedImmissionPlusOneCar.put(linkId, isolatedImmissionPlusOneCar);
	}

	public TObjectDoubleMap<Id<Link>> getLinkId2IsolatedImmissionPlusOneHGV() {
		return new TUnmodifiableObjectDoubleMap<>(linkId2IsolatedImmissionPlusOneHGV);
	}

	public void setIsolatedImmissionPlusOneHGV(Id<Link> linkId, Double isolatedImmissionPlusOneHGV) {
		this.linkId2IsolatedImmissionPlusOneHGV.put(linkId, isolatedImmissionPlusOneHGV);
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

	public void reset() {
		resetTimeInterval();
		this.personId2actInfos.clear();
	}
	
	public void resetTimeInterval() {
		linkId2IsolatedImmission.clear();
		this.setFinalImmission(0.);
		this.setAffectedAgentUnits(0.);
		this.setDamageCosts(0.);
		this.setDamageCostsPerAffectedAgentUnit(0.);
	}
}
