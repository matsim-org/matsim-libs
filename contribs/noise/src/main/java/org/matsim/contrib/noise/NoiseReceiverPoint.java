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
package org.matsim.contrib.noise;

import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

import java.util.*;

/**
 * 
 * Extends the basic information of a receiver point towards data required for the computation of noise.
 * 
 * @author ikaddoura, nkuehnel
 *
 */
public class NoiseReceiverPoint extends ReceiverPoint {

	public NoiseReceiverPoint(Id<ReceiverPoint> id, Coord coord) {
		super(id, coord);
	}

	private Map<Id<Person>, List<PersonActivityInfo>> personId2actInfos = null;//new HashMap<>(0);

	/**
	 * Set to true once correction terms are set to re-use points over multiple MATSim runs
	 * without the need to recalculate correction terms.
	 */
	private boolean initialized = false;

	private TObjectDoubleMap<Id<Link>> linkId2Correction = null;
	private TObjectDoubleMap<Id<Link>> linkId2IsolatedImmission = null;

	// time-specific information
	private double currentImmission = 0;
	private Map<? extends NoiseVehicleType, TObjectDoubleMap<Id<Link>>> linkId2IsolatedImmissionPlusOneVehicle = null;

	private double affectedAgentUnits = 0.;
	private double damageCosts;
	private double damageCostsPerAffectedAgentUnit;

	//aggregated daily values
	private double aggregatedImmissionTermLden = 0.;
	private double aggregatedImmissionTerm69 = 0;
	private double aggregatedImmissionTerm1619 = 0;

	Map<Id<Person>, List<PersonActivityInfo>> getPersonId2actInfos() {
		if(personId2actInfos == null) {
			personId2actInfos = new HashMap<>(4);
		}
		return Collections.unmodifiableMap(personId2actInfos);
	}

	void addPersonActInfo(Id<Person> person, PersonActivityInfo info) {
		if(personId2actInfos == null) {
			personId2actInfos = new HashMap<>(4);
		}
		List<PersonActivityInfo> infos = this.personId2actInfos.get(person);
		if(infos == null) {
			infos = new ArrayList<>();
			this.personId2actInfos.put(person, infos);
		}
		infos.add(info);
	}

	Collection<Id<Link>> getRelevantLinks() {
		if(linkId2Correction == null) {
			return Collections.emptySet();
		} else {
			return linkId2Correction.keySet();
		}
	}

	synchronized void setLinkId2Correction(Id<Link> linkId, double correction) {
		if(linkId2Correction== null) {
			linkId2Correction = new TObjectDoubleHashMap<>();
		}
		this.linkId2Correction.put(linkId, correction);
	}

	double getLinkCorrection(Id<Link> linkId) {
		if(linkId2Correction == null) {
			return 0;
		}
		if(linkId2Correction.containsKey(linkId)) {
			return linkId2Correction.get(linkId);
		} else {
			return 0;
		}
	}

	/**
	 * deliberately public for outside access
	 */
	public double getCurrentImmission() {
		return currentImmission;
	}

	void setCurrentImmission(double currentImmission, double time) {
		this.currentImmission = currentImmission;

		if(time <= 24 * 3600.) {

			double adjustedImmision = currentImmission;

			if (time > 19 * 3600. && time <= 23 * 3600.) {
				adjustedImmision += 5;
			} else if ((time > 23 * 3600. && time <= 24 * 3600.)
					|| (time > 0 * 3600. && time <= 7 * 3600.)) {
				adjustedImmision += 10;
			}

			aggregatedImmissionTermLden += Math.pow(10, adjustedImmision / 10.);

			if (time > 6 * 3600. && time <= 9 * 3600.) {
				aggregatedImmissionTerm69 += Math.pow(10, currentImmission / 10.);
			} else if (time > 16 * 3600. && time <= 19 * 3600.) {
				aggregatedImmissionTerm1619 += Math.pow(10, currentImmission / 10.);
			}
		}
	}

	public double getDamageCosts() {
		return damageCosts;
	}

	void setDamageCosts(double damageCosts) {
		this.damageCosts = damageCosts;
	}

	public double getDamageCostsPerAffectedAgentUnit() {
		return damageCostsPerAffectedAgentUnit;
	}

	void setDamageCostsPerAffectedAgentUnit(
			double damageCostsPerAffectedAgentUnit) {
		this.damageCostsPerAffectedAgentUnit = damageCostsPerAffectedAgentUnit;
	}

	public double getAffectedAgentUnits() {
		return affectedAgentUnits;
	}

	void setAffectedAgentUnits(double affectedAgentsUnits) {
		this.affectedAgentUnits = affectedAgentsUnits;
	}

	@Override
	public String toString() {
		return "NoiseReceiverPoint [personId2actInfos=" + personId2actInfos
//				+ ", linkId2distanceCorrection=" + linkId2distanceCorrection
//				+ ", linkId2angleCorrection=" + linkId2angleCorrection
//				+ ", linkId2IsolatedImmission=" + linkId2IsolatedImmission
//				+ ", linkId2IsolatedImmissionPlusOneCar=" + linkId2IsolatedImmissionPlusOneCar
//				+ ", linkId2IsolatedImmissionPlusOneHGV=" + linkId2IsolatedImmissionPlusOneHGV 
				+ ", finalImmission=" + currentImmission
				+ ", affectedAgentUnits=" + affectedAgentUnits
				+ ", damageCosts=" + damageCosts 
				+ ", damageCostsPerAffectedAgentUnit=" + damageCostsPerAffectedAgentUnit + "]";
	}

	void reset() {
		resetTimeInterval();
		this.personId2actInfos = null;
		this.currentImmission = 0;
		this.linkId2IsolatedImmission = null;
		this.linkId2IsolatedImmissionPlusOneVehicle = null;
		aggregatedImmissionTermLden = 0;
		aggregatedImmissionTerm69 = 0;
		aggregatedImmissionTerm1619 = 0;
	}
	
	void resetTimeInterval() {
		this.currentImmission = 0;
		this.linkId2IsolatedImmission = null;
		this.linkId2IsolatedImmissionPlusOneVehicle = null;
		this.setAffectedAgentUnits(0.);
		this.setDamageCosts(0.);
		this.setDamageCostsPerAffectedAgentUnit(0.);
	}

	/**
	 * @return the L_DEN (day (D) - evening (E) - night (N) level (L).
	 * A weighted average with added penalties for night and evening noise levels.
	 */
	public double getLden() {
		return 10 * Math.log10(1./24. * aggregatedImmissionTermLden);
	}

	public double getL69() {
		return 10 * Math.log10(1./3. * aggregatedImmissionTerm69);
	}

	public double getL1619() {
		return 10 * Math.log10(1./3. * aggregatedImmissionTerm1619);
	}

	void setInitialized() {
		this.initialized = true;
	}

	boolean isInitialized() {
		return initialized;
	}

	void setLinkId2IsolatedImmissionPlusOneVehicle(Map<? extends NoiseVehicleType, TObjectDoubleMap<Id<Link>>> linkId2IsolatedImmissionPlusOneVehicle) {
		this.linkId2IsolatedImmissionPlusOneVehicle = linkId2IsolatedImmissionPlusOneVehicle;
	}

	void setLinkId2IsolatedImmission(TObjectDoubleMap<Id<Link>> linkId2IsolatedImmission) {
		this.linkId2IsolatedImmission = linkId2IsolatedImmission;
	}

	Map<? extends NoiseVehicleType, TObjectDoubleMap<Id<Link>>> getLinkId2IsolatedImmissionPlusOneVehicle() {
		return linkId2IsolatedImmissionPlusOneVehicle;
	}

	TObjectDoubleMap<Id<Link>> getLinkId2IsolatedImmission() {
		return linkId2IsolatedImmission;
	}
}
