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
package playground.ikaddoura.noise2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

/**
 * @author ikaddoura
 *
 */
public class ReceiverPoint implements Identifiable<ReceiverPoint>{
	
	// initialization
	private Id<ReceiverPoint> id;
	private Coord coord;
	private Map<Id<Link>, Double> linkId2distanceCorrection = new HashMap<Id<Link>, Double>();
	private Map<Id<Link>, Double> linkId2angleCorrection = new HashMap<Id<Link>, Double>();
	
	// immission
	private Map<Double, Map<Id<Link>, Double>> timeInterval2LinkId2IsolatedImmission = new HashMap<Double, Map<Id<Link>, Double>>();
	private Map<Double, Double> timeInterval2immission = new HashMap<Double, Double>();
	// new implementation:
	private Map<Id<Link>, Double> linkId2IsolatedImmission = new HashMap<Id<Link>, Double>();
	private double finalImmission;
	
	// activity tracker
	private Map<Double, List<PersonActivityInfo>> timeInterval2actInfos = new HashMap<Double, List<PersonActivityInfo>>();
	private Map<Double, Double> timeInterval2affectedAgentUnits = new HashMap<Double, Double>();
	// new implementation:
	private Map<Id<Person>, List<PersonActivityInfo>> personId2actInfos = new HashMap<Id<Person>, List<PersonActivityInfo>>();
	private List<Id<Person>> personIdsToRemoveNextTimeInterval = new ArrayList<Id<Person>>();
	
	private double consideredAgentUnitsCurrentTimeInterval;
	private double consideredAgentUnitsNextTimeInterval;
	
	// damages
	private Map<Double, Double> timeInterval2damageCosts = new HashMap<Double, Double>();
	private Map<Double,Double> timeInterval2damageCostPerAffectedAgentUnit = new HashMap<Double, Double>();
	// new implementation:
	private double damageCosts;
	private double damageCostsPerAffectedAgentUnit;

	public ReceiverPoint(Id<ReceiverPoint> id) {
		this.id = id;
	}
	
	@Override
	public Id<ReceiverPoint> getId() {
		return this.id;
	}

	public Coord getCoord() {
		return coord;
	}

	public void setCoord(Coord coord) {
		this.coord = coord;
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

	public Map<Double,Map<Id<Link>,Double>> getTimeInterval2LinkId2IsolatedImmission() {
		return timeInterval2LinkId2IsolatedImmission;
	}

	public void setTimeInterval2LinkId2IsolatedImmission(
			Map<Double,Map<Id<Link>,Double>> timeInterval2LinkId2IsolatedImmission) {
		this.timeInterval2LinkId2IsolatedImmission = timeInterval2LinkId2IsolatedImmission;
	}

	public Map<Double,Double> getTimeInterval2immission() {
		return timeInterval2immission;
	}

	public void setTimeInterval2immission(Map<Double,Double> timeInterval2immission) {
		this.timeInterval2immission = timeInterval2immission;
	}

	public Map<Double, List<PersonActivityInfo>> getTimeInterval2actInfos() {
		return timeInterval2actInfos;
	}

	public void setTimeInterval2actInfos(Map<Double, List<PersonActivityInfo>> timeInterval2actInfos) {
		this.timeInterval2actInfos = timeInterval2actInfos;
	}

	public Map<Double, Double> getTimeInterval2affectedAgentUnits() {
		return timeInterval2affectedAgentUnits;
	}

	public void setTimeInterval2affectedAgentUnits(
			Map<Double, Double> timeInterval2affectedAgentUnits) {
		this.timeInterval2affectedAgentUnits = timeInterval2affectedAgentUnits;
	}

	public Map<Double, Double> getTimeInterval2damageCosts() {
		return timeInterval2damageCosts;
	}

	public void setTimeInterval2damageCosts(Map<Double, Double> timeInterval2damageCosts) {
		this.timeInterval2damageCosts = timeInterval2damageCosts;
	}

	public Map<Double,Double> getTimeInterval2damageCostPerAffectedAgentUnit() {
		return timeInterval2damageCostPerAffectedAgentUnit;
	}

	public void setTimeInterval2damageCostPerAffectedAgentUnit(
			Map<Double,Double> timeInterval2damageCostPerAffectedAgentUnit) {
		this.timeInterval2damageCostPerAffectedAgentUnit = timeInterval2damageCostPerAffectedAgentUnit;
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

	public double getConsideredAgentUnitsCurrentTimeInterval() {
		return consideredAgentUnitsCurrentTimeInterval;
	}

	public void setConsideredAgentUnitsCurrentTimeInterval(double consideredAgentUnits) {
		this.consideredAgentUnitsCurrentTimeInterval = consideredAgentUnits;
	}

	public double getConsideredAgentUnitsNextTimeInterval() {
		return consideredAgentUnitsNextTimeInterval;
	}

	public void setConsideredAgentUnitsNextTimeInterval(
			double consideredAgentUnitsFromPreviousTimeInterval) {
		this.consideredAgentUnitsNextTimeInterval = consideredAgentUnitsFromPreviousTimeInterval;
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

	public Map<Id<Person>, List<PersonActivityInfo>> getPersonId2actInfos() {
		return personId2actInfos;
	}

	public void setPersonId2actInfos(Map<Id<Person>, List<PersonActivityInfo>> personId2actInfos) {
		this.personId2actInfos = personId2actInfos;
	}

	public List<Id<Person>> getPersonIdsToRemoveNextTimeInterval() {
		return personIdsToRemoveNextTimeInterval;
	}

	public void setPersonIdsToRemoveNextTimeInterval(
			List<Id<Person>> personIdsToRemoveNextTimeInterval) {
		this.personIdsToRemoveNextTimeInterval = personIdsToRemoveNextTimeInterval;
	}

}
