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

package playground.mmoyo.taste_variations;

import org.matsim.api.core.v01.Id;

public class IndividualPreferences{

	private Id idAgent;
	private double weigth_trWalkTime;
	private double weigth_trTime;
	private double weigth_trDistance;
	private double weight_changes;
	
	public IndividualPreferences(Id idAgent, double weigth_trWalkTime, double weigth_trTime, double weigth_trDistance, double weight_changes) {
		this.idAgent = idAgent;
		this.weigth_trWalkTime = weigth_trWalkTime;
		this.weigth_trTime = weigth_trTime;
		this.weigth_trDistance = weigth_trDistance;
		this.weight_changes = weight_changes;
	}
	
	//just getters
	public Id getIdAgent() {return idAgent;}
	public double getWeight_trWalkTime() {return weigth_trWalkTime;}
	public double getWeight_trTime() {	return weigth_trTime;}
	public double getWeight_trDistance() {return weigth_trDistance;}
	public double getWeight_changes() {return weight_changes;}
}