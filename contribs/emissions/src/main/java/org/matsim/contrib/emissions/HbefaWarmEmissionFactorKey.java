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
	private String roadGradient; // added var roadGradient
	// It is most probably correct to add roadGradient here.  However, the way it is done right now it is far from complete.  Including that it is
	// not used in equals and in HashCode.  kai, apr'23

	// What is also missing in general is some wildcard syntax.  This now comes in as a problem here since it makes retrofitting the non-grade
	// code more difficult.  So far an easy solution for wildcards did not come to mind, and for a complicated solution we
	// did not have the patience.  Maybe there is a library?  In the meantime, we have operated with the fallbacks in the config.  So one could
	// now include something there either fall back to zero grade if grade is not available in the link.  Or abort.  (I find the latter more
	// plausible; one can add a grade to each individual link in the pre-processing.)  kai, apr'23


	/*package-private*/ HbefaWarmEmissionFactorKey() {
	}

	public HbefaWarmEmissionFactorKey(HbefaWarmEmissionFactorKey key) {
		super(key);
		this.roadCategory = key.roadCategory;
		this.trafficSituation = key.trafficSituation;
		this.roadGradient = key.roadGradient;
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

	/*package-private*/ void setRoadGradient(String roadGradient) {
		this.roadGradient = roadGradient;
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
		result = 31 * result + roadGradient.hashCode();
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
						+ roadGradient + "; "
						+ getVehicleAttributes();
	}
}
