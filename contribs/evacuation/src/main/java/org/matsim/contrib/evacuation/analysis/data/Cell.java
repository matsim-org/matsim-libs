/* *********************************************************************** *
 * project: org.matsim.*
 * RoadClosuresEditor.java
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

package org.matsim.contrib.evacuation.analysis.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordImpl;

public class Cell {
	private double timeSum;
	private int count;
	private List<Event> data;
	private List<Double> arrivalTimes;
	private List<Tuple<Tuple<Id<Link>, Id<Person>>, Double>> linkLeaveTimes;
	private List<Tuple<Tuple<Id<Link>, Id<Person>>, Double>> linkEnterTimes;
	private CoordImpl coord;
	private double clearingTime;
	private Id<Cell> id;

	private static int currentId = 0;

	public static String CELLSIZE = "cellsize";

	public Cell(List<Event> data) {
		this.data = data;
		this.linkLeaveTimes = new ArrayList<>();
		this.linkEnterTimes = new ArrayList<>();
		this.arrivalTimes = new ArrayList<Double>();
		this.clearingTime = 0d;

		currentId++;
		this.id = Id.create(currentId, Cell.class);
	}

	public double getTimeSum() {
		return timeSum;
	}

	public void setTimeSum(double timeSum) {
		this.timeSum = timeSum;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public void incrementCount() {
		this.count++;
	}

	public List<Event> getData() {
		return data;
	}

	public void setData(List<Event> data) {
		this.data = data;
	}

	public void setArrivalTimes(List<Double> arrivalTimes) {
		this.arrivalTimes = arrivalTimes;
	}

	public List<Double> getArrivalTimes() {
		return arrivalTimes;
	}

	public List<Tuple<Tuple<Id<Link>, Id<Person>>, Double>> getLinkEnterTimes() {
		return linkEnterTimes;
	}

	public List<Tuple<Tuple<Id<Link>, Id<Person>>, Double>> getLinkLeaveTimes() {
		return linkLeaveTimes;
	}

	public void setLinkEnterTimes(List<Tuple<Tuple<Id<Link>, Id<Person>>, Double>> linkEnterTimes) {
		this.linkEnterTimes = linkEnterTimes;
	}

	public void setLinkLeaveTimes(List<Tuple<Tuple<Id<Link>, Id<Person>>, Double>> linkLeaveTimes) {
		this.linkLeaveTimes = linkLeaveTimes;
	}

	public void addLinkEnterTime(Id<Link> linkId, Id<Person> personId, Double time) {
		if (this.linkEnterTimes == null)
			this.linkEnterTimes = new ArrayList<>();

		this.linkEnterTimes.add(new Tuple<Tuple<Id<Link>, Id<Person>>, Double>(new Tuple<Id<Link>, Id<Person>>(linkId, personId), time));
	}

	public void addLinkLeaveTime(Id<Link> linkId, Id<Person> personId, Double time) {
		if (this.linkLeaveTimes == null)
			this.linkLeaveTimes = new ArrayList<Tuple<Tuple<Id<Link>, Id<Person>>, Double>>();

		this.linkLeaveTimes.add(new Tuple<Tuple<Id<Link>, Id<Person>>, Double>(new Tuple<Id<Link>, Id<Person>>(linkId, personId), time));
	}

	public void setCoord(CoordImpl centroid) {
		this.coord = centroid;
	}

	public CoordImpl getCoord() {
		return coord;
	}

	public void updateClearanceTime(double latestTime) {
		if (latestTime > clearingTime)
			clearingTime = latestTime;
	}

	public double getClearingTime() {
		return clearingTime;
	}

	public Id<Cell> getId() {
		return this.id;
	}

	public void addArrivalTime(double time) {
		arrivalTimes.add(time);
	}

	public double getMeanArrivalTime() {
		double mean = 0d;

		for (double time : this.arrivalTimes)
			mean += time;

		return mean / this.arrivalTimes.size();
	}

	public double getMedianArrivalTime() {
		if (this.arrivalTimes.size() < 1)
			return 0d;

		Collections.sort(this.arrivalTimes);

		if (this.arrivalTimes.size() % 2 == 1)
			return this.arrivalTimes.get((this.arrivalTimes.size() + 1) / 2 - 1);
		else {
			double lower = this.arrivalTimes.get(this.arrivalTimes.size() / 2 - 1);
			double upper = this.arrivalTimes.get(this.arrivalTimes.size() / 2);

			return (lower + upper) / 2.0;
		}
	}

	public static int getCurrentId() {
		return currentId;
	}

}
