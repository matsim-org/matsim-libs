/* *********************************************************************** *
 * project: org.matsim.*
 * DgDestatisCompare
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;

import org.apache.log4j.Logger;
import org.matsim.core.utils.collections.Tuple;


/**
 * @author dgrether
 *
 */
public class DgDestatisCompare {

	private static final Logger log = Logger.getLogger(DgDestatisCompare.class);
	
	private List<FlightODRelation> compare(String odDemand21, String odDemand22) throws IOException {
		List<FlightODRelation> demand21 = new DgDemandReader().readFile(odDemand21);
		List<FlightODRelation> demand22 = new DgDemandReader().readFile(odDemand22);
		DgDemandUtils.convertToDailyDemand(demand21);
		DgDemandUtils.convertToDailyDemand(demand22);
		List<FlightODRelation> diff = this.createDifferenceList(demand21, demand22);
		return diff;
	}

	private void calculateTotalLineSwitch(List<FlightODRelation> diff) {
		int lineSwitch = 0;
		for (FlightODRelation od : diff){
			lineSwitch += Math.abs(od.getNumberOfTrips());
		}
		log.info("Total line switch: " + lineSwitch);
	}
	
	/**
	 * demand1 - demand2
	 */
	public List<FlightODRelation> createDifferenceList(List<FlightODRelation> demand1, List<FlightODRelation> demand2){
		List<FlightODRelation> result = new ArrayList<FlightODRelation>();
		for (FlightODRelation od1 : demand1){
			FlightODRelation od2 = this.searchCorrespondingODRelation(od1, demand2);
			Double diff = null;
			if (od1.getNumberOfTrips() != null && od2.getNumberOfTrips() != null) {
				diff = od1.getNumberOfTrips() - od2.getNumberOfTrips();
			}
			result.add(new FlightODRelation(od1.getFromAirportCode(), od1.getToAirportCode(), diff));
		}
		return result;
	}
	
	private FlightODRelation searchCorrespondingODRelation(FlightODRelation rel, List<FlightODRelation> relations){
		for (FlightODRelation od : relations){
			if (od.getFromAirportCode().compareTo(rel.getFromAirportCode()) == 0 && od.getToAirportCode().compareTo(rel.getToAirportCode()) == 0) {
				return od;
			}
		}
		throw new RuntimeException("no od pair found for from " + rel.getFromAirportCode() + " to " + rel.getToAirportCode());
	}
	
	
	public static void main(String[] args) throws Exception {
		String odDemand22 = "/media/data/work/repos/shared-svn/studies/countries/de/flight/demand/destatis/2011_september/demand_september_2011_tabelle_2.2.2.csv";
		String odDemand21 = "/media/data/work/repos/shared-svn/studies/countries/de/flight/demand/destatis/2011_september/demand_september_2011_tabelle_2.2.1.csv";
		String odDiffOutput = "/media/data/work/repos/shared-svn/studies/countries/de/flight/demand/destatis/2011_september/demand_september_2011_diff_2.2.1-2.2.2.csv";
		String odDiffOutputAbs = "/media/data/work/repos/shared-svn/studies/countries/de/flight/demand/destatis/2011_september/demand_september_2011_diff_2.2.1-2.2.2abs.csv";

		List<FlightODRelation> demand21 = new DgDemandReader().readFile(odDemand21);
		List<FlightODRelation> demand22 = new DgDemandReader().readFile(odDemand22);
		DgDemandUtils.convertToDailyDemand(demand21);
		DgDemandUtils.convertToDailyDemand(demand22);
		int totalDirectFlights = 0;
		for (FlightODRelation od : demand21) {
			totalDirectFlights += od.getNumberOfTrips();
		}
		DgDestatisCompare compare = new DgDestatisCompare();
		List<FlightODRelation> diff = compare.createDifferenceList(demand21, demand22);
		SortedMap<String, SortedMap<String, FlightODRelation>> diffMap = DgDemandUtils.createFromAirportCodeToAirportCodeMap(diff);
		Tuple<Double, Integer> variance = DgDemandUtils.calcVariance(diffMap);
		
		//		List<FlightODRelation> diff = compare.compare(odDemand21, odDemand22);
		new DgDemandWriter().writeFlightODRelations(odDiffOutput, diffMap, totalDirectFlights, 0, false);
		new DgDemandWriter().writeFlightODRelations(odDiffOutputAbs, diffMap, totalDirectFlights, 0, true);
		compare.calculateTotalLineSwitch(diff);
		System.out.println("Variance " + variance.getFirst() + " std deviation: " + Double.toString(Math.sqrt(variance.getFirst())));
	}

}
