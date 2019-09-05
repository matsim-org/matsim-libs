package commercialtraffic.demandAssigment;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.tuple.Triple;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import commercialtraffic.companyGeneration.CommericalCompany;
import commercialtraffic.companyGeneration.CompanyGenerator;
import ft.utils.ctDemandPrep.Demand4CompanyClass;
import ft.utils.ctDemandPrep.DemandGenerator;

public class AssignService {
	
	// !!!!! Important !!!!!
	// SET MAX SERVICES PER ACTIVITY
	// !!!!! Important !!!!!
	int maxServicesPerAct = 3;
	
	// !!!!! Filter CT Trips !!!!!
	Double filterFactor=0.1;
	
	
	CommercialTripsReader commercialTripReader;
	String plansFile;

	Set<String> acceptedMainModes = new HashSet<>(
			Arrays.asList("car", "pt", "drt", "walk", "ride", "bike", "stayHome"));
	Set<String> acceptedActivities = new HashSet<>(
			Arrays.asList("home", "work", "shopping", "leisure", "other", "education"));

	// ServiceType->cust.Relation->zone->PersonID->ListPE
	Map<String, ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<Id<Person>, HashSet<Integer>>>>> actCandidatesMap = new ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<Id<Person>, HashSet<Integer>>>>>();
	Map<String, Map<String, ServiceDefinition>> serviceDefinitionMap = new ConcurrentHashMap<String, Map<String, ServiceDefinition>>();
	Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	String companyFolder;
	String zoneSHP;
	String outputpath;
	DemandGenerator demand;
	String commercialTripsFile;
	String serviceDurationDistPath;
	String networkFile;
	String matsimInput;
	MutableInt noFoundCounter = new MutableInt(0);
	MutableInt foundCounter = new MutableInt(0);
	List<Job> jobList = new ArrayList<Job>();
	MutableInt jobIdCounter = new MutableInt(0);
	CompanyGenerator companyGenerator;
	String comercialVehicleCSV;
	long nr = 1896;
	Random r = MatsimRandom.getRandom();

	AssignService(String plansFile, String commercialTripsFile, String comercialVehicleCSV, String networkFile,
			String serviceDurationDistPath, String companyFolder, String zoneSHP, String matsimInput,
			String outputpath) {
		this.outputpath = outputpath;
		this.comercialVehicleCSV = comercialVehicleCSV;
		this.networkFile = networkFile;
		this.matsimInput = matsimInput;
		new PopulationReader(scenario).readFile(plansFile);
		r.setSeed(nr);
		commercialTripReader = new CommercialTripsReader(commercialTripsFile, serviceDurationDistPath,filterFactor);
		commercialTripReader.run();
		demand = new DemandGenerator(companyFolder, zoneSHP, outputpath);
		demand.findNeighbourZones();

		// companyGenerator is required in order to construct carriers with services
		this.companyGenerator = new CompanyGenerator(comercialVehicleCSV, commercialTripsFile, serviceDurationDistPath,
				networkFile, companyFolder, zoneSHP, outputpath, matsimInput + "Carrier\\");
		this.companyGenerator.initalize();

		// CompanyGenerator demand = new
		// CompanyGenerator("D:\\Thiel\\Programme\\WVModell\\WV_Modell_KIT_H\\fahrzeuge.csv",
		// "D:\\Thiel\\Programme\\WVModell\\WV_Modell_KIT_H\\wege.csv",
		// "D:\\Thiel\\Programme\\WVModell\\ServiceDurCalc\\Distributions\\",
		// "D:\\Thiel\\Programme\\MatSim\\01_HannoverModel_2.0\\Network\\00_Final_Network\\network_editedPt.xml.gz",
		// "D:\\Thiel\\Programme\\WVModell\\00_Eingangsdaten\\Unternehmen\\",
		// "D:\\Thiel\\Programme\\WVModell\\00_Eingangsdaten\\Zellen\\FNP_Merged\\baseShapeH.shp",
		// "D:\\Thiel\\Programme\\WVModell\\00_Eingangsdaten\\",
		// "D:\\Thiel\\Programme\\WVModell\\01_MatSimInput\\Carrier\\");

	}

	public static void main(String[] args) {

		AssignService assignData = new AssignService(
				"D:\\Thiel\\Programme\\MatSim\\01_HannoverModel_2.0\\Simulation\\output\\vw243_cadON_ptSpeedAdj.0.1\\vw243_cadON_ptSpeedAdj.0.1.output_plans.xml.gz",
				"D:\\Thiel\\Programme\\WVModell\\WV_Modell_KIT_H\\wege.csv",
				"D:\\Thiel\\Programme\\WVModell\\WV_Modell_KIT_H\\fahrzeuge.csv",
				"D:\\Thiel\\Programme\\MatSim\\01_HannoverModel_2.0\\Network\\00_Final_Network\\network_editedPt.xml.gz",
				"D:\\Thiel\\Programme\\WVModell\\ServiceDurCalc\\Distributions\\",
				"D:\\Thiel\\Programme\\WVModell\\00_Eingangsdaten\\Unternehmen\\",
				"D:\\Thiel\\Programme\\WVModell\\00_Eingangsdaten\\Zellen\\FNP_Merged\\baseShapeH.shp",
				"D:\\Thiel\\Programme\\WVModell\\01_MatSimInput\\",
				"D:\\Thiel\\Programme\\WVModell\\00_Eingangsdaten\\");

		assignData.initializeActCandidateMap();

		assignData.findCandidates();
		assignData.manipulatePopulationAndCreateServices();
		assignData.writeCustomerDemand();
		assignData.writePopulation();
		assignData.companyGenerator.writeCarriers();

		System.out.println("Found: " + assignData.foundCounter + " || " + assignData.noFoundCounter);

	}

	// Add services to customers and add service objects to carriers
	public void manipulatePopulationAndCreateServices() {

		// companyCarrierMap String Key = companyId
		Map<String, CommericalCompany> companyMap = companyGenerator.commercialCompanyMap;
		for (Entry<String, List<CommercialTrip>> tripsPerServiceType : commercialTripReader.commercialTripMap
				.entrySet())

		{
			for (CommercialTrip commercialTrip : tripsPerServiceType.getValue()) {
				String customerRelation = commercialTrip.customerRelation;
				String serviceType = commercialTrip.wirtschaftszweig;
				String zone = commercialTrip.zielzelle;
				String carrierId = serviceType + "_" + commercialTrip.unternehmensID;
				String companyId = String.valueOf(commercialTrip.unternehmensID);
				if (customerRelation=="private") {
					continue;
				}

				double serviceDuration = this.commercialTripReader.getRandomServiceDurationPerType(serviceType);

				Job finalJob = getActivityCandiate(serviceType, customerRelation, zone, serviceDuration, carrierId);

				if (finalJob != null) {

					this.jobList.add(finalJob);

					companyMap.get(companyId).addService(finalJob.jobId, finalJob.regularAgentActivity.getLinkId(),
							finalJob.startTime, finalJob.endTime, serviceDuration);

					// Check for Attributes
					Object actAttr = this.scenario.getPopulation().getPersons().get(finalJob.personid).getSelectedPlan()
							.getPlanElements().get(finalJob.planIdx).getAttributes().getAttribute("jobId");

					if (actAttr == null) {
						this.scenario.getPopulation().getPersons().get(finalJob.personid).getSelectedPlan()
								.getPlanElements().get(finalJob.planIdx).getAttributes()
								.putAttribute("jobId", finalJob.jobId);
					} else {
						String prevJobId = actAttr.toString();
						this.scenario.getPopulation().getPersons().get(finalJob.personid).getSelectedPlan()
								.getPlanElements().get(finalJob.planIdx).getAttributes()
								.putAttribute("jobId", prevJobId + ";" + finalJob.jobId);
						
						//System.out.println(prevJobId + ";" + finalJob.jobId);

					}

					this.jobIdCounter.increment();
					foundCounter.increment();

				} else

					noFoundCounter.increment();
			}

		}
	}

	public void writePopulation() {
		String filename = this.matsimInput + "Population//populationWithCTdemand.xml.gz";
		PopulationWriter writer = new PopulationWriter(this.scenario.getPopulation());
		writer.write(filename);
	}

	public Job getActivityCandiate(String serviceType, String customerRelation, String zone, double serviceDuration,
			String carrierId) {

		int maxCheckedZones = 3;
		int checkedZonesCounter = 0;

		List<String> neighbourZoneList = demand.neighbourMap.get(zone);
		java.util.Collections.sort(neighbourZoneList);

		if (actCandidatesMap.containsKey(serviceType)) {
			if (actCandidatesMap.get(serviceType).containsKey(customerRelation)) {

				while (checkedZonesCounter < maxCheckedZones)

				{

					int maxProofedCandidatesCounter = 50;
					int candidateCounter = 0;

					while (candidateCounter < maxProofedCandidatesCounter) {

						if (actCandidatesMap.get(serviceType).get(customerRelation).containsKey(zone)) {

							List<Id<Person>> potentialCandidateKeys = actCandidatesMap.get(serviceType)
									.get(customerRelation).get(zone).keySet().stream().collect(Collectors.toList());

							java.util.Collections.sort(potentialCandidateKeys);

							if (!potentialCandidateKeys.isEmpty()) {

								int randromIdx = r.nextInt(potentialCandidateKeys.size());

								Id<Person> candidatePerson = potentialCandidateKeys.get(randromIdx);

								List<Integer> peIndexList = actCandidatesMap.get(serviceType).get(customerRelation)
										.get(zone).get(candidatePerson).stream().collect(Collectors.toList());

								java.util.Collections.sort(peIndexList);

								Triple<Integer, Double, Double> result = getTimeConstrainedActivity(candidatePerson,
										peIndexList, serviceDuration);

								if (result != null) {

									Integer matchedPeIdx = result.getLeft();
									Double startTime = result.getMiddle();
									Double endTime = result.getRight();
									Activity finalActivityDestination = (Activity) this.scenario.getPopulation()
											.getPersons().get(candidatePerson).getSelectedPlan().getPlanElements()
											.get(matchedPeIdx);

									// return finalActivityDestination;

									return new Job(jobIdCounter.toString(), carrierId, candidatePerson, serviceType,
											customerRelation, zone, serviceDuration, matchedPeIdx,
											finalActivityDestination, startTime, endTime);

								}
							}

						}

						candidateCounter++;

					}
					String randomNeighbourZone = neighbourZoneList.get(r.nextInt(neighbourZoneList.size()));
					zone = randomNeighbourZone;
					checkedZonesCounter++;
				}
			}
		}
		return null;

	}

	public Triple<Integer, Double, Double> getTimeConstrainedActivity(Id<Person> candidatePerson,
			List<Integer> peIndexList, double serviceDuration) {

		// List<Integer> matchedActsList = new ArrayList<Integer>();
		// Left = peIdx, Middle = ActStartTime, Right = ActEndTime
		List<Triple<Integer, Double, Double>> matchedActsList2 = new ArrayList<>();

		Plan plan = this.scenario.getPopulation().getPersons().get(candidatePerson).getSelectedPlan();

		for (Integer peIdx : peIndexList) {
			Double actStartTime = Double.NaN;
			Double actEndTime = Double.NaN;

			Activity activity = (Activity) plan.getPlanElements().get(peIdx);

			Object actAttr = activity.getAttributes().getAttribute("jobId");

			if (actAttr != null) {

				int servicesPerAct = Arrays.asList(activity.getAttributes().getAttribute("jobId").toString().split(";"))
						.size();

				if (servicesPerAct >= maxServicesPerAct) {
					continue;
				}

			}

			if (peIdx > 1) {

				Leg prevLeg = (Leg) plan.getPlanElements().get(peIdx - 1);

				if (prevLeg.getTravelTime() == Double.NEGATIVE_INFINITY) {
					throw new IllegalArgumentException(
							"Found leg with no travle time! Please provide iterated output plans");
				}

				actStartTime = prevLeg.getDepartureTime() + prevLeg.getTravelTime();
			} else {
				actStartTime = 0.0;
			}

			actEndTime = activity.getEndTime();

			if (actEndTime == Double.NEGATIVE_INFINITY) {
				actEndTime = 30 * 3600.0;
			}

			if (actStartTime != Double.NaN && actEndTime != Double.NaN) {

				Double actDur = actEndTime - actStartTime;

				if (serviceDuration < actDur) {
					// matchedActsList.add(peIdx);
					matchedActsList2.add(Triple.of(peIdx, actStartTime, actEndTime));

				}

			} else
				throw new IllegalArgumentException("Act duration in plans undefinied!");

		}

		if (!matchedActsList2.isEmpty()) {
			return matchedActsList2.get(r.nextInt(matchedActsList2.size()));
		}
		return null;

	}

	public void writeCustomerDemand() {
		String header = "serviceType;activityType;x;y";

		String sep = ";";
		BufferedWriter bw = IOUtils.getBufferedWriter(this.matsimInput + "Population//CustomerDemandXY.csv");
		try {
			bw.write(header);
			for (Job job : jobList) {

				bw.newLine();
				String row = job.serviceType + sep + job.regularAgentActivity.getType() + sep
						+ job.regularAgentActivity.getCoord().getX() + sep + job.regularAgentActivity.getCoord().getY();
				bw.write(row);

			}
			bw.flush();
			bw.close();
			System.out.println("Customer Demand written!");
		} catch (IOException entry) {
			// TODO Auto-generated catch block
			entry.printStackTrace();
		}

	}

	public void initializeActCandidateMap() {
		// Set<String> serviceTypeKeys =
		// this.commercialTripReader.commercialTripMap.keySet();
		Set<String> serviceTypeKeys = new HashSet<String>(this.commercialTripReader.commercialTripMap.keySet());

		char[] alphabetCharArray = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
		String[] result = new String(alphabetCharArray).split("");
		Set<String> alphabet = new HashSet<>(Arrays.asList(result));
		serviceTypeKeys.addAll(alphabet);

		Set<String> custRelKeys = new HashSet<String>(Arrays.asList("B2B", "B2C", "private"));
		Set<String> zoneKeys = demand.zoneMap.keySet();

		for (String serviceType : serviceTypeKeys) {

			actCandidatesMap.put(serviceType,
					new ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<Id<Person>, HashSet<Integer>>>>());

			for (String custRel : custRelKeys) {

				actCandidatesMap.get(serviceType).put(custRel,
						new ConcurrentHashMap<String, ConcurrentHashMap<Id<Person>, HashSet<Integer>>>());

				for (String zoneKey : zoneKeys) {
					actCandidatesMap.get(serviceType).get(custRel).put(zoneKey,
							new ConcurrentHashMap<Id<Person>, HashSet<Integer>>());

				}
			}
		}

	}

	// public void findNeighbourZones() {
	//
	// Map<String, Geometry> zoneMap = demand.zoneMap;
	//
	// for (String zone : zoneMap.keySet()) {
	// ArrayList<String> neighbourzones = new ArrayList<String>();
	// Geometry geometry = zoneMap.get(zone);
	// for (String neighbourzone : zoneMap.keySet()) {
	// Geometry neighbourgeometry = zoneMap.get(neighbourzone);
	// if (zone != neighbourzone && geometry.intersects(neighbourgeometry)) {
	// // System.out.println("Coordinate in "+ zone);
	// neighbourzones.add(neighbourzone);
	// // System.out.println("gefunden" + neighbourzone);
	// }
	// }
	// neighbourMap.put(zone, neighbourzones);
	// }
	// // System.out.println("halt Stop!");
	// }

	public void findCandidates() {

		Population population = scenario.getPopulation();

		///// B2B Type Set
		// ################################################################################

		Set<String> ATypeSet_B2B = new HashSet<String>();
		ATypeSet_B2B.add("work");

		Set<String> BTypeSet_B2B = new HashSet<String>();
		BTypeSet_B2B.add("work");

		Set<String> CTypeSet_B2B = new HashSet<String>();
		CTypeSet_B2B.add("work");

		Set<String> DTypeSet_B2B = new HashSet<String>();
		DTypeSet_B2B.add("work");

		Set<String> ETypeSet_B2B = new HashSet<String>();
		ETypeSet_B2B.add("work");

		Set<String> FTypeSet_B2B = new HashSet<String>();
		FTypeSet_B2B.add("work");

		Set<String> GTypeSet_B2B = new HashSet<String>();
		GTypeSet_B2B.add("work");

		Set<String> HTypeSet_B2B = new HashSet<String>();
		HTypeSet_B2B.add("work");

		Set<String> ITypeSet_B2B = new HashSet<String>();
		ITypeSet_B2B.add("work");

		Set<String> JTypeSet_B2B = new HashSet<String>();
		JTypeSet_B2B.add("work");

		Set<String> KTypeSet_B2B = new HashSet<String>();
		KTypeSet_B2B.add("work");

		Set<String> LTypeSet_B2B = new HashSet<String>();
		LTypeSet_B2B.add("work");

		Set<String> NTypeSet_B2B = new HashSet<String>();
		NTypeSet_B2B.add("work");

		Set<String> MTypeSet_B2B = new HashSet<String>();
		MTypeSet_B2B.add("work");

		Set<String> OTypeSet_B2B = new HashSet<String>();
		OTypeSet_B2B.add("work");

		Set<String> PTypeSet_B2B = new HashSet<String>();
		PTypeSet_B2B.add("work");

		Set<String> QTypeSet_B2B = new HashSet<String>();
		QTypeSet_B2B.add("work");

		Set<String> RTypeSet_B2B = new HashSet<String>();
		RTypeSet_B2B.add("work");

		Set<String> STypeSet_B2B = new HashSet<String>();
		STypeSet_B2B.add("work");

		Set<String> TTypeSet_B2B = new HashSet<String>();
		TTypeSet_B2B.add("work");

		Set<String> UTypeSet_B2B = new HashSet<String>();
		UTypeSet_B2B.add("work");

		Set<String> VTypeSet_B2B = new HashSet<String>();
		VTypeSet_B2B.add("work");

		////// B2C Type Set
		// ################################################################################
		Set<String> ATypeSet_B2C = new HashSet<String>();
		ATypeSet_B2C.add("home");

		Set<String> BTypeSet_B2C = new HashSet<String>();
		BTypeSet_B2C.add("home");

		Set<String> CTypeSet_B2C = new HashSet<String>();
		CTypeSet_B2C.add("home");

		Set<String> DTypeSet_B2C = new HashSet<String>();
		DTypeSet_B2C.add("home");

		Set<String> ETypeSet_B2C = new HashSet<String>();
		ETypeSet_B2C.add("work");

		Set<String> FTypeSet_B2C = new HashSet<String>();
		FTypeSet_B2C.add("home");

		Set<String> GTypeSet_B2C = new HashSet<String>();
		GTypeSet_B2C.add("home");

		Set<String> HTypeSet_B2C = new HashSet<String>();
		HTypeSet_B2C.add("home");

		Set<String> ITypeSet_B2C = new HashSet<String>();
		ITypeSet_B2C.add("home");

		Set<String> JTypeSet_B2C = new HashSet<String>();
		JTypeSet_B2C.add("home");

		Set<String> KTypeSet_B2C = new HashSet<String>();
		KTypeSet_B2C.add("home");

		Set<String> LTypeSet_B2C = new HashSet<String>();
		LTypeSet_B2C.add("home");

		Set<String> MTypeSet_B2C = new HashSet<String>();
		MTypeSet_B2C.add("home");

		Set<String> NTypeSet_B2C = new HashSet<String>();
		NTypeSet_B2C.add("home");

		Set<String> OTypeSet_B2C = new HashSet<String>();
		OTypeSet_B2C.add("home");

		Set<String> PTypeSet_B2C = new HashSet<String>();
		PTypeSet_B2C.add("home");

		Set<String> QTypeSet_B2C = new HashSet<String>();
		QTypeSet_B2C.add("home");

		Set<String> RTypeSet_B2C = new HashSet<String>();
		RTypeSet_B2C.add("home");

		Set<String> STypeSet_B2C = new HashSet<String>();
		STypeSet_B2C.add("home");

		Set<String> TTypeSet_B2C = new HashSet<String>();
		TTypeSet_B2C.add("home");

		Set<String> UTypeSet_B2C = new HashSet<String>();
		UTypeSet_B2C.add("home");

		Set<String> VTypeSet_B2C = new HashSet<String>();
		VTypeSet_B2C.add("home");

		Set<Id<Person>> potentialPersons = new HashSet<>();
		potentialPersons.addAll(population.getPersons().keySet());

		potentialPersons.parallelStream().forEach(personId -> {

			Person person = population.getPersons().get(personId);

			PersonUtils.removeUnselectedPlans(person);

			// Person person = myPerson.next();

			// for (Person person : population.getPersons().values()) {

			int age = (int) person.getAttributes().getAttribute("age");
			String mode = null;
			int peIdx = 0;
			List<Integer> primaryActIndices = getPrimaryActivitiesPeIdx(person.getSelectedPlan());

			for (PlanElement pe : person.getSelectedPlan().getPlanElements()) {

				if (pe instanceof Activity) {
					Double actStartTime = Double.NaN;
					Double actEndTime = Double.NaN;

					Activity activity = (Activity) pe;

					Coord coord = activity.getCoord();
					String zone = new Demand4CompanyClass().getZone(coord, demand.zoneMap);

					if (acceptedActivities.contains(activity.getType().split("_")[0]) && (zone != null)) {

						if (peIdx > 1) {

							Leg prevLeg = (Leg) person.getSelectedPlan().getPlanElements().get(peIdx - 1);

							if (prevLeg.getTravelTime() == Double.NEGATIVE_INFINITY) {
								throw new IllegalArgumentException(
										"Found leg with no travle time! Please provide iterated output plans");
							}

							actStartTime = prevLeg.getDepartureTime() + prevLeg.getTravelTime();
						} else {
							actStartTime = 0.0;
						}

						actEndTime = activity.getEndTime();

						if (actEndTime == Double.NEGATIVE_INFINITY) {
							actEndTime = 30 * 3600.0;
						}

						Double[] actInterval = new Double[] { actStartTime, actEndTime };

						///// B2B Act only
						//
						// #######################################################################################
						// Service Type A
						ActivityChecker ServiceTypeA_B2B_Checker = new ActivityChecker(activity, ATypeSet_B2B,
								actInterval, null);
						boolean isServiceTypeA_B2B = ServiceTypeA_B2B_Checker.proof();
						if (isServiceTypeA_B2B) {
							FillServiceMap("A", "B2B", zone, person.getId(), peIdx);
						}
						// Service Type B
						ActivityChecker ServiceTypeB_B2B_Checker = new ActivityChecker(activity, BTypeSet_B2B,
								actInterval, null);
						boolean isServiceTypeB_B2B = ServiceTypeB_B2B_Checker.proof();
						if (isServiceTypeB_B2B) {
							FillServiceMap("B", "B2B", zone, person.getId(), peIdx);
						}
						// Service Type C
						ActivityChecker ServiceTypeC_B2B_Checker = new ActivityChecker(activity, CTypeSet_B2B,
								actInterval, null);
						boolean isServiceTypeC_B2B = ServiceTypeC_B2B_Checker.proof();
						if (isServiceTypeC_B2B) {
							FillServiceMap("C", "B2B", zone, person.getId(), peIdx);
						}
						// Service Type D
						ActivityChecker ServiceTypeD_B2B_Checker = new ActivityChecker(activity, DTypeSet_B2B,
								actInterval, null);
						boolean isServiceTypeD_B2B = ServiceTypeD_B2B_Checker.proof();
						if (isServiceTypeD_B2B) {
							FillServiceMap("D", "B2B", zone, person.getId(), peIdx);
						}
						// Service Type E
						ActivityChecker ServiceTypeE_B2B_Checker = new ActivityChecker(activity, ETypeSet_B2B,
								actInterval, null);
						boolean isServiceTypeE_B2B = ServiceTypeE_B2B_Checker.proof();
						if (isServiceTypeE_B2B) {
							FillServiceMap("E", "B2B", zone, person.getId(), peIdx);
						}
						// Service Type F
						ActivityChecker ServiceTypeF_B2B_Checker = new ActivityChecker(activity, FTypeSet_B2B,
								actInterval, null);
						boolean isServiceTypeF_B2B = ServiceTypeF_B2B_Checker.proof();
						if (isServiceTypeF_B2B) {
							FillServiceMap("F", "B2B", zone, person.getId(), peIdx);
						}
						// Service Type G
						ActivityChecker ServiceTypeG_B2B_Checker = new ActivityChecker(activity, GTypeSet_B2B,
								actInterval, null);
						boolean isServiceTypeG_B2B = ServiceTypeG_B2B_Checker.proof();
						if (isServiceTypeG_B2B) {
							FillServiceMap("G", "B2B", zone, person.getId(), peIdx);
						}
						// Service Type H
						ActivityChecker ServiceTypeH_B2B_Checker = new ActivityChecker(activity, HTypeSet_B2B,
								actInterval, null);
						boolean isServiceTypeH_B2B = ServiceTypeH_B2B_Checker.proof();
						if (isServiceTypeH_B2B) {
							FillServiceMap("H", "B2B", zone, person.getId(), peIdx);
						}
						// Service Type I
						ActivityChecker ServiceTypeI_B2B_Checker = new ActivityChecker(activity, ITypeSet_B2B,
								actInterval, null);
						boolean isServiceTypeI_B2B = ServiceTypeI_B2B_Checker.proof();
						if (isServiceTypeI_B2B) {
							FillServiceMap("I", "B2B", zone, person.getId(), peIdx);
						}
						// Service Type J
						ActivityChecker ServiceTypeJ_B2B_Checker = new ActivityChecker(activity, JTypeSet_B2B,
								actInterval, null);
						boolean isServiceTypeJ_B2B = ServiceTypeJ_B2B_Checker.proof();
						if (isServiceTypeJ_B2B) {
							FillServiceMap("J", "B2B", zone, person.getId(), peIdx);
						}
						// Service Type K
						ActivityChecker ServiceTypeK_B2B_Checker = new ActivityChecker(activity, KTypeSet_B2B,
								actInterval, null);
						boolean isServiceTypeK_B2B = ServiceTypeK_B2B_Checker.proof();
						if (isServiceTypeK_B2B) {
							FillServiceMap("K", "B2B", zone, person.getId(), peIdx);
						}
						// Service Type L
						ActivityChecker ServiceTypeL_B2B_Checker = new ActivityChecker(activity, LTypeSet_B2B,
								actInterval, null);
						boolean isServiceTypeL_B2B = ServiceTypeL_B2B_Checker.proof();
						if (isServiceTypeL_B2B) {
							FillServiceMap("L", "B2B", zone, person.getId(), peIdx);
						}
						// Service Type M
						ActivityChecker ServiceTypeM_B2B_Checker = new ActivityChecker(activity, MTypeSet_B2B,
								actInterval, null);
						boolean isServiceTypeM_B2B = ServiceTypeM_B2B_Checker.proof();
						if (isServiceTypeM_B2B) {
							FillServiceMap("M", "B2B", zone, person.getId(), peIdx);
						}
						// Service Type N
						ActivityChecker ServiceTypeN_B2B_Checker = new ActivityChecker(activity, NTypeSet_B2B,
								actInterval, null);
						boolean isServiceTypeN_B2B = ServiceTypeN_B2B_Checker.proof();
						if (isServiceTypeN_B2B) {
							FillServiceMap("N", "B2B", zone, person.getId(), peIdx);
						}
						// Service Type O
						ActivityChecker ServiceTypeO_B2B_Checker = new ActivityChecker(activity, OTypeSet_B2B,
								actInterval, null);
						boolean isServiceTypeO_B2B = ServiceTypeO_B2B_Checker.proof();
						if (isServiceTypeO_B2B) {
							FillServiceMap("O", "B2B", zone, person.getId(), peIdx);
						}
						// Service Type P
						ActivityChecker ServiceTypeP_B2B_Checker = new ActivityChecker(activity, PTypeSet_B2B,
								actInterval, null);
						boolean isServiceTypeP_B2B = ServiceTypeP_B2B_Checker.proof();
						if (isServiceTypeP_B2B) {
							FillServiceMap("P", "B2B", zone, person.getId(), peIdx);
						}
						// Service Type Q
						ActivityChecker ServiceTypeQ_B2B_Checker = new ActivityChecker(activity, QTypeSet_B2B,
								actInterval, null);
						boolean isServiceTypeQ_B2B = ServiceTypeQ_B2B_Checker.proof();
						if (isServiceTypeQ_B2B) {
							FillServiceMap("Q", "B2B", zone, person.getId(), peIdx);
						}
						// Service Type R
						ActivityChecker ServiceTypeR_B2B_Checker = new ActivityChecker(activity, RTypeSet_B2B,
								actInterval, null);
						boolean isServiceTypeR_B2B = ServiceTypeR_B2B_Checker.proof();
						if (isServiceTypeR_B2B) {
							FillServiceMap("R", "B2B", zone, person.getId(), peIdx);
						}
						// Service Type S
						ActivityChecker ServiceTypeS_B2B_Checker = new ActivityChecker(activity, STypeSet_B2B,
								actInterval, null);
						boolean isServiceTypeS_B2B = ServiceTypeS_B2B_Checker.proof();
						if (isServiceTypeS_B2B) {
							FillServiceMap("S", "B2B", zone, person.getId(), peIdx);
						}
						// Service Type T
						ActivityChecker ServiceTypeT_B2B_Checker = new ActivityChecker(activity, TTypeSet_B2B,
								actInterval, null);
						boolean isServiceTypeT_B2B = ServiceTypeT_B2B_Checker.proof();
						if (isServiceTypeT_B2B) {
							FillServiceMap("T", "B2B", zone, person.getId(), peIdx);
						}
						// Service Type U
						ActivityChecker ServiceTypeU_B2B_Checker = new ActivityChecker(activity, UTypeSet_B2B,
								actInterval, null);
						boolean isServiceTypeU_B2B = ServiceTypeU_B2B_Checker.proof();
						if (isServiceTypeU_B2B) {
							FillServiceMap("U", "B2B", zone, person.getId(), peIdx);
						}
						// Service Type V
						ActivityChecker ServiceTypeV_B2B_Checker = new ActivityChecker(activity, VTypeSet_B2B,
								actInterval, null);
						boolean isServiceTypeV_B2B = ServiceTypeV_B2B_Checker.proof();
						if (isServiceTypeV_B2B) {
							FillServiceMap("V", "B2B", zone, person.getId(), peIdx);
						}

						//
						// #######################################################################################
						///// B2B Act and Age only
						//
						// #######################################################################################

						//
						// #######################################################################################
						///// B2B Act, Age, Mode
						//
						// #######################################################################################
						// Service Type A
						// ActivityChecker ServiceTypeA_B2B_Age_Mode_Checker = new
						// ActivityChecker(activity,
						// new int[] { 18, 55 }, ATypeSet_B2B, TransportMode.car, mode, age);
						// boolean isServiceTypeA_B2B_Age_Mode =
						// ServiceTypeA_B2B_Age_Mode_Checker.proof();

						//
						// #######################################################################################
						///// B2C Act only
						//
						// #######################################################################################
						// Service Type A
						ActivityChecker ServiceTypeA_B2C_Checker = new ActivityChecker(activity, ATypeSet_B2C,
								actInterval, null);
						boolean isServiceTypeA_B2C = ServiceTypeA_B2C_Checker.proof();
						if (isServiceTypeA_B2C) {
							FillServiceMap("A", "B2C", zone, person.getId(), peIdx);
						}
						// Service Type B
						ActivityChecker ServiceTypeB_B2C_Checker = new ActivityChecker(activity, BTypeSet_B2C,
								actInterval, null);
						boolean isServiceTypeB_B2C = ServiceTypeB_B2C_Checker.proof();
						if (isServiceTypeB_B2C) {
							FillServiceMap("B", "B2C", zone, person.getId(), peIdx);
						}
						// Service Type C
						ActivityChecker ServiceTypeC_B2C_Checker = new ActivityChecker(activity, CTypeSet_B2C,
								actInterval, null);
						boolean isServiceTypeC_B2C = ServiceTypeC_B2C_Checker.proof();
						if (isServiceTypeC_B2C) {
							FillServiceMap("C", "B2C", zone, person.getId(), peIdx);
						}
						// Service Type D
						ActivityChecker ServiceTypeD_B2C_Checker = new ActivityChecker(activity, DTypeSet_B2C,
								actInterval, null);
						boolean isServiceTypeD_B2C = ServiceTypeD_B2C_Checker.proof();
						if (isServiceTypeD_B2C) {
							FillServiceMap("D", "B2C", zone, person.getId(), peIdx);
						}
						// Service Type E
						ActivityChecker ServiceTypeE_B2C_Checker = new ActivityChecker(activity, ETypeSet_B2C,
								actInterval, null);
						boolean isServiceTypeE_B2C = ServiceTypeE_B2C_Checker.proof();
						if (isServiceTypeE_B2C) {
							FillServiceMap("E", "B2C", zone, person.getId(), peIdx);
						}
						// Service Type F
						ActivityChecker ServiceTypeF_B2C_Checker = new ActivityChecker(activity, FTypeSet_B2C,
								actInterval, null);
						boolean isServiceTypeF_B2C = ServiceTypeF_B2C_Checker.proof();
						if (isServiceTypeF_B2C) {
							FillServiceMap("F", "B2C", zone, person.getId(), peIdx);
						}
						// Service Type G
						ActivityChecker ServiceTypeG_B2C_Checker = new ActivityChecker(activity, GTypeSet_B2C,
								actInterval, null);
						boolean isServiceTypeG_B2C = ServiceTypeG_B2C_Checker.proof();
						if (isServiceTypeG_B2C) {
							FillServiceMap("G", "B2C", zone, person.getId(), peIdx);
						}
						// Service Type H
						ActivityChecker ServiceTypeH_B2C_Checker = new ActivityChecker(activity, HTypeSet_B2C,
								actInterval, null);
						boolean isServiceTypeH_B2C = ServiceTypeH_B2C_Checker.proof();
						if (isServiceTypeH_B2C) {
							FillServiceMap("H", "B2C", zone, person.getId(), peIdx);
						}
						// Service Type I
						ActivityChecker ServiceTypeI_B2C_Checker = new ActivityChecker(activity, ITypeSet_B2C,
								actInterval, null);
						boolean isServiceTypeI_B2C = ServiceTypeI_B2C_Checker.proof();
						if (isServiceTypeI_B2C) {
							FillServiceMap("I", "B2C", zone, person.getId(), peIdx);
						}
						// Service Type J
						ActivityChecker ServiceTypeJ_B2C_Checker = new ActivityChecker(activity, JTypeSet_B2C,
								actInterval, null);
						boolean isServiceTypeJ_B2C = ServiceTypeJ_B2C_Checker.proof();
						if (isServiceTypeJ_B2C) {
							FillServiceMap("J", "B2C", zone, person.getId(), peIdx);
						}
						// Service Type K
						ActivityChecker ServiceTypeK_B2C_Checker = new ActivityChecker(activity, KTypeSet_B2C,
								actInterval, null);
						boolean isServiceTypeK_B2C = ServiceTypeK_B2C_Checker.proof();
						if (isServiceTypeK_B2C) {
							FillServiceMap("K", "B2C", zone, person.getId(), peIdx);
						}
						// Service Type L
						ActivityChecker ServiceTypeL_B2C_Checker = new ActivityChecker(activity, LTypeSet_B2C,
								actInterval, null);
						boolean isServiceTypeL_B2C = ServiceTypeL_B2C_Checker.proof();
						if (isServiceTypeL_B2C) {
							FillServiceMap("L", "B2C", zone, person.getId(), peIdx);
						}
						// Service Type M
						ActivityChecker ServiceTypeM_B2C_Checker = new ActivityChecker(activity, MTypeSet_B2C,
								actInterval, null);
						boolean isServiceTypeM_B2C = ServiceTypeM_B2C_Checker.proof();
						if (isServiceTypeM_B2C) {
							FillServiceMap("M", "B2C", zone, person.getId(), peIdx);
						}
						// Service Type N
						ActivityChecker ServiceTypeN_B2C_Checker = new ActivityChecker(activity, NTypeSet_B2C,
								actInterval, null);
						boolean isServiceTypeN_B2C = ServiceTypeN_B2C_Checker.proof();
						if (isServiceTypeN_B2C) {
							FillServiceMap("N", "B2C", zone, person.getId(), peIdx);
						}
						// Service Type O
						ActivityChecker ServiceTypeO_B2C_Checker = new ActivityChecker(activity, OTypeSet_B2C,
								actInterval, null);
						boolean isServiceTypeO_B2C = ServiceTypeO_B2C_Checker.proof();
						if (isServiceTypeO_B2C) {
							FillServiceMap("O", "B2C", zone, person.getId(), peIdx);
						}
						// Service Type P
						ActivityChecker ServiceTypeP_B2C_Checker = new ActivityChecker(activity, PTypeSet_B2C,
								actInterval, null);
						boolean isServiceTypeP_B2C = ServiceTypeP_B2C_Checker.proof();
						if (isServiceTypeP_B2C) {
							FillServiceMap("P", "B2C", zone, person.getId(), peIdx);
						}
						// Service Type Q
						ActivityChecker ServiceTypeQ_B2C_Checker = new ActivityChecker(activity, QTypeSet_B2C,
								actInterval, null);
						boolean isServiceTypeQ_B2C = ServiceTypeQ_B2C_Checker.proof();
						if (isServiceTypeQ_B2C) {
							FillServiceMap("Q", "B2C", zone, person.getId(), peIdx);
						}
						// Service Type R
						ActivityChecker ServiceTypeR_B2C_Checker = new ActivityChecker(activity, RTypeSet_B2C,
								actInterval, null);
						boolean isServiceTypeR_B2C = ServiceTypeR_B2C_Checker.proof();
						if (isServiceTypeR_B2C) {
							FillServiceMap("R", "B2C", zone, person.getId(), peIdx);
						}
						// Service Type S
						ActivityChecker ServiceTypeS_B2C_Checker = new ActivityChecker(activity, STypeSet_B2C,
								actInterval, null);
						boolean isServiceTypeS_B2C = ServiceTypeS_B2C_Checker.proof();
						if (isServiceTypeS_B2C) {
							FillServiceMap("S", "B2C", zone, person.getId(), peIdx);
						}
						// Service Type T
						ActivityChecker ServiceTypeT_B2C_Checker = new ActivityChecker(activity, TTypeSet_B2C,
								actInterval, null);
						boolean isServiceTypeT_B2C = ServiceTypeT_B2C_Checker.proof();
						if (isServiceTypeT_B2C) {
							FillServiceMap("T", "B2C", zone, person.getId(), peIdx);
						}
						// Service Type U
						ActivityChecker ServiceTypeU_B2C_Checker = new ActivityChecker(activity, UTypeSet_B2C,
								actInterval, null);
						boolean isServiceTypeU_B2C = ServiceTypeU_B2C_Checker.proof();
						if (isServiceTypeU_B2C) {
							FillServiceMap("U", "B2C", zone, person.getId(), peIdx);
						}
						// Service Type V
						ActivityChecker ServiceTypeV_B2C_Checker = new ActivityChecker(activity, VTypeSet_B2C,
								actInterval, null);
						boolean isServiceTypeV_B2C = ServiceTypeV_B2C_Checker.proof();
						if (isServiceTypeV_B2C) {
							FillServiceMap("V", "B2C", zone, person.getId(), peIdx);
						}

						//
						// #######################################################################################
						///// B2C Act and Age only
						//
						// #######################################################################################

						//
						// #######################################################################################
						///// B2C Act, Age, Mode
						//
						// #######################################################################################

						//
						// #######################################################################################

						mode = null;
					}

				}

				// Find valid main mode per act
				if (pe instanceof Leg) {
					Leg leg = (Leg) pe;

					if (leg.getTravelTime() == Double.NEGATIVE_INFINITY) {
						throw new IllegalArgumentException(
								"Found leg with no travle time! Please provide iterated output plans");
					}

					if (acceptedMainModes.contains(leg.getMode())) {
						mode = leg.getMode();

					}

				}

				peIdx++;
			}

		});

	}

	// public static void fillServiceDefintionMap()
	// {
	// String serviceF_B2B = new ServiceDefinition("None","1","F",(8*3600);,String
	// deliveryTimeEnd)
	// }

	public List<Integer> getPrimaryActivitiesPeIdx(Plan plan) {
		List<Integer> idxList = new ArrayList<Integer>();
		int actIdx = 0;
		for (PlanElement pe : (plan.getPlanElements())) {

			if (pe instanceof Activity) {
				Activity act = (Activity) pe;
				String actType = act.getType().split("_")[0];
				if (acceptedActivities.contains(actType)) {
					idxList.add(actIdx);
				}
			}
			actIdx++;
		}
		return idxList;
	}

	public void FillServiceMap(String serviceType, String custRel, String zone, Id<Person> personID, Integer peIdx) {
		// static Map<String, Map<String,Map<String,Map<String,
		// Set<String>>>>>actCandidatesMap =new
		// HashMap<String,Map<String,Map<String,Map<String, Set<String>>>>>();
		if (actCandidatesMap.containsKey(serviceType)) {
			if (actCandidatesMap.get(serviceType).containsKey(custRel)) {

				if (actCandidatesMap.get(serviceType).get(custRel).containsKey(zone)) {
					if (actCandidatesMap.get(serviceType).get(custRel).get(zone).containsKey(personID)) {
						if (actCandidatesMap.get(serviceType).get(custRel).get(zone).get(personID).contains(peIdx)) {
							// finish do nothing
						} else {
							actCandidatesMap.get(serviceType).get(custRel).get(zone).get(personID).add(peIdx);
						}

					} else {
						actCandidatesMap.get(serviceType).get(custRel).get(zone).put(personID, new HashSet<Integer>());

						actCandidatesMap.get(serviceType).get(custRel).get(zone).get(personID).add(peIdx);
					}

				}
				// else {
				// actCandidatesMap.get(serviceType).get(custRel).put(zone,
				// new ConcurrentHashMap<Id<Person>, Set<Integer>>());
				//
				// actCandidatesMap.get(serviceType).get(custRel).get(zone).put(personID,
				// Collections.newSetFromMap(new ConcurrentHashMap<Integer, Boolean>()));
				// actCandidatesMap.get(serviceType).get(custRel).get(zone).get(personID).add(peIdx);
				// }
			}
			// else {
			// actCandidatesMap.get(serviceType).put(custRel,
			// new ConcurrentHashMap<String, ConcurrentHashMap<Id<Person>,
			// Set<Integer>>>());
			// actCandidatesMap.get(serviceType).get(custRel).put(zone,
			// new ConcurrentHashMap<Id<Person>, Set<Integer>>());
			// actCandidatesMap.get(serviceType).get(custRel).get(zone).put(personID,
			// Collections.newSetFromMap(new ConcurrentHashMap<Integer, Boolean>()));
			// actCandidatesMap.get(serviceType).get(custRel).get(zone).get(personID).add(peIdx);
			// }
		}

		// else {
		//
		// actCandidatesMap.put(serviceType,
		// new ConcurrentHashMap<String, ConcurrentHashMap<String,
		// ConcurrentHashMap<Id<Person>, Set<Integer>>>>());
		// actCandidatesMap.get(serviceType).put(custRel,
		// new ConcurrentHashMap<String, ConcurrentHashMap<Id<Person>,
		// Set<Integer>>>());
		// actCandidatesMap.get(serviceType).get(custRel).put(zone, new
		// ConcurrentHashMap<Id<Person>, Set<Integer>>());
		// actCandidatesMap.get(serviceType).get(custRel).get(zone).put(personID,
		// Collections.newSetFromMap(new ConcurrentHashMap<Integer, Boolean>()));
		// actCandidatesMap.get(serviceType).get(custRel).get(zone).get(personID).add(peIdx);
		// }

	}
}
