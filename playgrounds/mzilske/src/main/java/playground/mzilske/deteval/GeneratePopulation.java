/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.mzilske.deteval;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import org.matsim.households.Household;
import org.matsim.households.Households;
import org.matsim.households.HouseholdsImpl;
import org.matsim.households.HouseholdsWriterV10;
import org.matsim.households.Income.IncomePeriod;
import org.matsim.population.algorithms.PlanMutateTimeAllocationSimplified;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;
import org.opengis.feature.simple.SimpleFeature;

import playground.mzilske.deteval.Case.Car;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

public class GeneratePopulation {

	private static Logger logger = Logger.getLogger(GeneratePopulation.class);

	private static final String MID_PERSONENDATENSATZ = "../detailedEval/eingangsdaten/MidMUC_2002/MiD2002_Personendatensatz_MUC.csv";

	private static final String MID_HAUSHALTSDATENSATZ = "../detailedEval/eingangsdaten/MidMUC_2002/MiD2002_Haushaltsdatensatz_MUC.csv";

	private static final String MID_WEGEDATENSATZ = "../detailedEval/eingangsdaten/MidMUC_2002/MiD2002_Wegedatensatz_MUC.csv";

	private static final String MID_WEGEKODIERUNG = "../detailedEval/eingangsdaten/MidMUC_2002/Mid2002_Wegekodierung_MUC-LH.csv";

	private static final String MID_VERKEHRSZELLEN = "../detailedEval/Net/shapeFromVISUM/Verkehrszellen_Umrisse_zone.SHP";

	private static final String PLANS = "../detailedEval/pop/befragte-personen/plans.xml";

	private static final String HOUSEHOLDS_FILE = "../detailedEval/pop/befragte-personen/households.xml";

	private static final String CLONED_PLANS = "../detailedEval/pop/140k-synthetische-personen/plans.xml.gz";

	private static final String CLONED_HOUSEHOLDS_FILE = "../detailedEval/pop/140k-synthetische-personen/households.xml.gz";

	private static final Integer NUMBER_OF_SIMULATED_PEOPLE = 140000;

	private static final String VEHICLE_FILE = "../detailedEval/pop/befragte-personen/vehicles.xml";

	private static final String CLONED_VEHICLE_FILE = "../detailedEval/pop/140k-synthetische-personen/vehicles.xml.gz";
	
	private Random random = new Random();

	private Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

	private Map<String, Case> cases = new HashMap<String, Case>();

	private Households households = new HouseholdsImpl();

	private Vehicles vehicles = VehicleUtils.createVehiclesContainer();

	private Map<Id, Person> persons = new HashMap<Id, Person>();

	private Map<Id<Person>, Plan> plans = new HashMap<>();

	private Map<Integer, SimpleFeature> verkehrszellen = new HashMap<Integer, SimpleFeature>();

	private Map<Activity, Integer> activity2verkehrszelle = new HashMap<Activity, Integer>();

	public static void main(String[] args) throws IOException {
		// generateSurveyPopulation();
		generateFullPopulation();
	}

	private static void generateSurveyPopulation() throws IOException {
		GeneratePopulation generatePopulation = new GeneratePopulation();
		generatePopulation.parseVerkehrszellen();
		generatePopulation.parseHouseholds();
		generatePopulation.parsePersons();
		generatePopulation.parsePlans();
		generatePopulation.addPlans();
		generatePopulation.dropPlanlessPeople();
		generatePopulation.addPopulationToScenario();
		generatePopulation.addHouseholds();
		generatePopulation.assertAllDriversHaveCar();
		generatePopulation.writeHouseholds(HOUSEHOLDS_FILE);
		generatePopulation.writeVehicles(VEHICLE_FILE);
		generatePopulation.writePlans(PLANS);
	}

	private void assertAllDriversHaveCar() {
		for (Person person : scenario.getPopulation().getPersons().values()) {
			if (CarAssigner.wantsCar(person)) {
				if (!vehicles.getVehicles().containsKey(person.getId())) {
					throw new RuntimeException("Didn't generate a car for person "+person.getId());
				}
			}
		}
	}

	private static void generateFullPopulation() throws IOException {
		GeneratePopulation generatePopulation = new GeneratePopulation();
		generatePopulation.parseVerkehrszellen();
		generatePopulation.parseHouseholds();
		generatePopulation.parsePersons();
		generatePopulation.parsePlans();
		generatePopulation.addPlans();
		generatePopulation.dropPlanlessPeople();
		generatePopulation.multiplyPopulation();
		generatePopulation.addPopulationToScenario();
		generatePopulation.mutateTimes();
		generatePopulation.addHouseholds();
		generatePopulation.assertAllDriversHaveCar();
		generatePopulation.writeHouseholds(CLONED_HOUSEHOLDS_FILE);
		generatePopulation.writeVehicles(CLONED_VEHICLE_FILE);
		generatePopulation.writePlans(CLONED_PLANS);
	}

	private void mutateTimes() {
		boolean affectingDuration = true ;

		PlanMutateTimeAllocationSimplified planMutateTimeAllocation = new PlanMutateTimeAllocationSimplified(15 * 60, affectingDuration, random);

//		planMutateTimeAllocation.setUseActivityDurations(false);
		// replaced this by completely separate class.  results will not be fully backwards compatible.  kai, jun'12

		for (Person person : scenario.getPopulation().getPersons().values()) {
			Plan plan = person.getPlans().iterator().next();
			planMutateTimeAllocation.run(plan);
		}
	}

	private void addHouseholds() throws IOException {
		for (String caseid : cases.keySet()) {
			Id<Household> householdId = Id.create(caseid, Household.class);
			Household household = households.getFactory().createHousehold(householdId);
			Case caze = cases.get(caseid);
			for (Person person : caze.members) {
				household.getMemberIds().add(person.getId());
			}
			Set<Id<Person>> peopleWhoNeedCar = new HashSet<>();
			for (Person person : caze.members) {
				if (CarAssigner.wantsCar(person)) {
					peopleWhoNeedCar.add(person.getId());
				}
			}
			int nCar = 0;
			// TODO: Give people who are the primary user of a car but don't seem to be using it a car anyway.
			// TODO: Then: Let the subtourmodechoice only select car legs for people who have a car.
			// TODO: Later: Create only as many cars as households report on owning, let members compete for them.
			for (Car car : caze.cars) {
				Id<Vehicle> vehicleId;
				Id<Person> primaryUserPersonId;
				if (car.primary_user >= 0 && car.primary_user < household.getMemberIds().size()) {
					primaryUserPersonId = household.getMemberIds().get(car.primary_user);
				} else {
					primaryUserPersonId = null;
				}
				if (primaryUserPersonId != null && peopleWhoNeedCar.contains(primaryUserPersonId)) {
					vehicleId = Id.create(primaryUserPersonId, Vehicle.class);
				} else if (!peopleWhoNeedCar.isEmpty()) {
					vehicleId = Id.create(peopleWhoNeedCar.iterator().next(), Vehicle.class);
				} else {
					vehicleId = Id.create(caseid + "." + "additional_car_" + nCar, Vehicle.class);
				}
				Vehicle vehicle = vehicles.getFactory().createVehicle(vehicleId, vehicleType(car.baujahr, car.antriebsart, car.hubraum));
				vehicles.addVehicle( vehicle);
				household.getVehicleIds().add(vehicle.getId());
				peopleWhoNeedCar.remove(vehicleId);
				++nCar;
			}
			for (Id<Person> personWhoNeedsCar : peopleWhoNeedCar) {
				VehicleType vehicleType;
				if (household.getVehicleIds().isEmpty()) {
					vehicleType = defaultVehicleType();
				} else {
					Vehicle vehicleToClone = vehicles.getVehicles().get(household.getVehicleIds().iterator().next());
					vehicleType = vehicleToClone.getType();
				}
				
				Vehicle vehicle = vehicles.getFactory().createVehicle(Id.create(personWhoNeedsCar, Vehicle.class), vehicleType);
				vehicles.addVehicle( vehicle);
				household.getVehicleIds().add(vehicle.getId());
				++nCar;
			}
			household.setIncome(households.getFactory().createIncome(caze.income, IncomePeriod.month));
			households.getHouseholds().put(householdId, household);
		}
	}

	private void writeHouseholds(String householdsFile) {
		HouseholdsWriterV10 writer = new HouseholdsWriterV10(households);
		writer.writeFile(householdsFile);
	}

	private void writeVehicles(String vehicleFile) {
		VehicleWriterV1 writer = new VehicleWriterV1(vehicles);
		writer.writeFile(vehicleFile);
	}

	private VehicleType vehicleType(int baujahr, int antriebsart, int hubraum) {
		Id<VehicleType> vehicleTypeId = Id.create(baujahr+"_"+antriebsart+"_"+hubraum, VehicleType.class);
		VehicleType vehicleType = vehicles.getVehicleTypes().get(vehicleTypeId);
		if (vehicleType == null) {
			vehicleType = vehicles.getFactory().createVehicleType(vehicleTypeId);
			vehicleType.setDescription("Baujahr:"+baujahr+";Antriebsart:"+antriebsart+";Hubraum:"+hubraum);
			vehicles.addVehicleType( vehicleType);
		}
		return vehicleType;
	}

	private VehicleType defaultVehicleType() {
		Id<VehicleType> defaultVehicleTypeId = Id.create("default", VehicleType.class);
		VehicleType defaultVehicleType = vehicles.getVehicleTypes().get(defaultVehicleTypeId);
		if (defaultVehicleType == null) {
			defaultVehicleType = vehicles.getFactory().createVehicleType(defaultVehicleTypeId);
			defaultVehicleType.setDescription("default");
			vehicles.addVehicleType( defaultVehicleType);
		}
		return defaultVehicleType;
	}

	private void dropPlanlessPeople() {
		logger.info("There are " + persons.size() + " good-looking people.");
		int mPeople = 0;
		for (Case caze : cases.values()) {
			mPeople += caze.members.size();
		}
		logger.info("We have " + mPeople + " in households.");
		Iterator<Map.Entry<String, Case>> i = cases.entrySet().iterator();
		while (i.hasNext()) {
			Map.Entry<String, Case> household = i.next();
			Collection<Person> members = household.getValue().members;
			Iterator<Person> ii = members.iterator();
			while (ii.hasNext()) {
				Person person = ii.next();
				if (person.getPlans().isEmpty()) {
					ii.remove();
					persons.remove(person.getId());
				}
			}
			if (members.isEmpty()) {
				i.remove();
			}
		}
		logger.info("There are " + persons.size() + " good-looking people.");
		int nPeople = 0;
		for (Case caze : cases.values()) {
			nPeople += caze.members.size();
		}
		logger.info("We have " + nPeople + " in households.");
	}

	private void addPopulationToScenario() {
		for (Case haushalt : cases.values()) {
			for (Person person : haushalt.members) {
				scenario.getPopulation().addPerson(person);
			}
		}
	}

	private void parseVerkehrszellen() {
		Collection<SimpleFeature> fts = ShapeFileReader.getAllFeatures(MID_VERKEHRSZELLEN);
		System.out.println(fts.size());
		for (SimpleFeature feature : fts) {
			Integer no = (Integer) feature.getAttribute("NO");
			verkehrszellen.put(no, feature);
		}
	}

	private static Point getRandomPointInFeature(Random rnd, SimpleFeature ft) {
		Point p = null;
		double x, y;
		do {
			x = ft.getBounds().getMinX() + rnd.nextDouble() * (ft.getBounds().getMaxX() - ft.getBounds().getMinX());
			y = ft.getBounds().getMinY() + rnd.nextDouble() * (ft.getBounds().getMaxY() - ft.getBounds().getMinY());
			p = MGC.xy2Point(x, y);
		} while (!((Geometry) ft.getDefaultGeometry()).contains(p));
		return p;
	}

	private void addPlans() {
		logger.info("Got " + plans.size() + " plans.");
		for (Map.Entry<Id<Person>, Plan> entry : plans.entrySet()) {
			Id<Person> personId = entry.getKey();
			Plan plan = entry.getValue();
			boolean isGood = true;
			for (PlanElement planElement : plan.getPlanElements()) {
				if (planElement instanceof Activity) {
					Activity activity = (Activity) planElement;
					if (activity.getCoord() == null) {
						logger.trace("Dumped a plan because of a coordinateless activity.");
						isGood = false;
					} else if (activity.getEndTime() == 362340) { //  99:99 Uhr
						logger.trace("Dumped a plan because of invalid activity time.");
						isGood = false;
					}
				}
			}
			if (isGood) {
				Person person = persons.get(personId);
				if (person != null) {
					person.addPlan(plan);
				} else {
					logger.warn("Plan for a person without a person record: " + personId);
				}
			}
		}
	}

	private void parsePlans()
	throws IOException {
		final List<String[]> wegekodierungRows = new ArrayList<String[]>();
		final List<String[]> wegedatensatzRows = new ArrayList<String[]>();
		TabularFileParserConfig tabFileParserConfig = new TabularFileParserConfig();
		tabFileParserConfig.setFileName(MID_WEGEKODIERUNG);
		tabFileParserConfig.setDelimiterTags(new String[] {";"});
		new TabularFileParser().parse(tabFileParserConfig, new CheckingTabularFileHandler() {

			@Override
			public void startRow(String[] row) {
				check(row);
				if(!first) {
					wegekodierungRows.add(row);
				} else {
					// This is the header. Nothing to do.
				}
				first = false;
			}

		});
		tabFileParserConfig.setFileName(MID_WEGEDATENSATZ);
		new TabularFileParser().parse(tabFileParserConfig, new CheckingTabularFileHandler() {

			@Override
			public void startRow(String[] row) {
				check(row);
				if(!first) {
					wegedatensatzRows.add(row);
				} else {
					// This is the header. Nothing to do.
				}
				first = false;
			}

		});

		Iterator<String[]> iKodierung = wegekodierungRows.iterator();
		Iterator<String[]> iDatensatz = wegedatensatzRows.iterator();
		while (iKodierung.hasNext()) {
			String[] kRow = iKodierung.next();
			String[] dRow = iDatensatz.next();
			parseAndAddLeg(kRow, dRow);
		}
	}


	private static final int K_CASEID = 0;

	private static final int K_PID = 1;

	private static final int K_VONVBEZ = 6;

	private static final int K_NACHVBEZ = 7;

	private static final int D_W03_HS = 51;

	private static final int D_W03_MS = 52;

	private static final int D_W04 = 6; // Wegzweck

	private static final int D_W05 = 61; // Hauptverkehrsmittel

	private static final int D_WEGDAUER = 56;

	private static final int[] H_H0412 = new int[] {53, 71, 89};

	private static final int[] H_H048 = new int[] {59, 77, 95};

	private static final int[] H_H0410 = new int[] {63, 81, 99};

	private static final int[] H_H044 = new int[] {55, 73, 91};

	private static final int[] H_H045 = new int[] {56, 74, 92};

	private Random rnd = new Random();

	private void parseAndAddLeg(String[] kRow, String[] dRow) {
		PopulationFactory factory = scenario.getPopulation().getFactory();
		String caseid = kRow[K_CASEID];
		String pid = kRow[K_PID];
		Id personId = createPersonId(caseid, pid);
		Plan plan;
		Activity previousActivity;
		if (!plans.containsKey(personId)) {
			plan = factory.createPlan();
			plans.put(personId, plan);
			String cellNumberString = kRow[K_VONVBEZ];
			int cellNumber = Integer.parseInt(cellNumberString);
			Coord coord = createCentroidCoordIfAvailable(cellNumber);
			Activity firstHomeActivity = factory.createActivityFromCoord("home", coord);
			activity2verkehrszelle.put(firstHomeActivity, cellNumber);
			plan.addActivity(firstHomeActivity);
			previousActivity = firstHomeActivity;
		} else {
			plan = plans.get(personId);
			previousActivity = lastActicity(plan);
		}
		int h = Integer.parseInt(dRow[D_W03_HS]);
		int m = Integer.parseInt(dRow[D_W03_MS]);
		int endTimeInSeconds = m * 60 + h * 60 * 60;
		previousActivity.setEndTime(endTimeInSeconds);
		Leg leg = factory.createLeg(parseLegMode(dRow[D_W05]));
		final double travelTime = Double.parseDouble(dRow[D_WEGDAUER]);
		if (travelTime > 999990) {
			logger.trace("Zeit falsch.");
		} else {
			leg.setTravelTime(travelTime * 60);
		}
		plan.addLeg(leg);
		String cellNumberString = kRow[K_NACHVBEZ];
		int cellNumber = Integer.parseInt(cellNumberString);
		Coord coord = createCentroidCoordIfAvailable(cellNumber);
		Activity activity = factory.createActivityFromCoord(parseActivityType(dRow[D_W04], plan), coord);
		activity2verkehrszelle.put(activity, cellNumber);
		plan.addActivity(activity);
	}

	private String parseLegMode(String hauptverkehrsmittel) {
		if (hauptverkehrsmittel.equals("1")) {
			return TransportMode.walk;
		} else if (hauptverkehrsmittel.equals("2")) {
			return TransportMode.bike;
		} else if (hauptverkehrsmittel.equals("3")) {
			// Mofa, Moped
			return TransportMode.car;
		} else if (hauptverkehrsmittel.equals("4")) {
			// Motorrad
			return TransportMode.car;
		} else if (hauptverkehrsmittel.equals("5")) {
			// Mitfahrer
			return TransportMode.ride;
		} else if (hauptverkehrsmittel.equals("8")) {
			// PT
			// return TransportMode.pt;
			return TransportMode.pt;
		} else if (hauptverkehrsmittel.equals("6")) {
			return TransportMode.car;
		} else if (hauptverkehrsmittel.equals("7")) {
			// LKW
			return TransportMode.car;
		} else if (hauptverkehrsmittel.equals("9")) {
			// Taxi
			return TransportMode.ride;
		} else if (hauptverkehrsmittel.equals("10")) {
			// Schiff, Bahn, Bus, Flugzeug
			return TransportMode.pt;
		} else if (hauptverkehrsmittel.equals("11")) {
			// other
			return "undefined";
		} else if (hauptverkehrsmittel.equals("97")) {
			return "undefined";
		} else {
			logger.warn(hauptverkehrsmittel);
			return "undefined";
		}
	}

	private String parseActivityType(String wegzweck, Plan plan) {
		if (wegzweck.equals("1")) {
			return "work";
		} else if (wegzweck.equals("2")) {
			return "business";
		} else if (wegzweck.equals("3")) {
			return "education";
		} else if (wegzweck.equals("4")) {
			return "shopping";
		} else if (wegzweck.equals("5")) {
			return "private";
		} else if (wegzweck.equals("6")) {
			return "pickup";
		} else if (wegzweck.equals("7")) {
			return "leisure";
		} else if (wegzweck.equals("8")) {
			return "home";
		} else if (wegzweck.equals("9")) {
			Activity previousActivity = previousActivity(plan);
			return previousActivity.getType();
		} else if (wegzweck.equals("10")) {
			return "other";
		} else if (wegzweck.equals("11")) {
			return "with adult";
		} else if (wegzweck.equals("31")) {
			return "education";
		} else if (wegzweck.equals("32")) {
			return "education";
		} else if (wegzweck.equals("32")) {
			return "education";
		} else if (wegzweck.equals("40")) {
			return "sports";
		} else if (wegzweck.equals("41")) {
			return "friends";
		} else if (wegzweck.equals("97")) {
			return "unknown";
		} else if (wegzweck.equals("98")) {
			return "unknown";
		} else if (wegzweck.equals("99")) {
			return "unknown";
		} else {
			return "unknown";
		}
	}

	private Activity previousActivity(Plan plan) {
		Activity previousActivity;
		int nPlanElements = plan.getPlanElements().size();
		if (nPlanElements >= 4) {
			previousActivity = (Activity) plan.getPlanElements().get(nPlanElements - 4);
		} else {
			previousActivity = (Activity) plan.getPlanElements().get(nPlanElements - 2);
			logger.warn("Bad round trip.");
		}
		return previousActivity;
	}

	private Activity lastActicity(Plan plan) {
		int nPlanElements = plan.getPlanElements().size();
		Activity lastActivity = (Activity) plan.getPlanElements().get(nPlanElements - 1);
		return lastActivity;
	}

	private void parseHouseholds() throws IOException {
		TabularFileParserConfig tabFileParserConfig = new TabularFileParserConfig();
		tabFileParserConfig.setFileName(MID_HAUSHALTSDATENSATZ);
		tabFileParserConfig.setDelimiterTags(new String[] {";"});
		new TabularFileParser().parse(tabFileParserConfig, new CheckingTabularFileHandler() {

			private static final int CASEID = 0;

			private static final int H_H07 = 120;

			private static final int H_H071 = 121; // Einkommen in DM

			private static final int H_H072 = 125; // Einkommen in Euro

			protected static final int H_H04_3 = 108;

			@Override
			public void startRow(String[] row) {
				check(row);
				if(!first) {
					parseAndAddHousehold(scenario, row);
				} else {
					// This is the header. Nothing to do.
				}
				first = false;
			}

			private void parseAndAddHousehold(Scenario scenario, String[] row) {
				Case household = new Case();
				String caseid = row[CASEID];
				int DMorEuro = Integer.parseInt(row[H_H07]);
				if (DMorEuro == 1) {
					int incomeInTDM = Integer.parseInt(row[H_H071]);
					if (incomeInTDM > 0 && incomeInTDM < 95) {
						household.income = (incomeInTDM * 1000) / 2; // TDM to Euro
					} else {
						household.income = -1;
					}
				} else if (DMorEuro == 2) {
					int incomeInTEur = Integer.parseInt(row[H_H072]);
					if (incomeInTEur > 0 && incomeInTEur < 95) {
						household.income = (incomeInTEur * 1000); // TDM to Euro
					} else {
						household.income = -1;
					}
				} else {
					household.income = -1;
				}
				int nCars = Integer.parseInt(row[H_H04_3]);
				if (nCars > 3) {
					logger.error("nCars = "+nCars+", don't know what to do with values > 3");
					nCars = 3;
				}
				for (int i=0; i<nCars; i++) {
					Car car = new Car();
					car.hubraum = Integer.parseInt(row[H_H0410[i]]);
					car.baujahr = Integer.parseInt(row[H_H0412[i]]);
					car.antriebsart = Integer.parseInt(row[H_H048[i]]);
					car.primary_user = Integer.parseInt(row[H_H045[i]]);
					household.cars.add(car);
				}
				cases.put(caseid, household);
			}

		});
	}

	private void parsePersons()
	throws IOException {
		TabularFileParserConfig tabFileParserConfig = new TabularFileParserConfig();
		tabFileParserConfig.setFileName(MID_PERSONENDATENSATZ);
		tabFileParserConfig.setDelimiterTags(new String[] {";"});
		new TabularFileParser().parse(tabFileParserConfig, new CheckingTabularFileHandler() {

			private static final int CASEID = 0;

			private static final int PID = 1;

			private static final int PALTER = 112;

			private static final int PSEX = 113;

			@Override
			public void startRow(String[] row) {
				check(row);
				if(!first) {
					parseAndAddPerson(scenario, row);
				} else {
					// This is the header. Nothing to do.
				}
				first = false;
			}

			private void parseAndAddPerson(final Scenario scenario, String[] row) {
				String caseid = row[CASEID];
				String pid = row[PID];
				Id id = createPersonId(caseid, pid);
				Person person = scenario.getPopulation().getFactory().createPerson(id);
				String palter = row[PALTER];
				if (palter.equals("997")) {
					// Verweigert
				} else if (palter.equals("998")) {
					// Weiss nicht
				} else if (palter.equals("999")) {
					// Keine Angabe
				} else {
					PersonUtils.setAge(person, Integer.parseInt(palter));
				}
				String psex = row[PSEX];
				if (psex.equals("1")) {
					PersonUtils.setSex(person, "m");
				} else if (psex.equals("2")) {
					PersonUtils.setSex(person, "f");
				} else {
					// unknown
				}

				Case household = cases.get(caseid);
				household.members.add(person);
				persons.put(id, person);
			}

		});
	}

	private void multiplyPopulation() {
		Integer householdId = 0;
		Integer personId = 0;
		Map<String, Case> householdSeeds = new HashMap<String, Case>(cases);
		cases.clear();
		persons.clear();
		HashMap<String, Case> newCases = new HashMap<String, Case>();
		while (personId < NUMBER_OF_SIMULATED_PEOPLE) {
			for (Map.Entry<String, Case> haushalt : householdSeeds.entrySet()) {
				Integer homeCell = determineHomeCell(haushalt.getValue());
				Coord homeCoord = createRandomCoord(homeCell);
				Case haushaltCopy = new Case();
				haushaltCopy.income = haushalt.getValue().income;
				for (Person person : haushalt.getValue().members) {
					Person newPerson = copyPersonWithNewLocationsInSameCell(homeCoord, person, (personId++).toString());
					haushaltCopy.members.add(newPerson);
					persons.put(newPerson.getId(), newPerson);
				}
				haushaltCopy.cars = haushalt.getValue().cars;
				newCases.put((householdId++).toString(), haushaltCopy);
			}
		}
		cases.putAll(newCases);
	}

	private Integer determineHomeCell(Case haushalt) {
		for (Person person : haushalt.members) {
			for (Plan plan : person.getPlans()) {
				for (PlanElement planElement : plan.getPlanElements()) {
					if (planElement instanceof Activity) {
						Activity activity = (Activity) planElement;
						if (activity.getType().equals("home")) {
							Integer homeCell = activity2verkehrszelle.get(activity);
							if (homeCell != null) {
								return homeCell;
							}
						}
					}
				}
			}
		}
		throw new RuntimeException();
	}

	private Person copyPersonWithNewLocationsInSameCell(Coord homeCoord, Person person, String cloneId) {
		Person oldPerson = person;
		Person newPerson = scenario.getPopulation().getFactory().createPerson(Id.create(person.getId().toString() + "#" + cloneId, Person.class));
		PersonUtils.setAge(newPerson, PersonUtils.getAge(oldPerson));
		PersonUtils.setSex(newPerson, PersonUtils.getSex(oldPerson));
		for(Plan oldPlan : oldPerson.getPlans()) {
			Plan newPlan = scenario.getPopulation().getFactory().createPlan();
			for (PlanElement planElement : oldPlan.getPlanElements()) {
				if (planElement instanceof Activity) {
					Activity activity = (Activity) planElement;
					Coord activityCoord;
					if (activity.getType().equals("home")) {
						activityCoord = homeCoord;
					} else {
						activityCoord = createRandomCoord(activity2verkehrszelle.get(activity));
					}
					Activity newActivity = scenario.getPopulation().getFactory().createActivityFromCoord(activity.getType(), activityCoord);
					newActivity.setEndTime(activity.getEndTime());
					newPlan.addActivity(newActivity);
				} else if (planElement instanceof Leg) {
					Leg leg = (Leg) planElement;
					Leg newLeg = scenario.getPopulation().getFactory().createLeg(leg.getMode());
					newLeg.setTravelTime(leg.getTravelTime());
					newPlan.addLeg(newLeg);
				}
			}
			newPerson.addPlan(newPlan);
		}
		return newPerson;
	}

	private Coord createRandomCoord(Integer integer) {
		Point point = getRandomPointInFeature(rnd, verkehrszellen.get(integer));
		return scenario.createCoord(point.getX(), point.getY());
	}

	private Coord createCentroidCoordIfAvailable(Integer integer) {
		SimpleFeature verkehrszelle = verkehrszellen.get(integer);
		if (verkehrszelle != null) {
			Point point = ((Geometry) verkehrszelle.getDefaultGeometry()).getCentroid();
			return scenario.createCoord(point.getX(), point.getY());
		} else {
			return null;
		}
	}

	private void writePlans(String plansFile) {
		PopulationWriter populationWriter = new PopulationWriter(scenario.getPopulation(), scenario.getNetwork());
		populationWriter.write(plansFile);
	}

	private Id<Person> createPersonId(String caseid, String pid) {
		return Id.create(caseid + "." + pid, Person.class);
	}

}
