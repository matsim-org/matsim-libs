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

package playground.anhorni.surprice.preprocess;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.ActivityFacility;

public class Zone {	
	String name;
	private Coord topleft;
	private double height;
	private double length;
	private List<ActivityFacility> facilitiesInZone = new Vector<ActivityFacility>();
	private final static Logger log = Logger.getLogger(Zone.class);	
	
	private Coord center;
	private double radius = -1.0;
	
	public Zone(String name, Coord topleft, double height, double length) {
		this.name = name;
		this.topleft = topleft;
		this.height = height;
		this.length = length;
	}
	
	public Zone(String name, Coord center, double radius) {
		this.name = name;
		this.center = center;
		this.radius = radius;
	}
	
	public Coord getTopleft() {
		return topleft;
	}
	public void setTopleft(Coord topleft) {
		this.topleft = topleft;
	}
	public double getHeight() {
		return height;
	}
	public void setHeight(double height) {
		this.height = height;
	}
	public double getLength() {
		return length;
	}
	public void setLength(double length) {
		this.length = length;
	}
	
	public void addFacility(ActivityFacility facility) {
		this.facilitiesInZone.add(facility);
	}
	
	public ActivityFacility getRandomLocationInZone(Random random) {
		if (facilitiesInZone.size() == 0) {
			log.error("No facilities in zone " + this.name);
			System.exit(-1);
		}
		int index = random.nextInt(facilitiesInZone.size());
		return this.facilitiesInZone.get(index);
	}
	
	public List<Id> getlinksInZone() {
		List<Id> links = new ArrayList<Id>();
		for (ActivityFacility facility : this.facilitiesInZone) {
			links.add(facility.getLinkId());
		}
		return links;
	}
 	
	public boolean inZone(Coord point) {	
		if (center != null && radius > 0.0) {
			if (CoordUtils.calcDistance(center, point) < radius) {
				return true;
			}
			else {
				return false;
			}
		} else {
			if ((point.getX() >= topleft.getX() && point.getX() <= topleft.getX() + length) && 
					point.getY() <= topleft.getY() && point.getY() >= topleft.getY() - height) {
				return true;
			}
			else {
				return false;
			}
		}
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
