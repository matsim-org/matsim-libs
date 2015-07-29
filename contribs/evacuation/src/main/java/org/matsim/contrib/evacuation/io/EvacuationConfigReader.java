/* *********************************************************************** *
 * project: org.matsim.*
 * EvacuationConfigReader.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package org.matsim.contrib.evacuation.io;

import java.util.Stack;

import org.matsim.contrib.evacuation.model.config.EvacuationConfigModule;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

public class EvacuationConfigReader extends MatsimXmlParser {

	private final static String EVACUATION_CONFIG = "grips_config";
	private final static String NETWORK_FILE = "networkFile";
	private final static String MAIN_TRAFFIC_TYPE = "mainTrafficType";
	private final static String EVACUATION_AREA_FILE = "evacuationAreaFile";
	private final static String POPULATION_FILE = "populationFile";
	private final static String OUTPUT_DIR = "outputDir";
	private final static String SAMPLE_SIZE = "sampleSize";
	private final static String DEPARTURE_TIME_DISTRIBUTION = "departureTimeDistribution";
	private final static String DISTRIBUTION = "distribution";
	private final static String SIGMA = "sigma";
	private final static String MU = "mu";
	private final static String EARLIEST = "earliest";
	private final static String LATEST ="latest";
	private final static String INPUT_FILE = "inputFile";
	private final EvacuationConfigModule gcm;
	private DepartureTimeDistribution dtd;

	private String currentEntity = null;


	
	public EvacuationConfigReader(EvacuationConfigModule gcm) {
		this.gcm = gcm;
	}


	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		if (EVACUATION_CONFIG.equals(name)) {
			startEvacuationConfig(atts);
		} else if (NETWORK_FILE.equals(name)){
			startNetworkFile(atts);
		} else if (MAIN_TRAFFIC_TYPE.equals(name)){
			startMainTrafficType(atts);
		} else if (EVACUATION_AREA_FILE.equals(name)){
			startEvacuationAreaFile(atts);
		} else if (POPULATION_FILE.equals(name)) {
			startPopulationFile(atts);
		} else if (OUTPUT_DIR.equals(name)){
			startOutputDir(atts);
		} else if (SAMPLE_SIZE.equals(name)){
			startSampleSize(atts);
		} else if (DEPARTURE_TIME_DISTRIBUTION.equals(name)){
			startDepartureTimeDistribution(atts);
		} else if (DISTRIBUTION.equals(name)){
			startDistribution(atts);
		} else if (SIGMA.equals(name)){
			startSigma(atts);
		} else if (MU.equals(name)){
			startMu(atts);
		} else if (EARLIEST.equals(name)){
			startEarliest(atts);
		} else if (LATEST.equals(name)){
			startLatest(atts);
		} else if (INPUT_FILE.equals(name)){
			startInputFile(atts);
		}else {
			System.out.println(name);
		}

	}

	private void startInputFile(Attributes atts) {
		// Nothing to be done here.
	}




	private void startLatest(Attributes atts) {

	}




	private void startEarliest(Attributes atts) {
		// TODO Auto-generated method stub

	}




	private void startMu(Attributes atts) {
		// TODO Auto-generated method stub

	}




	private void startSigma(Attributes atts) {

	}



	private void startDistribution(Attributes atts) {


	}



	private void startDepartureTimeDistribution(Attributes atts) {
		this.dtd = new DepartureTimeDistribution();
		this.currentEntity = DEPARTURE_TIME_DISTRIBUTION;
	}



	private void startSampleSize(Attributes atts) {
		this.currentEntity = SAMPLE_SIZE;

	}



	private void startOutputDir(Attributes atts) {
		this.currentEntity = OUTPUT_DIR;

	}



	private void startPopulationFile(Attributes atts) {
		this.currentEntity = POPULATION_FILE;		
	}



	private void startEvacuationAreaFile(Attributes atts) {
		this.currentEntity = EVACUATION_AREA_FILE;

	}



	private void startMainTrafficType(Attributes atts) {
		this.currentEntity = MAIN_TRAFFIC_TYPE;

	}



	private void startNetworkFile(Attributes atts) {
		this.currentEntity = NETWORK_FILE;

	}



	private void startEvacuationConfig(Attributes atts) {
		// TODO Auto-generated method stub

	}



	@Override
	public void endTag(String name, String content, Stack<String> context) {
		if (INPUT_FILE.equals(name)){
			if (NETWORK_FILE.equals(this.currentEntity)){
				this.gcm.setNetworkFileName(content);
			} else if (EVACUATION_AREA_FILE.equals(this.currentEntity)){
				this.gcm.setEvacuationAreaFileName(content);
			} else if (POPULATION_FILE.equals(this.currentEntity)) {
				this.gcm.setPopulationFileName(content);
			} else if (OUTPUT_DIR.equals(this.currentEntity)){
				this.gcm.setOutputDir(content);
			} else {
				throw new RuntimeException("unhandled entity:" + this.currentEntity);
			}
		} else if (MAIN_TRAFFIC_TYPE.equals(name)){
					this.gcm.setMainTrafficType(content);
		} else if (SAMPLE_SIZE.equals(name)){
			this.gcm.setSampleSize(content);
		} else if (DISTRIBUTION.equals(name)){
			this.dtd.setDistribution(content);
		} else if (SIGMA.equals(name)){
			this.dtd.setSigma(Double.parseDouble(content));
		} else if (MU.equals(name)){
			this.dtd.setMu(Double.parseDouble(content));
		} else if (EARLIEST.equals(name)){
			this.dtd.setEarliest(Double.parseDouble(content));
		} else if (LATEST.equals(name)){
			this.dtd.setLatest(Double.parseDouble(content));
		}else if (DEPARTURE_TIME_DISTRIBUTION.equals(name)) {
			this.gcm.setDepartureTimeDistribution(this.dtd);
		}

	}

}
