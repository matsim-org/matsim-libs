/* *********************************************************************** *
 * project: org.matsim.*
 * PanicModel.java
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

package playground.christoph.evacuation.mobsim.decisionmodel;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.io.IOUtils;

import playground.christoph.evacuation.mobsim.decisiondata.DecisionDataProvider;
import playground.christoph.evacuation.mobsim.decisiondata.PersonDecisionData;

/**
 * (So far) decides randomly whether an agents acts rational or in panic. 
 * 
 * @author cdobler
 */
public class PanicModel implements PersonDecisionModel {
	
	private static final Logger log = Logger.getLogger(PanicModel.class);
	
	public static final String panicModelFile = "panicModel.txt.gz";
	
	private final DecisionDataProvider decisionDataProvider;
	private final double share;
	private final Random random;
	private int inPanic = 0;
	private int notInPanic = 0;
	
	public PanicModel(DecisionDataProvider decisionDataProvider, double share) {
		this.decisionDataProvider = decisionDataProvider;
		this.share = share;
		
		this.random = MatsimRandom.getLocalInstance();
	}

	@Override
	public void runModel(Person person) {
		if (this.random.nextDouble() <= share) {
			decisionDataProvider.getPersonDecisionData(person.getId()).setInPanic(true);
			inPanic++;
		} else {
			decisionDataProvider.getPersonDecisionData(person.getId()).setInPanic(false);
			notInPanic++;
		}
	}

	@Override
	public void runModel(Population population) {
		for (Person person : population.getPersons().values()) runModel(person);
	}

	@Override
	public void printStatistics() {
		log.info("Agents in panic:\t" + inPanic);
		log.info("Agents not in panic:\t" + notInPanic);
	}

	@Override
	public void writeDecisionsToFile(String file) {
		
		try {
			BufferedWriter modelWriter = IOUtils.getBufferedWriter(file);
			
			writeHeader(modelWriter);
			writeRows(modelWriter);
			
			modelWriter.flush();
			modelWriter.close();			
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void writeHeader(BufferedWriter modelWriter) throws IOException {
		modelWriter.write("personId");
		modelWriter.write(delimiter);
		modelWriter.write("in panic");
		modelWriter.write(newLine);
	}
	
	private void writeRows(BufferedWriter modelWriter) throws IOException {
		for (PersonDecisionData pdd : this.decisionDataProvider.getPersonDecisionData()) {
			modelWriter.write(pdd.getPersonId().toString());
			modelWriter.write(delimiter);
			if (pdd.isInPanic()) modelWriter.write("true");
			else modelWriter.write("false");
			modelWriter.write(newLine);
		}
	}
	
	@Override
	public void readDecisionsFromFile(String file) {
		try {
			BufferedReader modelReader = IOUtils.getBufferedReader(file);
			
			readHeader(modelReader);
			readRows(modelReader);
			
			modelReader.close();
			
		} catch (IOException e) {
			throw new RuntimeException(e);
		}	
	}
	
	private void readHeader(BufferedReader modelReader) throws IOException {
		// just skip header
		modelReader.readLine();
	}
	
	private void readRows(BufferedReader modelReader) throws IOException {
		
		String line = null;
		while ((line = modelReader.readLine()) != null) {
			String[] columns = line.split(delimiter);
			Id<Person> personId = Id.create(columns[0], Person.class);
			if (columns[1].equals("true")) {
				this.decisionDataProvider.getPersonDecisionData(personId).setInPanic(true);
				this.inPanic++;
			}
			else if (columns[1].equals("false")) {
				this.decisionDataProvider.getPersonDecisionData(personId).setInPanic(false);
				this.notInPanic++;
			}
			else throw new RuntimeException("Could not parse person's panic level: " + line);
		}
	}
}
