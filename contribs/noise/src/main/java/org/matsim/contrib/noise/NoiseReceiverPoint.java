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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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


	private Map<Id<Person>, List<PersonActivityInfo>> personId2actInfos = null;//new HashMap<>(0);

	/**
	 * Set to true once correction terms are set to re-use points over multiple MATSim runs
	 * without the need to recalculate correction terms.
	 */
	private boolean initialized = false;

	// initialization
	private Map<Id<Link>, Double> linkId2distanceCorrection = null;//new HashMap<>(0);
	private Map<Id<Link>, Double> linkId2angleCorrection = null;//new HashMap<>(0);
	private Map<Id<Link>, Double> linkId2ShieldingCorrection = null;

	// time-specific information
	private double currentImmission = 0.;
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
	
	Map<Id<Link>, Double> getLinkId2distanceCorrection() {
		if(linkId2distanceCorrection == null) {
			linkId2distanceCorrection = new HashMap<>();
		}
		return Collections.unmodifiableMap(linkId2distanceCorrection);
	}

	synchronized void setLinkId2distanceCorrection(Id<Link> linkId, Double distanceCorrection) {
		if(linkId2distanceCorrection == null) {
			linkId2distanceCorrection = new HashMap<>();
		}
		this.linkId2distanceCorrection.put(linkId, distanceCorrection);
	}

	Map<Id<Link>, Double> getLinkId2angleCorrection() {
		if(linkId2angleCorrection== null) {
			linkId2angleCorrection = new HashMap<>();
		}
		return Collections.unmodifiableMap(linkId2angleCorrection);
	}

	synchronized void setLinkId2angleCorrection(Id<Link> linkId, Double angleCorrection) {
		if(linkId2angleCorrection== null) {
			linkId2angleCorrection = new HashMap<>();
		}
		this.linkId2angleCorrection.put(linkId, angleCorrection);
	}

	synchronized void setLinkId2ShieldingCorrection(Id<Link> linkId, Double shieldingCorrection) {
		if(linkId2ShieldingCorrection== null) {
			linkId2ShieldingCorrection = new HashMap<>();
		}
		this.linkId2ShieldingCorrection.put(linkId, shieldingCorrection);
	}

	Map<Id<Link>, Double> getLinkId2ShieldingCorrection() {
		if(linkId2ShieldingCorrection == null) {
			linkId2ShieldingCorrection = new HashMap<>();
		}
		return Collections.unmodifiableMap(linkId2ShieldingCorrection);
	}

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
				+ ", linkId2distanceCorrection=" + linkId2distanceCorrection
				+ ", linkId2angleCorrection=" + linkId2angleCorrection
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
		aggregatedImmissionTermLden = 0;
		aggregatedImmissionTerm69 = 0;
		aggregatedImmissionTerm1619 = 0;
	}
	
	void resetTimeInterval() {
//		linkId2IsolatedImmission.clear();
		currentImmission = 0;
		this.setAffectedAgentUnits(0.);
		this.setDamageCosts(0.);
		this.setDamageCostsPerAffectedAgentUnit(0.);
	}

	/**
	 * @return the German L_DEN, where "L" stands for "Laerm" (=noise), and DEN for  DayEveningNight.  It is some weighted average according to the German
	 * norm.
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
}
