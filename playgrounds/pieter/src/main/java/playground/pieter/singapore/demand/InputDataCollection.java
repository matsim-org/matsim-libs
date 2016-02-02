package playground.pieter.singapore.demand;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import javax.management.timer.Timer;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.ActivityFacilitiesImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.ActivityOptionImpl;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.matsim.facilities.OpeningTimeImpl;
import org.matsim.facilities.OpeningTime.DayType;

import others.sergioo.util.dataBase.DataBaseAdmin;
import others.sergioo.util.dataBase.NoConnectionException;
import playground.pieter.singapore.utils.FacilitiesToSQL;

class InputDataCollection implements Serializable {
	private transient DataBaseAdmin dba;
	private transient Properties diverseScriptProperties;
	transient MatsimFacilitiesReader facilitiesReader;
	public static final double EARLIEST_START_TIME = Time.parseTime("00:10:00");
	public static final double DEFAULT_START_TIME = Time.parseTime("07:30:00");
	public static final double LATEST_END_TIME = Time.parseTime("23:50:00");
	public static final double DEFAULT_END_TIME = Time.parseTime("19:00:00");
	final HashMap<String, String> facilityIdDescLookup = new HashMap<>();
	private final HashMap<String, String> landUseToHITSTypeLookup = new HashMap<>();
	private final HashMap<String, HashMap<String, Double>> workLandUseSecondaryActCapacities = new HashMap<>();
	private HashMap<Integer, HouseholdSG> households;
//	HashMap<String, HashMap<String, Double>> landUseGivenOccupation = new HashMap<String, HashMap<String, Double>>();
	HashSet<String> landUseTypes = new HashSet<>();
	final HashMap<String, LocationSampler> locationSamplers = new HashMap<>();
	HashMap<Integer, LocationSampler> subdgpWorkFlowSamplers = new HashMap<>();
	final HashSet<String> mainActivityTypes = new HashSet<>();
	final HashMap<String, ArrayList<PaxSG>> mainActPaxCollection = new HashMap<>();
	HashSet<String> occupationTypes = new HashSet<>();
	HashMap<Integer, PaxSG> persons;

	private final HashMap<Integer, HashMap<Integer, HashMap<String, HashMap<String, Double>>>> bizActFrequencies = new HashMap<>();
	private final HashMap<Integer, HashMap<Integer, HashMap<String, HashMap<String, Double>>>> leisureActFrequencies = new HashMap<>();

	private transient MutableScenario scenario;
	private transient Logger inputLog;
	HashMap<Integer, HashMap<String, LocationSampler>> subDGPActivityLocationSamplers;
	HashMap<String, Integer> facilityToSubDGP;

	public InputDataCollection(DataBaseAdmin dba,
			Properties diverseScriptProperties, MutableScenario scenario,
			boolean facilitiesAllInOneXML) throws SQLException,
			NoConnectionException {
		inputLog = Logger.getLogger("InputData");
		this.dba = dba;
		this.diverseScriptProperties = diverseScriptProperties;
		this.scenario = scenario;
		facilitiesReader = new MatsimFacilitiesReader(this.scenario);
		loadSecondaryActFrequencies();
		loadLandUsetoHITSType();
		loadWorkLandUseSecondaryActCapacities();
		loadData(facilitiesAllInOneXML);
		getMainActivityTypes();
//		getOccupationAndLandUseTypes();
		createLocationSamplers();
		loadSubDGPWorkTripFlows();
		loadSubDGPLocationSamplers();
		createMainActPaxCollections();
		createFacilityIdtoDescriptionLookup();
	}

	private void loadSecondaryActFrequencies() {
		inputLog.info("Loading secondary act frequencies");
		String secondaryActFrequenciesTable = diverseScriptProperties
				.getProperty("secondaryActFrequenciesTable");
		String bizActs = "\'biz\', \'errand\', \'medi\'";
		String leisureActs = "\'eat\',\'rec\',\'shop\',\'social\',\'sport\',\'fun\'";
		String[] activityGroups = { bizActs, leisureActs };
		for (String activityGroup : activityGroups) {

			try {
				HashMap<Integer, HashMap<Integer, HashMap<String, HashMap<String, Double>>>> relevantMap;
				if (activityGroup.equals(bizActs))
					relevantMap = this.bizActFrequencies;
				else
					relevantMap = this.leisureActFrequencies;
				// start with income_hh
				ResultSet rs_income = dba
						.executeQuery(String
								.format("SELECT distinct income_hh FROM %s where t6_purpose in(%s)",
										secondaryActFrequenciesTable,
										activityGroup));
				while (rs_income.next()) {
					int income = rs_income.getInt("income_hh");
					HashMap<Integer, HashMap<String, HashMap<String, Double>>> incomeMap = new HashMap<>();
					relevantMap.put(income, incomeMap);
					ResultSet rs_age = dba
							.executeQuery(String
									.format("SELECT distinct age FROM %s where t6_purpose in(%s)",
											secondaryActFrequenciesTable,
											activityGroup));
					inputLog.info("\t finished income_hh "+income);
					while (rs_age.next()) {
						int age = rs_age.getInt("age");
						HashMap<String, HashMap<String, Double>> ageMap = new HashMap<>();
						incomeMap.put(age, ageMap);
						ResultSet rs_occup = dba
								.executeQuery(String
										.format("SELECT distinct occup FROM %s where t6_purpose in(%s)",
												secondaryActFrequenciesTable,
												activityGroup));
						while (rs_occup.next()) {
							String occup = rs_occup.getString("occup");
							HashMap<String, Double> occupMap = new HashMap<>();
							ageMap.put(occup, occupMap);
							ResultSet rs_purpose = dba
									.executeQuery(String
											.format("SELECT distinct t6_purpose FROM %s where t6_purpose in(%s)",
													secondaryActFrequenciesTable,
													activityGroup));
							while (rs_purpose.next()) {
								String purpose = rs_purpose
										.getString("t6_purpose");
								double weight = 0.0001;
								ResultSet rs_weight = dba
										.executeQuery(String
												.format("SELECT hipf10sum FROM %s where t6_purpose=\'%s\' and occup=\'%s\' and age=%d and income_hh=%d",
														secondaryActFrequenciesTable,
														purpose, occup, age,
														income));
								while (rs_weight.next()) {
									weight = rs_weight.getDouble("hipf10sum");
								}
								occupMap.put(purpose, weight);
							}
						}
					}
				}

			} catch (SQLException | NoConnectionException e) {
				e.printStackTrace();
			}
        }
		inputLog.info("DONE: Loading secondary act frequencies");
	}

	private void loadWorkLandUseSecondaryActCapacities() {
		inputLog.info("Loading masterplan land use types to HITS activity type frequencies.");
		String masterplanSecondaryActCapacitiesTable = diverseScriptProperties
				.getProperty("masterplanSecondaryActCapacities");
		try {
			ResultSet rs = dba.executeQuery(String.format(
					"select distinct description from %s",
					masterplanSecondaryActCapacitiesTable));
			while (rs.next()) {
				String currenttype = rs.getString("description");
				HashMap<String, Double> activityCaps = new HashMap<>();
				workLandUseSecondaryActCapacities
						.put(currenttype, activityCaps);
				ResultSet rs2 = dba.executeQuery(String.format(
						"select * from %s where description = \'%s\'",
						masterplanSecondaryActCapacitiesTable, currenttype));
				while (rs2.next()) {

					activityCaps.put(rs2.getString("act"),
							rs2.getDouble("activitycap"));
				}
			}
		} catch (SQLException | NoConnectionException e) {
			e.printStackTrace();
		}
        inputLog.info("DONE: Loading masterplan land use types to HITS activity type frequencies.");
	}
	
	private void loadSubDGPWorkTripFlows() {
		inputLog.info("Loading work trip flows for weighted assignment");
		String workTripFlowTable = diverseScriptProperties
				.getProperty("workTripFlows");
		
		this.subdgpWorkFlowSamplers = new HashMap<>();
		try {
			ResultSet rs = dba.executeQuery(String.format(
					"select distinct origsubdgp from %s order by origsubdgp",
					workTripFlowTable));
			while (rs.next()) {
				int origsubdgp = rs.getInt("origsubdgp");
				
				ResultSet rs2 = dba.executeQuery(String.format(
						"select destsubdgp,predict from %s where origsubdgp = %d",
						workTripFlowTable, origsubdgp));
				HashMap<Integer,Double> tripflows = new HashMap<>();
				while (rs2.next()) {
					tripflows.put(rs2.getInt("destsubdgp"),
							rs2.getDouble("predict"));
				}
//				write it to arrays
				Iterator<Entry<Integer,Double>> fi = tripflows.entrySet()
						.iterator();
				String[] ids = new String[tripflows.entrySet().size()];
				double[] caps = new double[tripflows.entrySet().size()];
				int i = 0;
				while (fi.hasNext()) {
					Entry<Integer, Double> e = fi.next();
					ids[i] = e.getKey().toString();
					caps[i] = e.getValue();
					i++;
				}
				LocationSampler ls = new LocationSampler(Integer.toString(origsubdgp), ids, caps);
				this.subdgpWorkFlowSamplers.put(origsubdgp, ls);
			}
		} catch (SQLException | NoConnectionException e) {
			e.printStackTrace();
		}
        inputLog.info("DONE: Loading work trip flows for weighted assignment");
	}
	
	private void loadSubDGPLocationSamplers() {
		inputLog.info("Loading locationsamplers for each subdgp and work activity type");
		String facilityToSubDGPTable = diverseScriptProperties
				.getProperty("workFacilitiesToSubDGP");
		HashMap<String, Integer> facilityToSubDGP = new HashMap<>();
		HashMap<Integer, HashMap<String,Tuple<ArrayList<String>,ArrayList<Double>>>> subDGPActivityMapping = 
				new HashMap<>();
		HashMap<Integer, HashMap<String,LocationSampler>> subDGPActivityLocationSamplers = 
				new HashMap<>();
		try {
//		start by mapping facilities to subdgps
			inputLog.info("\tMapping facilities to subdgps");
			ResultSet rs = dba.executeQuery(String.format(
					"select * from %s", facilityToSubDGPTable));
			while (rs.next()) {
				facilityToSubDGP.put(rs.getString("id"), rs.getInt("subdgp"));
			}
//			initialize the hashmaps
			inputLog.info("\tInitializing hashmaps");
			rs = dba.executeQuery(String.format(
					"select distinct subdgp  from %s order by subdgp", facilityToSubDGPTable));
			while (rs.next()) {
				subDGPActivityMapping.put(rs.getInt("subdgp"), 
						new HashMap<String,Tuple<ArrayList<String>,ArrayList<Double>>>());
				subDGPActivityLocationSamplers.put(rs.getInt("subdgp"),
						new HashMap<String,LocationSampler>());
			}
//			further initialization of the hashmaps
			for (String activityType : this.mainActivityTypes) {
				if(!activityType.startsWith("w_"))
					continue;
				rs = dba.executeQuery(String.format(
						"select distinct subdgp  from %s order by subdgp", facilityToSubDGPTable));
				while(rs.next()){
					subDGPActivityMapping.get(rs.getInt("subdgp")).put(activityType, 
							new Tuple<>(
									new ArrayList<String>(), new ArrayList<Double>()));
					
				}
			}
		} catch (SQLException | NoConnectionException e) {
			e.printStackTrace();
		}
//		then, go through the facilities for each work activity, find the subdgp theyre in
//		put a reference to them in that subdgp's activity type map
		inputLog.info("\tFilling capacities");
		for (String activityType : this.mainActivityTypes) {
//			only look at work activities
			if(!activityType.startsWith("w_"))
				continue;
			TreeMap<Id<ActivityFacility>, ? extends ActivityFacility> facilities = this.scenario
					.getActivityFacilities().getFacilitiesForActivityType(
                            activityType);
			for (Entry<Id<ActivityFacility>, ? extends ActivityFacility> e : facilities.entrySet()) {
				String currid = e.getKey().toString();
				double currcap = e.getValue().getActivityOptions().get(activityType).getCapacity();
//				TODO: check spatial join of facilities to subdgps
//				there are a few facilities that  havent spatially joined properly, skip over them
				try{
					int subDGP = facilityToSubDGP.get(currid);
					subDGPActivityMapping.get(subDGP).get(activityType).getFirst().add(currid);
					subDGPActivityMapping.get(subDGP).get(activityType).getSecond().add(currcap);
					
				}catch(NullPointerException ne){
                }
			}
		}
//		now, convert all the arraylists to arrays, and create location samplers
		inputLog.info("\tConverting to arrays");
		for(int subdgp:subDGPActivityMapping.keySet()){
			for(String acttype:subDGPActivityMapping.get(subdgp).keySet()){
				ArrayList ids = subDGPActivityMapping.get(subdgp).get(acttype).getFirst();
				ArrayList caps = subDGPActivityMapping.get(subdgp).get(acttype).getSecond();
				String[] idsA = (String[]) ids.toArray(new String[ids.size()]);
				double[] capsA = ArrayUtils.toPrimitive((Double[]) caps.toArray(new Double[caps.size()]));
				subDGPActivityLocationSamplers.get(subdgp).put(acttype, new LocationSampler(acttype, idsA, capsA));
			}
		}
		this.subDGPActivityLocationSamplers = subDGPActivityLocationSamplers;
		this.facilityToSubDGP = facilityToSubDGP;
		inputLog.info("DONE: Loading locationsamplers for each subdgp and work activity type");
	}
	
	
	private void addSecondaryActivityTypes() {
		for (Entry<Id<ActivityFacility>, ? extends ActivityFacility> facilityset : this.scenario
				.getActivityFacilities().getFacilities().entrySet()) {
			// get the land use for each facility
			ActivityFacilityImpl facility = (ActivityFacilityImpl) facilityset
					.getValue();
			String description = facility.getDesc();
			HashMap<String, Double> secondaryCapacities = this.workLandUseSecondaryActCapacities
					.get(description);
			// check if this facility type actually has other activity types
			if (secondaryCapacities != null) {
				if (facility.getId().toString().startsWith("home")) {
					for (String activityType : secondaryCapacities.keySet()) {
						double startCap = facility.getActivityOptions()
								.get("home").getCapacity();
						ActivityOptionImpl option = facility
								.createAndAddActivityOption(activityType);
						option.addOpeningTime(new OpeningTimeImpl(
								DayType.wkday, Time.parseTime("10:00:00"), Time
										.parseTime("22:00:00")));
						option.setCapacity(secondaryCapacities
								.get(activityType) * startCap);
					}

				} else {

					for (String activityType : secondaryCapacities.keySet()) {
						ActivityOptionImpl option = facility
								.createAndAddActivityOption(activityType);
						option.addOpeningTime(new OpeningTimeImpl(
								DayType.wkday, Time.parseTime("10:00:00"), Time
										.parseTime("22:00:00")));
						option.setCapacity(secondaryCapacities
								.get(activityType));
					}
				}
			}
		}
	}

	private void createFacilityIdtoDescriptionLookup() {
		inputLog.info("Creating facility ID to land use type or description lookup table.");
        for (ActivityFacility activityFacility : this.scenario.getActivityFacilities()
                .getFacilities().values()) {
            ActivityFacilityImpl f = (ActivityFacilityImpl) activityFacility;
            this.facilityIdDescLookup.put(f.getId().toString(), f.getDesc());
        }
	}

	private void createLocationSamplers() {
		inputLog.info("Creating weighted samplers for each activity type,\n indexing the relevant facility ids and their capacity for the particular activity.");
		for (String activityType : this.mainActivityTypes) {
			TreeMap<Id<ActivityFacility>, ActivityFacility> facilities = this.scenario
					.getActivityFacilities().getFacilitiesForActivityType(
                            activityType);
			Iterator<Entry<Id<ActivityFacility>, ActivityFacility>> fi = facilities.entrySet()
					.iterator();
			String[] ids = new String[facilities.entrySet().size()];
			double[] caps = new double[facilities.entrySet().size()];
			int i = 0;
			while (fi.hasNext()) {
				Entry<Id<ActivityFacility>, ActivityFacility> e = fi.next();
				ids[i] = e.getKey().toString();
				caps[i] = e.getValue().getActivityOptions().get(activityType)
						.getCapacity();

				i++;
			}
			this.locationSamplers.put(activityType, new LocationSampler(
					activityType, ids, caps));
		}
		inputLog.info("DONE: Creating weighted samplers for each activity type.");
	}

	private void createMainActPaxCollections() {
		inputLog.info("Creating collections of people participating in each major activity type.");
		for (String activityType : this.mainActivityTypes) {
			this.mainActPaxCollection.put(activityType, new ArrayList<PaxSG>());
		}
		for (PaxSG p : this.getPersons().values()) {
			if (p.mainActType == null)
				continue;
			mainActPaxCollection.get(p.mainActType).add(p);
		}
		inputLog.info("DONE: Creating collections of people participating in each major activity type.");
	}

	void dumpFacilitiesToSQL() throws SQLException,
			NoConnectionException {
		FacilitiesToSQL fc2sql = new FacilitiesToSQL(this.dba, this.scenario);
		fc2sql.createCompleteFacilityAndActivityTablePostgres(diverseScriptProperties.getProperty("completeFacilitiesSQLTable"));

	}

	private void dumpFacilitiesToXML() {
		FacilitiesWriter fcw = new FacilitiesWriter(
				this.scenario.getActivityFacilities());
		String completeFacilitiesXMLFile = this.diverseScriptProperties
				.getProperty("completeFacilitiesXMLFile");
		fcw.write(completeFacilitiesXMLFile);
	}

	public HashMap<Integer, HouseholdSG> getHouseholds() {
		return households;
	}

	private void getMainActivityTypes() {
		inputLog.info("Listing the main activity types in the scenario.");
        for (ActivityFacility activityFacility : this.scenario.getActivityFacilities().getFacilities().values()) {
            ActivityFacilityImpl f = (ActivityFacilityImpl) activityFacility;
            Set<String> actops = f.getActivityOptions().keySet();
            this.mainActivityTypes.addAll(actops);

        }
	}

//	private void getOccupationAndLandUseTypes() {
//		this.landUseTypes.addAll(this.landUseGivenOccupation.keySet());
//		this.occupationTypes.addAll(this.landUseGivenOccupation.values()
//				.iterator().next().keySet());
//	}

	public HashMap<Integer, PaxSG> getPersons() {
		return persons;
	}

	void loadData(boolean facilitiesAllInOneXML) throws SQLException,
			NoConnectionException {
		loadHouseholdsAndPersons();
		loadFacilities(facilitiesAllInOneXML);
		// dumpFacilitiesToSQL();
//		loadOccupationLandUseProbabilities();
	}

	private void loadFacilitiesFromSQL() {
		String secondaryFacilitiesTable = diverseScriptProperties
				.getProperty("secondaryFacilitiesTable");
		try {
			ResultSet rs = dba.executeQuery(String.format(
					"SELECT * FROM %s where latitude>0 and longitude>0",
					secondaryFacilitiesTable));
			int counter = 1;
			while (rs.next()) {
				Coord coord = new Coord(rs.getDouble("longitude"), rs.getDouble("latitude"));
				ActivityFacilityImpl facility = ((ActivityFacilitiesImpl) scenario
						.getActivityFacilities())
						.createAndAddFacility(
								Id.create("leisure_" + counter, ActivityFacility.class),
								TransformationFactory
										.getCoordinateTransformation(
												TransformationFactory.WGS84,
												TransformationFactory.WGS84_UTM48N)
										.transform(coord));
				facility.setDesc(rs.getString("type"));
				counter++;
			}

			String homeFacilitiesTable = diverseScriptProperties
					.getProperty("homeFacilitiesTable");
			rs = dba.executeQuery(String.format("SELECT * FROM %s",
					homeFacilitiesTable));

			while (rs.next()) {
				ActivityFacilityImpl facility = ((ActivityFacilitiesImpl) scenario
						.getActivityFacilities()).createAndAddFacility(
								Id.create("home_"
                                        + rs.getInt("id_res_facility"), ActivityFacility.class),
						new Coord(rs.getDouble("x_utm48n"), rs
								.getDouble("y_utm48n")));
				facility.setDesc(rs.getString("property_type"));
				ActivityOptionImpl actOption = facility
						.createAndAddActivityOption("home");
				actOption.setCapacity((double) rs.getInt("units"));

			}
		} catch (SQLException | NoConnectionException e) {
			e.printStackTrace();
		}
    }

	void loadFacilities(boolean completeFile) {
		if (completeFile) {
			facilitiesReader.readFile(diverseScriptProperties
					.getProperty("completeFacilitiesXMLFile"));
		} else {
			facilitiesReader.readFile(diverseScriptProperties
					.getProperty("workFacilitiesXMLFile"));
			facilitiesReader.readFile(diverseScriptProperties
					.getProperty("eduFacilitiesXMLFile"));
			loadFacilitiesFromSQL();
			FacilitiesToSQL f2s = new FacilitiesToSQL(dba, scenario);
			f2s.stripDescription();
			addSecondaryActivityTypes();
			dumpFacilitiesToXML();
			try {
				dumpFacilitiesToSQL();
			} catch (SQLException | NoConnectionException e) {
				e.printStackTrace();
			}
        }
	}

	private void loadHouseholdsAndPersons() {
		inputLog.info("Loading persons and households");
		households = new HashMap<>();
		persons = new HashMap<>();
		String synthHouseholdIdField = diverseScriptProperties
				.getProperty("synthHouseholdId");
		String carAvailabilityField = diverseScriptProperties
				.getProperty("carAvailability");
		String homeFacilityIdField = diverseScriptProperties
				.getProperty("homeFacilityId");
		String householdTableNameField = diverseScriptProperties
				.getProperty("householdTableName");
		String householdSelectorTableNameField = diverseScriptProperties
				.getProperty("householdSelectorTableName");
		String householdSelectorField = diverseScriptProperties
				.getProperty("householdSelectorField");

		String personTableNameField = diverseScriptProperties
				.getProperty("personTableName");
		String paxIdField = diverseScriptProperties.getProperty("paxId");
		String foreignerField = diverseScriptProperties
				.getProperty("foreigner");
		String carLicenseHolderField = diverseScriptProperties
				.getProperty("carLicenseHolder");
		String ageField = diverseScriptProperties.getProperty("age");
		String sexField = diverseScriptProperties.getProperty("sex");
		String incomePaxField = diverseScriptProperties
				.getProperty("incomePax");
		String modeSuggestionField = diverseScriptProperties
				.getProperty("modeSuggestion");
		String incomeHouseholdField = diverseScriptProperties
				.getProperty("incomeHousehold");
		String occupField = diverseScriptProperties.getProperty("occup");
		String chainField = diverseScriptProperties.getProperty("chain");
		String chainTypeField = diverseScriptProperties
				.getProperty("chainType");
		String mainActStartField = diverseScriptProperties
				.getProperty("mainActStart");
		String mainActDurField = diverseScriptProperties
				.getProperty("mainActDur");
		String mainActTypeField = diverseScriptProperties
				.getProperty("mainActType");

		int householdLoadLimit = Integer.parseInt(diverseScriptProperties
				.getProperty("householdLoadLimit"));
		Date startDate = new Date();

		int counter = 0;
		ResultSet rs;
		try {
			// sample random household ids without replacement
//			rs = dba.executeQuery(String.format("SELECT max(%s) FROM %s;",
//					synthHouseholdIdField, householdTableNameField));
//			rs.next();
//			int[] hhids = Sample.sampleMfromN(householdLoadLimitField,
//					rs.getInt(1));
//			//
//			if (householdLoadLimitField > 1000000) {
//				rs = dba.executeQuery(String.format(
//						"SELECT DISTINCT %s, %s, %s FROM %s LIMIT %d;",
//						synthHouseholdIdField, homeFacilityIdField,
//						carAvailabilityField, householdTableNameField,
//						householdLoadLimitField));
//			} else {
//				String hhidsToLoad = "" + hhids[0];
//				for (int i = 1; i < hhids.length; i++) {
//					hhidsToLoad = hhidsToLoad + "," + hhids[i];
//				}

//				rs = dba.executeQuery(String.format(
//						"SELECT DISTINCT %s, %s, %s FROM %s WHERE %s IN(%s);",
//						synthHouseholdIdField, homeFacilityIdField,
//						carAvailabilityField, householdTableNameField,
//						synthHouseholdIdField, hhidsToLoad));
			rs = dba.executeQuery(String.format(
					"SELECT count(*) as num FROM %s WHERE %s = 1;",
					householdSelectorTableNameField,
					householdSelectorField));	
			rs.next();
			int householdCount = rs.getInt("num");
			
			rs = dba.executeQuery(String.format(
						"SELECT hhs.%s, hhs.%s, hhs.%s FROM %s hhs,%s sel WHERE hhs.%s = sel.%s and %s = 1;",
						synthHouseholdIdField, homeFacilityIdField,
						carAvailabilityField, householdTableNameField,
						householdSelectorTableNameField,
						synthHouseholdIdField, synthHouseholdIdField,
						householdSelectorField));
//			}
			while (rs.next()) {
				if(counter>=householdLoadLimit)
					break;
				int synthHouseholdId = rs.getInt(synthHouseholdIdField);
				int carAvailability = rs.getInt(carAvailabilityField);
				String homeFacilityId = "home_"
                        + rs.getInt(homeFacilityIdField);
				HouseholdSG currentHousehold = new HouseholdSG(
						synthHouseholdId, carAvailability, homeFacilityId);
				households.put(synthHouseholdId, currentHousehold);
				ResultSet rspax = dba.executeQuery(String.format(
						"SELECT * FROM %s WHERE %s = %d", personTableNameField,
						synthHouseholdIdField, synthHouseholdId));
				while (rspax.next()) {
					String modeSuggestion = rspax
							.getString(modeSuggestionField);
					if ((modeSuggestion.equals("not_assigned") || modeSuggestion
							.equals("notravel")))
						continue;
					String chainType = rspax.getString(chainTypeField);
					if(chainType == null)
						continue;
					// generate half mixed mode users
					if (modeSuggestion.equals("ptmix") && Math.random() > 0.5)
						continue;
					int paxId = rspax.getInt(paxIdField);
					String foreigner = rspax.getString(foreignerField);
					boolean carLicenseHolder = rspax
                            .getInt(carLicenseHolderField) > 0;
					String chain = rspax.getString(chainField);
					int age = 0;
					if (rspax.getString(ageField).equals("age65_up"))
						age = 70;
					else
						age = Integer.parseInt(new StringBuilder(rspax
								.getString(ageField)).substring(3, 5));
					String sex = rspax.getString(sexField);
					String occup = rspax.getString(occupField);
					int income = Integer.parseInt(rspax
							.getString(incomePaxField));
					int incomeHousehold = rspax.getInt(incomeHouseholdField);
					String mainActType = rspax.getString(mainActTypeField);
					double mainActStart = Math.max(
							rspax.getDouble(mainActStartField), 0);
					double mainActDur = rspax.getDouble(mainActDurField);
					//skip if this guy is not meant to be realised
					PaxSG newPax = new PaxSG(paxId, foreigner,
							currentHousehold, carLicenseHolder, age, sex,
							income, occup, chain, chainType, mainActStart,
							mainActDur, mainActType, modeSuggestion);
					newPax.household.incomeHousehold = incomeHousehold;
					newPax.bizActFrequencies = assignSecondaryActFrequencies(
							"b", newPax);
					newPax.leisureActFrequencies = assignSecondaryActFrequencies(
							"l", newPax);
					currentHousehold.pax.add(newPax);
					this.persons.put(paxId, newPax);
					// timer info

				}
				if ((counter % 10000) == 0) {

					Date currDate = new Date();
					long timePastLong = currDate.getTime()
							- startDate.getTime();
					double timePastSec = (double) timePastLong
							/ (double) Timer.ONE_SECOND;
					int agentsToGo = householdCount - counter;
					double agentsPerSecond = (double) counter / timePastSec;
					long timeToGo = (long) ((double) agentsToGo / agentsPerSecond);
					inputLog.info(String
							.format("%6d of %8d households done in %.3f seconds at %.3f hhs/sec, %s sec to go.",
									counter, householdCount,
									timePastSec, agentsPerSecond, timeToGo));
				}
				counter++;
			}
		} catch (SQLException | NoConnectionException e) {
			e.printStackTrace();
		}
        inputLog.info("DONE: Loading persons and households");
	}

	private Tuple<String[], double[]> assignSecondaryActFrequencies(
			String activityGroup, PaxSG pax) {
		HashMap<String, Double> paxFreqs;
		if (activityGroup.equals("l")) {
			paxFreqs = leisureActFrequencies.get(pax.household.incomeHousehold)
					.get(pax.age).get(pax.occup);
		} else {
			paxFreqs = bizActFrequencies.get(pax.household.incomeHousehold)
					.get(pax.age).get(pax.occup);
		}
		String[] activityTypes = new String[paxFreqs.size()];
		double[] activityFrequencies = new double[paxFreqs.size()];
		Iterator<Entry<String, Double>> it = paxFreqs.entrySet().iterator();
		int i = 0;
		while (it.hasNext()) {
			Entry<String, Double> e = it.next();
			activityTypes[i] = e.getKey();
			activityFrequencies[i] = e.getValue();
			i++;
		}
		return new Tuple<>(activityTypes, activityFrequencies);
	}
	

	
	private void loadLandUsetoHITSType() {
		inputLog.info("Loading masterplan land use to HITS place type lookup.");
		String masterplan2hitstypefield = diverseScriptProperties
				.getProperty("masterplan2hitstype");
		try {
			ResultSet rs = dba.executeQuery(String.format("SELECT * FROM %s",
					masterplan2hitstypefield));
			while (rs.next()) {
				this.landUseToHITSTypeLookup.put(rs.getString("description"),
						rs.getString("hits_type"));
			}
		} catch (SQLException | NoConnectionException e) {
			e.printStackTrace();
		}
        inputLog.info("DONE: Loading masterplan land use to HITS place type lookup.");
	}

//	private void loadOccupationLandUseProbabilities() {
//		inputLog.info("Loading occupation vs masterplan landuse type frequencies");
//		String occupXlandUseTable = this.diverseScriptProperties
//				.getProperty("occupXlandUseTable");
//		try {
//			ResultSet rs = dba.executeQuery(String.format(
//					"SELECT DISTINCT occup FROM %s", occupXlandUseTable));
//			// initialize and populate the hashmaps
//			while (rs.next()) {
//				String currentOccupation = rs.getString("occup");
//				HashMap<String, Double> occupMap = new HashMap<String, Double>();
//				this.landUseGivenOccupation.put(currentOccupation, occupMap);
//				ResultSet rs2 = dba.executeQuery(String.format(
//						"SELECT * FROM %s WHERE occup = \'%s\'",
//						occupXlandUseTable, currentOccupation));
//				while (rs2.next()) {
//					occupMap.put(rs2.getString("lu_type"),
//							rs2.getDouble("problandusegivenoccup"));
//				}
//			}
//
//		} catch (SQLException e) {
//			e.printStackTrace();
//		} catch (NoConnectionException e) {
//			e.printStackTrace();
//		}
//		inputLog.info("DONE: Loading occupation vs masterplan landuse type frequencies");
//	}

	public void restoreIfDeserialized(MutableScenario scenario, DataBaseAdmin dba,
			Properties p) {
		this.setScenario(scenario);
		this.setDiverseScriptProperties(p);
		this.setDba(dba);
		inputLog = Logger.getLogger("InputData");
	}

	void setDba(DataBaseAdmin dba) {
		this.dba = dba;
	}

	void setDiverseScriptProperties(Properties diverseScriptProperties) {
		this.diverseScriptProperties = diverseScriptProperties;
	}

	void setScenario(MutableScenario scenario) {
		this.scenario = scenario;
	}

}
