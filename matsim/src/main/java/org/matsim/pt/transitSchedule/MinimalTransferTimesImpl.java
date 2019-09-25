/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package org.matsim.pt.transitSchedule;

import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.MinimalTransferTimes;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of {@link org.matsim.pt.transitSchedule.api.MinimalTransferTimes}
 *
 * @author mrieser / SBB
 */
class MinimalTransferTimesImpl implements MinimalTransferTimes {

	private Map<Id<org.matsim.facilities.Facility>, Map<Id<org.matsim.facilities.Facility>, Double>> minimalTransferTimes = new ConcurrentHashMap<>();

	@Override
	public double set(Id<org.matsim.facilities.Facility> fromStop, Id<org.matsim.facilities.Facility> toStop, double seconds) {
		if (Double.isNaN(seconds) || Time.isUndefinedTime(seconds)) {
			return remove(fromStop, toStop);
		}
		Map<Id<org.matsim.facilities.Facility>, Double> innerMap = this.minimalTransferTimes.computeIfAbsent(fromStop, key -> new ConcurrentHashMap<>());
		Double value = innerMap.put(toStop, seconds);
		if (value == null) {
			return Double.NaN;
		}
		return 0;
	}

	@Override
	public double get(Id<org.matsim.facilities.Facility> fromStop, Id<org.matsim.facilities.Facility> toStop) {
		return get(fromStop, toStop, Double.NaN);
	}

	@Override
	public double get(Id<org.matsim.facilities.Facility> fromStop, Id<org.matsim.facilities.Facility> toStop, double defaultSeconds) {
		Map<Id<org.matsim.facilities.Facility>, Double> innerMap = this.minimalTransferTimes.get(fromStop);
		if (innerMap == null) {
			return defaultSeconds;
		}
		Double value = innerMap.get(toStop);
		if (value == null) {
			return defaultSeconds;
		}
		return value;
	}

	@Override
	public double remove(Id<org.matsim.facilities.Facility> fromStop, Id<org.matsim.facilities.Facility> toStop) {
		Map<Id<org.matsim.facilities.Facility>, Double> innerMap = this.minimalTransferTimes.get(fromStop);
		if (innerMap == null) {
			return Double.NaN;
		}
		Double value = innerMap.remove(toStop);
		if (value == null) {
			return Double.NaN;
		}
		return value;
	}

	@Override
	public MinimalTransferTimesIterator iterator() {
		return new MinimalTransferTimesIteratorImpl(this.minimalTransferTimes);
	}

	private static class MinimalTransferTimesIteratorImpl implements  MinimalTransferTimesIterator {

		private Id<org.matsim.facilities.Facility> nextFromStopId = null;
		private Id<org.matsim.facilities.Facility> fromStopId = null;
		private Id<org.matsim.facilities.Facility> toStopId = null;
		private double seconds = Double.NaN;
		private boolean hasElement = false;

		private Iterator<Map.Entry<Id<org.matsim.facilities.Facility>, Map<Id<org.matsim.facilities.Facility>, Double>>> outerIterator;
		private Iterator<Map.Entry<Id<org.matsim.facilities.Facility>, Double>> innerIterator;

		MinimalTransferTimesIteratorImpl(Map<Id<org.matsim.facilities.Facility>, Map<Id<org.matsim.facilities.Facility>, Double>> values) {
			this.outerIterator = values.entrySet().iterator();
		}

		@Override
		public boolean hasNext() {
			if (this.innerIterator != null && this.innerIterator.hasNext()) {
				return true;
			}
			while (this.outerIterator.hasNext()) {
				Map.Entry<Id<org.matsim.facilities.Facility>, Map<Id<org.matsim.facilities.Facility>, Double>> outerEntry = this.outerIterator.next();
				Map<Id<org.matsim.facilities.Facility>, Double> innerMap = outerEntry.getValue();
				this.innerIterator = innerMap.entrySet().iterator();
				if (this.innerIterator.hasNext()) {
					this.nextFromStopId = outerEntry.getKey();
					return true;
				}
			}

			this.nextFromStopId = null;
			return false;
		}

		@Override
		public void next() {
			if (this.innerIterator != null && this.innerIterator.hasNext()) {
				Map.Entry<Id<org.matsim.facilities.Facility>, Double> e = this.innerIterator.next();
				this.fromStopId = this.nextFromStopId;
				this.toStopId = e.getKey();
				this.seconds = e.getValue();
				this.hasElement = true;
			} else {
				this.hasElement = false;
				throw new NoSuchElementException();
			}
		}

		@Override
		public Id<org.matsim.facilities.Facility> getFromStopId() {
			if (this.hasElement) {
				return this.fromStopId;
			}
			throw new NoSuchElementException();
		}

		@Override
		public Id<org.matsim.facilities.Facility> getToStopId() {
			if (this.hasElement) {
				return this.toStopId;
			}
			throw new NoSuchElementException();
		}

		@Override
		public double getSeconds() {
			if (this.hasElement) {
				return this.seconds;
			}
			throw new NoSuchElementException();
		}
	}
}
