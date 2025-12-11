/* *********************************************************************** *
 * project: org.matsim.*
 * HbefaVehicleAttributes.java
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
 *
 */
final class HbefaVehicleAttributes {
	// yy I can see what this is good for.  I am at this point not totally sure if it makes a lot of sense to use this outside the emissions contrib.  kai, jan'19

	private String hbefaTechnology = "average";
	private String hbefaSizeClass = "average";
	private String hbefaEmConcept = "average";

	public HbefaVehicleAttributes(){
	}

	private static final String TECHNOLOGY_CMT="Normally something like |diesel| or |petrol|." ;
	/**
	 * @return {@value #TECHNOLOGY_CMT}
	 */
	String getHbefaTechnology(){
		return this.hbefaTechnology;
	}
	/**
	 * @param hbefaTechnology - {@value #TECHNOLOGY_CMT}
	 */
	public void setHbefaTechnology(String hbefaTechnology) {
		this.hbefaTechnology = hbefaTechnology;
	}


	private static final String SIZE_CLASS_CMT="Normally something like |<1,4L| or |>=2L|." ;
	/**
	 * @return {@value #SIZE_CLASS_CMT}
	 */
	public String getHbefaSizeClass(){
		return this.hbefaSizeClass;
	}
	/**
	 * @param hbefaSizeClass - {@value #SIZE_CLASS_CMT}
	 */
	public void setHbefaSizeClass(String hbefaSizeClass) {
		this.hbefaSizeClass = hbefaSizeClass;
	}

	private static final String EM_CONCEPT_CMT="Normally something like PC-P/D-Euro-X." ;
	/**
	 * @return {@value #EM_CONCEPT_CMT}
	 */
	public String getHbefaEmConcept() {
		return this.hbefaEmConcept;
	}

	/**
	 * @param hbefaEmConcept - {@value #EM_CONCEPT_CMT}
	 */
	public void setHbefaEmConcept(String hbefaEmConcept) {
		this.hbefaEmConcept = hbefaEmConcept;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		HbefaVehicleAttributes that = (HbefaVehicleAttributes) o;

		if (!hbefaTechnology.equals(that.hbefaTechnology)) return false;
		if (!hbefaSizeClass.equals(that.hbefaSizeClass)) return false;
		return hbefaEmConcept.equals(that.hbefaEmConcept);
	}

	@Override
	public int hashCode() {
		int result = hbefaTechnology.hashCode();
		result = 31 * result + hbefaSizeClass.hashCode();
		result = 31 * result + hbefaEmConcept.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return
				"hbefaTechnology: " + hbefaTechnology + "; " +
				"hbefasizeClass: " + hbefaSizeClass + "; " +
					"hbefaEmConcept: " + hbefaEmConcept;
	}

	public boolean isDetailed() {
		return !hbefaTechnology.equals("average") && !hbefaSizeClass.equals("average") && !hbefaEmConcept.equals("average");
	}
}
