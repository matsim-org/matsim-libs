/* *********************************************************************** *
 * project: org.matsim.*
 * ColdEmissionEvent.java
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
package playground.juliakern.distribution;

import org.matsim.api.core.v01.Id;

/**
 * store (distributed) emission cell-wise
 * 
 * @author julia
 */
public class EmPerCell {
	
	private Integer xBin;
	private Integer yBin;
	private Id responsiblePersonId;
	private Double concentration;
	private Double emissionEventStartTime;
	
	public EmPerCell(Integer xBin, Integer yBin, Id personId, Double concentration, Double emissisionEventStartTime) {
		this.xBin = xBin;
		this.yBin = yBin;
		this.responsiblePersonId = personId;
		this.concentration = concentration;
		this.emissionEventStartTime = emissisionEventStartTime;
	}

	public Integer getXbin() {
		return this.xBin;
	}
	
	public Integer getYbin() {
		return this.yBin;
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
