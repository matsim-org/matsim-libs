/*
 * Copyright 2018 Gunnar Flötteröd
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.flotterod@gmail.com
 *
 */
package org.matsim.contrib.pseudosimulation.searchacceleration.listeners;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import floetteroed.utilities.Time;
import floetteroed.utilities.Tuple;

/**
 * Identifies the next possible departure from a given TransitRouteStop along a
 * given TransitRoute. Assumes that vehicles serve the TransitRouteStop in the
 * same order as they initially depart on the TransitRoute.
 *
 * @author Gunnar Flötteröd
 *
 */
public class NextAvailableDepartures {

	/*
	 * Time-ordered list of departures and corresponding latest times at which one
	 * needs to arrive at the stop in order to catch the vehicle corresponding to
	 * the departure.
	 */
	private final List<Id<Departure>> departureIds;
	private final List<Double> latestStopArrivalTimes_s;

	/**
	 * Schedule-based initialization.
	 */
	public NextAvailableDepartures(final TransitRoute route, final TransitRouteStop stop) {

		final List<Departure> tmpDepartures = new ArrayList<>(route.getDepartures().values());
		Collections.sort(tmpDepartures, new Comparator<Departure>() {
			@Override
			public int compare(final Departure dpt1, final Departure dpt2) {
				return Double.compare(dpt1.getDepartureTime(), dpt2.getDepartureTime());
			}
		});

		this.departureIds = new ArrayList<>(tmpDepartures.size());
		this.latestStopArrivalTimes_s = new ArrayList<>(tmpDepartures.size());
		for (Departure departure : tmpDepartures) {
			this.departureIds.add(departure.getId());
			this.latestStopArrivalTimes_s.add(departure.getDepartureTime() + stop.getDepartureOffset());
		}
	}

	/**
	 * Adjusts, if necessary, the latest stop arrival times such that they are
	 * compatible with this observation.
	 * <p>
	 * A realizedDepartureId of null means that someone has gotten stuck, i.e. that
	 * no more vehicles are coming after the given arrival time.
	 */
	public void adjustToRealizedDeparture(final double realizedStopArrivalTime_s,
			final Id<Departure> realizedDepartureId) {
		if (realizedDepartureId != null) {
			final int realizedDepartureIndex = this.departureIds.indexOf(realizedDepartureId);
			for (int i = realizedDepartureIndex; (i < this.latestStopArrivalTimes_s.size())
					&& (realizedStopArrivalTime_s > this.latestStopArrivalTimes_s.get(i)); i++) {
				this.latestStopArrivalTimes_s.set(i, realizedStopArrivalTime_s);
			}
			for (int i = realizedDepartureIndex - 1; (i >= 0)
					&& (realizedStopArrivalTime_s < this.latestStopArrivalTimes_s.get(i)); i--) {
				this.latestStopArrivalTimes_s.set(i, realizedStopArrivalTime_s);
			}
		} else {
			for (int i = 0; i < this.departureIds.size(); i++) {
				if (latestStopArrivalTimes_s.get(i) >= realizedStopArrivalTime_s) {
					latestStopArrivalTimes_s.set(i, realizedStopArrivalTime_s);
				}
			}
		}
	}

	/**
	 * May return null if stopArrivalTime_s is too late.
	 */
	public Tuple<Id<Departure>, Double> getNextAvailableDepartureIdAndTime_s(final double stopArrivalTime_s) {
		for (int i = 0; i < this.departureIds.size(); i++) {
			if (this.latestStopArrivalTimes_s.get(i) >= stopArrivalTime_s) {
				return new Tuple<>(this.departureIds.get(i), this.latestStopArrivalTimes_s.get(i));
			}
		}
		return null;
	}

	// ==================== EVERYTHING BELOW FOR TESTING ONLY ====================

	public String toString() {
		final StringBuffer result = new StringBuffer();
		for (int i = 0; i < this.departureIds.size(); i++) {
			final Id<Departure> dpt = this.departureIds.get(i);
			result.append("Departure " + dpt + " has latest stop arrival time "
					+ Time.strFromSec(this.latestStopArrivalTimes_s.get(i).intValue()) + ".\n");
		}
		return result.toString();
	}

	static void adjust(NextAvailableDepartures nextAvailDpt, String timeStr, Departure dpt) {
		System.out.println(
				"Reached " + (dpt != null ? dpt.getId() : "no departure") + " with stop arrival time " + timeStr + ".");
		nextAvailDpt.adjustToRealizedDeparture(Time.secFromStr(timeStr), (dpt != null ? dpt.getId() : null));
		System.out.println(nextAvailDpt);
	}

	public static void main(String[] args) {

		TransitScheduleFactory factory = new TransitScheduleFactoryImpl();

		TransitStopFacility stopFac = factory.createTransitStopFacility(Id.create("stopFac", TransitStopFacility.class),
				null, false);
		TransitRouteStop stop = factory.createTransitRouteStop(stopFac, Time.secFromStr("00:20:00"),
				Time.secFromStr("00:30:00"));
		List<TransitRouteStop> stops = new ArrayList<>(1);
		stops.add(stop);
		TransitRoute route = factory.createTransitRoute(Id.create("route", TransitRoute.class), null, stops, "pt");

		Departure dpt1 = factory.createDeparture(Id.create("dpt1", Departure.class), Time.secFromStr("07:00:00"));
		Departure dpt2 = factory.createDeparture(Id.create("dpt2", Departure.class), Time.secFromStr("08:00:00"));
		Departure dpt3 = factory.createDeparture(Id.create("dpt3", Departure.class), Time.secFromStr("09:00:00"));

		route.addDeparture(dpt1);
		route.addDeparture(dpt2);
		route.addDeparture(dpt3);

		NextAvailableDepartures nextAvailDpt = new NextAvailableDepartures(route, stop);
		System.out.println(nextAvailDpt);

		// no adjustment

		adjust(nextAvailDpt, "6:45:00", dpt1);
		adjust(nextAvailDpt, "7:15:00", dpt1);
		adjust(nextAvailDpt, "7:28:00", dpt1);

		adjust(nextAvailDpt, "7:45:00", dpt2);
		adjust(nextAvailDpt, "8:15:00", dpt2);
		adjust(nextAvailDpt, "8:28:00", dpt2);

		adjust(nextAvailDpt, "8:45:00", dpt3);
		adjust(nextAvailDpt, "9:15:00", dpt3);
		adjust(nextAvailDpt, "9:28:00", dpt3);

		// all a bit later

		adjust(nextAvailDpt, "07:35:00", dpt1);
		adjust(nextAvailDpt, "08:35:00", dpt2);
		adjust(nextAvailDpt, "09:35:00", dpt3);

		// all very much later

		adjust(nextAvailDpt, "08:35:00", dpt1);
		adjust(nextAvailDpt, "09:35:00", dpt2);
		adjust(nextAvailDpt, "10:35:00", dpt3);

		// all a bit earlier

		adjust(nextAvailDpt, "08:30:00", dpt1);
		adjust(nextAvailDpt, "09:30:00", dpt2);
		adjust(nextAvailDpt, "10:30:00", dpt3);

		// all very much earlier

		adjust(nextAvailDpt, "06:30:00", dpt1);
		adjust(nextAvailDpt, "07:30:00", dpt2);
		adjust(nextAvailDpt, "08:30:00", dpt3);
		adjust(nextAvailDpt, "07:15:00", dpt3);

		// failure to reach a departure

		adjust(nextAvailDpt, "09:00:00", null);
		adjust(nextAvailDpt, "07:00:00", null);

	}
}
