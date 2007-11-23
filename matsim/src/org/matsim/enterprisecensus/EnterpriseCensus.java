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
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.matsim.enterprisecensus.algorithms.EnterpriseCensusAlgorithm;
import org.matsim.gbl.Gbl;

public class EnterpriseCensus {

    //////////////////////////////////////////////////////////////////////
    // member variables
    //////////////////////////////////////////////////////////////////////

	// config variables

	public static final String EC_MODULE = "enterprisecensus";
	public static final String EC_NOGACODELENGTH = "NOGACodeLength";
	public static final String EC_PRESENCECODEFILE = "presenceCodeFile";
	public static final String EC_PRESENCECODESEPARATOR = "presenceCodeSeparator";
	public static final String EC_NUMATTRIBUTES = "numOfAttributesInPresenceCodeFile";
	public static final String EC_FIRSTNOGACOLUMN = "columnOfFirstNOGACode";
	public static final String EC_MUNICIPALITYAGGREGATIONFILE = "municipalityAggregationFile";
	public static final String EC_MUNICIPALITYAGGREGATIONSEPARATOR = "municipalityAggregationSeparator";
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

//  private static final int numPresenceCodeFileAttributes = Integer.valueOf(
//			Gbl.getConfig().getParam(EC_MODULE, EC_NUMATTRIBUTES)).intValue();

	private static int numHectareAggregationFileAttributes;

//	private static final int firstNOGAColumn = Integer.valueOf(Gbl.getConfig().getParam(EC_MODULE, EC_FIRSTNOGACOLUMN))
//			.intValue();
//
//	private static final int numNOGACodes = Integer.valueOf(Gbl.getConfig().getParam(EC_MODULE, EC_NUMATTRIBUTES))
//			.intValue()
//			- Integer.valueOf(Gbl.getConfig().getParam(EC_MODULE, EC_FIRSTNOGACOLUMN)).intValue();

//	private HashMap enterpriseCensusMunicipality = new HashMap();
	private TreeMap<String, double[]> enterpriseCensusHectareAggregation = new TreeMap<String, double[]>();
//	private HashMap enterpriseCensusHectarePresenceCodes = new HashMap();
	private ArrayList<String> nogaCodeStrings = new ArrayList<String>();
	private ArrayList<String> hectareAttributeIdentifiers = new ArrayList<String>();

// class MunicipalityData {
//
//		public int numFacilities;
//		public int numJobs;
//
//		public MunicipalityData() {
//			numFacilities = Integer.MIN_VALUE;
//			numJobs = Integer.MIN_VALUE;
//		}
//
//    }
//
//    class HectareData {
//
//		public int B01S2;
//		public int B01VZTS2;
//		public int B01TZTS2;
//		public int B01S3;
//		public int B01VZTS3;
//		public int B01TZTS3;
//		public double B01EQTS2;
//		public double B01EQTS3;
//		public BitSet presenceCodes = new BitSet();
//
//		public HectareData() {
//			B01S2 = Integer.MIN_VALUE;
//			B01VZTS2 = Integer.MIN_VALUE;
//			B01TZTS2 = Integer.MIN_VALUE;
//			B01S3 = Integer.MIN_VALUE;
//			B01VZTS3 = Integer.MIN_VALUE;
//			B01TZTS3 = Integer.MIN_VALUE;
//			B01EQTS2 = Double.MIN_VALUE;
//			B01EQTS3 = Double.MIN_VALUE;
//			presenceCodes.clear();
//		}
//
//    }

    private final ArrayList<EnterpriseCensusAlgorithm> algorithms = new ArrayList<EnterpriseCensusAlgorithm>();

    //////////////////////////////////////////////////////////////////////
    // constructor
    //////////////////////////////////////////////////////////////////////

    public EnterpriseCensus() {
		this.enterpriseCensusHectareAggregation.clear();
//		this.enterpriseCensusHectarePresenceCodes.clear();
//		this.enterpriseCensusMunicipality.clear();
		this.nogaCodeStrings.clear();
		this.hectareAttributeIdentifiers.clear();
    }

    //////////////////////////////////////////////////////////////////////
    // add/set methods
    //////////////////////////////////////////////////////////////////////

//    public final void addPresenceCodeLine(final String hectare_id, final String[] aCSVLine) {
//		BitSet pcl = new BitSet(numNOGACodes);
//
//		for (int ii = 0; ii < numNOGACodes; ii++)
//		{
//			String anEntry = aCSVLine[ii + firstNOGAColumn];
//			if (anEntry.equals("1"))
//			{
//				pcl.set(ii);
//			}
//		}
//
//		HectareData hectareData = new HectareData();
//		if (enterpriseCensusHectarePresenceCodes.containsKey(hectare_id))
//		{
//			hectareData = (HectareData)(enterpriseCensusHectarePresenceCodes.get(hectare_id));
//		}
//
//		hectareData.presenceCodes = pcl;
//		enterpriseCensusHectarePresenceCodes.put(hectare_id, hectareData);
//    }

//    public final void addMunicipalityAggregationLine(final String[] aCSVLine) {
//		int index = -1;
//		String tmp;
//
//		String municipality_id = null;
//		String nogaCodeString = null;
//
//		MunicipalityData municipalityData = new MunicipalityData();
//		//int numFacilities = Integer.MIN_VALUE;
//		//int numJobs = Integer.MIN_VALUE;
//
//		for (int ii=0; ii<=3; ii++) {
//			tmp = aCSVLine[ii];
//			//System.out.print(tmp + "\t");
//			// remove german decimal separators from number
//			index = tmp.indexOf('\'');
//			while (index > -1)
//			{
//				tmp = tmp.substring(0, index) + tmp.substring(index + 1);
//				index = tmp.indexOf('\'');
//			}
//			switch(ii) {
//				case 0:
//					municipality_id = tmp;
//					break;
//				case 1:
//					nogaCodeString = tmp;
//					break;
//				case 2:
//					municipalityData.numFacilities = Integer.parseInt(tmp);
//					break;
//				case 3:
//					municipalityData.numJobs = Integer.parseInt(tmp);
//					break;
//			}
//		}
//		//System.out.println();
//
//		//municipalityData.numFacilities = numFacilities;
//		//municipalityData.numJobs = numJobs;
//
//		enterpriseCensusMunicipality.put(getMNKey(municipality_id, nogaCodeString), municipalityData);
//    }

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

     public final void addHectareAggregationLine(final String line) {

		String separator = Gbl.getConfig().getParam(EC_MODULE, EC_INPUTHECTAREAGGREGATIONSEPARATOR);
		String[] aCSVLine = line.split(separator);

		if (aCSVLine.length != numHectareAggregationFileAttributes) {
			Gbl.errorMsg("Wrong number of attributes.");
		}

		//Coord lowerLeftCorner = new Coord(aCSVLine[1], aCSVLine[2]);
		String RELI = aCSVLine[0];

		double[] hectareData = new double[numHectareAggregationFileAttributes];

		for (int i=0; i < numHectareAggregationFileAttributes; i++) {
			hectareData[i] = Double.parseDouble(aCSVLine[i]);
		}

		this.enterpriseCensusHectareAggregation.put(RELI, hectareData);
     }

//    public final HashMap getEnterpriseCensusHectareAggregation() {
//    	return enterpriseCensusHectareAggregation;
//    }

     public final Set<String> getHectareAggregationKeys() {
    	 return this.enterpriseCensusHectareAggregation.keySet();
     }

    public final TreeMap<String, double[]> getEnterpriseCensusHectareAggregation() {
    	return this.enterpriseCensusHectareAggregation;
    }

    public final String getHectareAggregationLine(String reli) {

		String separator = Gbl.getConfig().getParam(EC_MODULE, EC_OUTPUTHECTAREAGGREGATIONSEPARATOR);
    	String line = "";

    	//double[] hectareData = (double[]) enterpriseCensusHectareAggregation.get(coord);
    	double[] hectareData = this.enterpriseCensusHectareAggregation.get(reli);
    	double storedDoubleValue;
    	int intValue;

		for (int i=0; i < numHectareAggregationFileAttributes; i++) {

			storedDoubleValue = hectareData[i];
			intValue = (int) storedDoubleValue;

			if (Double.compare(storedDoubleValue, intValue) != 0) {
				line = line.concat(Double.toString(storedDoubleValue));
			} else {
				line = line.concat(Integer.toString(intValue));
			}
			line = line.concat(separator);
		}
 		// take away the last separator
 		line = line.substring(0, line.length() - 1);
 		// append Windows line feed, because original file is given in this format
 		line = line.concat("\r\n");

    	return line;

    }

    public final void addAlgorithm(EnterpriseCensusAlgorithm algo) {
		this.algorithms.add(algo);
    }

    public final void addNOGACode(final String codeString) {
		this.nogaCodeStrings.add(codeString);
    }
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

//    public final int getNumFacilities(final String municipality_id, final String nogaCodeString) {
//		String strKey = this.getMNKey(municipality_id, nogaCodeString);
//		int numFacilities = -1;
//		if (enterpriseCensusMunicipality.containsKey(strKey)) {
//			numFacilities = ((MunicipalityData)enterpriseCensusMunicipality.get(strKey)).numFacilities;
//		}
//		return numFacilities;
//    }
//
//    public final int getNumJobs(final String municipality_id, final String nogaCodeString) {
//		String strKey = this.getMNKey(municipality_id, nogaCodeString);
//		int numJobs = -1;
//		if (enterpriseCensusMunicipality.containsKey(strKey)) {
//			numJobs = ((MunicipalityData)enterpriseCensusMunicipality.get(strKey)).numJobs;
//		}
//		return numJobs;
//    }
//
//    public final TreeSet getFacilityTypesFromMunicipalityAggregation(final String municipality_id) {
//		TreeSet ts = new TreeSet();
//		ts.clear();
//		String mnKey;
//
//		Iterator it = enterpriseCensusMunicipality.keySet().iterator();
//		while (it.hasNext()) {
//			mnKey = (String)it.next();
//			if (municipality_id.compareTo(mnKey.substring(5)) == 0)
//			{
//				ts.add(mnKey.substring(0, 5));
//			}
//		}
//
//		return ts;
//    }
//
    public final int getHectareAttributeFloor(final String reli, final String attribute_id) {

    	int index = this.getHectareAttributeIdentifierIndex(attribute_id);
		return (int) ((this.enterpriseCensusHectareAggregation.get(reli)))[index];

    }

    public final int getHectareAttributeRound(final String reli, final String attribute_id) {

    	int index = this.getHectareAttributeIdentifierIndex(attribute_id);
		return (int) Math.round(((this.enterpriseCensusHectareAggregation.get(reli)))[index]);

    }

    public final double getHectareAttributeDouble(final String reli, final String attribute_id) {

    	int index = this.getHectareAttributeIdentifierIndex(attribute_id);
		return ((this.enterpriseCensusHectareAggregation.get(reli)))[index];

    }

//    public final String[] getPresenceCodeLine(final String hectare_id) throws Exception {
//
//		//System.out.println("getPresenceCodeLine for hektar " + hektar.getX100() + " " + hektar.getY100());
//		HectareData hectareData = (HectareData)(enterpriseCensusHectare.get(hectare_id));
//		BitSet pcl = hectareData.presenceCodes;
//		String[] aCSVParsedPresenceCodeLine = new String[numNOGACodes];
//		if (pcl == null)
//		{
//			return null;
//		}
//
//		for (int ii = 0; ii < numNOGACodes; ii++)
//		{
//			if(pcl.get(ii))
//			{
//				aCSVParsedPresenceCodeLine[ii] = "1";
//			}
//			else
//			{
//				aCSVParsedPresenceCodeLine[ii] = "0";
//			}
//		}
//
//		return aCSVParsedPresenceCodeLine;
//    }

    public final String getMNKey(final String municipality_id, final String nogaCodeString) {
		String strKey = nogaCodeString.concat(municipality_id);
		return strKey;
    }

    public final String getNOGACode(final int index) {
		return this.nogaCodeStrings.get(index);
    }

    public final int getNOGAIndex(final String nogaCodeString) {
		return this.nogaCodeStrings.indexOf(nogaCodeString);
    }

    public final ArrayList<String> getNOGACodes() {
		return this.nogaCodeStrings;
    }

    public final int getHectareAttributeIdentifierIndex(final String identifier) {
		return this.hectareAttributeIdentifiers.indexOf(identifier);
    }

//    public final boolean containsHectare(final String hectare_id) {
//		if (enterpriseCensusHectare.containsKey(hectare_id))
//		{
//			return true;
//		}
//		else
//		{
//			return false;
//		}
//    }
//
//    public final TreeSet getNOGACodesInHectare(final String hectare_id) {
//		TreeSet ts = new TreeSet();
//		ts.clear();
//
//		if (this.containsHectare(hectare_id)) {
//			HectareData hectareData = (HectareData)(enterpriseCensusHectare.get(hectare_id));
//			BitSet pcl = hectareData.presenceCodes;
//
//			for (int ii=0; ii < numNOGACodes; ii++)
//			{
//				if (pcl.get(ii))
//				{
//					ts.add(getNOGACode(ii));
//				}
//			}
//		}
//
//		return ts;
//    }

    public String trim(final String string, final char mark) {

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

 }
