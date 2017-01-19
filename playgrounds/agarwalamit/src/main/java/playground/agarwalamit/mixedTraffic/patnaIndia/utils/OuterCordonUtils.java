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
package playground.agarwalamit.mixedTraffic.patnaIndia.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.vehicles.Vehicle;

import playground.agarwalamit.mixedTraffic.patnaIndia.input.extDemand.OuterCordonLinks;
import playground.agarwalamit.mixedTraffic.patnaIndia.input.extDemand.OuterCordonModalCountsAdjustmentCalculator;

/**
 * @author amit
 */

public class OuterCordonUtils {

	public enum PatnaNetworkType {osmNetwork, shpNetwork /*shp network is from transcad files*/}
	public static final double SAMPLE_SIZE = PatnaUtils.SAMPLE_SIZE;

	/**
	 * modal correction factor only from car/motorbike/truck/bike and not from all vehicles (additionally, walk, cycle rickshaw, pt)
	 * see {@link OuterCordonModalCountsAdjustmentCalculator}
	 */
//	public static final double E2I_TRIP_REDUCTION_FACTOR = 0.853; 
	public static Map<String,Double> getModalOutTrafficAdjustmentFactor (){
		Map<String, Double> mode2adjustmentFactor = new HashMap<>();
		mode2adjustmentFactor.put("car", 0.98);
		mode2adjustmentFactor.put("motorbike", 0.86);
		mode2adjustmentFactor.put("bike", 0.68);
		mode2adjustmentFactor.put("truck", 1.16);
		mode2adjustmentFactor.put("total", 0.94);
		return mode2adjustmentFactor;
	}

	public static Map<String, List<Integer>> getAreaType2ZoneIds(){//(from Fig.4-9 in PatnaReport)
		Map<String, List<Integer>> areas2zones = new HashMap<>();
		areas2zones.put("Institutional", Arrays.asList(8,9,20));
		areas2zones.put("Industrial", Arrays.asList(61,62));
		areas2zones.put("CBD and Railway Station", Arrays.asList(19,21,28));
		areas2zones.put("Educational", Arrays.asList(37,38,39,40,41,42));
		areas2zones.put("Old city and market area", Arrays.asList(54,57));
		return areas2zones;
	}

	/**
	 * See, table 3-13 in Patna CMP for the ext-ext trip shares.
	 */
	public static double getExtExtTripShareBetweenCountingStations(String originCountingStation, String destinationCountingStation){
		double factor = 0;
		if(originCountingStation.equals(destinationCountingStation)) return 0.;
		else if(originCountingStation.equals("OC1") ) {
			switch(destinationCountingStation) {
			case "OC2" : factor = 0.0; break;
			case "OC3" : factor = 0.02; break;//.01-->.02 (to make total 1.0)
			case "OC4" : factor = 0.49; break;
			case "OC5" : factor = 0.15; break;
			case "OC6" : factor = 0.03; break;
			case "OC7" : factor = 0.31; break;
			}
		} else if(originCountingStation.equals("OC2") ) {
			switch(destinationCountingStation) {
			case "OC1" : factor = 0.01; break;
			case "OC3" : factor = 0.0; break;
			case "OC4" : factor = 0.84; break;
			case "OC5" : factor = 0.05; break;
			case "OC6" : factor = 0.0; break;
			case "OC7" : factor = 0.10; break;
			}
		} else if(originCountingStation.equals("OC3") ) {
			switch(destinationCountingStation) {
			case "OC1" : factor = 0.19; break;
			case "OC2" : factor = 0.04; break;
			case "OC4" : factor = 0.04; break;
			case "OC5" : factor = 0.17; break;
			case "OC6" : factor = 0.23; break;
			case "OC7" : factor = 0.33; break;
			}
		} else if(originCountingStation.equals("OC4") ) {
			switch(destinationCountingStation) {
			case "OC1" : factor = 0.76; break;
			case "OC2" : factor = 0.16; break;
			case "OC3" : factor = 0.0; break;
			case "OC5" : factor = 0.03; break;
			case "OC6" : factor = 0.0; break;
			case "OC7" : factor = 0.05; break;
			}
		} else if(originCountingStation.equals("OC5") ) {
			switch(destinationCountingStation) {
			case "OC1" : factor = 0.35; break;
			case "OC2" : factor = 0.07; break;
			case "OC3" : factor = 0.04; break;
			case "OC4" : factor = 0.38; break;
			case "OC6" : factor = 0.08; break;
			case "OC7" : factor = 0.08; break;
			}
		} else if(originCountingStation.equals("OC6") ) {
			switch(destinationCountingStation) {
			case "OC1" : factor = 0.30; break;
			case "OC2" : factor = 0.07; break;//.06-->.07 (to make total 1.0)
			case "OC3" : factor = 0.23; break;
			case "OC4" : factor = 0.0; break;
			case "OC5" : factor = 0.13; break;
			case "OC7" : factor = 0.27; break;
			}
		} else if(originCountingStation.equals("OC7") ) {
			switch(destinationCountingStation) {
			case "OC1" : factor = 0.34; break;
			case "OC2" : factor = 0.07; break;
			case "OC3" : factor = 0.0; break;
			case "OC4" : factor = 0.09; break;
			case "OC5" : factor = 0.5; break;
			case "OC6" : factor = 0.0; break;
			}
		}
		return factor;
	}

	/**
	 * Table 3-12 in PatnaCMP
	 */
	public enum OuterCordonDirectionalFactors{
		/*
		 * E -- external
		 * I -- internal
		 */
		OC1_X2P_E2I (0.70), OC1_X2P_E2E (0.30),
		OC2_X2P_E2I (0.58), OC2_X2P_E2E (0.42),
		OC3_X2P_E2I (0.94), OC3_X2P_E2E (0.06),//(0.20), with 0.06 and 1% sample, ext-ext trips are zero in all time bin and for all modes
		OC4_X2P_E2I (0.66), OC4_X2P_E2E (0.34),
		OC5_X2P_E2I (0.76), OC5_X2P_E2E (0.24),
		OC6_X2P_E2I (0.86), OC6_X2P_E2E (0.14),
		OC7_X2P_E2I (0.95), OC7_X2P_E2E (0.05);//(0.25); with 0.05 and 1% sample, ext-ext trips are zero in all time bin and for all modes

		private final double factor;

		OuterCordonDirectionalFactors(final double factor){
			this.factor = factor;
		}

		public double getDirectionalFactor(){
			return factor;
		}
	}

	public static double getDirectionalFactorFromOuterCordonKey(final String countingStationKey, final String extIntKey){
		List<String> extIntKeys = Arrays.asList("E2I", "I2E", "E2E") ;

		if(! extIntKeys.contains(extIntKey) ) {
			throw new RuntimeException(extIntKey+" is not known. Possible options are -- "+extIntKeys.toString());
		}
		String key = countingStationKey+"_"+extIntKey;
        return OuterCordonDirectionalFactors.valueOf(key).getDirectionalFactor();
	}

	/**
	 * @param countingStationNumber OC1 or OC2 ...
	 * @param countingDirection "In" for outside to Patna, "Out" for Patna to outside.
	 * @return
	 */
	public static String getCountingStationKey(final String countingStationNumber, final String countingDirection){
		String prefix = countingDirection.equalsIgnoreCase("In") ? "X2P" : "P2X" ;
		return countingStationNumber.toUpperCase()+"_"+prefix.toUpperCase();
	}

	public static List<Id<Link>> getExternalToInternalCountStationLinkIds(final PatnaNetworkType pnt){
		List<Id<Link>> links = new ArrayList<>();
		for(Entry<String,String> e : new OuterCordonLinks(pnt).getCountingStationToLink().entrySet()){
			if(e.getKey().split("_")[1].equals("X2P")) links.add( Id.createLinkId(e.getValue()) ) ;
		}
		return links;
	}

	public static List<Id<Link>> getInternalToExternalCountStationLinkIds(final PatnaNetworkType pnt){
		List<Id<Link>> links = new ArrayList<>();
		for(Entry<String,String> e : new OuterCordonLinks(pnt).getCountingStationToLink().entrySet()){
			if(e.getKey().split("_")[1].equals("P2X")) links.add( Id.createLinkId(e.getValue()) ) ;
		}
		return links;
	}

	public static boolean isVehicleFromThroughTraffic(Id<Vehicle> vehicleId){
		return vehicleId.toString().split("_")[2].equals("E2E");
	}

	public static boolean isVehicleCommuter(Id<Vehicle> vehicleId){
		return vehicleId.toString().split("_")[2].equals("E2I");
	}
}
