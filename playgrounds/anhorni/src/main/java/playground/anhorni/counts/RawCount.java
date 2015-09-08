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

package playground.anhorni.counts;

import org.matsim.api.core.v01.Coord;

public class RawCount {
	String id;
	int year;
	int month;
	int day;
	int hour;
	double vol1;
	double vol2;
	
	// after mapping
	String direction;
	String linkidTeleatlas;
	String linkidNavteq;
	String linkidIVTCH;
	
	Coord coord;
	
	
	public RawCount(String id, String year, String month,
			String day, String hour, String vol1, String vol2) {
		this.id = id;
		this.year = Integer.parseInt(year);
		this.month = Integer.parseInt(month);
		this.day = Integer.parseInt(day);
		this.hour = Integer.parseInt(hour) -1;
		this.vol1 = Double.parseDouble(vol1);
		this.vol2 = Double.parseDouble(vol2);
	}
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public int getYear() {
		return year;
	}
	public void setYear(String year) {
		this.year = Integer.parseInt(year);
	}
	public int getMonth() {
		return this.month;
	}
	public void setMonth(String month) {
		this.month = Integer.parseInt(month);
	}
	public int getDay() {
		return day;
	}
	public void setDay(String day) {
		this.day = Integer.parseInt(day);
	}
	public int getHour() {
		return hour;
	}
	public void setHour(String hour) {
		this.hour = Integer.parseInt(hour);
	}
	public double getVol1() {
		return vol1;
	}
	public void setVol1(String vol1) {
		this.vol1 = Double.parseDouble(vol1);
	}
	public double getVol2() {
		return this.vol2;
	}
	public void setVol2(String vol2) {
		this.vol2 = Double.parseDouble(vol2);
	}

	public String getLinkidTeleatlas() {
		return linkidTeleatlas;
	}

	public void setLinkidTeleatlas(String linkidTeleatlas) {
		this.linkidTeleatlas = linkidTeleatlas;
	}

	public String getLinkidNavteq() {
		return linkidNavteq;
	}

	public void setLinkidNavteq(String linkidNavteq) {
		this.linkidNavteq = linkidNavteq;
	}

	public String getLinkidIVTCH() {
		return linkidIVTCH;
	}

	public void setLinkidIVTCH(String linkidIVTCH) {
		this.linkidIVTCH = linkidIVTCH;
	}

	public Coord getCoord() {
		return coord;
	}

	public void setCoord(Coord coord) {
		this.coord = coord;
	}

	public String getDirection() {
		return direction;
	}

	public void setDirection(String direction) {
		this.direction = direction;
	}
	

}
