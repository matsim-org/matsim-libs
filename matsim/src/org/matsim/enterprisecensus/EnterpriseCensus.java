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

package org.matsim.enterprisecensus;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.enterprisecensus.algorithms.EnterpriseCensusAlgorithm;
import org.matsim.facilities.algorithms.FacilitiesAllActivitiesFTE;
import org.matsim.gbl.Gbl;


public class EnterpriseCensus {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private static Logger log = Logger.getLogger(EnterpriseCensus.class);

	// config variables

	public static final String EC_MODULE = "enterprisecensus";
	public static final String EC_NOGACODELENGTH = "NOGACodeLength";
	public static final String EC_PRESENCECODEFILE = "presenceCodeFile";
	public static final String EC_PRESENCECODESEPARATOR = "presenceCodeSeparator";
	public static final String EC_INPUTHECTAREAGGREGATIONFILE = "inputHectareAggregationFile";
	public static final String EC_INPUTHECTAREAGGREGATIONSEPARATOR = "inputHectareAggregationSeparator";
	public static final String EC_OUTPUTHECTAREAGGREGATIONFILE = "outputHectareAggregationFile";
	public static final String EC_OUTPUTHECTAREAGGREGATIONSEPARATOR = "outputHectareAggregationSeparator";
	public static final String EC_OUTPUTDIRFACILITIES = "outputDirFacilities";
	public static final String EC_OUTPUTDIRJOBS = "outputDirJobs";
	public static final String EC_OUTPUTSUBDIRCOLUMN = "outputSubdirColumn";
	public static final String EC_OUTPUTSUBDIRROW = "outputSubdirRow";
	public static final String EC_OUTPUTSUBDIRMATRIX = "outputSubdirMatrix";
	public static final String EC_OUTPUTDIRPRESENTHECTARES = "outputDirPresentHectares";
	public static final String EC_OUTPUTDIRPRESENTFACILITYTYPES = "outputDirPresentFacilityTypes";
	public static final String EC_FIRSTELEMENTINDEX = "firstElementIndex";

	private static int numHectareAggregationFileAttributes;

	private TreeMap<Double, TreeMap<String, Double>> hectareAggregation = new TreeMap<Double, TreeMap<String, Double>>();

	/**
	 * Maps a RELI coordinate to one or more NOGA types for all presence codes == "1"
	 * Examples: 
	 * 48661119 -> {B011310A}
	 * 48661120 -> {B011310B, B012300A}
	 */
	private TreeMap<Double, HashSet<String>> presenceCodes = new TreeMap<Double, HashSet<String>>();

	
	
	private ArrayList<String> hectareAttributeIdentifiers = new ArrayList<String>();

	private final ArrayList<EnterpriseCensusAlgorithm> algorithms = new ArrayList<EnterpriseCensusAlgorithm>();

	//////////////////////////////////////////////////////////////////////
	// constructor
	//////////////////////////////////////////////////////////////////////

	public EnterpriseCensus() {
		this.hectareAggregation.clear();
		this.presenceCodes.clear();
//		this.enterpriseCensusMunicipality.clear();
		this.hectareAttributeIdentifiers.clear();
	}

	//////////////////////////////////////////////////////////////////////
	// add/set methods
	//////////////////////////////////////////////////////////////////////

	public final ArrayList<String> getHectareAttributeIdentifiers() {
		return this.hectareAttributeIdentifiers;
	}

	public final TreeSet<String> getHectareAttributeIdentifiersBySector(final int sector) {

		TreeSet<String> attributeIds = new TreeSet<String>();
		int first = 0, last = 0;

		if (sector == 2) {
			first = this.getHectareAttributeIdentifierIndex("B011001");
			last = this.getHectareAttributeIdentifierIndex("B014504");
		} else if (sector == 3) {
			first = this.getHectareAttributeIdentifierIndex("B015001");
			last = this.getHectareAttributeIdentifierIndex("B019304");
		} else {
			Gbl.errorMsg("Invalid economy sector id.");
		}

		for (int i = first; i <= last; i++) {
			attributeIds.add(this.hectareAttributeIdentifiers.get(i));
		}

		return attributeIds;
	}

	public final void processHectareAggregationHeaderLine(final String headerLine) {

		String separator = Gbl.getConfig().getParam(EC_MODULE, EC_INPUTHECTAREAGGREGATIONSEPARATOR);
		String[] aCSVLine = headerLine.split(separator);
		String str;

		numHectareAggregationFileAttributes = aCSVLine.length;
		for (int ii=0; ii < aCSVLine.length; ii++) {
			str = aCSVLine[ii];
			str = this.trim(str, '\"');
			this.hectareAttributeIdentifiers.add(str);

		}

	}

	public final String getHectareAggregationHeaderLine() {

		String separator = Gbl.getConfig().getParam(EC_MODULE, EC_OUTPUTHECTAREAGGREGATIONSEPARATOR);
		String headerLine = "";

		Iterator<String> it = this.hectareAttributeIdentifiers.iterator();
		while (it.hasNext()) {
			headerLine = headerLine.concat(this.quote(it.next(), '\"'));
			headerLine = headerLine.concat(separator);
		}
		// take away the last separator
		headerLine = headerLine.substring(0, headerLine.length() - 1);
		// append a newline
		headerLine = headerLine.concat("\r\n");

		return headerLine;
	}

	public final void addHectareAggregationInformation(String reli, String noga, double value) {
		
		double reliDouble = Double.parseDouble(reli);
		
		TreeMap<String, Double> entries = null;
		
		if (this.hectareAggregation.containsKey(reliDouble)) {
			entries = this.hectareAggregation.get(reliDouble);
			entries.put(noga, new Double(value));
		} else {
			entries = new TreeMap<String, Double>();
			entries.put(noga, new Double(value));
			this.hectareAggregation.put(reliDouble, entries);
		}
		
	}
	
//	public final void addHectareAggregationLine(final String line) {
//
//		String separator = Gbl.getConfig().getParam(EC_MODULE, EC_INPUTHECTAREAGGREGATIONSEPARATOR);
//		String[] aCSVLine = line.split(separator);
//
//		if (aCSVLine.length != numHectareAggregationFileAttributes) {
//			Gbl.errorMsg("Wrong number of attributes.");
//		}
//
//		//Coord lowerLeftCorner = new Coord(aCSVLine[1], aCSVLine[2]);
//		String RELI = aCSVLine[0];
//
//		double[] hectareData = new double[numHectareAggregationFileAttributes];
//
//		for (int i=0; i < numHectareAggregationFileAttributes; i++) {
//			hectareData[i] = Double.parseDouble(aCSVLine[i]);
//		}
//
//		this.hectareAggregation.put(RELI, hectareData);
//	}

//	public final HashMap getEnterpriseCensusHectareAggregation() {
//	return enterpriseCensusHectareAggregation;
//	}

	public final Set<Double> getHectareAggregationKeys() {
		return this.hectareAggregation.keySet();
	}

//	public final TreeMap<String, double[]> getEnterpriseCensusHectareAggregation() {
//		return this.hectareAggregation;
//	}

//	public final String getHectareAggregationLine(String reli) {
//
//		String separator = Gbl.getConfig().getParam(EC_MODULE, EC_OUTPUTHECTAREAGGREGATIONSEPARATOR);
//		String line = "";
//
//		//double[] hectareData = (double[]) enterpriseCensusHectareAggregation.get(coord);
//		double[] hectareData = this.hectareAggregation.get(reli);
//		double storedDoubleValue;
//		int intValue;
//
//		for (int i=0; i < numHectareAggregationFileAttributes; i++) {
//
//			storedDoubleValue = hectareData[i];
//			intValue = (int) storedDoubleValue;
//
//			if (Double.compare(storedDoubleValue, intValue) != 0) {
//				line = line.concat(Double.toString(storedDoubleValue));
//			} else {
//				line = line.concat(Integer.toString(intValue));
//			}
//			line = line.concat(separator);
//		}
//		// take away the last separator
//		line = line.substring(0, line.length() - 1);
//		// append Windows line feed, because original file is given in this format
//		line = line.concat("\r\n");
//
//		return line;
//
//	}

	public final void addPresenceCode(String reli, String noga) {

		double reliDouble = Double.parseDouble(reli);
		
		HashSet<String> nogasInReli = null;

		if (this.presenceCodes.containsKey(reliDouble)) {
			this.presenceCodes.get(reliDouble).add(noga);
		} else {
			nogasInReli = new HashSet<String>();
			nogasInReli.add(noga);
			this.presenceCodes.put(reliDouble, nogasInReli);
		}

	}

	public final void addAlgorithm(EnterpriseCensusAlgorithm algo) {
		this.algorithms.add(algo);
	}

//	public final void addNOGACode(final String codeString) {
//	this.nogaCodeStrings.add(codeString);
//	}
	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	public final void runAlgorithms() throws Exception {
		for (int i = 0; i < this.algorithms.size(); i++) {
			EnterpriseCensusAlgorithm algo = this.algorithms.get(i);
			algo.run(this);
		}
	}

	//////////////////////////////////////////////////////////////////////
	// get methods
	//////////////////////////////////////////////////////////////////////

//	public final int getNumFacilities(final String municipality_id, final String nogaCodeString) {
//	String strKey = this.getMNKey(municipality_id, nogaCodeString);
//	int numFacilities = -1;
//	if (enterpriseCensusMunicipality.containsKey(strKey)) {
//	numFacilities = ((MunicipalityData)enterpriseCensusMunicipality.get(strKey)).numFacilities;
//	}
//	return numFacilities;
//	}

//	public final int getNumJobs(final String municipality_id, final String nogaCodeString) {
//	String strKey = this.getMNKey(municipality_id, nogaCodeString);
//	int numJobs = -1;
//	if (enterpriseCensusMunicipality.containsKey(strKey)) {
//	numJobs = ((MunicipalityData)enterpriseCensusMunicipality.get(strKey)).numJobs;
//	}
//	return numJobs;
//	}


	public final int getHectareAggregationInformationFloor(final Double reli, final String noga) {
		return (int) (this.hectareAggregation.get(reli).get(noga).doubleValue());
	}

	public final int getHectareAttributeRound(final Double reli, final String noga) {
		return (int) Math.round(this.hectareAggregation.get(reli).get(noga).doubleValue());
	}

	public final double getHectareAttributeDouble(final Double reli, final String noga) {
		return this.hectareAggregation.get(reli).get(noga).doubleValue();
	}

	public final String getMNKey(final String municipality_id, final String nogaCodeString) {
		String strKey = nogaCodeString.concat(municipality_id);
		return strKey;
	}

	public final int getHectareAttributeIdentifierIndex(final String identifier) {
		return this.hectareAttributeIdentifiers.indexOf(identifier);
	}

	public static String trim(final String string, final char mark) {

		String str = string;
		int index = str.indexOf(mark);

		while (index > -1)
		{
			str = str.substring(0, index) + str.substring(index + 1);
			index = str.indexOf(mark);
		}

		return str;
	}

	//private String quote(final String string, final String mark) {
	private String quote(final String string, final char mark) {

		String markString = new String(new char[]{mark});
		return markString.concat(string).concat(markString);

	}

	public TreeMap<Double, HashSet<String>> getPresenceCodes() {
		return presenceCodes;
	}

	public TreeMap<Double, TreeMap<String, Double>> getHectareAggregation() {
		return hectareAggregation;
	}
	
	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////

	public void printHectareAggregationReport() {
		
		int numEntries = 0;
		for (Double reli : this.hectareAggregation.keySet()) {
			for (String noga : this.hectareAggregation.get(reli).keySet()) {
//				System.out.println(reli + ":" + noga + " -> " + this.hectareAggregation.get(reli).get(noga).toString());
				numEntries++;
			}
		}		

		log.info("Number of hectare aggregation entries: " + numEntries);

	}
	
	public void printPresenceCodesReport() {
		
		int numPresenceCodes = 0;
		
		for (Double reli : this.presenceCodes.keySet()) {
			for (String noga : this.presenceCodes.get(reli)) {
//				System.out.println(reli + " -> " + noga);
				numPresenceCodes++;
			}
		}
		
		log.info("Number of presence code entries: " + numPresenceCodes);
		
	}

}
