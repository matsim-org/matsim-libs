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
class NoiseReceiverPoint extends ReceiverPoint {

	public NoiseReceiverPoint(Id<ReceiverPoint> id, Coord coord) {
		super(id, coord);
	}


	private Map<Id<Person>, List<PersonActivityInfo>> personId2actInfos = null;//new HashMap<>(0);

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

	private double lden = Double.NaN;
	private double l69 = Double.NaN;
	private double l1619 = Double.NaN;


	public Map<Id<Person>, List<PersonActivityInfo>> getPersonId2actInfos() {
		if(personId2actInfos == null) {
			personId2actInfos = new HashMap<>(4);
		}
		return Collections.unmodifiableMap(personId2actInfos);
	}

	public void addPersonActInfo(Id<Person> person, PersonActivityInfo info) {
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
	
	public Map<Id<Link>, Double> getLinkId2distanceCorrection() {
		if(linkId2distanceCorrection == null) {
			linkId2distanceCorrection = new HashMap<>();
		}
		return Collections.unmodifiableMap(linkId2distanceCorrection);
	}

	public synchronized void setLinkId2distanceCorrection(Id<Link> linkId,  Double distanceCorrection) {
		if(linkId2distanceCorrection == null) {
			linkId2distanceCorrection = new HashMap<>();
		}
		this.linkId2distanceCorrection.put(linkId, distanceCorrection);
	}

	public Map<Id<Link>, Double> getLinkId2angleCorrection() {
		if(linkId2angleCorrection== null) {
			linkId2angleCorrection = new HashMap<>();
		}
		return Collections.unmodifiableMap(linkId2angleCorrection);
	}

	public synchronized void setLinkId2angleCorrection(Id<Link> linkId, Double angleCorrection) {
		if(linkId2angleCorrection== null) {
			linkId2angleCorrection = new HashMap<>();
		}
		this.linkId2angleCorrection.put(linkId, angleCorrection);
	}

	public synchronized void setLinkId2ShieldingCorrection(Id<Link> linkId, Double shieldingCorrection) {
		if(linkId2ShieldingCorrection== null) {
			linkId2ShieldingCorrection = new HashMap<>();
		}
		this.linkId2ShieldingCorrection.put(linkId, shieldingCorrection);
	}

	public Map<Id<Link>, Double> getLinkId2ShieldingCorrection() {
		if(linkId2ShieldingCorrection == null) {
			linkId2ShieldingCorrection = new HashMap<>();
		}
		return Collections.unmodifiableMap(linkId2ShieldingCorrection);
	}

	public double getCurrentImmission() {
		return currentImmission;
	}

	public void setCurrentImmission(double currentImmission, double time) {
		this.currentImmission = currentImmission;

		if(time <= 24 * 3600.) {

			double adjustedImmision = currentImmission;

			if (time > 19 * 3600. && time <= 23 * 3600.) {
				adjustedImmision = currentImmission + 5;
			} else if ((time > 23 * 3600. && time <= 24 * 3600.)
					|| (time > 0 * 3600. && time <= 7 * 3600.)) {
				adjustedImmision = currentImmission + 10;
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
		lden = Double.NaN;
		l69 = Double.NaN;
		l1619 = Double.NaN;
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

	void processImmission() {
		lden = 10 * Math.log10(1./24. * aggregatedImmissionTermLden);
		l69 =  10 * Math.log10(1./3. * aggregatedImmissionTerm69);
		l1619 =  10 * Math.log10(1./3. * aggregatedImmissionTerm1619);
	}

	double getLden() {
		return lden;
	}

	double getL69() {
		return l69;
	}

	double getL1619() {
		return l1619;
	}
}
