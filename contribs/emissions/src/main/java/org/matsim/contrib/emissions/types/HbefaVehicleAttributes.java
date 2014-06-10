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
package org.matsim.contrib.emissions.types;

/**
 * @author benjamin
 *
 */
public class HbefaVehicleAttributes {
	private String hbefaTechnology = "average";
	private String hbefaSizeClass = "average";
	private String hbefaEmConcept = "average";
	
	public HbefaVehicleAttributes(){
	}

	public String getHbefaTechnology(){
		return this.hbefaTechnology;
	}
	
	public void setHbefaTechnology(String hbefaTechnology) {
		this.hbefaTechnology = hbefaTechnology;		
	}
	
	public String getHbefaSizeClass(){
		return this.hbefaSizeClass;
	}

	public void setHbefaSizeClass(String hbefaSizeClass) {
		this.hbefaSizeClass = hbefaSizeClass;
	}

	public String getHbefaEmConcept(){
		return this.hbefaEmConcept;
	}
	
	public void setHbefaEmConcept(String hbefaEmConcept) {
		this.hbefaEmConcept = hbefaEmConcept;
	}

	/* need to implement the "equals" method in order to be able to construct an "equal" key
	 later on (e.g. from data available in the simulation)*/
	@Override
	public boolean equals(Object obj) {
	        if(this == obj) {
	              return true;
	         }
	         if (!(obj instanceof HbefaVehicleAttributes)) {
	                return false; 
	         }
	         HbefaVehicleAttributes key = (HbefaVehicleAttributes) obj;
	         return
	            hbefaTechnology.equals(key.getHbefaTechnology())
	         && hbefaSizeClass.equals(key.getHbefaSizeClass())
	         && hbefaEmConcept.equals(key.getHbefaEmConcept());
	}

	// if "equals" is implemented, "hashCode also needs to be implemented
	@Override
	public int hashCode(){
		return toString().hashCode();
	}

	// needed for "hashCode" method
	@Override
	public String toString(){
		return
		  hbefaTechnology + "; "
		+ hbefaSizeClass + "; "
		+ hbefaEmConcept;
	}
}