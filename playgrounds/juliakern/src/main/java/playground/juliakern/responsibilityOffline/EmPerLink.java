/* *********************************************************************** *
 * project: org.matsim.*
 * SpatialAveragingForLinkEmissions.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.juliakern.responsibilityOffline;

import org.matsim.api.core.v01.Id;

/**
 * store (distributed) emissions for each links 
 * with associated responsible person, time, concentration value
 * @author julia
 *
 */
public class EmPerLink {
	
	private Id linkId;
	private Id responsiblePersonId;
	private Double concentration;
	private Double emissionEventStartTime;

	public EmPerLink(Id linkId, Id personId, Double concentration, Double emissionEventStartTime) {
		this.linkId = linkId;
		this.responsiblePersonId = personId;
		this.concentration = concentration;
		this.emissionEventStartTime = emissionEventStartTime;
	}

	public Id getLinkId() {
		return this.linkId;
	}

	public Id getPersonId() {
		return this.responsiblePersonId;
	}

	public Double getConcentration() {
		return this.concentration;
	}

	public Double getEmissionEventStartTime() {
		return emissionEventStartTime;
	}

}
