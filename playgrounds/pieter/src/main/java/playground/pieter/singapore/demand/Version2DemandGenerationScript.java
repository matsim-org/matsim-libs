package playground.pieter.singapore.demand;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.SQLException;
import java.util.Date;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.management.timer.Timer;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.*;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.MatsimFacilitiesReader;

import others.sergioo.util.dataBase.DataBaseAdmin;
import others.sergioo.util.dataBase.NoConnectionException;

public class Version2DemandGenerationScript {
	private static final double DEFAULT_LEISURE_TIME = Time
			.parseTime("01:00:00");
	private static final double DEFAULT_LEISURE_HOME_DEPARTTIME = Time
			.parseTime("09:00:00");
	private InputDataCollection inputData;
	private MutableScenario scenario;
	private DataBaseAdmin dba;
	private Properties diverseScriptProperties;
	private Logger scriptLog;

	private Version2DemandGenerationScript(String dbaProperties,
                                           String otherProperties, boolean deserialize,
                                           boolean facilitiesAllInOneXML) throws InstantiationException,
			IllegalAccessException, ClassNotFoundException, IOException,
			SQLException {
		this.dba = new DataBaseAdmin(new File(dbaProperties));
		this.diverseScriptProperties = new Properties();
		this.diverseScriptProperties.load(new FileInputStream(new File(
				otherProperties)));
		this.scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils
				.createConfig());
		scriptLog = Logger.getLogger("DemandGenScript");
		scriptLog.info("Starting.");
		if (deserialize) {
			this.deserializeInputdata();
			this.inputData.restoreIfDeserialized(this.scenario, this.dba,
					this.diverseScriptProperties);
			scriptLog.info("Deserialized the input data");
			inputData.facilitiesReader = new MatsimFacilitiesReader(
					this.scenario);
			inputData.loadFacilities(true);

		} else {
			loadInputData(facilitiesAllInOneXML);
			scriptLog.info("LOADED the input data");
			writeInputData();
			scriptLog.info("SERIALIZED the input data");
		}
		int numpax = this.inputData.getPersons().size();
		int numhhs = this.inputData.getHouseholds().size();
		int numfacilities = this.scenario.getActivityFacilities()
				.getFacilities().size();
		scriptLog.info(String.format(
				"\nSummary of input data:\n%45s:%8d\n%45s:%8d\n%45s:%8d",
				"Number of persons", numpax, "Number of households", numhhs,
				"Number of facilities", numfacilities));
	}

	private void deserializeInputdata() {
		FileInputStream fis;
		String fileName = this.diverseScriptProperties
				.getProperty("fullDataSerialFileName");
		try {
			fis = new FileInputStream(fileName);
			ObjectInputStream ois = new ObjectInputStream(fis);
			this.inputData = (InputDataCollection) ois.readObject();
			ois.close();
		} catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}
    }


	void run() {

		assignPrimaryActLocation();
		createPlans();
	}

	private void createPlans() {
		PopulationImpl pop = (PopulationImpl) scenario.getPopulation();
		pop.setIsStreaming(true);
		PopulationWriter popWriter = new PopulationWriter(pop,
				scenario.getNetwork());
		String plansFile = diverseScriptProperties.getProperty("plansFile");
		popWriter.startStreaming(plansFile);
		// popWriter.write(plansFile);
		PopulationFactory popFactory = pop.getFactory();
		for (PaxSG pax : this.inputData.persons.values()) {

			Person person = popFactory
					.createPerson(Id.createPersonId((long) pax.paxId));
			PersonUtils.setAge(person, pax.age);
			PersonUtils.setEmployed(person, !pax.occup.equals("NA"));
			// the ptmix plans allowed through should have car assigned too if
			// they have a license and have a car available
			PersonUtils.setCarAvail(person, pax.modeSuggestion.equals("car")
					|| pax.modeSuggestion.equals("ptmix")
					&& pax.carLicenseHolder && pax.household.carAvailability ? "always"
					: "never");
			PersonUtils.setLicence(person, PersonUtils.getCarAvail(person).equals("always") ? "yes"
					: null);
			PersonUtils.setSex(person, pax.sex);
			person.getCustomAttributes().put("income_pax", pax.income);
			person.getCustomAttributes().put("foreigner", pax.foreigner);
			person.getCustomAttributes().put("synth_hh_id",
					pax.household.synthHouseholdId);
			person.getCustomAttributes().put("occup", pax.occup);
			PlanImpl plan = (PlanImpl) popFactory.createPlan();
			// skip this guy if he doesn't have a chain
			if (pax.chain.equals("NA"))
				continue;
			StringTokenizer st = new StringTokenizer(pax.chain, "-");
			int actNumber = 1;
			int chainLength = st.countTokens();
			// need to refer to the previous act
			ActivityImpl lastAct = null;
			double dayStartTime = getHomeDepartTime(pax, chainLength);
			double dayEndTime = getDayEndTime(pax, chainLength);
			double lastActEndTime = 0;
			while (st.hasMoreElements()) {
				String actType = st.nextToken();
				ActivityImpl act = null;
				if (actType.equals("h")) {
					act = (ActivityImpl) popFactory.createActivityFromCoord(
							"home", null);
					act.setFacilityId(Id.create(pax.household.homeFacilityId, ActivityFacility.class));
					if (lastAct == null) {
						act.setEndTime(dayStartTime);
					} else if (actNumber < chainLength) {
						// inbetween home act, stay for an hour
						act.setEndTime(getEndTime(lastActEndTime, dayEndTime,
								3600, chainLength, actNumber));
					}
					// no further elses; this is the last home act for the day
				}
				if (actType.equals("w")) {
					act = (ActivityImpl) popFactory.createActivityFromCoord(
							pax.mainActType, null);
					act.setFacilityId(Id.create(pax.mainActFacility,ActivityFacility.class));
					if (pax.chainType.equals("workOnly")
							|| pax.chainType.equals("actAfterWork")) {
						act.setEndTime(pax.mainActStart + pax.mainActDur);
					} else {
						act.setEndTime(getEndTime(lastActEndTime, dayEndTime,
								(pax.mainActStart + pax.mainActDur),
								chainLength, actNumber));
					}
				}
				if (actType.equals("s")) {
					if (pax.chainType.equals("schoolAfterWork")) {
						act = (ActivityImpl) popFactory
								.createActivityFromCoord("s_tertiary", null);
						String destinationFacilityId = assignSchoolActivity(pax);
						act.setFacilityId(Id.create(destinationFacilityId,ActivityFacility.class));
						// default duration of 4 hours' school
						act.setEndTime(getEndTime(lastActEndTime, dayEndTime,
								Time.parseTime("02:00:00"), chainLength,
								actNumber));
					} else {
						act = (ActivityImpl) popFactory
								.createActivityFromCoord(pax.mainActType, null);
						act.setFacilityId(Id.create(pax.mainActFacility,ActivityFacility.class));
						// default duration of 4 hours' school
						act.setEndTime(getEndTime(lastActEndTime, dayEndTime,
								Time.parseTime("08:00:00"), chainLength,
								actNumber));

					}
				}
				if (actType.equals("b") || actType.equals("l")) {
					actType = assignSecondaryActivityType(actType, pax);
					act = (ActivityImpl) popFactory.createActivityFromCoord(
							actType, null);
					String destinationFacilityId = assignSecondaryActivityLocation(
							actType, pax, lastAct.getFacilityId());
					act.setFacilityId(Id.create(destinationFacilityId,ActivityFacility.class));
					if (pax.chainType.equals("secOnly")) {
						act.setEndTime(getSecondarActsEndTime(lastActEndTime,
								dayEndTime, chainLength, actNumber));
					} else {

						act.setEndTime(getEndTime(lastActEndTime, dayEndTime,
								DEFAULT_LEISURE_TIME, chainLength, actNumber));
					}
				}
				actNumber++;
				lastAct = act;
				lastActEndTime = act.getEndTime();
				plan.addActivity(act);
				if (st.hasMoreTokens()) {
					Leg currLeg;
					if (PersonUtils.hasLicense(person))
						currLeg = popFactory.createLeg("car");
					else
						currLeg = popFactory.createLeg("pt");
					plan.addLeg(currLeg);

				}
			}
			person.addPlan(plan);
			// pop.addPerson(person);
			popWriter.writePerson(person);
		}
		popWriter.closeStreaming();
	}


	private double getDayEndTime(PaxSG pax, int chainLength) {
		if (pax.chainType.equals("workOnly")
				|| pax.chainType.equals("actBeforeWork")
				|| pax.chainType.equals("actDuringWork"))
			return Math.min(pax.mainActStart + pax.mainActDur,
					InputDataCollection.LATEST_END_TIME);
		if (pax.chainType.equals("schoolOnly")) {
			String schoolType = pax.mainActType;
			int age = pax.age;
			if (schoolType.equals("s_primary"))
				return Time.parseTime("13:00:00") + randomTime(1800d);
			if (schoolType.equals("s_secondary"))
				return Time.parseTime("15:00:00") + randomTime(1800d);
			if (schoolType.equals("s_tertiary"))
				return Time.parseTime("18:30:00") + randomTime(1800d);
			if (schoolType.equals("s_other") && age < 10) {
				return Time.parseTime("13:00:00") + randomTime(1800d);
			} else {
				return Time.parseTime("15:00:00") + randomTime(1800d);
			}
		}
		if (pax.chainType.equals("secOnly"))
			return InputDataCollection.DEFAULT_END_TIME;
		if (pax.chainType.equals("schoolAfterWork"))
			return Math.min(pax.mainActStart + pax.mainActDur + 3600,
					InputDataCollection.LATEST_END_TIME);
		if (pax.chainType.equals("actAfterWork"))
			return Math.min(pax.mainActStart + pax.mainActDur
					+ (chainLength - 3) * 1800,
					InputDataCollection.LATEST_END_TIME);
		if (pax.chainType.equals("actAfterSchool")) {
			String schoolType = pax.mainActType;
			int age = pax.age;
			if (schoolType.equals("s_primary"))
				return Math.min(Time.parseTime("13:00:00") + randomTime(1800d)
						+ (chainLength - 3) * 1800,
						InputDataCollection.DEFAULT_END_TIME);
			if (schoolType.equals("s_secondary"))
				return Math.min(Time.parseTime("15:00:00") + randomTime(1800d)
						+ (chainLength - 3) * 1800,
						InputDataCollection.DEFAULT_END_TIME);
			if (schoolType.equals("s_tertiary"))
				return Math.min(Time.parseTime("18:30:00") + randomTime(1800d)
						+ (chainLength - 3) * 1800,
						InputDataCollection.LATEST_END_TIME);
			if (schoolType.equals("s_other") && age < 10) {
				return Math.min(Time.parseTime("13:00:00") + randomTime(1800d)
						+ (chainLength - 3) * 1800,
						InputDataCollection.DEFAULT_END_TIME);
			} else {
				return Math.min(Time.parseTime("15:00:00") + randomTime(1800d)
						+ (chainLength - 3) * 1800,
						InputDataCollection.DEFAULT_END_TIME);
			}
		}
		if (pax.chainType.equals("actDuringSchool"))
			return Time.parseTime("19:00:00") + randomTime(1800d);

		// default return value
		return InputDataCollection.DEFAULT_END_TIME;
	}

	private double getHomeDepartTime(PaxSG pax, int chainLength) {
		if (pax.chainType.equals("workOnly")
				|| pax.chainType.equals("actAfterWork")
				|| pax.chainType.equals("actDuringWork")
				|| pax.chainType.equals("schoolAfterWork"))
			return Math.max(pax.mainActStart - 600,
					InputDataCollection.EARLIEST_START_TIME);
		if (pax.chainType.equals("schoolOnly")
				|| pax.chainType.equals("actAfterSchool")
				|| pax.chainType.equals("actDuringSchool")) {
			String schoolType = pax.mainActType;
			int age = pax.age;
			if (schoolType.equals("s_primary")
					|| schoolType.equals("s_secondary"))
				return Time.parseTime("07:00:00") + randomTime(1800d);
			if (schoolType.equals("s_tertiary"))
				return Time.parseTime("07:30:00") + randomTime(1800d);
			if (schoolType.equals("s_other") && age < 10) {
				return Time.parseTime("07:30:00") + randomTime(1800d);
			} else {
				return Time.parseTime("08:00:00") + randomTime(1800d);
			}
		}
		if (pax.chainType.equals("secOnly"))
			return DEFAULT_LEISURE_HOME_DEPARTTIME + randomTime(3600d);

		if (pax.chainType.equals("actBeforeWork"))
			return Math.max(pax.mainActStart - (chainLength - 3) * 1800,
					InputDataCollection.EARLIEST_START_TIME);

		// default return value
		return InputDataCollection.DEFAULT_START_TIME;
	}

	private double randomTime(double d) {
		return Math.random() * d - d / 2d;
	}

	private double getEndTime(double lastActEndTime, double dayEndTime,
			double suggestedTime, int chainLength, int actNumber) {
		int actsToGo = chainLength - actNumber;
		double timeChunk = (dayEndTime - lastActEndTime) / actsToGo;
		return lastActEndTime + Math.min(timeChunk, suggestedTime);
	}

	private double getSecondarActsEndTime(double lastActEndTime,
			double dayEndTime, int chainLength, int actNumber) {
		int actsToGo = chainLength - actNumber;
		double availableTime = (dayEndTime - lastActEndTime)
				- DEFAULT_LEISURE_TIME * actsToGo;
		double timeChunk = Math.random() * availableTime;
		return lastActEndTime + timeChunk;
	}

	private void writeInputData() {
		FileOutputStream fos;
		try {
			fos = new FileOutputStream(
					this.diverseScriptProperties
							.getProperty("fullDataSerialFileName"));
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(this.inputData);
			oos.flush();
			oos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void assignPrimaryActLocation() {
		// for each work activity type
		int totalCount = 0;
		Date startDate = new Date();
		for (String actType : inputData.mainActivityTypes) {

			totalCount += inputData.mainActPaxCollection.get(actType).size();

		}
		scriptLog.info(String.format("Allocating %d primary activities",
				totalCount));
		int primeActors[] = new int[inputData.mainActivityTypes.size()];

		int actcounter = 0;
		int totalCounter = 0;
		// write the allocations to SQL
		try {
			dba.executeStatement("DROP TABLE IF EXISTS matsim2.main_act_locations;");
			dba.executeStatement("CREATE TABLE matsim2.main_act_locations (full_pop_pid int , facility_id varchar(255), distance double);");
		} catch (SQLException | NoConnectionException e1) {
			e1.printStackTrace();
		}
        for (String actType : inputData.mainActivityTypes) {
			int counter = 0;
			primeActors[actcounter] = inputData.mainActPaxCollection.get(
					actType).size();
			for (PaxSG p : inputData.mainActPaxCollection.get(actType)) {
				LocationSampler ls = inputData.locationSamplers.get(actType);
				int sampleSize = Integer.parseInt(this.diverseScriptProperties
						.getProperty("locationSampleSize"));
				Tuple<String[], double[]> locations = ls
						.sampleLocationsNoWeight(sampleSize);
//				HashMap<String, Double> landUseScaler = null;
				// only apply the scaling of attraction by land use given
				// occupation if this is work act
//				if (actType.startsWith("w"))
//					landUseScaler = inputData.landUseGivenOccupation
//							.get(p.occup);
				String[] ids = locations.getFirst();
				double[] samplingWeight = locations.getSecond();
				Tuple<Double,Double> gravityOutput = new Tuple<>(1.0, 0.0);
				// scale capacities of the set
				for (int i = 0; i < ids.length; i++) {
					String facilityLandUseType = inputData.facilityIdDescLookup
							.get(ids[i]);
					try {
						double scale = 1;

//						if (landUseScaler != null)
//							scale = landUseScaler.get(facilityLandUseType);
						double attenuatedCap = scale * samplingWeight[i];
						gravityOutput= getGravityFactor(p,
								p.household.homeFacilityId, ids[i], actType,
								attenuatedCap);
						samplingWeight[i] =gravityOutput.getFirst();
					} catch (NullPointerException e) {
						// String errMsg = String.format(
						// "No frequency for occup %s x land use %s.",
						// p.occup, facilityLandUseType);
						// scriptLog.error(errMsg);
						gravityOutput = getGravityFactor(p,
								p.household.homeFacilityId, ids[i], actType,
								0.001 * samplingWeight[i]);
						samplingWeight[i] =gravityOutput.getFirst();
					}

				}

				// do a weighted sampling for a single location
				p.mainActFacility = new LocationSampler(actType, ids,
						samplingWeight).sampleLocations(1).getFirst()[0];
				try {
					dba.executeStatement(String
							.format("INSERT INTO matsim2.main_act_locations VALUES(%d,\'%s\',%f)",
									p.paxId, p.mainActFacility,gravityOutput.getSecond()));
				} catch (SQLException | NoConnectionException e) {
					e.printStackTrace();
				}
                counter++;
				if ((primeActors[actcounter] % counter) == 0)
					scriptLog.info(String.format(
							"\t\t%6d of %6d persons allocated main act %s.",
							counter, primeActors[actcounter], actType));
				totalCounter++;
			}
			scriptLog.info(String.format(
					"%6d of %6d persons allocated main act %s.", counter,
					primeActors[actcounter], actType));
			Date currDate = new Date();
			long timePastLong = currDate.getTime() - startDate.getTime();
			double timePastSec = (double) timePastLong
					/ (double) Timer.ONE_SECOND;
			int agentsToGo = totalCount - totalCounter;
			double agentsPerSecond = (double) totalCounter / timePastSec;
			long timeToGo = (long) ((double) agentsToGo / agentsPerSecond);
			scriptLog
					.info(String
							.format("%6d of %8d persons done in %.3f seconds at %.3f agents/sec, %s sec to go.",
									totalCounter, totalCount, timePastSec,
									agentsPerSecond, timeToGo));

		}
	}

	private Tuple<Double, Double> getGravityFactor(PaxSG p, String originFacilityId,
			String destinationFacilityId, String actType, double capacity) {
		ActivityFacilityImpl origin = (ActivityFacilityImpl) this.scenario
				.getActivityFacilities().getFacilities().get(
						Id.create(originFacilityId,ActivityFacility.class));
		BasicLocation destination = this.scenario.getActivityFacilities()
				.getFacilities().get(Id.create(destinationFacilityId,ActivityFacility.class));
		double distance = origin.calcDistance(destination.getCoord());
		// double distance =
		// HITSAnalyser.getShortestPathDistance(home.getCoord(),
		// away.getCoord());
		boolean carOwner = p.household.carAvailability;
		String carOwnerString = carOwner ? "carOwner" : "nonCarOwner";
		double feta = 0;
		double beta = 0;
		if (actType.startsWith("w")) {
			feta = Double.parseDouble(diverseScriptProperties
					.getProperty(carOwnerString + "WorkFeta"));
			beta = Double.parseDouble(diverseScriptProperties
					.getProperty(carOwnerString + "WorkBeta"));
		} else if (actType.startsWith("s")) {
			feta = Double.parseDouble(diverseScriptProperties
					.getProperty(carOwnerString + "EduFeta"));
			beta = Double.parseDouble(diverseScriptProperties
					.getProperty(carOwnerString + "EduBeta"));
		} else {
			feta = Double.parseDouble(diverseScriptProperties
					.getProperty("otherTripsFeta"));
			beta = Double.parseDouble(diverseScriptProperties
					.getProperty("otherTripsBeta"));
		}
		if (feta > 0 && beta > 0) {
			return new Tuple<>(((feta * capacity) / Math.pow(distance, beta)),distance);
		}
		// default is to do no scaling
		return new Tuple<>(1.0, distance);
	}

	private String assignSecondaryActivityLocation(String activityType,
			PaxSG p, Id lastFacilityId) {
		LocationSampler ls = inputData.locationSamplers.get(activityType);
		int sampleSize = Integer.parseInt(this.diverseScriptProperties
				.getProperty("locationSampleSize"));
		Tuple<String[], double[]> locations = ls
				.sampleLocationsNoWeight(sampleSize);
		String[] ids = locations.getFirst();
		double[] samplingWeight = locations.getSecond();
		// do gravityAssignment
		for (int i = 0; i < ids.length; i++) {

			samplingWeight[i] = getGravityFactor(p, lastFacilityId.toString(),
					ids[i], activityType, samplingWeight[i]).getFirst();

		}

		// do a weighted sampling for a single location
		return new LocationSampler(activityType, ids, samplingWeight)
				.sampleLocations(1).getFirst()[0];

	}

	private String assignSecondaryActivityType(String activityGroup, PaxSG pax) {
		Tuple<String[], double[]> paxFreqs;
		// if (activityGroup.equals("l")) {
		// paxFreqs = inputData.leisureActFrequencies
		// .get(pax.household.incomeHousehold).get(pax.age)
		// .get(pax.occup);
		// } else {
		// paxFreqs = inputData.bizActFrequencies
		// .get(pax.household.incomeHousehold).get(pax.age)
		// .get(pax.occup);
		// }
		// String[] activityTypes = new String[paxFreqs.size()];
		// double[] activityFrequencies = new double[paxFreqs.size()];
		// Iterator<Entry<String, Double>> it=paxFreqs.entrySet().iterator();
		// int i = 0;
		// while (it.hasNext()) {
		// Entry<String, Double> e = it.next();
		// activityTypes[i] = e.getKey();
		// activityFrequencies[i] = e.getValue();
		// i++;
		// }
		if (activityGroup.equals("l"))
			paxFreqs = pax.leisureActFrequencies;
		else
			paxFreqs = pax.bizActFrequencies;
		return new LocationSampler(activityGroup, paxFreqs.getFirst(),
				paxFreqs.getSecond()).sampleLocations(1).getFirst()[0];
		// return null;
	}

	private String assignSchoolActivity(PaxSG p) {
		double actSelector = Math.random();
		String actType;
		if (actSelector > 0.8) {
			actType = "s_postsecondary";
		} else {
			actType = "s_tertiary";
		}

		LocationSampler ls = inputData.locationSamplers.get(actType);
		int sampleSize = Integer.parseInt(this.diverseScriptProperties
				.getProperty("locationSampleSize"));
		Tuple<String[], double[]> locations = ls
				.sampleLocationsNoWeight(sampleSize);

		String[] ids = locations.getFirst();
		double[] samplingWeight = locations.getSecond();
		// scale capacities of the set
		for (int i = 0; i < ids.length; i++) {
			String facilityLandUseType = inputData.facilityIdDescLookup
					.get(ids[i]);
			try {
				double scale = 1;
				double attenuatedCap = scale * samplingWeight[i];
				samplingWeight[i] = getGravityFactor(p,
						p.household.homeFacilityId, ids[i], actType,
						attenuatedCap).getFirst();
			} catch (NullPointerException e) {
				String errMsg = String.format(
						"No frequency for occup %s x land use %s.", p.occup,
						facilityLandUseType);
				scriptLog.error(errMsg);
				samplingWeight[i] = getGravityFactor(p,
						p.household.homeFacilityId, ids[i], actType,
						0.0000001 * samplingWeight[i]).getFirst();
			}
		}
		// do a weighted sampling for a single location
		return new LocationSampler(actType, ids, samplingWeight)
				.sampleLocations(1).getFirst()[0];
	}

	private void loadInputData(boolean facilitiesAllInOneXML) {
		try {
			this.inputData = new InputDataCollection(this.dba,
					this.diverseScriptProperties, this.scenario,
					facilitiesAllInOneXML);
			System.out.println("Input data loaded.");
		} catch (SQLException | NoConnectionException e) {
			e.printStackTrace();
		}
    }

	public static void main(String[] args) throws InstantiationException,
			IllegalAccessException, ClassNotFoundException, IOException,
			SQLException {
		String dbaProperties = "data/matsim2.properties";
		String otherProperties = "data/matsimSG2DemandGen/demandAssignment.properties";
		boolean deserialize = false;
		boolean facilitiesAllInOneXML = true;
		if (args.length == 2) {
			deserialize = Boolean.parseBoolean(args[0]);
			facilitiesAllInOneXML = Boolean.parseBoolean(args[1]);
		}
		System.out
				.println("First arg is deserialize, second is whether all facilities are to be loaded from single xml file");
		Version2DemandGenerationScript script = new Version2DemandGenerationScript(
				dbaProperties, otherProperties, deserialize,
				facilitiesAllInOneXML);
		script.run();

	}

}
