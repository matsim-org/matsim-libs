/* *********************************************************************** *
 * project: org.matsim.*
 * CommuterDataElement
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.dhosse.scenarios.generic.population.io.commuters;

/**
 * @author dhosse
 *
 */
public class CommuterDataElement {
	private String fromId;
	private String toId;
	private String fromName;
	private String toName;
	private int commuters;
	private int adminLevel;
	private double shareOfMaleCommuters;
	
	private int malePersonsCreated = 0;
	private int femalePersonsCreated = 0;
	
	public CommuterDataElement(String fromId, String fromName,  String toId, String toName,  int commuters){
		this.fromId = fromId;
		this.toId = toId;
		this.commuters = commuters;
		this.fromName = fromName;
		this.toName = toName;
		this.adminLevel = fromId.length();
	}
	
	public CommuterDataElement(String fromId, String fromName,  String toId, String toName,  int commuters, double shareOfMaleCommuters){
		this.fromId = fromId;
		this.toId = toId;
		this.commuters = commuters;
		this.fromName = fromName;
		this.toName = toName;
		this.adminLevel = fromId.length();
		this.shareOfMaleCommuters = shareOfMaleCommuters;
	}
	
	

	public String getFromName() {
		return fromName;
	}



	public void setFromName(String fromName) {
		this.fromName = fromName;
	}



	public String getToName() {
		return toName;
	}



	public void setToName(String toName) {
		this.toName = toName;
	}



	public String getFromId() {
		return fromId;
	}

	public String getToId() {
		return toId;
	}

	public int getCommuters() {
		return commuters;
	}
	
	public String toString(){
		return ("F: "+fromId+" T: "+toId+" C: "+commuters);
	}
	
	public int getAdminLevel(){
		return this.adminLevel;
	}
	
	public double getShareOfMaleCommuters(){
		return this.shareOfMaleCommuters;
	}
	
	public int getMalePersonsCreated(){
		
		return this.malePersonsCreated;
		
	}
	
	public void incMalePersonsCreated(){
		this.malePersonsCreated++;
	}
	
	public int getFemalePersonsCreated(){
		
		return this.femalePersonsCreated;
		
	}
	
	public void incFemalePersonsCreated(){
		this.femalePersonsCreated++;
	}

}