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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.network.Link;

/**
 * @author ikaddoura
 *
 */
public class ReceiverPoint implements Identifiable<ReceiverPoint>{
	
	private Id<ReceiverPoint> id;
	private Coord coord;
	private Map<Id<Link>, Double> linkId2distanceCorrection = new HashMap<Id<Link>, Double>();
	private Map<Id<Link>, Double> linkId2angleCorrection = new HashMap<Id<Link>, Double>();
	
	private Map<Double, Map<Id<Link>, Double>> timeInterval2LinkId2IsolatedImmission = new HashMap<Double, Map<Id<Link>, Double>>();
	private Map<Double, Double> timeInterval2immission = new HashMap<Double, Double>();
	
	private Map<Double, List<PersonActivityInfo>> timeInterval2actInfos = new HashMap<Double, List<PersonActivityInfo>>();
	private Map<Double, Double> timeInterval2affectedAgentUnits = new HashMap<Double, Double>();

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

}
