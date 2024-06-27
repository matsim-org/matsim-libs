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

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

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
import org.matsim.core.utils.io.IOUtils;
import org.matsim.households.Household;
import org.matsim.households.HouseholdImpl;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.attributable.Attributes;

import playground.vsp.openberlinscenario.Gender;
import playground.vsp.openberlinscenario.cemdap.LogToOutputSaver;

/**
 * This class creates a full population of a study region (in Germany) based on the Zensus and the Pendlerstatistik. People are assigned
 * places or residence and with demographic attributes based on the Zensus and with commuter relations based on the Pendlerstatistik.
 *
 * @author dziemke
 */
public class SynPopCreator {
	private static final Logger LOG = LogManager.getLogger(SynPopCreator.class);

	private static final Random random = MatsimRandom.getLocalInstance(); // Make sure that stream of random variables is reproducible.

	// Storage objects
	private Population population;
	private Map<Id<Household>, Household> households;
	private ObjectAttributes municipalities;
	private Map<String, Map<String, CommuterRelationV2>> relationsMap;
	private boolean memorizeAllPopulations = false; // default = false for backwards compatibility; false saves RAM
	private List<Population> allPopulations = new ArrayList<>();
	// Optional (links municipality ID to LOR/PLZ IDs in that municipality)
	private final Map<String, List<String>> spatialRefinementZoneIds = new HashMap<>();
	private List<String> idsOfMunicipalitiesForSpatialRefinement;

	// Parameters
	private String outputBase;
	private List<String> idsOfFederalStatesIncluded;
	private int numberOfPlansPerPerson;
	private double defaultAdultsToEmployeesRatio;
	private double defaultEmployeesToCommutersRatio;
	boolean includeChildren = false;
	boolean writeCemdapInputFiles = true; // Default is true for backwards compatibility
	boolean writeMatsimPlanFiles = false;
	// Optional
	private String shapeFileForSpatialRefinement;
	private String refinementFeatureKeyInShapefile;
	private String municipalityFeatureKeyInShapefile;

	// Counters
	private int counterMissingComRel = 0;
	private int counterExternalCommuters = 0;
	private int counterComRelUnassigned = 0;
	private int allEmployees = 0;
	private int allPersons = 0;
	private int allStudents = 0;


	public static void main(String[] args) {
		// Input and output files
		String commuterFileOutgoing1 = "../../shared-svn/studies/countries/de/open_berlin_scenario/input/pendlerstatistik_2009/Berlin_2009/B2009Ga.txt";
		String commuterFileOutgoing2 = "../../shared-svn/studies/countries/de/open_berlin_scenario/input/pendlerstatistik_2009/Brandenburg_2009/Teil1BR2009Ga.txt";
		String commuterFileOutgoing3 = "../../shared-svn/studies/countries/de/open_berlin_scenario/input/pendlerstatistik_2009/Brandenburg_2009/Teil2BR2009Ga.txt";
		String commuterFileOutgoing4 = "../../shared-svn/studies/countries/de/open_berlin_scenario/input/pendlerstatistik_2009/Brandenburg_2009/Teil3BR2009Ga.txt";
		String[] commuterFilesOutgoing = {commuterFileOutgoing1, commuterFileOutgoing2, commuterFileOutgoing3, commuterFileOutgoing4};
		String censusFile = "../../shared-svn/studies/countries/de/open_berlin_scenario/input/zensus_2011/bevoelkerung/csv_Bevoelkerung/Zensus11_Datensatz_Bevoelkerung_BE_BB.csv";
		String outputBase = "../../shared-svn/studies/countries/de/open_berlin_scenario/be_5/cemdap_input/505/";

		// Parameters
		int numberOfPlansPerPerson = 10; // Note: Set this higher to a value higher than 1 if spatial refinement is used.
		List<String> idsOfFederalStatesIncluded = Arrays.asList("11", "12"); // 11=Berlin, 12=Brandenburg

		// Default ratios are used for cases where information is missing, which is the case for smaller municipalities.
		double defaultAdultsToEmployeesRatio = 1.23;  // Calibrated based on sum value from Zensus 2011.
		double defaultCensusEmployeesToCommutersRatio = 2.5;  // This is an assumption, oriented on observed values, deliberately chosen slightly too high.
		// Choosing this too high effects that too many commuter relations are created, which is uncritical as relative shares will still be correct.
		// Choosing this too low effects that employed people (according to the census) are left without workplace. Minimize this number!

		SynPopCreator demandGeneratorCensus = new SynPopCreator(commuterFilesOutgoing, censusFile, outputBase, numberOfPlansPerPerson,
				idsOfFederalStatesIncluded, defaultAdultsToEmployeesRatio, defaultCensusEmployeesToCommutersRatio);
		demandGeneratorCensus.setWriteMatsimPlanFiles(true);
		demandGeneratorCensus.setShapeFileForSpatialRefinement("../../shared-svn/studies/countries/de/open_berlin_scenario/input/shapefiles/2016/Planungsraum_PLN_ID.shp");
		demandGeneratorCensus.setIdsOfMunicipalitiesForSpatialRefinement(Arrays.asList("11000000")); // "Amtlicher Gemeindeschlüssel (AGS)" of Berlin is "11000000"
		demandGeneratorCensus.setRefinementFeatureKeyInShapefile("PLN_ID"); // Key of the features in the shapefile used for spatial refinement
		demandGeneratorCensus.setMunicipalityFeatureKeyInShapefile(null); // If spatial refinement only for one municipality, no distinction necessary

		demandGeneratorCensus.generateDemand();
	}


	public SynPopCreator(String[] commuterFilesOutgoing, String censusFile, String outputBase, int numberOfPlansPerPerson,
			List<String> idsOfFederalStatesIncluded, double defaultAdultsToEmployeesRatio, double defaultEmployeesToCommutersRatio) {
		LogToOutputSaver.setOutputDirectory(outputBase);

		this.outputBase = outputBase;
		this.numberOfPlansPerPerson = numberOfPlansPerPerson;

		this.idsOfFederalStatesIncluded = idsOfFederalStatesIncluded;
		this.idsOfFederalStatesIncluded.stream().forEach(e -> {
			if (e.length()!=2) throw new IllegalArgumentException("Length of the id for each federal state must be equal to 2. This is not the case for "+ e);
		});

		this.defaultAdultsToEmployeesRatio = defaultAdultsToEmployeesRatio;
		this.defaultEmployeesToCommutersRatio = defaultEmployeesToCommutersRatio;

		this.population = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getPopulation();
		this.households = new HashMap<>();

		// Read census
		CensusReader censusReader = new CensusReader(censusFile, ";");
		this.municipalities = censusReader.getMunicipalities();

		// Read commuter relations
		this.relationsMap = new HashMap<>();
		for (String commuterFileOutgoing : commuterFilesOutgoing) {
			CommuterFileReaderV2 commuterFileReader = new CommuterFileReaderV2(commuterFileOutgoing, "\t");
			Map<String, Map<String, CommuterRelationV2>> currentRelationMap = commuterFileReader.getRelationsMap();
			this.relationsMap.putAll(currentRelationMap);
		}
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

		int counter = 1;

		for (String munId : relationsMap.keySet()) { // Loop over municipalities from commuter file
			Map<String, CommuterRelationV2> relationsFromMunicipality = relationsMap.get(munId);

			// Employees in census are all employees, not only socially-secured employees
			if (this.municipalities.getAttribute(munId, CensusAttributes.employedMale.toString()) == null || this.municipalities.getAttribute(munId, CensusAttributes.employedFemale.toString()) == null) {
				LOG.warn("Employed male (and possibly other) information is not available in the census data for munId "+ munId + ". Skipping this municipality.");
				continue;
			}

			int employeesMale = (int) this.municipalities.getAttribute(munId, CensusAttributes.employedMale.toString());
			int employeesFemale = (int) this.municipalities.getAttribute(munId, CensusAttributes.employedFemale.toString());

			scaleRelations(relationsFromMunicipality, employeesMale, employeesFemale, this.defaultEmployeesToCommutersRatio);
			List<String> commuterRelationListMale = createRelationList(relationsFromMunicipality, Gender.male);
			List<String> commuterRelationListFemale = createRelationList(relationsFromMunicipality, Gender.female);

			int pop0_2Male = (int) this.municipalities.getAttribute(munId, CensusAttributes.pop0_2Male.toString());
			int pop3_5Male = (int) this.municipalities.getAttribute(munId, CensusAttributes.pop3_5Male.toString());
			int pop6_14Male = (int) this.municipalities.getAttribute(munId, CensusAttributes.pop6_14Male.toString());
			int pop15_17Male = (int) this.municipalities.getAttribute(munId, CensusAttributes.pop15_17Male.toString());
			int pop18_24Male = (int) this.municipalities.getAttribute(munId, CensusAttributes.pop18_24Male.toString());
			int pop25_29Male = (int) this.municipalities.getAttribute(munId, CensusAttributes.pop25_29Male.toString());
			int pop30_39Male = (int) this.municipalities.getAttribute(munId, CensusAttributes.pop30_39Male.toString());
			int pop40_49Male = (int) this.municipalities.getAttribute(munId, CensusAttributes.pop40_49Male.toString());
			int pop50_64Male = (int) this.municipalities.getAttribute(munId, CensusAttributes.pop50_64Male.toString());
			int pop65_74Male = (int) this.municipalities.getAttribute(munId, CensusAttributes.pop65_74Male.toString());
			int pop75PlusMale = (int) this.municipalities.getAttribute(munId, CensusAttributes.pop75PlusMale.toString());

			int pop0_2Female = (int) this.municipalities.getAttribute(munId, CensusAttributes.pop0_2Female.toString());
			int pop3_5Female = (int) this.municipalities.getAttribute(munId, CensusAttributes.pop3_5Female.toString());
			int pop6_14Female = (int) this.municipalities.getAttribute(munId, CensusAttributes.pop6_14Female.toString());
			int pop15_17Female = (int) this.municipalities.getAttribute(munId, CensusAttributes.pop15_17Female.toString());
			int pop18_24Female = (int) this.municipalities.getAttribute(munId, CensusAttributes.pop18_24Female.toString());
			int pop25_29Female = (int) this.municipalities.getAttribute(munId, CensusAttributes.pop25_29Female.toString());
			int pop30_39Female = (int) this.municipalities.getAttribute(munId, CensusAttributes.pop30_39Female.toString());
			int pop40_49Female = (int) this.municipalities.getAttribute(munId, CensusAttributes.pop40_49Female.toString());
			int pop50_64Female = (int) this.municipalities.getAttribute(munId, CensusAttributes.pop50_64Female.toString());
			int pop65_74Female = (int) this.municipalities.getAttribute(munId, CensusAttributes.pop65_74Female.toString());
			int pop75PlusFemale = (int) this.municipalities.getAttribute(munId, CensusAttributes.pop75PlusFemale.toString());

			int adultsMale = pop18_24Male + pop25_29Male + pop30_39Male + pop40_49Male + pop50_64Male;
			int adultsFemale = pop18_24Female + pop25_29Female + pop30_39Female + pop40_49Female + pop50_64Female;

			// The adults-to-employees ratio is needed to determine if a given person has a job
			double adultsToEmployeesMaleRatio;
			double adultsToEmployeesFemaleRatio;
			if (employeesMale != 0) { // Avoid dividing by zero
				adultsToEmployeesMaleRatio = (double) adultsMale / (double) employeesMale;
			} else {
				adultsToEmployeesMaleRatio = this.defaultAdultsToEmployeesRatio;
			}
			if (employeesFemale != 0) { // Avoid dividing by zero
				adultsToEmployeesFemaleRatio = (double) adultsFemale / (double) employeesFemale;
			} else {
				adultsToEmployeesFemaleRatio = this.defaultAdultsToEmployeesRatio;
			}

			if (includeChildren) {
				createHouseholdsAndPersons(counter, munId, pop0_2Male, Gender.male, 0, 2, adultsToEmployeesMaleRatio, commuterRelationListMale);
				counter += pop0_2Male;
				createHouseholdsAndPersons(counter, munId, pop0_2Female, Gender.female, 0, 2, adultsToEmployeesFemaleRatio, commuterRelationListFemale);
				counter += pop0_2Female;
				createHouseholdsAndPersons(counter, munId, pop3_5Male, Gender.male, 3, 5, adultsToEmployeesMaleRatio, commuterRelationListMale);
				counter += pop3_5Male;
				createHouseholdsAndPersons(counter, munId, pop3_5Female, Gender.female, 3, 5, adultsToEmployeesFemaleRatio, commuterRelationListFemale);
				counter += pop3_5Female;
				createHouseholdsAndPersons(counter, munId, pop6_14Male, Gender.male, 6, 14, adultsToEmployeesMaleRatio, commuterRelationListMale);
				counter += pop6_14Male;
				createHouseholdsAndPersons(counter, munId, pop6_14Female, Gender.female, 6, 14, adultsToEmployeesFemaleRatio, commuterRelationListFemale);
				counter += pop6_14Female;
				createHouseholdsAndPersons(counter, munId, pop15_17Male, Gender.male, 15, 17, adultsToEmployeesMaleRatio, commuterRelationListMale);
				counter += pop15_17Male;
				createHouseholdsAndPersons(counter, munId, pop15_17Female, Gender.female, 15, 17, adultsToEmployeesFemaleRatio, commuterRelationListFemale);
				counter += pop15_17Female;
			}
			createHouseholdsAndPersons(counter, munId, pop18_24Male, Gender.male, 18, 24, adultsToEmployeesMaleRatio, commuterRelationListMale);
			counter += pop18_24Male;
			createHouseholdsAndPersons(counter, munId, pop18_24Female, Gender.female, 18, 24, adultsToEmployeesFemaleRatio, commuterRelationListFemale);
			counter += pop18_24Female;
			createHouseholdsAndPersons(counter, munId, pop25_29Male, Gender.male, 25, 29, adultsToEmployeesMaleRatio, commuterRelationListMale);
			counter += pop25_29Male;
			createHouseholdsAndPersons(counter, munId, pop25_29Female, Gender.female, 25, 29, adultsToEmployeesFemaleRatio, commuterRelationListFemale);
			counter += pop25_29Female;
			createHouseholdsAndPersons(counter, munId, pop30_39Male, Gender.male, 30, 39, adultsToEmployeesMaleRatio, commuterRelationListMale);
			counter += pop30_39Male;
			createHouseholdsAndPersons(counter, munId, pop30_39Female, Gender.female, 30, 39, adultsToEmployeesFemaleRatio, commuterRelationListFemale);
			counter += pop30_39Female;
			createHouseholdsAndPersons(counter, munId, pop40_49Male, Gender.male, 40, 49, adultsToEmployeesMaleRatio, commuterRelationListMale);
			counter += pop40_49Male;
			createHouseholdsAndPersons(counter, munId, pop40_49Female, Gender.female, 40, 49, adultsToEmployeesFemaleRatio, commuterRelationListFemale);
			counter += pop40_49Female;
			createHouseholdsAndPersons(counter, munId, pop50_64Male, Gender.male, 50, 64, adultsToEmployeesMaleRatio, commuterRelationListMale);
			counter += pop50_64Male;
			createHouseholdsAndPersons(counter, munId, pop50_64Female, Gender.female, 50, 64, adultsToEmployeesFemaleRatio, commuterRelationListFemale);
			counter += pop50_64Female;
			createHouseholdsAndPersons(counter, munId, pop65_74Male, Gender.male, 65, 74, adultsToEmployeesMaleRatio, commuterRelationListMale);
			counter += pop65_74Male;
			createHouseholdsAndPersons(counter, munId, pop65_74Female, Gender.female, 65, 74, adultsToEmployeesFemaleRatio, commuterRelationListFemale);
			counter += pop65_74Female;
			// 90 years as the upper bound is a simplifying assumption!
			createHouseholdsAndPersons(counter, munId, pop75PlusMale, Gender.male, 75, 90, adultsToEmployeesMaleRatio, commuterRelationListMale);
			counter += pop75PlusMale;
			createHouseholdsAndPersons(counter, munId, pop75PlusFemale, Gender.female, 75, 90, adultsToEmployeesFemaleRatio, commuterRelationListFemale);
			counter += pop75PlusFemale;

			// Information on unassigned commuter relations
			this.counterComRelUnassigned += commuterRelationListMale.size();
			if (commuterRelationListMale.size() > 100) {
				LOG.info(commuterRelationListMale.size() + " male commuter relations from " + munId +
						" remain unassigned; based on census, there are " + employeesMale + " male employees.");
			}
			this.counterComRelUnassigned += commuterRelationListFemale.size();
			if (commuterRelationListFemale.size() > 100) {
				LOG.info(commuterRelationListFemale.size() + " female commuter relations from " + munId +
						" remain unassigned; based on census, there are " + employeesFemale + " female employees.");
			}
		}

		// Write some relevant information on console
		LOG.warn("There are " + this.counterMissingComRel + " employees who have been set to unemployed since no commuter relation could be assigned to them.");
		LOG.warn("Share of employees that had to be set to unemployed due to lack of commuter relations: " + ((double) this.counterMissingComRel / (double) this.allEmployees));
		LOG.warn("Altogether " + this.counterComRelUnassigned + " commuter relations remain unassigned.");
		LOG.warn("There are " + this.counterExternalCommuters + " people who commute outside of Berlin and Brandenburg.");
		LOG.warn("Total number of employees: " + this.allEmployees);
		LOG.warn("Total population: " + this.allPersons);
		LOG.warn("Total number of students: " + this.allStudents);

		// Write output files
		if (this.writeCemdapInputFiles) {
			writeCemdapHouseholdsFile(this.households, this.outputBase + "households.dat.gz");
		}
		for (int i = 1; i <= numberOfPlansPerPerson; i++) {
			Population clonedPopulation = clonePopulationAndAdjustLocations(this.population);
			if (this.writeCemdapInputFiles) {
				writeCemdapPersonsFile(clonedPopulation, this.outputBase + "persons" + i + ".dat.gz");
			}
			if (this.writeMatsimPlanFiles) {
				writeMatsimPlansFile(clonedPopulation, this.outputBase + "plans" + i + ".xml.gz");
			}
			if (memorizeAllPopulations) {
				allPopulations.add(clonedPopulation);
			}
		}
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
			// copy attributes
			for(CEMDAPPersonAttributes attributeKey : CEMDAPPersonAttributes.values()){
				clonedPerson.getAttributes().putAttribute(attributeKey.toString(), person.getAttributes().getAttribute(attributeKey.toString()));
			}

			// change locations or use spatially refined location
			if ((boolean) person.getAttributes().getAttribute(CEMDAPPersonAttributes.employed.toString())) {
				String locationOfWork = (String) person.getAttributes().getAttribute(CEMDAPPersonAttributes.locationOfWork.toString());
				if (locationOfWork.length() == 8) {
					clonedPerson.getAttributes().putAttribute(CEMDAPPersonAttributes.locationOfWork.toString(), getExactLocation(locationOfWork));
				} else if (locationOfWork.equals("-99")) {
					throw new RuntimeException("This combination of attribute values is implausible.");
				} else {
					throw new RuntimeException("The identifier of the work location (" + locationOfWork + ") cannot have a length other than 8.");
				}
			}

			if ((boolean) person.getAttributes().getAttribute(CEMDAPPersonAttributes.student.toString())) {
				String locationOfSchool = (String) person.getAttributes().getAttribute(CEMDAPPersonAttributes.locationOfSchool.toString());
				if (locationOfSchool.length() == 8) {
					clonedPerson.getAttributes().putAttribute(CEMDAPPersonAttributes.locationOfSchool.toString(), getExactLocation(locationOfSchool));
				} else if (locationOfSchool.equals("-99")) {
					throw new RuntimeException("This combination of attribute values is implausible.");
				} else {
					throw new RuntimeException("The identifier of the work location (" + locationOfSchool + ") cannot have a length other than 8.");
				}
			}
			clonedPopulation.addPerson(clonedPerson);
		}
		return clonedPopulation;
	}


	private void createHouseholdsAndPersons(int counter, String municipalityId, int numberOfPersons, Gender gender, int lowerAgeBound, int upperAgeBound,
			double adultsToEmployeesRatio, List<String> commuterRelationList) {
		for (int i = 0; i < numberOfPersons; i++) {
			this.allPersons++;
			Id<Household> householdId = Id.create((counter + i), Household.class);
			HouseholdImpl household = new HouseholdImpl(householdId); // TODO Or use factory?
			household.getAttributes().putAttribute(CEMDAPHouseholdAttributes.numberOfAdults.toString(), 1); // Always 1; no household structure
			household.getAttributes().putAttribute(CEMDAPHouseholdAttributes.totalNumberOfHouseholdVehicles.toString(), 1);
			// using spatially refined location directly for home locations. Amit Dec'17
			household.getAttributes().putAttribute(CEMDAPHouseholdAttributes.homeTSZLocation.toString(), getExactLocation(municipalityId));
			household.getAttributes().putAttribute(CEMDAPHouseholdAttributes.numberOfChildren.toString(), 0); // None, ignore them in this version
			household.getAttributes().putAttribute(CEMDAPHouseholdAttributes.householdStructure.toString(), 1); // 1 = single, no children

			Id<Person> personId = Id.create(householdId + "01", Person.class); // TODO Currently only singel-person households
			Person person = this.population.getFactory().createPerson(personId);

			person.getAttributes().putAttribute(CEMDAPPersonAttributes.householdId.toString(), householdId.toString()); // toString() will enable writing it to person attributes
			boolean employed = false;
			if (lowerAgeBound < 65 && upperAgeBound > 17) { // Younger and older people are never employed
				employed = getEmployed(adultsToEmployeesRatio);
			}
			person.getAttributes().putAttribute(CEMDAPPersonAttributes.employed.toString(), employed);

			boolean student = false;
			if (lowerAgeBound < 30 && upperAgeBound > 17 && !employed) { // Younger and older people are never a student, employed people neither
				student = true; // TODO quite simplistic assumption, which may be improved later
				allStudents++;
			}
			person.getAttributes().putAttribute(CEMDAPPersonAttributes.student.toString(), student);

			if (employed) {
				allEmployees++;
				if (commuterRelationList.size() == 0) { // No relations left in list, which employee could choose from
					counterMissingComRel++;
					person.getAttributes().putAttribute(CEMDAPPersonAttributes.locationOfWork.toString(), "-99");
					person.getAttributes().putAttribute(CEMDAPPersonAttributes.employed.toString(), false);
				} else {
					String locationOfWork = getWorkMunicipalityFromCommuterRelationList(commuterRelationList); // municipality id
					if (locationOfWork.length() == 8 && !this.idsOfFederalStatesIncluded.contains(locationOfWork.substring(0,2))) { // TODO external commuter are currently treated as non workers
						counterExternalCommuters++;
						person.getAttributes().putAttribute(CEMDAPPersonAttributes.locationOfWork.toString(), "-99");
						person.getAttributes().putAttribute(CEMDAPPersonAttributes.employed.toString(), false);
					} else {
						person.getAttributes().putAttribute(CEMDAPPersonAttributes.locationOfWork.toString(), locationOfWork);
					}
				}
			} else {
				person.getAttributes().putAttribute(CEMDAPPersonAttributes.locationOfWork.toString(), "-99");
			}

			if (student) {
				// TODO This is a quite simple assumption (students study in their residential municipality), which may be improved later
				person.getAttributes().putAttribute(CEMDAPPersonAttributes.locationOfSchool.toString(), municipalityId);
			} else {
				person.getAttributes().putAttribute(CEMDAPPersonAttributes.locationOfSchool.toString(), "-99");
			}

			person.getAttributes().putAttribute(CEMDAPPersonAttributes.hasLicense.toString(), true); // for CEMDAP's "driversLicence" variable
			person.getAttributes().putAttribute(CEMDAPPersonAttributes.gender.toString(), gender.name()); // for CEMDAP's "female" variable
			person.getAttributes().putAttribute(CEMDAPPersonAttributes.age.toString(), getAgeInBounds(lowerAgeBound, upperAgeBound));
			person.getAttributes().putAttribute(CEMDAPPersonAttributes.parent.toString(), false);

			this.population.addPerson(person);

			List<Id<Person>> personIds = new ArrayList<>(); // Does in current implementation (only 1 p/hh) not make much sense
			personIds.add(personId);
			household.setMemberIds(personIds);
			this.households.put(householdId, household);
		}
	}


	private static void scaleRelations(Map<String, CommuterRelationV2> relationsFromMunicipality, int employeesMale,
			int employeesFemale, double defaultEmployeesToCommutersRatio) {
		// Count all commuters starting in the given municipality
		int commutersMale = 0;
		for (CommuterRelationV2 relation : relationsFromMunicipality.values()) {
			if (relation.getTripsMale() == null) { // This is the case when there are very few people traveling on that relation
				if (relation.getTrips() == null || relation.getTrips() == 0) {
					throw new RuntimeException("No travellers at all on this relation! This should not happen.");
				} else {
					relation.setTripsMale((relation.getTrips() / 2));
				}
			}
			commutersMale += relation.getTripsMale();
		}
		int commutersFemale = 0;
		for (CommuterRelationV2 relation : relationsFromMunicipality.values()) {
			if (relation.getTripsFemale() == null) { // This is the case when there are very few people traveling on that relation
				if (relation.getTrips() == null || relation.getTrips() == 0) {
					throw new RuntimeException("No travellers at all on this relation! This should not happen.");
				} else {
					relation.setTripsFemale((relation.getTrips() / 2));
				}
			}
			commutersFemale += relation.getTripsFemale();
		}

		// Compute ratios
		double employeesToCommutersMaleRatio;
		double employeesToCommutersFemaleRatio;
		if (employeesMale != 0) {
			employeesToCommutersMaleRatio = (double) employeesMale / (double) commutersMale;
		} else {
			employeesToCommutersMaleRatio = defaultEmployeesToCommutersRatio;
		}
		if (employeesFemale != 0) {
			employeesToCommutersFemaleRatio = (double) employeesFemale / (double) commutersFemale;
		} else {
			employeesToCommutersFemaleRatio = defaultEmployeesToCommutersRatio;
		}

		// Scale
		for (CommuterRelationV2 relation : relationsFromMunicipality.values()) {
			relation.setTripsMale((int) Math.ceil(relation.getTripsMale() * employeesToCommutersMaleRatio));
		}
		for (CommuterRelationV2 relation : relationsFromMunicipality.values()) {
			relation.setTripsFemale((int) Math.ceil(relation.getTripsFemale() * employeesToCommutersFemaleRatio));
		}
	}


	private static List<String>  createRelationList(Map<String, CommuterRelationV2> relationsFromMunicipality, Gender gender) {
		List<String> commuterRelationsList = new ArrayList<>();
		for (String destination : relationsFromMunicipality.keySet()) {
			int trips;
			if (gender.equals(Gender.male)) {
				trips = relationsFromMunicipality.get(destination).getTripsMale();
			} else if (gender.equals(Gender.female)) {
				trips = relationsFromMunicipality.get(destination).getTripsFemale();
			} else {
				throw new IllegalArgumentException("Must either be male or female.");
			}
			for (int i = 0; i < trips ; i++) {
				commuterRelationsList.add(destination);
			}
		}
		return commuterRelationsList;
	}


	private static String getWorkMunicipalityFromCommuterRelationList(List<String> commuterRelationList) {
		int position = random.nextInt(commuterRelationList.size());
		String workMunicipalityId = commuterRelationList.get(position);
		commuterRelationList.remove(position);
		return workMunicipalityId;
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


	private static boolean getEmployed(double adultsToEmployeesRatio) {
		return random.nextDouble() * adultsToEmployeesRatio < 1;
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


	private void writeCemdapHouseholdsFile(Map<Id<Household>, Household> households, String fileName) {
		BufferedWriter bufferedWriterHouseholds = null;

		try {
			bufferedWriterHouseholds = IOUtils.getBufferedWriter(fileName);

    		for (Household household : households.values()) {
    			int householdId = Integer.parseInt(household.getId().toString());
    			int numberOfAdults = (Integer) household.getAttributes().getAttribute(CEMDAPHouseholdAttributes.numberOfAdults.toString());
    			int totalNumberOfHouseholdVehicles = (Integer) household.getAttributes().getAttribute(CEMDAPHouseholdAttributes.totalNumberOfHouseholdVehicles.toString());
				int homeTSZLocation = Integer.valueOf( household.getAttributes().getAttribute(CEMDAPHouseholdAttributes.homeTSZLocation.toString()).toString() );
    			int numberOfChildren = (Integer) household.getAttributes().getAttribute(CEMDAPHouseholdAttributes.numberOfChildren.toString());
    			int householdStructure = (Integer) household.getAttributes().getAttribute(CEMDAPHouseholdAttributes.householdStructure.toString());

    			// Altogether this creates 32 columns = number in query file
    			bufferedWriterHouseholds.write(householdId + "\t" + numberOfAdults + "\t" + totalNumberOfHouseholdVehicles
    					+ "\t" + homeTSZLocation + "\t" + numberOfChildren + "\t" + householdStructure + "\t" + 0
    					+ "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0
    					+ "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0
    					+ "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0
    					+ "\t" + 0);
    			bufferedWriterHouseholds.newLine();
    		}

    	} catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (bufferedWriterHouseholds != null) {
                    bufferedWriterHouseholds.flush();
                    bufferedWriterHouseholds.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
		LOG.info("Households file " + fileName + " written.");
    }


	private void writeCemdapPersonsFile(Population population, String fileName) {
		BufferedWriter bufferedWriterPersons = null;

		try {
			bufferedWriterPersons = IOUtils.getBufferedWriter(fileName);

			for (Person person : population.getPersons().values()) {
				Attributes attr = person.getAttributes();
				int householdId = Integer.parseInt(attr.getAttribute(CEMDAPPersonAttributes.householdId.toString()).toString());
				int personId = Integer.parseInt(person.getId().toString());

				int employed;
				if ((boolean) attr.getAttribute(CEMDAPPersonAttributes.employed.toString())) {
					employed = 1;
				} else {
					employed = 0;
				}

				int student;
				if ((boolean) attr.getAttribute(CEMDAPPersonAttributes.student.toString())) {
					student = 1;
				} else {
					student = 0;
				}

				int driversLicence;
				if ((boolean) attr.getAttribute(CEMDAPPersonAttributes.hasLicense.toString())) {
					driversLicence = 1;
				} else {
					driversLicence = 0;
				}

				int locationOfWork = Integer.parseInt(attr.getAttribute(CEMDAPPersonAttributes.locationOfWork.toString()).toString());
				int locationOfSchool = Integer.parseInt(attr.getAttribute(CEMDAPPersonAttributes.locationOfSchool.toString()).toString());

				int female;
				if (Gender.valueOf((String) attr.getAttribute(CEMDAPPersonAttributes.gender.toString())) == Gender.male) {
					female = 0;
				} else if (Gender.valueOf((String) attr.getAttribute(CEMDAPPersonAttributes.gender.toString())) == Gender.female) {
					female = 1;
				} else {
					throw new IllegalArgumentException("Gender must either be male or female.");
				}

				int age = (Integer) attr.getAttribute(CEMDAPPersonAttributes.age.toString());

				int parent;
				if ((boolean) attr.getAttribute(CEMDAPPersonAttributes.parent.toString())) {
					parent = 1;
				} else {
					parent = 0;
				}

				// Altogether this creates 59 columns = number in query file
				bufferedWriterPersons.write(householdId + "\t" + personId + "\t" + employed  + "\t" + student
						+ "\t" + driversLicence + "\t" + locationOfWork + "\t" + locationOfSchool
						+ "\t" + female + "\t" + age + "\t" + parent + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0
						+ "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0
						+ "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0
						+ "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0
						+ "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0
						+ "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0
						+ "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0 );
				bufferedWriterPersons.newLine();
			}
		} catch (IOException ex) {
	        ex.printStackTrace();
	    } finally {
	        try {
	            if (bufferedWriterPersons != null) {
	                bufferedWriterPersons.flush();
	                bufferedWriterPersons.close();
	            }
	        } catch (IOException ex) {
	            ex.printStackTrace();
	        }
	    }
		LOG.info("Persons file " + fileName + " written.");
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

	public void setWriteCemdapInputFiles(boolean writeCemdapInputFiles) {
    	this.writeCemdapInputFiles = writeCemdapInputFiles;
    }

	public void setWriteMatsimPlanFiles(boolean writeMatsimPlanFiles) {
    	this.writeMatsimPlanFiles = writeMatsimPlanFiles;
    }

    public void setIncludeChildren(boolean includeChildren) {
    	this.includeChildren = includeChildren;
    }

    public List<Population> getAllPopulations() {
    	if (!memorizeAllPopulations) {
    		throw new RuntimeException("The corresponding container object has not been filles. 'memorizeAllPopulations' needs to be activated.");
    	}
    	return this.allPopulations;
	}

    public void setMemorizeAllPopulations(boolean memorizeAllPopulations) {
    	this.memorizeAllPopulations = memorizeAllPopulations;
    }
}
