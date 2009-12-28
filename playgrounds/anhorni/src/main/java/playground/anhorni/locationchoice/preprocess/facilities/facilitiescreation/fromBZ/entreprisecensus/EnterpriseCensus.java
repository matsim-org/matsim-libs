/* *********************************************************************** *
 * project: org.matsim.*
 * EnterpriseCensus.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.anhorni.locationchoice.preprocess.facilities.facilitiescreation.fromBZ.entreprisecensus;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.matsim.core.gbl.Gbl;


public class EnterpriseCensus {

	private static Logger log = Logger.getLogger(EnterpriseCensus.class);

	// config variables
	public static final String EC_MODULE = "enterprisecensus";
	public static final String EC_NOGACODELENGTH = "NOGACodeLength";
	public static final String EC_PRESENCECODEFILE = "presenceCodeFile";
	public static final String EC_PRESENCECODESEPARATOR = "presenceCodeSeparator";
	public static final String EC_INPUTHECTAREAGGREGATIONFILE = "inputHectareAggregationFile";
	public static final String EC_INPUTHECTAREAGGREGATIONSEPARATOR = "inputHectareAggregationSeparator";

	// some things from the dataset
	public static final String EC01_PREFIX = "B01";

	public static final int SHOP_NOGA_SECTION = 52;
	public static final int HOSPITALITY_NOGA_SECTION = 55;
	public static final int CULTURE_NOGA_SECTION = 92;
	
	public enum ProductionSector {
		
		SECTOR2("B011001", "B014504", "B01EQTS2"),
		SECTOR3("B015001", "B019304", "B01EQTS3");

		private final String first;
		private final String last;
		/**
		 * Item containing the full time equivalent
		 */
		private final String fteItem;
		
		private ProductionSector(String first, String last, String item) {
			this.first = first;
			this.last = last;
			this.fteItem = item;
		}

		public String getFirst() {
			return first;
		}

		public String getLast() {
			return last;
		}

		public String getFteItem() {
			return fteItem;
		}
}
	
	private TreeMap<Integer, TreeMap<String, Double>> hectareAggregation = new TreeMap<Integer, TreeMap<String, Double>>();

	/**
	 * Maps a RELI coordinate to one or more NOGA types for all presence codes == "1"
	 * Examples: 
	 * 48661119 -> {B011310A}
	 * 48661120 -> {B011310B, B012300A}
	 */
	private TreeMap<Integer, HashSet<String>> presenceCodes = new TreeMap<Integer, HashSet<String>>();
	private ArrayList<String> hectareAggregationItems = new ArrayList<String>();
	private ArrayList<String> presenceCodeNOGATypes = new ArrayList<String>();


	public EnterpriseCensus() {
		this.hectareAggregation.clear();
		this.presenceCodes.clear();
		this.hectareAggregationItems.clear();
	}

	public final String getHectareAggregationNOGAType(int pos) {
		return this.hectareAggregationItems.get(pos);
	}
	
	public final TreeSet<String> getHectareAttributeIdentifiersBySector(final ProductionSector sector) {

		TreeSet<String> attributeIds = new TreeSet<String>();
		int first = 0, last = 0;
		
		if (sector.equals(ProductionSector.SECTOR2)) {
			first = this.getHectareAttributeIdentifierIndex(ProductionSector.SECTOR2.getFirst());
			last = this.getHectareAttributeIdentifierIndex(ProductionSector.SECTOR2.getLast());
		} else if (sector.equals(ProductionSector.SECTOR3)) {
			first = this.getHectareAttributeIdentifierIndex(ProductionSector.SECTOR3.getFirst());
			last = this.getHectareAttributeIdentifierIndex(ProductionSector.SECTOR3.getLast());
		} else {
			Gbl.errorMsg("Invalid economy sector id.");
		}
		for (int i = first; i <= last; i++) {
			attributeIds.add(this.hectareAggregationItems.get(i));
		}
		return attributeIds;
	}

	public void addhectareAggregationNOGAType(String type) {
		this.hectareAggregationItems.add(type);
	}
	
	public final void addHectareAggregationInformation(String reli, String noga, double value) {
		
		int reliInt = Integer.parseInt(reli);
		TreeMap<String, Double> entries = null;
		if (this.hectareAggregation.containsKey(reliInt)) {
			entries = this.hectareAggregation.get(reliInt);
			entries.put(noga, new Double(value));
		} else {
			entries = new TreeMap<String, Double>();
			entries.put(noga, new Double(value));
			this.hectareAggregation.put(reliInt, entries);
		}
	}
	
	public final Set<Integer> getHectareAggregationKeys() {
		return this.hectareAggregation.keySet();
	}

	public final void addPresenceCodeNOGAType(String type) {
		this.presenceCodeNOGATypes.add(type);
	}
	
	public final String getPresenceCodeNOGAType(int pos) {
		return this.presenceCodeNOGATypes.get(pos);
	}
	
	public final void addPresenceCode(String reli, String noga) {

		int reliInt = Integer.parseInt(reli);
		HashSet<String> nogasInReli = null;
		if (this.presenceCodes.containsKey(reliInt)) {
			this.presenceCodes.get(reliInt).add(noga);
		} else {
			nogasInReli = new HashSet<String>();
			nogasInReli.add(noga);
			this.presenceCodes.put(reliInt, nogasInReli);
		}
	}

	//////////////////////////////////////////////////////////////////////
	// get methods
	//////////////////////////////////////////////////////////////////////

	public final double getHectareAggregationInformation(final Integer reli, final String noga) {
		
		double value = 0.0;
		
		if (this.hectareAggregation.containsKey(reli)) {
			if (this.hectareAggregation.get(reli).containsKey(noga)) {
				value = this.hectareAggregation.get(reli).get(noga).doubleValue();
			}
		}
		return value;
	}

	public final int getHectareAttributeIdentifierIndex(final String identifier) {
		return this.hectareAggregationItems.indexOf(identifier);
	}

	public TreeMap<Integer, HashSet<String>> getPresenceCodes() {
		return presenceCodes;
	}

	public TreeMap<Integer, TreeMap<String, Double>> getHectareAggregation() {
		return hectareAggregation;
	}
	
	public final HashSet<String> getPresenceCodeItemsPerNOGASection(final Integer reli, final String noga) {
		
		HashSet<String> presenceCodeItems = null;
		
		String nogaSection = noga.substring(0, 5);

		if (this.presenceCodes.containsKey(reli)) {
			if (presenceCodeItems == null) {
				presenceCodeItems = new HashSet<String>();
			}
			for (String presenceCodeItem : this.presenceCodes.get(reli)) {
				if (Pattern.matches(nogaSection + ".*", presenceCodeItem)) {
					presenceCodeItems.add(presenceCodeItem);
				}
			}
		}
		return presenceCodeItems;
	}
	
	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////

	public void printHectareAggregationReport() {	
		int numEntries = 0;
		for (Integer reli : this.hectareAggregation.keySet()) {
			for (String noga : this.hectareAggregation.get(reli).keySet()) {
//				System.out.println(reli + ":" + noga + " -> " + this.hectareAggregation.get(reli).get(noga).toString());
				numEntries++;
			}
		}		
		log.info("Number of hectare aggregation entries: " + numEntries);
	}
	
	public void printPresenceCodesReport() {
		int numPresenceCodes = 0;
		for (Integer reli : this.presenceCodes.keySet()) {
			for (String noga : this.presenceCodes.get(reli)) {
//				System.out.println(reli + " -> " + noga);
				numPresenceCodes++;
			}
		}
		log.info("Number of presence code entries: " + numPresenceCodes);
	}
}
