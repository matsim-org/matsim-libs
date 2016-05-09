/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package playground.boescpa.ivtBaseline.preparation.crossborderCreation;

import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * Provides all the parameters required for the creation of a single trip population
 *
 * @author boescpa
 */
public class CreateSingleTripPopulationConfigGroup extends ReflectiveConfigGroup {

	public static final String GROUP_NAME = "SingleTripPopulationCreation";

	// Config Group Parameters
	// delimiter: the delimiter used in the various input files
	private String delimiter = ";";
	// tag: the tag used to mark the sub-population
	private String population_tag = "sp";
	// facilities: all scenario facilities incl secondary facilities and border-crossing facilities
	private String pathToFacilities;
	// cumulative departure probabilities: hourly probabilities used to determine the departures of the single trip agents
	private String pathToCumulativeDepartureProbabilities;
	// subpopulation origin: the facilities which will be used as home locations (file can also be used otherwise)
	private String pathToSubpopulation_Origin;
	// subpopulation destination: the facilities which will be used for the activity locations (file can also be used otherwise)
	private String pathToSubpopulation_Destination;
	// output: path to where the output population will be written to (pop attributes and facilities are written to the same place with different filenames).
	private String pathToOutput;
	// sample percentage: the share of the input sub-population which will be used for the output. E.g. for a 1%-sub-population: '0.01'
	private double samplePercentage;
	// random seed: the random seed used for the random aspects of the population generation
	private long randomSeed;

	public CreateSingleTripPopulationConfigGroup() {super(GROUP_NAME);}


	@StringGetter("delimiter")
	@DoNotConvertNull
	public String getDelimiter() {
		return delimiter;
	}

	@StringSetter("delimiter")
	@DoNotConvertNull
	public void setDelimiter(String delimiter) {
		if (delimiter == null) throw new IllegalArgumentException();
		this.delimiter = delimiter;
	}

	@StringGetter("tag")
	@DoNotConvertNull
	public String getTag() {
		return population_tag;
	}

	@StringSetter("tag")
	@DoNotConvertNull
	public void setTag(String tag) {
		if (tag == null) throw new IllegalArgumentException();
		this.population_tag = tag;
	}

	@StringGetter("inputFile_Origin")
	public String getPathToOriginsFile() {
		return pathToSubpopulation_Origin;
	}

	@StringSetter("inputFile_Origin")
	public void setPathToOriginsFile(String pathTo_Origin) {
		this.pathToSubpopulation_Origin = pathTo_Origin;
	}

	@StringGetter("inputFile_Destination")
	public String getPathToDestinationsFile() {
		return pathToSubpopulation_Destination;
	}

	@StringSetter("inputFile_Destination")
	public void setPathToDestinationsFile(String pathTo_Destination) {
		this.pathToSubpopulation_Destination = pathTo_Destination;
	}

	@StringGetter("inputFile_Facilities")
	public String getPathToFacilities() {
		return pathToFacilities;
	}

	@StringSetter("inputFile_Facilities")
	public void setPathToFacilities(String pathToFacilities) {
		this.pathToFacilities = pathToFacilities;
	}

	@StringGetter("inputFile_CumulativeDepartureProbabilities")
	public String getPathToCumulativeDepartureProbabilities() {
		return pathToCumulativeDepartureProbabilities;
	}

	@StringSetter("inputFile_CumulativeDepartureProbabilities")
	public void setPathToCumulativeDepartureProbabilities(String pathToCumulativeDepartureProbabilities) {
		this.pathToCumulativeDepartureProbabilities = pathToCumulativeDepartureProbabilities;
	}

	@StringGetter("outputFile_Population")
	public String getPathToOutput() {
		return pathToOutput;
	}

	@StringSetter("outputFile_Population")
	public void setPathToOutput(String pathToOutput) {
		this.pathToOutput = pathToOutput;
	}

	@StringGetter("randomSeed")
	public long getRandomSeed() {
		return randomSeed;
	}

	@StringSetter("randomSeed")
	public void setRandomSeed(long randomSeed) {
		this.randomSeed = randomSeed;
	}

	@StringGetter("samplePercentage")
	public double getSamplePercentage() {
		return samplePercentage;
	}

	@StringSetter("samplePercentage")
	public void setSamplePercentage(double samplePercentage) {
		this.samplePercentage = samplePercentage;
	}

	public CreateSingleTripPopulationConfigGroup copy() {
		CreateSingleTripPopulationConfigGroup copy = new CreateSingleTripPopulationConfigGroup();
		for (String param : this.getParams().keySet()) {
			copy.addParam(param, this.getParams().get(param));
		}
		return copy;
	}
}
