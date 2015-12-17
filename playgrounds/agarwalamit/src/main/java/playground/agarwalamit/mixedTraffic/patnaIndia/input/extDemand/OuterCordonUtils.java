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
package playground.agarwalamit.mixedTraffic.patnaIndia.input.extDemand;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.vehicles.Vehicle;

/**
 * @author amit
 */

public class OuterCordonUtils {
	
	public static Map<String, List<Integer>> getAreaType2ZoneIds(){//(from Fig.4-9 in PatnaReport)
		Map<String, List<Integer>> areas2zones = new HashMap<>();
		areas2zones.put("Institutional", Arrays.asList(8,9,20));
		areas2zones.put("Industrial", Arrays.asList(61,62));
		areas2zones.put("CBD and Railway Station", Arrays.asList(21));
		areas2zones.put("Educational", Arrays.asList(37,38,39,40,41,42));
		areas2zones.put("Old city and market area", Arrays.asList(54,57));
		return areas2zones;
	}

	public enum OuterCordonLinks{
		/*
		 * P -- Patna
		 * F -- Fatua
		 * PU -- Punpun
		 * M -- Muzafarpur
		 * D -- Danapur
		 * N -- Noera (patna)
		 */
		// x=fatua
		OC1_P2X ("13878-13876"), 
		OC1_X2P ("1387610000-1387810000"),
		// x=fatua
		OC2_P2X ("18237-1823810000-1825010000-1825110000-16398-1851010000-1851310000-1851210000-18505"),
		OC2_X2P ("1850510000-18512-18513-18510-1639810000-18251-18250-18238-1823710000"),
		// x=Punpun
		OC3_P2X ("4908-490910000-4517-4776"),
		OC3_X2P ("477610000-451710000-4909-490810000"),
		// x= Muzafarpur
		OC4_P2X ("1683110000-1683210000-1678010000-1684210000-1678210000-1685010000-1684510000-1684710000-1684410000-16853-1668010000-1727110000-1731610000-1734510000-17268-1735410000-1735510000-1639510000"),
		OC4_X2P ("16395-17355-17354-1726810000-17345-17316-17271-16680-1685310000-16844-16847-16845-16850-16782-16842-16780-16832-16831"),
		// x= danapur
		OC5_P2X ("860310000-8604-852910000-857810000-8568-857410000-856910000-857310000-857110000-854910000-8560-8555-855810000-778310000-7946-794210000-794410000-794310000-7919-7929-789710000-7913-7914-791510000-790310000-791010000-7852-789210000-788510000-7888-7853-786210000-2910000-437-475-476-47310000-474-43910000-446-464-46610000-426"),
		OC5_X2P ("42610000-466-46410000-44610000-439-47410000-473-47610000-47510000-43710000-29-7862-785310000-788810000-7885-7892-785210000-7910-7903-7915-791410000-791310000-7897-792910000-791910000-7943-7944-7942-794610000-7783-8558-855510000-856010000-8549-8571-8573-8569-8574-856810000-8578-8529-860410000-8603"),
		 //x = fatua; fatua to noera (Patna)
		OC6_X2P ("1457-1363-1536-155610000-135810000-169510000-169110000-169610000-170110000-170010000-1349-181410000-181110000-180410000-181710000-177810000-1848"),
		//x = fatua; noera to fatua
		OC6_P2X ("184810000-1778-1817-1804-1811-1814-134910000-1700-1701-1696-1691-1695-1358-1556-153610000-136310000-145710000"),
		// x= danapur
		OC7_P2X ("219410000-2128"),
		OC7_X2P ("212810000-2194");

		private final String linkIdAsString;

		private OuterCordonLinks(final String linkId){
			this.linkIdAsString = linkId;
		}

		public Id<Link> getLinkId(){
			return Id.createLinkId( linkIdAsString );
		}
		
		public static OuterCordonLinks getOuterCordonNumberFromLink(String linkId){
			for(OuterCordonLinks l :OuterCordonLinks.values()){
				if(l.getLinkId().toString().equals(linkId)) return l;
			}
			return null;
		}
	}

	public enum OuterCordonDirectionalFactors{
		/*
		 * E -- external
		 * I -- internal
		 */
//		OC1_P2X_I2E (0.50), OC1_P2X_E2E (0.50),
		OC1_X2P_E2I (0.70), OC1_X2P_E2E (0.30),
//		OC2_P2X_I2E (0.51), OC2_P2X_E2E (0.49),
		OC2_X2P_E2I (0.58), OC2_X2P_E2E (0.42),
//		OC3_P2X_I2E (0.99), OC3_P2X_E2E (0.01),
		OC3_X2P_E2I (0.94), OC3_X2P_E2E (0.20),//(0.06), with 0.06 and 1% sample, ext-ext trips are zero in all time bin and for all modes
//		OC4_P2X_I2E (0.87), OC4_P2X_E2E (0.13),
		OC4_X2P_E2I (0.66), OC4_X2P_E2E (0.34),
//		OC5_P2X_I2E (0.65), OC5_P2X_E2E (0.35),
		OC5_X2P_E2I (0.76), OC5_X2P_E2E (0.24),
		OC6_X2P_E2I (0.86), OC6_X2P_E2E (0.14),
//		OC6_P2X_I2E (0.67), OC6_P2X_E2E (0.33), 
//		OC7_P2X_I2E (0.62), OC7_P2X_E2E (0.38),
		OC7_X2P_E2I (0.95), OC7_X2P_E2E (0.25);//(0.05); with 0.05 and 1% sample, ext-ext trips are zero in all time bin and for all modes

		private final double factor;

		private OuterCordonDirectionalFactors(final double factor){
			this.factor = factor;
		}

		public double getDirectionalFactor(){
			return factor;
		}
	}

	public static Id<Link> getCountStationLinkId(final String countingStationKey){
		return OuterCordonLinks.valueOf(countingStationKey).getLinkId();
	}

	public static double getDirectionalFactorFromOuterCordonKey(final String countingStationKey, final String extIntKey){
		List<String> extIntKeys = Arrays.asList("E2I", "I2E", "E2E") ;

		if(! extIntKeys.contains(extIntKey) ) {
			throw new RuntimeException(extIntKey+" is not known. Possible options are -- "+extIntKeys.toString());
		}
		String key = countingStationKey+"_"+extIntKey;
		double factor = OuterCordonDirectionalFactors.valueOf(key).getDirectionalFactor();
		return factor;
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
	
	public static List<Id<Link>> getExternalToInternalCountStationLinkIds(){
		List<Id<Link>> links = new ArrayList<>();
		for(OuterCordonLinks l : OuterCordonLinks.values()){
			if(l.toString().split("_")[1].equals("X2P")) links.add( l.getLinkId() ) ;
		}
		return links;
	}
	
	public static List<Id<Link>> getInternalToExternalCountStationLinkIds(){
		List<Id<Link>> links = new ArrayList<>();
		for(OuterCordonLinks l : OuterCordonLinks.values()){
			if(l.toString().split("_")[1].equals("P2X")) links.add( l.getLinkId() ) ;
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