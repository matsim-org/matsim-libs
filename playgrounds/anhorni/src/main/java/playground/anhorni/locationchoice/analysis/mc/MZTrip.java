/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.anhorni.locationchoice.analysis.mc;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

public class MZTrip {
	
	private Id<Person> personId = null;
	private Coord home = null;
	private Coord coordEnd = null;
	private Coord coordStart = null;
	
	// F58
	private double startTime = 0.0;
	// F514
	private double endTime = 0.0;
	private String wmittel;
	private String ausmittel;	
	private String purposeCode;	
	private String purpose;
	private String wzweck2;
	
	public MZTrip(Id<Person> personId, Coord coordStart, Coord coordEnd, double startTime, double endTime) {
		super();
		this.personId = personId;
		this.coordStart = new Coord(coordStart.getX(), coordStart.getY());
		this.coordEnd = new Coord(coordEnd.getX(), coordEnd.getY());
		this.startTime = startTime;
		this.endTime = endTime;
	}

	public Id<Person> getPersonId() {
		return personId;
	}
	public void setPersonId(Id<Person> personId) {
		this.personId = personId;
	}
	public Coord getCoordEnd() {
		return coordEnd;
	}
	public void setCoordEnd(Coord coordEnd) {
		this.coordEnd = new Coord(coordEnd.getX(), coordEnd.getY());
	}
	public Coord getCoordStart() {
		return coordStart;
	}
	public void setCoordStart(Coord coordStart) {
		this.coordStart = new Coord(coordStart.getX(), coordStart.getY());
	}
	public double getStartTime() {
		return startTime;
	}
	public void setStartTime(double startTime) {
		this.startTime = startTime;
	}
	public double getEndTime() {
		return endTime;
	}
	public void setEndTime(double endTime) {
		this.endTime = endTime;
	}
	public String getWmittel() {
		return wmittel;
	}
	public void setWmittel(String wmittel) {
		this.wmittel = wmittel;
	}
	public String getAusmittel() {
		return ausmittel;
	}
	public void setAusmittel(String ausmittel) {
		this.ausmittel = ausmittel;
	}
	public String getPurposeCode() {
		return purposeCode;
	}
	public void setPurposeCode(String purposeCode) {
		this.purposeCode = purposeCode;
	}
	public String getPurpose() {
		return purpose;
	}
	public void setPurpose(String purpose) {
		this.purpose = purpose;
	}
	public String getWzweck2() {
		return wzweck2;
	}
	public void setWzweck2(String wzweck2) {
		this.wzweck2 = wzweck2;
	}

	public Coord getHome() {
		return home;
	}
	public double getDuration() {
		return (this.endTime - this.startTime);
	}

	public void setHome(Coord home) {
		this.home = home;
	}
	public String getMatsimMode() {
		
		int wmittelInt = Integer.parseInt(this.wmittel);
		
		 //2: Bahn 3: Postauto 5: Tram 6: Bus
		if (wmittelInt == 2 || wmittelInt == 3 || wmittelInt == 5 || wmittelInt == 6) {
			return "pt";
		}
		// MIV
		//9: Auto  11: Taxi 12: Motorrad, Kleinmotorrad 13: Mofa
		else if (wmittelInt == 9 || wmittelInt == 11 || wmittelInt == 12 || wmittelInt == 13) {
			return "car";
		}
		//14: Velo
		else if (wmittelInt == 14) {
			return "bike";
		}
		//15: zu Fuss
		else if (wmittelInt == 15) {
			return "walk";
		}
		else return "undefined";
	}
}
