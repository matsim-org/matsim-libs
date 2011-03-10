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
package playground.jbischoff.commuterDemand;

/**
 * @author jbischoff
 *
 */
public class CommuterDataElement {
	private String fromId;
	private String toId;
	private String fromName;
	private String toName;
	private int commuters;
	
	public CommuterDataElement(String from,  String to,  int commuters){
		this.fromId = from;
		this.toId = to;
		this.commuters = commuters;
		this.fromName = "";
		this.toName = "";
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

}
