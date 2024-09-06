/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package playground.vsp.openberlinscenario.cemdap.input;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.api.feature.simple.SimpleFeature;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.GeoFileReader;
import org.matsim.utils.objectattributes.ObjectAttributes;
import playground.vsp.openberlinscenario.Gender;
import playground.vsp.openberlinscenario.cemdap.LogToOutputSaver;

import java.util.*;

/**
 * This class creates a children-only population of a study region (in Germany) based on the Zensus and the Pendlerstatistik.
 * Only meant as as supplement ot an already existing adults-only population.
 *
 * @author dziemke
 */
public class SynPopCreatorChildren {
	private static final Logger LOG = LogManager.getLogger(SynPopCreatorChildren.class);

	private static final Random random = MatsimRandom.getLocalInstance(); // Make sure that stream of random variables is reproducible.

	// Storage objects
	private Population population;
	private ObjectAttributes municipalities;
	private List<String> municipalityList;
	// Optional (links municipality ID to LOR/PLZ IDs in that municipality)
	private final Map<String, List<String>> spatialRefinementZoneIds = new HashMap<>();
	private List<String> idsOfMunicipalitiesForSpatialRefinement;

	// Parameters
	private String outputBase;
	private List<String> idsOfFederalStatesIncluded;
	boolean writeMatsimPlanFiles = false;
	// Optional
	private String shapeFileForSpatialRefinement;
	private String refinementFeatureKeyInShapefile;
	private String municipalityFeatureKeyInShapefile;

	// Counters
	private int allPersons = 0;
	private int counterInit = 5000000; // highest household no. in adult pop: 4917171, 491717101


	public static void main(String[] args) {
		// Input and output files
		String censusFile = "../../shared-svn/studies/countries/de/open_berlin_scenario/input/zensus_2011/bevoelkerung/csv_Bevoelkerung/Zensus11_Datensatz_Bevoelkerung_BE_BB.csv";
		String outputBase = "../../shared-svn/studies/countries/de/open_berlin_scenario/be_5/cemdap_input/500/";

		// Parameters
		List<String> idsOfFederalStatesIncluded = Arrays.asList("11", "12"); // 11=Berlin, 12=Brandenburg

		SynPopCreatorChildren demandGeneratorCensus = new SynPopCreatorChildren(censusFile, outputBase, idsOfFederalStatesIncluded);
		demandGeneratorCensus.setWriteMatsimPlanFiles(true);
		demandGeneratorCensus.setShapeFileForSpatialRefinement("../../shared-svn/studies/countries/de/open_berlin_scenario/input/shapefiles/2016/Planungsraum_PLN_ID.shp");
		demandGeneratorCensus.setIdsOfMunicipalitiesForSpatialRefinement(Arrays.asList("11000000")); // "Amtlicher Gemeindeschlüssel (AGS)" of Berlin is "11000000"
		demandGeneratorCensus.setRefinementFeatureKeyInShapefile("PLN_ID"); // Key of the features in the shapefile used for spatial refinement
		demandGeneratorCensus.setMunicipalityFeatureKeyInShapefile(null); // If spatial refinement only for one municipality, no distinction necessary

		demandGeneratorCensus.generateDemand();
	}


	public SynPopCreatorChildren(String censusFile, String outputBase, List<String> idsOfFederalStatesIncluded) {
		LogToOutputSaver.setOutputDirectory(outputBase);

		this.outputBase = outputBase;

		this.idsOfFederalStatesIncluded = idsOfFederalStatesIncluded;
		this.idsOfFederalStatesIncluded.stream().forEach(e -> {
			if (e.length()!=2) throw new IllegalArgumentException("Length of the id for each federal state must be equal to 2. This is not the case for "+ e);
		});

		this.population = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getPopulation();

		// Read census
		CensusReader censusReader = new CensusReader(censusFile, ";");
		this.municipalities = censusReader.getMunicipalities();
		this.municipalityList = censusReader.getMunicipalityList();
	}


	public void generateDemand() {
		if (this.shapeFileForSpatialRefinement != null && this.refinementFeatureKeyInShapefile != null ) {
			this.idsOfMunicipalitiesForSpatialRefinement.stream().forEach(e->spatialRefinementZoneIds.put(e, new ArrayList<>()));
			readShapeForSpatialRefinement();
		} else {
			LOG.info("A shape file and/or and the name of the attribute that contains the keys has not been provided.");
		}

		if (this.shapeFileForSpatialRefinement != null && this.municipalityFeatureKeyInShapefile == null && this.idsOfMunicipalitiesForSpatialRefinement.size() > 1) {
			throw new RuntimeException("A shape file for spatial refinement is provided and the number of municipality IDs for spatial refinement is greater than 1." +
					"However, no feature key is provided to distinguish between the municipalities.");
		}

		int counter = counterInit;

		for (String munId : municipalityList) {
			int pop0_2Male = (int) this.municipalities.getAttribute(munId, CensusAttributes.pop0_2Male.toString());
			int pop3_5Male = (int) this.municipalities.getAttribute(munId, CensusAttributes.pop3_5Male.toString());
			int pop6_14Male = (int) this.municipalities.getAttribute(munId, CensusAttributes.pop6_14Male.toString());
			int pop15_17Male = (int) this.municipalities.getAttribute(munId, CensusAttributes.pop15_17Male.toString());

			int pop0_2Female = (int) this.municipalities.getAttribute(munId, CensusAttributes.pop0_2Female.toString());
			int pop3_5Female = (int) this.municipalities.getAttribute(munId, CensusAttributes.pop3_5Female.toString());
			int pop6_14Female = (int) this.municipalities.getAttribute(munId, CensusAttributes.pop6_14Female.toString());
			int pop15_17Female = (int) this.municipalities.getAttribute(munId, CensusAttributes.pop15_17Female.toString());

			createChildren(counter, munId, pop0_2Male, Gender.male, 0, 2);
			counter += pop0_2Male;
			createChildren(counter, munId, pop0_2Female, Gender.female, 0, 2);
			counter += pop0_2Female;
			createChildren(counter, munId, pop3_5Male, Gender.male, 3, 5);
			counter += pop3_5Male;
			createChildren(counter, munId, pop3_5Female, Gender.female, 3, 5);
			counter += pop3_5Female;
			createChildren(counter, munId, pop6_14Male, Gender.male, 6, 14);
			counter += pop6_14Male;
			createChildren(counter, munId, pop6_14Female, Gender.female, 6, 14);
			counter += pop6_14Female;
			createChildren(counter, munId, pop15_17Male, Gender.male, 15, 17);
			counter += pop15_17Male;
			createChildren(counter, munId, pop15_17Female, Gender.female, 15, 17);
			counter += pop15_17Female;
		}

		// Write some relevant information on console
		LOG.warn("Total population: " + this.allPersons);

		// Write output files
		Population adjustedPopulation = clonePopulationAndAdjustLocations(this.population);
		writeMatsimPlansFile(adjustedPopulation, this.outputBase + "plans_children.xml.gz");
	}


	private Population clonePopulationAndAdjustLocations(Population inputPopulation){
		Population clonedPopulation = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getPopulation();
		for (Person person : inputPopulation.getPersons().values()) {
			Person clonedPerson = clonedPopulation.getFactory().createPerson(person.getId());
			// copy plans
			for (Plan plan : person.getPlans()) { // Although only one plan can exist at this point in time, iterating for all plans
				Plan clonedPlan = clonedPopulation.getFactory().createPlan();
				PopulationUtils.copyFromTo(plan, clonedPlan);
				clonedPerson.addPlan(clonedPlan);
			}

			// Adjust home location
			String locationOfHome = (String) person.getAttributes().getAttribute("municipalityId");
			clonedPerson.getAttributes().putAttribute("municipalityId", getExactLocation(locationOfHome));

			// copy attributes
			clonedPerson.getAttributes().putAttribute(CEMDAPPersonAttributes.gender.toString(), person.getAttributes().getAttribute(CEMDAPPersonAttributes.gender.toString()));
			clonedPerson.getAttributes().putAttribute(CEMDAPPersonAttributes.age.toString(), person.getAttributes().getAttribute(CEMDAPPersonAttributes.age.toString()));

			clonedPopulation.addPerson(clonedPerson);
		}
		return clonedPopulation;
	}


	private void createChildren(int counter, String municipalityId, int numberOfPersons, Gender gender, int lowerAgeBound, int upperAgeBound) {
		for (int i = 0; i < numberOfPersons; i++) {
			this.allPersons++;

			Id<Person> personId = Id.create(counter + i + "01", Person.class); // TODO Currently only singel-person households
			Person person = this.population.getFactory().createPerson(personId);

			person.getAttributes().putAttribute("municipalityId", municipalityId);
			person.getAttributes().putAttribute(CEMDAPPersonAttributes.gender.toString(), gender.name());
			person.getAttributes().putAttribute(CEMDAPPersonAttributes.age.toString(), getAgeInBounds(lowerAgeBound, upperAgeBound));

			this.population.addPerson(person);
		}
	}


	private String getExactLocation(String municipalityId) {
		String locationId;
		if (this.idsOfMunicipalitiesForSpatialRefinement !=null && this.idsOfMunicipalitiesForSpatialRefinement.contains(municipalityId)) {
			locationId = getSpatiallyRefinedZone(municipalityId);
		} else {
			locationId = municipalityId;
		}
		return locationId;
	}


	private String getSpatiallyRefinedZone(String municipalityId) {
		List<String> spatiallyRefinedZones = this.spatialRefinementZoneIds.get(municipalityId);
		return spatiallyRefinedZones.get(random.nextInt(spatiallyRefinedZones.size()));
	}


	private static int getAgeInBounds(int lowerBound, int upperBound) {
		return (int) (lowerBound + random.nextDouble() * (upperBound - lowerBound + 1));
	}


	private Map<String,List<String>> readShapeForSpatialRefinement() {
		Collection<SimpleFeature> features = GeoFileReader.getAllFeatures(this.shapeFileForSpatialRefinement);

		for (SimpleFeature feature : features) {
			String municipality;
			if (this.municipalityFeatureKeyInShapefile == null) {
				municipality = this.idsOfMunicipalitiesForSpatialRefinement.get(0); // checked already that size must be 1 (e.g., Berlin). Amit Nov'17
			} else {
				municipality = (String) feature.getAttribute(this.municipalityFeatureKeyInShapefile);
			}
			String key = (String) feature.getAttribute(this.refinementFeatureKeyInShapefile); //attributeKey --> SCHLUESSEL
			spatialRefinementZoneIds.get(municipality).add(key);
		}
		return spatialRefinementZoneIds;
	}


	private static void writeMatsimPlansFile(Population population, String fileName) {
		PopulationWriter popWriter = new PopulationWriter(population);
	    popWriter.write(fileName);
	}


	// Getters and setters
    public Population getPopulation() {
    	return this.population;
	}

    public void setShapeFileForSpatialRefinement(String shapeFileForSpatialRefinement) {
    	this.shapeFileForSpatialRefinement = shapeFileForSpatialRefinement;
    }

    public void setIdsOfMunicipalitiesForSpatialRefinement(List<String> idsOfMunicipalitiesForSpatialRefinement) {
    	this.idsOfMunicipalitiesForSpatialRefinement = idsOfMunicipalitiesForSpatialRefinement;
    }

    public void setRefinementFeatureKeyInShapefile(String refinementFeatureKeyInShapefile) {
    	this.refinementFeatureKeyInShapefile = refinementFeatureKeyInShapefile;
    }

	public void setMunicipalityFeatureKeyInShapefile(String municipalityFeatureKeyInShapefile) {
		this.municipalityFeatureKeyInShapefile = municipalityFeatureKeyInShapefile;
	}

	public void setWriteMatsimPlanFiles(boolean writeMatsimPlanFiles) {
    	this.writeMatsimPlanFiles = writeMatsimPlanFiles;
    }
}
