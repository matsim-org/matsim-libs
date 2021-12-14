/* *********************************************************************** *
 * project: org.matsim.*
 * HbefaWarmEmissionFactorKey.java
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
package org.matsim.contrib.emissions;

/**
 * @author benjamin
 */
class HbefaWarmEmissionFactorKey extends HbefaEmissionFactorKey {

	private String roadCategory;
	private HbefaTrafficSituation trafficSituation;

	/*package-private*/ HbefaWarmEmissionFactorKey() {
	}

	public HbefaWarmEmissionFactorKey(HbefaWarmEmissionFactorKey key) {
		super(key);
		this.roadCategory = key.roadCategory;
		this.trafficSituation = key.trafficSituation;
	}

	String getRoadCategory() {
		return this.roadCategory;
	}

	/*package-private*/ void setRoadCategory(String roadCategory) {
		this.roadCategory = roadCategory;
	}

	/*package-private*/ HbefaTrafficSituation getTrafficSituation() {
		return this.trafficSituation;
	}

	/*package-private*/ void setTrafficSituation(HbefaTrafficSituation trafficSituation) {
		this.trafficSituation = trafficSituation;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;

		var that = (HbefaWarmEmissionFactorKey) o;

		if (trafficSituation != that.trafficSituation) return false;
		return roadCategory.equals(that.roadCategory);
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + trafficSituation.hashCode();
		result = 31 * result + roadCategory.hashCode();
		return result;
	}

	// needed for "hashCode" method
	@Override
	public String toString(){
		return
				getVehicleCategory() + "; "
						+ getComponent() + "; "
						+ roadCategory + "; "
						+ trafficSituation + "; "
						+ getVehicleAttributes();
	}
}
