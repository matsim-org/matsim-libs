/* *********************************************************************** *
 * project: org.matsim.*
 * DgDemandUtils
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
package air.demand;

import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.core.utils.collections.Tuple;

/**
 * @author dgrether
 * 
 */
public class DgDemandUtils {

	/**
	 * demand1 - demand2, contains all entries that are in demand1
	 */
	public static SortedMap<String, SortedMap<String, FlightODRelation>> createDifferenceMap(
			SortedMap<String, SortedMap<String, FlightODRelation>> demand1,
			SortedMap<String, SortedMap<String, FlightODRelation>> demand2) {
		SortedMap<String, SortedMap<String, FlightODRelation>> result = new TreeMap<String, SortedMap<String, FlightODRelation>>();
		for (Entry<String, SortedMap<String, FlightODRelation>> e11 : demand1.entrySet()) {
			SortedMap<String, FlightODRelation> m21 = demand2.get(e11.getKey());
			if (m21 != null) {
				for (Entry<String, FlightODRelation> e12 : e11.getValue().entrySet()) {
					FlightODRelation od1 = e12.getValue();
					FlightODRelation od2 = m21.get(e12.getKey());
					String from = e11.getKey();
					String to = e12.getKey();
					Double numberOfTrips = null;
					if (od2 != null) {
						if (od1.getNumberOfTrips() != null && od2.getNumberOfTrips() != null) {
							if (od1.getFromAirportCode().compareTo(e11.getKey()) != 0
									|| od2.getFromAirportCode().compareTo(e11.getKey()) != 0) {
								throw new RuntimeException();
							}
							numberOfTrips = od1.getNumberOfTrips() - od2.getNumberOfTrips();
						}
					}
					FlightODRelation diffRelation = new FlightODRelation(from, to, numberOfTrips);
					addToMap(result, diffRelation);
				}
			}
			else {
				for (Entry<String, FlightODRelation> e12 : e11.getValue().entrySet()) {
					FlightODRelation diffRelation = new FlightODRelation(e11.getKey(), e12.getKey(), null);
					addToMap(result, diffRelation);
				}
			}
		}
		return result;
	}

	private static void addToMap(SortedMap<String, SortedMap<String, FlightODRelation>> result,
			FlightODRelation od) {
		SortedMap<String, FlightODRelation> r11 = result.get(od.getFromAirportCode());
		if (r11 == null) {
			r11 = new TreeMap<String, FlightODRelation>();
			result.put(od.getFromAirportCode(), r11);
		}
		r11.put(od.getToAirportCode(), od);
	}

	/**
	 * y - x / x
	 */
	public static SortedMap<String, SortedMap<String, FlightODRelation>> createRelativeErrorMap(
			SortedMap<String, SortedMap<String, FlightODRelation>> y,
			SortedMap<String, SortedMap<String, FlightODRelation>> x) {
		SortedMap<String, SortedMap<String, FlightODRelation>> result = new TreeMap<String, SortedMap<String, FlightODRelation>>();

		for (Entry<String, SortedMap<String, FlightODRelation>> eX : x.entrySet()) {
			String from = eX.getKey();
			for (Entry<String, FlightODRelation> eXX : eX.getValue().entrySet()) {
				String to = eXX.getKey();
				Double relError = null;
				SortedMap<String, FlightODRelation> mY = y.get(from);
				if (mY == null){
					continue;
				}
				FlightODRelation odY = mY.get(to);
				FlightODRelation odX = eXX.getValue();
				if (odX == null || odY == null || odY.getNumberOfTrips() == null || odX.getNumberOfTrips() == null) {
					continue;
				}
				if (odX.getNumberOfTrips() != 0.0) {
					relError = Math.abs(odY.getNumberOfTrips() - odX.getNumberOfTrips()) / odX.getNumberOfTrips();
					FlightODRelation diffRelation = new FlightODRelation(from, to, relError);
					addToMap(result, diffRelation);
				}
			}
		}
		return result;
	}

	public static SortedMap<String, SortedMap<String, FlightODRelation>> createFromAirportCodeToAirportCodeMap(
			List<FlightODRelation> odpairs) {
		SortedMap<String, SortedMap<String, FlightODRelation>> result = new TreeMap<String, SortedMap<String, FlightODRelation>>();
		for (FlightODRelation od : odpairs) {
			SortedMap<String, FlightODRelation> l = result.get(od.getFromAirportCode());
			if (l == null) {
				l = new TreeMap<String, FlightODRelation>();
				result.put(od.getFromAirportCode(), l);
			}
			l.put(od.getToAirportCode(), od);
		}
		return result;
	}

	public static void convertToDailyDemand(List<FlightODRelation> oddata) {
		for (FlightODRelation od : oddata) {
			if (od.getNumberOfTrips() != null) {
				od.setNumberOfTrips(od.getNumberOfTrips() / 30);
			}
		}
	}

	public static Tuple<Double, Integer> calcVariance(
			SortedMap<String, SortedMap<String, FlightODRelation>> diffMap) {
		double variance = 0.0;
		int n = 0;
		for (Entry<String, SortedMap<String, FlightODRelation>> e1 : diffMap.entrySet()) {
			for (Entry<String, FlightODRelation> e2 : e1.getValue().entrySet()) {
				if (e2.getValue().getNumberOfTrips() != null)
					variance += Math.pow(e2.getValue().getNumberOfTrips(), 2);
				n++;
			}
		}
		return new Tuple<Double, Integer>(variance, n);
	}

	public static Tuple<Double, Integer> calcRelativeErrorSum(
			SortedMap<String, SortedMap<String, FlightODRelation>> relErrorMap) {
		double relError = 0.0;
		int count = 0;
		for (Entry<String, SortedMap<String, FlightODRelation>> e1 : relErrorMap.entrySet()) {
			for (Entry<String, FlightODRelation> e2 : e1.getValue().entrySet()) {
				relError += e2.getValue().getNumberOfTrips();
				count++;
			}
		}
		return new Tuple<Double, Integer>(relError, count);
	}

}
