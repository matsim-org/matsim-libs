/* *********************************************************************** *
 * project: org.matsim.*
 * HouseholdSampler.java
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

package playground.southafrica.population;

import java.io.File;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.households.Households;
import org.matsim.households.HouseholdsImpl;
import org.matsim.households.HouseholdsReaderV10;
import org.matsim.households.HouseholdsWriterV10;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

import playground.southafrica.population.algorithms.HouseholdSamplerAlgorithm;
import playground.southafrica.utilities.Header;

public class HouseholdSampler {
	private final static Logger LOG = Logger.getLogger(HouseholdSampler.class);
	
	private final Scenario sc;
	private HouseholdSamplerAlgorithm algorithm;
	
	private Households sampledHouseholds;
	private Population sampledPopulation;
	private ObjectAttributes sampledHouseholdAttributes;
	private ObjectAttributes sampledPopulationAttributes;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(HouseholdSampler.class.toString(), args);
		String inputFolder = args[0];
		String outputFolder = args[1];
		double fraction = Double.parseDouble(args[2]);
		String networkFile = args[3];
		
		HouseholdSampler hs = new HouseholdSampler();
		hs.sampleHouseholds(inputFolder, fraction);
		hs.writeSample(outputFolder, networkFile);
		
		Header.printFooter();
	}
	
	
	public HouseholdSampler() {	
		this.sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	}
	
	
	/**
	 * Samples a fraction of the households, using {@link HouseholdSamplerAlgorithm}
	 * so that the household members are kept intact with the sampled households.
	 * @param householdFile
	 * @param populationFile
	 * @param fraction
	 */
	public void sampleHouseholds(String inputFolder, final double fraction){
		LOG.info("Sampling " + String.format("%.1f%%", fraction*100) + " of households.");
		LOG.info("Checking for the available files...");

		File hhf = new File(inputFolder + "households.xml.gz");
		File hhaf = new File(inputFolder + "householdAttributes.xml.gz");
		File pf = new File(inputFolder + "population.xml.gz");
		File paf = new File(inputFolder + "populationAttributes.xml.gz");
		LOG.info("  households: " + hhf.exists());
		LOG.info("  population: " + pf.exists());
		LOG.info("  household attributes: " + hhaf.exists());
		LOG.info("  population attributes: " + paf.exists());
		
		/* Sets up the sampling algorithm. */
		this.algorithm = new HouseholdSamplerAlgorithm(fraction);
		HouseholdsImpl hhs = new HouseholdsImpl();
		hhs.addAlgorithm(algorithm);

		/* Run the household algorithm. */
		HouseholdsReaderV10 hr = new HouseholdsReaderV10(hhs);
		hr.readFile(hhf.getAbsolutePath());		
		LOG.info("  original number of households: " + hhs.getHouseholds().size());
		hhs.runAlgorithms();
		
		/* Check if there are custom attributes with the households */
		ObjectAttributes hha = null;
		if(hhaf.exists()){
			hha = new ObjectAttributes();
			ObjectAttributesXmlReader oar = new ObjectAttributesXmlReader(hha);
			oar.parse(hhaf.getAbsolutePath());
		}
		
		/* Remove those households that have not been sampled. */
		LOG.info("  removing " + this.algorithm.getSampledIds().size() + " households...");
		Counter counter = new Counter("    removed # ");
		for(Id id : this.algorithm.getSampledIds()){
			hhs.getHouseholds().remove(id);
			if(hha != null){
				hha.removeAllAttributes(id.toString());
			}
			counter.incCounter();
		}
		counter.printCounter();
		LOG.info("  remaining number of households: " + hhs.getHouseholds().size());
		
		/* Check if there are custom attributes with the population */
		ObjectAttributes pa = null;
		if(paf.exists()){
			pa = new ObjectAttributes();
			ObjectAttributesXmlReader oar = new ObjectAttributesXmlReader(pa);
			oar.parse(paf.getAbsolutePath());
		}
		
		/* Read the population */
		MatsimPopulationReader pr = new MatsimPopulationReader(this.sc);
		pr.readFile(pf.getAbsolutePath());
		LOG.info("  original number of persons: " + sc.getPopulation().getPersons().size());
		
		/* Remove those persons that are not members of the sampled households. */
		Population pop = this.sc.getPopulation();
		LOG.info("  removing " + this.algorithm.getSampledMemberIds().size() + " household members...");
		counter.reset();
		for(Id id : this.algorithm.getSampledMemberIds()){
			pop.getPersons().remove(id);
			if(pa != null){
				pa.removeAllAttributes(id.toString());
			}
			counter.incCounter();
		}
		counter.printCounter();
		LOG.info("  remaining number of household members: " + pop.getPersons().size());
		
		this.sampledHouseholds = hhs;
		this.sampledHouseholdAttributes = hha;
		this.sampledPopulation = pop;
		this.sampledPopulationAttributes = pa;
	}
	
	
	/**
	 * Gets the household {@link Id}s of the sampled households.
	 * @return
	 * @throws IllegalAccessException if the method {@link #sampleHouseholds(String, double)}
	 * 		has not been called yet, and no sampling has been done.
	 */
	public List<Id> getSampledHouseholdIds() throws IllegalAccessException{
		if(this.algorithm == null){
			throw new IllegalAccessException("No sampling algorithm has been created. Firts run sampleHouseholds() method.");
		}
		return this.algorithm.getSampledIds();
	}

	
	/**
	 * Gets the household {@link Id}s of the sampled households.
	 * @return
	 * @throws IllegalAccessException if the method {@link #sampleHouseholds(String, double)}
	 * 		has not been called yet, and no sampling has been done.
	 */
	public List<Id> getSampledHouseholdMemberIds() throws IllegalAccessException{
		if(this.algorithm == null){
			throw new IllegalAccessException("No sampling algorithm has been created. Firts run sampleHouseholds() method.");
		}
		return this.algorithm.getSampledMemberIds();
	}
	
	
	public void writeSample(String outputFolder, String networkFile){
		LOG.info("Writing the sample to " + outputFolder);
		
		/* First read in the network. */
		MatsimNetworkReader nr = new MatsimNetworkReader(this.sc);
		nr.readFile(networkFile);
		
		/* Households. */
		HouseholdsWriterV10 hw = new HouseholdsWriterV10(this.sampledHouseholds);
		hw.writeFile(outputFolder + "households.xml.gz");
		
		/* Household attributes, if they exist. */
		if( this.sampledHouseholdAttributes != null){
			ObjectAttributesXmlWriter haw = new ObjectAttributesXmlWriter(this.sampledHouseholdAttributes);
			haw.writeFile(outputFolder + "householdAttributes.xml.gz");
		}
		
		/* Population. */
		PopulationWriter pw = new PopulationWriter(this.sampledPopulation, this.sc.getNetwork());
		pw.write(outputFolder + "population.xml.gz");
		
		/* Population attributes, if they exist. */
		if(this.sampledPopulationAttributes != null){
			ObjectAttributesXmlWriter paw = new ObjectAttributesXmlWriter(this.sampledPopulationAttributes);
			paw.writeFile(outputFolder + "populationAttributes.xml.gz");
		}
	}

}

