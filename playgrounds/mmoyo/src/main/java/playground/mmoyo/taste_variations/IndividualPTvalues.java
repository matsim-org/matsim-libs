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

class IndividualPTvalues {

	private final Id idAgent;
	private double trWalkTime;
	private double trTime;
	private double trDistance;
	private int changes;
	
	protected  IndividualPTvalues(Id idAgent) {
		this.idAgent = idAgent;
	}

	protected Id getIdAgent() {return idAgent;}
	protected double getTrWalkTime() {return trWalkTime;}
	protected void setTrWalkTime(double trWalkTime) {this.trWalkTime = trWalkTime;	}
	protected double getTrTime() {return trTime;	}
	protected void setTrTime(double trTime) {this.trTime = trTime;}
	protected double getTrDistance() {return trDistance;}
	protected void setTrDistance(double trDistance) {this.trDistance = trDistance;}
	protected int getChanges() {return changes;}
	protected void setChanges(int changes) {this.changes = changes;}

}