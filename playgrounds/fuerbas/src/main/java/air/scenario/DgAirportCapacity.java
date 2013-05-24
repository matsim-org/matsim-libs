/* *********************************************************************** *
 * project: org.matsim.*
 * DgAirportCapacity
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package air.scenario;


/**
 * @author dgrether
 *
 */
public class DgAirportCapacity {

	private double capacityPeriodSeconds;
	private double inboundTaxiwayFlowCapacityCarEquivPerHour = 3600.0;
	private double outboundTaxiwayFlowCapacityCarEquivPerHour = 3600.0;
	private double apronFlowCapacityCarEquivPerHour = outboundTaxiwayFlowCapacityCarEquivPerHour;
	private double runwayInboundFlowCapacityCarEquivPerHour = 3600.0;
	private double runwayOutboundFlowCapacityCarEquivPerHour = 3600.0;
	private String airportCode;
	
	
	public DgAirportCapacity(String airportCode, double capacityPeriodSeconds){
		this.capacityPeriodSeconds = capacityPeriodSeconds;
		this.normalizeDefaults();
		this.airportCode = airportCode;
	}

	public String getAirportCode() {
		return this.airportCode;
	}

	private void normalizeDefaults() {
		this.inboundTaxiwayFlowCapacityCarEquivPerHour *= this.capacityPeriodSeconds / 3600.0;
		this.outboundTaxiwayFlowCapacityCarEquivPerHour *= this.capacityPeriodSeconds / 3600.0;
		this.apronFlowCapacityCarEquivPerHour  *= this.capacityPeriodSeconds / 3600.0;
		this.runwayInboundFlowCapacityCarEquivPerHour *= this.capacityPeriodSeconds / 3600.0;
		this.runwayOutboundFlowCapacityCarEquivPerHour *= this.capacityPeriodSeconds / 3600.0;
	}

	
	public double getInboundTaxiwayFlowCapacityCarEquivPerHour() {
		return inboundTaxiwayFlowCapacityCarEquivPerHour;
	}

	
	public void setInboundTaxiwayFlowCapacityCarEquivPerHour(
			double inboundTaxiwayFlowCapacityCarEquivPerHour) {
		this.inboundTaxiwayFlowCapacityCarEquivPerHour = inboundTaxiwayFlowCapacityCarEquivPerHour;
	}

	
	public double getOutboundTaxiwayFlowCapacityCarEquivPerHour() {
		return outboundTaxiwayFlowCapacityCarEquivPerHour;
	}

	
	public void setOutboundTaxiwayFlowCapacityCarEquivPerHour(
			double outboundTaxiwayFlowCapacityCarEquivPerHour) {
		this.outboundTaxiwayFlowCapacityCarEquivPerHour = outboundTaxiwayFlowCapacityCarEquivPerHour;
	}

	
	public double getApronFlowCapacityCarEquivPerHour() {
		return apronFlowCapacityCarEquivPerHour;
	}

	
	public void setApronFlowCapacityCarEquivPerHour(double apronFlowCapacityCarEquivPerHour) {
		this.apronFlowCapacityCarEquivPerHour = apronFlowCapacityCarEquivPerHour;
	}

	
	public double getRunwayInboundFlowCapacityCarEquivPerHour() {
		return runwayInboundFlowCapacityCarEquivPerHour;
	}

	
	public void setRunwayInboundFlowCapacityCarEquivPerHour(
			double runwayInboundFlowCapacityCarEquivPerHour) {
		this.runwayInboundFlowCapacityCarEquivPerHour = runwayInboundFlowCapacityCarEquivPerHour;
	}

	
	public double getRunwayOutboundFlowCapacity_CarEquivPerHour() {
		return runwayOutboundFlowCapacityCarEquivPerHour;
	}

	
	public void setRunwayOutboundFlowCapacityCarEquivPerHour(
			double runwayOutboundFlowCapacityCarEquivPerHour) {
		this.runwayOutboundFlowCapacityCarEquivPerHour = runwayOutboundFlowCapacityCarEquivPerHour;
	}

	public double getInboundRunwayFreespeedForStorageRestriction(double length) {
		return length * this.getRunwayInboundFlowCapacityCarEquivPerHour() / 3600.0;
	}

	public double getOutboundRunwayFreespeedForStorageRestriction(double length) {
		return length * this.getRunwayOutboundFlowCapacity_CarEquivPerHour() / 3600.0;
	}

	
	
	
	
}
