/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.ikaddoura.incidents.data;

/**
* @author ikaddoura
*/

public class TMCAlert {
	
	private String phraseCode;
	private String description;
	private String alertDuration;
	private String updateClass;
	private String extent;
	
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getAltertDuration() {
		return alertDuration;
	}
	public void setAlertDuration(String alertDuration) {
		this.alertDuration = alertDuration;
	}
	public String getUpdateClass() {
		return updateClass;
	}
	public void setUpdateClass(String updateClass) {
		this.updateClass = updateClass;
	}
	public String getPhraseCode() {
		return phraseCode;
	}
	public void setPhraseCode(String phraseCode) {
		this.phraseCode = phraseCode;
	}
	public String getExtent() {
		return extent;
	}
	public void setExtent(String extent) {
		this.extent = extent;
	}
	@Override
	public String toString() {
		return "TMCAlert [description=" + description + ", alertDuration=" + alertDuration + ", updateClass="
				+ updateClass + ", phraseCode=" + phraseCode + ", extent=" + extent + "]";
	}

}

