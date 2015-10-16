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
import java.io.IOException;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.households.HouseholdsAlgorithmRunner;
import org.matsim.households.HouseholdsReaderV10;
import org.matsim.households.HouseholdsWriterV10;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

import playground.southafrica.population.algorithms.HouseholdSamplerAlgorithm;
import playground.southafrica.population.census2011.attributeConverters.CoordConverter;
import playground.southafrica.population.utilities.ComprehensivePopulationReader;
import playground.southafrica.utilities.Header;

/**
 * Class to sample entire households, with its members considered. Provides
 * methods for the sampling, and writing of sampled households.
 *
 * @author jwjoubert
 * @see HouseholdSamplerAlgorithm
 */
public class HouseholdSampler {
	private final static Logger LOG = Logger.getLogger(HouseholdSampler.class);
	private final Random rng;
	private Scenario sc;

	/**
	 * Implements the household sampler.
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(HouseholdSampler.class.toString(), args);
		String inputFolder = args[0];
		String outputFolder = args[1];
		double fraction = Double.parseDouble(args[2]);
		
		HouseholdSampler hs = new HouseholdSampler(MatsimRandom.getLocalInstance());
		hs.sampleHouseholds(inputFolder, fraction);
		try {
			hs.writeSample(outputFolder);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("May not overwrite existing files.");
		}
		
		Header.printFooter();
	}
	
	/**
	 * Default constructor. If you want more control over the sampling, 
	 * consider {@link #HouseholdSampler(Random)} where you can pass a specific
	 * random number generator.
	 */
	public HouseholdSampler(){
		this.rng = MatsimRandom.getLocalInstance();
	}
	
	
	/**
	 * Constructor where you have control over the random number generator 
	 * used. This may be useful for tests, for example.
	 *   
	 * @param random
	 */
	public HouseholdSampler(Random random) {	
		this.rng = random;
	}
	
		
	/**
	 * Samples a fraction of the households, using {@link HouseholdSamplerAlgorithm}
	 * so that the household members are kept intact with the sampled households. 
	 * The code deliberately does <i>not</i> use the {@link ComprehensivePopulationReader} 
	 * as it requires too much memory (June 2014: 100% Gauteng sample).
	 * 
	 * @param inputFolder where it is assumed a 'comprehensive' population 
	 * 		resides. That is, at least the following files must be present:
	 * 		<ul>
	 * 			<li> <code>population.xml.gz</code>
	 * 			<li> <code>populationAttributes.xml.gz</code>
	 * 			<li> <code>households.xml.gz</code>
	 * 			<li> <code>householdAttributes.xml.gz</code>
	 * 		</ul>
	 * 
	 * @param fraction the fraction of the original population that must be
	 * 		retained.
	 */
	public void sampleHouseholds(String inputFolder, final double fraction){
		LOG.info("Sampling " + String.format("%.1f%%", fraction*100) + " of households.");
		
		sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());

		/* Read just the households. */
		String householdsFile = inputFolder + (inputFolder.endsWith("/") ? "" : "/") + "households.xml.gz";
		new HouseholdsReaderV10(sc.getHouseholds()).parse(householdsFile);		
		LOG.info("  original number of households: " + sc.getHouseholds().getHouseholds().size());

		/* Run the sampling algorithm. */
		HouseholdsAlgorithmRunner algos = new HouseholdsAlgorithmRunner();
		HouseholdSamplerAlgorithm sampler = new HouseholdSamplerAlgorithm(fraction, rng);
		algos.addAlgorithm(sampler);
		algos.runAlgorithms(sc.getHouseholds());
		
		/* Remove those households that have not been sampled. */
		LOG.info("  removing " + sampler.getSampledIds().size() + " households...");
		Counter counter = new Counter("    removed # ");
		for(Id id : sampler.getSampledIds()){
			sc.getHouseholds().getHouseholds().remove(id);
			counter.incCounter();
		}
		counter.printCounter();
		LOG.info("  remaining number of households: " + sc.getHouseholds().getHouseholds().size());
		
		/* Read and clean the population. */
		String populationFile = inputFolder + (inputFolder.endsWith("/") ? "" : "/") + "population.xml.gz";
		new MatsimPopulationReader(sc).parse(populationFile);
		counter.reset();
		LOG.info("  removing " + sampler.getSampledMemberIds().size() + " household members...");
		for(Id id : sampler.getSampledMemberIds()){
			sc.getPopulation().getPersons().remove(id);
			counter.incCounter();
		}
		counter.printCounter();
		LOG.info("  remaining number of household members: " + sc.getPopulation().getPersons().size());

		/* Read and clean the household attributes. */
		String householdAttributeFile = inputFolder + (inputFolder.endsWith("/") ? "" : "/") + "householdAttributes.xml.gz";
		ObjectAttributesXmlReader hhar = new ObjectAttributesXmlReader(sc.getHouseholds().getHouseholdAttributes());
		hhar.putAttributeConverter(Coord.class, new CoordConverter());
		hhar.parse(householdAttributeFile);
		counter.reset();
		LOG.info("  removing " + sampler.getSampledIds().size() + " household's attributes...");
		for(Id id : sampler.getSampledIds()){
			sc.getHouseholds().getHouseholdAttributes().removeAllAttributes(id.toString());
			counter.incCounter();
		}
		counter.printCounter();
		
		/* Read and clean the person attributes. */
		String personAttributeFile = inputFolder + (inputFolder.endsWith("/") ? "" : "/") + "populationAttributes.xml.gz";
		new ObjectAttributesXmlReader(sc.getPopulation().getPersonAttributes()).parse(personAttributeFile);
		counter.reset();
		LOG.info("  removing " + sampler.getSampledIds().size() + " household members's attributes...");
		for(Id id : sampler.getSampledMemberIds()){
			sc.getPopulation().getPersonAttributes().removeAllAttributes(id.toString());
			counter.incCounter();
		}
		counter.printCounter();
	}
	
	/**
	 * Writes the households, their attributes, the population and its 
	 * attributes to a given folder. <br><br>
	 *  
	 * @param outputFolder
	 * @throws IOException if the output folder contains any files that may be overwritten.
	 */
	public void writeSample(String outputFolder) throws IOException{
		LOG.info("Writing the sample to " + outputFolder);
		
		File hhf = new File(outputFolder + "households.xml.gz");
		File hhaf = new File(outputFolder + "householdAttributes.xml.gz");
		File pf = new File(outputFolder + "population.xml.gz");
		File paf = new File(outputFolder + "populationAttributes.xml.gz");
		if(hhf.exists() || hhaf.exists() || pf.exists() || paf.exists()){
			throw new IOException("One or more of the output files already exists, and may not be overwritten.");
		}

		/* Households. */
		HouseholdsWriterV10 hw = new HouseholdsWriterV10(sc.getHouseholds());
		hw.writeFile(hhf.getAbsolutePath());

		/* Household attributes, if they exist. */
		ObjectAttributesXmlWriter hhaw = new ObjectAttributesXmlWriter(sc.getHouseholds().getHouseholdAttributes());
		hhaw.putAttributeConverter(Coord.class, new CoordConverter());
		hhaw.writeFile(hhaf.getAbsolutePath());

		/* Population. */
		PopulationWriter pw = new PopulationWriter(sc.getPopulation());
		pw.write(pf.getAbsolutePath());

		/* Population attributes, if they exist. */
		ObjectAttributesXmlWriter paw = new ObjectAttributesXmlWriter(sc.getPopulation().getPersonAttributes());
		paw.writeFile(paf.getAbsolutePath());
	}

}

