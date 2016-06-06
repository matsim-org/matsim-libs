/* *********************************************************************** *
 * project: org.matsim.*
 * Service.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.polettif.publicTransitMapping.gtfs.containers;

import org.matsim.core.utils.collections.MapUtils;

import java.time.LocalDate;
import java.util.*;

public class Service {
	
	//Attributes
	private final String id;
	private final boolean[] days;
	private final LocalDate startDate;
	private final LocalDate endDate;
	private Collection<LocalDate> additions;
	private Collection<LocalDate> exceptions;

	public static Map<LocalDate, Set<String>> dateStats = new HashMap<>();

	//Methods

	/**
	 *
	 * @param serviceId
	 * @param days
	 * @param startDateStr
	 * @param endDateStr
	 */
	public Service(String serviceId, boolean[] days, String startDateStr, String endDateStr) {
		super();
		this.id = serviceId;
		this.days = days;
		this.startDate = parseDateFormat(startDateStr);
		this.endDate = parseDateFormat(endDateStr);
		this.additions = new ArrayList<>();
		this.exceptions = new ArrayList<>();

		LocalDate currentDate = parseDateFormat(startDateStr);

		while(currentDate.isBefore(endDate)) {
			int weekday = currentDate.getDayOfWeek().getValue() - 1;
			if(days[weekday]) {
				MapUtils.getSet(currentDate, dateStats).add(serviceId);
			}
			currentDate = currentDate.plusDays(1);
		}
	}

	/**
	 * @return the days
	 */
	public boolean[] getDays() {
		return days;
	}
	/**
	 * @return the startDate
	 */
	public LocalDate getStartDate() {
		return startDate;
	}
	/**
	 * @return the endDate
	 */
	public LocalDate getEndDate() {
		return endDate;
	}
	/**
	 * @return the additions
	 */
	public Collection<LocalDate> getAdditions() {
		return additions;
	}
	/**
	 * @return the exceptions
	 */
	public Collection<LocalDate> getExceptions() {
		return exceptions;
	}
	/**
	 * Adds a new addition date
	 * @param addition
	 */
	public void addAddition(String addition) {
		LocalDate additionDate = parseDateFormat(addition);
		additions.add(additionDate);
		MapUtils.getSet(additionDate, dateStats).add(this.getId());
	}
	/**
	 * Adds a new exception date
	 * @param exception
	 */
	public void addException(String exception) {
		LocalDate exceptionDate = parseDateFormat(exception);
		additions.add(exceptionDate);
		MapUtils.getSet(exceptionDate, dateStats).remove(this.getId());
	}
	
	public String getId() {
		return id;
	}

	/**
	 * parses the date format YYYYMMDD to LocalDate
	 */
	private LocalDate parseDateFormat(String yyyymmdd) {
		return LocalDate.of(Integer.parseInt(yyyymmdd.substring(0, 4)), Integer.parseInt(yyyymmdd.substring(4, 6)), Integer.parseInt(yyyymmdd.substring(6, 8)));
	}
}
