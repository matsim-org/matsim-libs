package commercialtraffic.demandAssigment;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

import ft.utils.ctDemandPrep.Demand4CompanyClass;
import ft.utils.ctDemandPrep.DemandGenerator;

public class AssignService {
	String plansFile;
	static Set<String> acceptedMainModes = new HashSet<>(
			Arrays.asList("car", "pt", "drt", "walk", "ride", "bike", "stayHome"));
	static Set<String> acceptedActivities = new HashSet<>(
			Arrays.asList("home", "work", "shopping", "leisure", "other", "education"));

	// ServiceType->cust.Relation->zone->PersonID->ListPE
	static Map<String, Map<String, Map<String, Map<Id<Person>, Set<Integer>>>>> actCandidatesMap = new ConcurrentHashMap<String, Map<String, Map<String, Map<Id<Person>, Set<Integer>>>>>();

	Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	String companyFolder;
	String zoneSHP;
	String outputpath;

	AssignService(String plansFile) {
		new PopulationReader(scenario).readFile(plansFile);
	}

	public static void main(String[] args) {

		AssignService assignData = new AssignService(
				"D:\\Thiel\\Programme\\MatSim\\01_HannoverModel_2.0\\Simulation\\output\\vw243_cadON_ptSpeedAdj.0.1\\vw243_cadON_ptSpeedAdj.0.1.output_plans.xml.gz");

		String companyFolder = "D:\\Thiel\\Programme\\WVModell\\00_Eingangsdaten\\Unternehmen\\";
		String zoneSHP = "D:\\Thiel\\Programme\\WVModell\\00_Eingangsdaten\\Zellen\\FNP_Merged\\baseShapeH.shp";
		String outputpath = "D:\\Thiel\\Programme\\WVModell\\00_Eingangsdaten\\";
		DemandGenerator demand = new DemandGenerator(companyFolder, zoneSHP, outputpath);

		Population population = assignData.scenario.getPopulation();

		Set<Id<Person>> dsa = population.getPersons().keySet();

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

			// Person person = myPerson.next();

			//for (Person person : population.getPersons().values()) {

			int age = (int) person.getAttributes().getAttribute("age");
			String mode = null;
			int peIdx = 0;

			for (PlanElement pe : person.getSelectedPlan().getPlanElements()) {
				peIdx++;
				if (pe instanceof Activity) {

					Activity activity = (Activity) pe;
					Coord coord = activity.getCoord();
					String zone = new Demand4CompanyClass().getZone(coord, demand.zoneMap);

					if (acceptedActivities.contains(activity.getType().split("_")[0]) && (zone != null)) {

						///// B2B Act only
						//
						// #######################################################################################
						// Service Type A
						ActivityChecker ServiceTypeA_B2B_Checker = new ActivityChecker(activity, ATypeSet_B2B);
						boolean isServiceTypeA_B2B = ServiceTypeA_B2B_Checker.proof();
						if (isServiceTypeA_B2B) {
							FillServiceMap("A", "B2B", zone, person.getId(), peIdx);
						}
						// Service Type B
						ActivityChecker ServiceTypeB_B2B_Checker = new ActivityChecker(activity, BTypeSet_B2B);
						boolean isServiceTypeB_B2B = ServiceTypeB_B2B_Checker.proof();
						if (isServiceTypeB_B2B) {
							FillServiceMap("B", "B2B", zone, person.getId(), peIdx);
						}
						// Service Type C
						ActivityChecker ServiceTypeC_B2B_Checker = new ActivityChecker(activity, CTypeSet_B2B);
						boolean isServiceTypeC_B2B = ServiceTypeC_B2B_Checker.proof();
						if (isServiceTypeC_B2B) {
							FillServiceMap("C", "B2B", zone, person.getId(), peIdx);
						}
						// Service Type D
						ActivityChecker ServiceTypeD_B2B_Checker = new ActivityChecker(activity, DTypeSet_B2B);
						boolean isServiceTypeD_B2B = ServiceTypeD_B2B_Checker.proof();
						if (isServiceTypeD_B2B) {
							FillServiceMap("D", "B2B", zone, person.getId(), peIdx);
						}
						// Service Type E
						ActivityChecker ServiceTypeE_B2B_Checker = new ActivityChecker(activity, ETypeSet_B2B);
						boolean isServiceTypeE_B2B = ServiceTypeE_B2B_Checker.proof();
						if (isServiceTypeE_B2B) {
							FillServiceMap("E", "B2B", zone, person.getId(), peIdx);
						}
						// Service Type F
						ActivityChecker ServiceTypeF_B2B_Checker = new ActivityChecker(activity, FTypeSet_B2B);
						boolean isServiceTypeF_B2B = ServiceTypeF_B2B_Checker.proof();
						if (isServiceTypeF_B2B) {
							FillServiceMap("F", "B2B", zone, person.getId(), peIdx);
						}
						// Service Type G
						ActivityChecker ServiceTypeG_B2B_Checker = new ActivityChecker(activity, GTypeSet_B2B);
						boolean isServiceTypeG_B2B = ServiceTypeG_B2B_Checker.proof();
						if (isServiceTypeG_B2B) {
							FillServiceMap("G", "B2B", zone, person.getId(), peIdx);
						}
						// Service Type H
						ActivityChecker ServiceTypeH_B2B_Checker = new ActivityChecker(activity, HTypeSet_B2B);
						boolean isServiceTypeH_B2B = ServiceTypeH_B2B_Checker.proof();
						if (isServiceTypeH_B2B) {
							FillServiceMap("H", "B2B", zone, person.getId(), peIdx);
						}
						// Service Type I
						ActivityChecker ServiceTypeI_B2B_Checker = new ActivityChecker(activity, ITypeSet_B2B);
						boolean isServiceTypeI_B2B = ServiceTypeI_B2B_Checker.proof();
						if (isServiceTypeI_B2B) {
							FillServiceMap("I", "B2B", zone, person.getId(), peIdx);
						}
						// Service Type J
						ActivityChecker ServiceTypeJ_B2B_Checker = new ActivityChecker(activity, JTypeSet_B2B);
						boolean isServiceTypeJ_B2B = ServiceTypeJ_B2B_Checker.proof();
						if (isServiceTypeJ_B2B) {
							FillServiceMap("J", "B2B", zone, person.getId(), peIdx);
						}
						// Service Type K
						ActivityChecker ServiceTypeK_B2B_Checker = new ActivityChecker(activity, KTypeSet_B2B);
						boolean isServiceTypeK_B2B = ServiceTypeK_B2B_Checker.proof();
						if (isServiceTypeK_B2B) {
							FillServiceMap("K", "B2B", zone, person.getId(), peIdx);
						}
						// Service Type L
						ActivityChecker ServiceTypeL_B2B_Checker = new ActivityChecker(activity, LTypeSet_B2B);
						boolean isServiceTypeL_B2B = ServiceTypeL_B2B_Checker.proof();
						if (isServiceTypeL_B2B) {
							FillServiceMap("L", "B2B", zone, person.getId(), peIdx);
						}
						// Service Type M
						ActivityChecker ServiceTypeM_B2B_Checker = new ActivityChecker(activity, MTypeSet_B2B);
						boolean isServiceTypeM_B2B = ServiceTypeM_B2B_Checker.proof();
						if (isServiceTypeM_B2B) {
							FillServiceMap("M", "B2B", zone, person.getId(), peIdx);
						}
						// Service Type N
						ActivityChecker ServiceTypeN_B2B_Checker = new ActivityChecker(activity, NTypeSet_B2B);
						boolean isServiceTypeN_B2B = ServiceTypeN_B2B_Checker.proof();
						if (isServiceTypeN_B2B) {
							FillServiceMap("N", "B2B", zone, person.getId(), peIdx);
						}
						// Service Type O
						ActivityChecker ServiceTypeO_B2B_Checker = new ActivityChecker(activity, OTypeSet_B2B);
						boolean isServiceTypeO_B2B = ServiceTypeO_B2B_Checker.proof();
						if (isServiceTypeO_B2B) {
							FillServiceMap("O", "B2B", zone, person.getId(), peIdx);
						}
						// Service Type P
						ActivityChecker ServiceTypeP_B2B_Checker = new ActivityChecker(activity, PTypeSet_B2B);
						boolean isServiceTypeP_B2B = ServiceTypeP_B2B_Checker.proof();
						if (isServiceTypeP_B2B) {
							FillServiceMap("P", "B2B", zone, person.getId(), peIdx);
						}
						// Service Type Q
						ActivityChecker ServiceTypeQ_B2B_Checker = new ActivityChecker(activity, QTypeSet_B2B);
						boolean isServiceTypeQ_B2B = ServiceTypeQ_B2B_Checker.proof();
						if (isServiceTypeQ_B2B) {
							FillServiceMap("Q", "B2B", zone, person.getId(), peIdx);
						}
						// Service Type R
						ActivityChecker ServiceTypeR_B2B_Checker = new ActivityChecker(activity, RTypeSet_B2B);
						boolean isServiceTypeR_B2B = ServiceTypeR_B2B_Checker.proof();
						if (isServiceTypeR_B2B) {
							FillServiceMap("R", "B2B", zone, person.getId(), peIdx);
						}
						// Service Type S
						ActivityChecker ServiceTypeS_B2B_Checker = new ActivityChecker(activity, STypeSet_B2B);
						boolean isServiceTypeS_B2B = ServiceTypeS_B2B_Checker.proof();
						if (isServiceTypeS_B2B) {
							FillServiceMap("S", "B2B", zone, person.getId(), peIdx);
						}
						// Service Type T
						ActivityChecker ServiceTypeT_B2B_Checker = new ActivityChecker(activity, TTypeSet_B2B);
						boolean isServiceTypeT_B2B = ServiceTypeT_B2B_Checker.proof();
						if (isServiceTypeT_B2B) {
							FillServiceMap("T", "B2B", zone, person.getId(), peIdx);
						}
						// Service Type U
						ActivityChecker ServiceTypeU_B2B_Checker = new ActivityChecker(activity, UTypeSet_B2B);
						boolean isServiceTypeU_B2B = ServiceTypeU_B2B_Checker.proof();
						if (isServiceTypeU_B2B) {
							FillServiceMap("U", "B2B", zone, person.getId(), peIdx);
						}
						// Service Type V
						ActivityChecker ServiceTypeV_B2B_Checker = new ActivityChecker(activity, VTypeSet_B2B);
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
						ActivityChecker ServiceTypeA_B2C_Checker = new ActivityChecker(activity, ATypeSet_B2C);
						boolean isServiceTypeA_B2C = ServiceTypeA_B2C_Checker.proof();
						// Service Type B
						ActivityChecker ServiceTypeB_B2C_Checker = new ActivityChecker(activity, BTypeSet_B2C);
						boolean isServiceTypeB_B2C = ServiceTypeB_B2C_Checker.proof();
						// Service Type C
						ActivityChecker ServiceTypeC_B2C_Checker = new ActivityChecker(activity, CTypeSet_B2C);
						boolean isServiceTypeC_B2C = ServiceTypeC_B2C_Checker.proof();
						// Service Type D
						ActivityChecker ServiceTypeD_B2C_Checker = new ActivityChecker(activity, DTypeSet_B2C);
						boolean isServiceTypeD_B2C = ServiceTypeD_B2C_Checker.proof();
						// Service Type E
						ActivityChecker ServiceTypeE_B2C_Checker = new ActivityChecker(activity, ETypeSet_B2C);
						boolean isServiceTypeE_B2C = ServiceTypeE_B2C_Checker.proof();
						// Service Type F
						ActivityChecker ServiceTypeF_B2C_Checker = new ActivityChecker(activity, FTypeSet_B2C);
						boolean isServiceTypeF_B2C = ServiceTypeF_B2C_Checker.proof();
						// Service Type G
						ActivityChecker ServiceTypeG_B2C_Checker = new ActivityChecker(activity, GTypeSet_B2C);
						boolean isServiceTypeG_B2C = ServiceTypeG_B2C_Checker.proof();
						// Service Type H
						ActivityChecker ServiceTypeH_B2C_Checker = new ActivityChecker(activity, HTypeSet_B2C);
						boolean isServiceTypeH_B2C = ServiceTypeH_B2C_Checker.proof();
						// Service Type I
						ActivityChecker ServiceTypeI_B2C_Checker = new ActivityChecker(activity, ITypeSet_B2C);
						boolean isServiceTypeI_B2C = ServiceTypeI_B2C_Checker.proof();
						// Service Type J
						ActivityChecker ServiceTypeJ_B2C_Checker = new ActivityChecker(activity, JTypeSet_B2C);
						boolean isServiceTypeJ_B2C = ServiceTypeJ_B2C_Checker.proof();
						// Service Type K
						ActivityChecker ServiceTypeK_B2C_Checker = new ActivityChecker(activity, KTypeSet_B2C);
						boolean isServiceTypeK_B2C = ServiceTypeK_B2C_Checker.proof();
						// Service Type L
						ActivityChecker ServiceTypeL_B2C_Checker = new ActivityChecker(activity, LTypeSet_B2C);
						boolean isServiceTypeL_B2C = ServiceTypeL_B2C_Checker.proof();
						// Service Type M
						ActivityChecker ServiceTypeM_B2C_Checker = new ActivityChecker(activity, MTypeSet_B2C);
						boolean isServiceTypeM_B2C = ServiceTypeM_B2C_Checker.proof();
						// Service Type N
						ActivityChecker ServiceTypeN_B2C_Checker = new ActivityChecker(activity, NTypeSet_B2C);
						boolean isServiceTypeN_B2C = ServiceTypeN_B2C_Checker.proof();
						// Service Type O
						ActivityChecker ServiceTypeO_B2C_Checker = new ActivityChecker(activity, OTypeSet_B2C);
						boolean isServiceTypeO_B2C = ServiceTypeO_B2C_Checker.proof();
						// Service Type P
						ActivityChecker ServiceTypeP_B2C_Checker = new ActivityChecker(activity, PTypeSet_B2C);
						boolean isServiceTypeP_B2C = ServiceTypeP_B2C_Checker.proof();
						// Service Type Q
						ActivityChecker ServiceTypeQ_B2C_Checker = new ActivityChecker(activity, QTypeSet_B2C);
						boolean isServiceTypeQ_B2C = ServiceTypeQ_B2C_Checker.proof();
						// Service Type R
						ActivityChecker ServiceTypeR_B2C_Checker = new ActivityChecker(activity, RTypeSet_B2C);
						boolean isServiceTypeR_B2C = ServiceTypeR_B2C_Checker.proof();
						// Service Type S
						ActivityChecker ServiceTypeS_B2C_Checker = new ActivityChecker(activity, STypeSet_B2C);
						boolean isServiceTypeS_B2C = ServiceTypeS_B2C_Checker.proof();
						// Service Type T
						ActivityChecker ServiceTypeT_B2C_Checker = new ActivityChecker(activity, TTypeSet_B2C);
						boolean isServiceTypeT_B2C = ServiceTypeT_B2C_Checker.proof();
						// Service Type U
						ActivityChecker ServiceTypeU_B2C_Checker = new ActivityChecker(activity, UTypeSet_B2C);
						boolean isServiceTypeU_B2C = ServiceTypeU_B2C_Checker.proof();
						// Service Type V
						ActivityChecker ServiceTypeV_B2C_Checker = new ActivityChecker(activity, VTypeSet_B2C);
						boolean isServiceTypeV_B2C = ServiceTypeV_B2C_Checker.proof();

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

					if (acceptedMainModes.contains(leg.getMode())) {
						mode = leg.getMode();

					}

				}

			}

		}
		);


	}

	public static void FillServiceMap(String serviceType, String custRel, String zone, Id<Person> personID,
			Integer peIdx) {
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

				} else {
					actCandidatesMap.get(serviceType).get(custRel).put(zone, new ConcurrentHashMap<Id<Person>, Set<Integer>>());

					actCandidatesMap.get(serviceType).get(custRel).get(zone).put(personID, new HashSet<Integer>());
					actCandidatesMap.get(serviceType).get(custRel).get(zone).get(personID).add(peIdx);
				}
			} else {
				actCandidatesMap.get(serviceType).put(custRel, new ConcurrentHashMap<String, Map<Id<Person>, Set<Integer>>>());
				actCandidatesMap.get(serviceType).get(custRel).put(zone, new ConcurrentHashMap<Id<Person>, Set<Integer>>());
				actCandidatesMap.get(serviceType).get(custRel).get(zone).put(personID, new HashSet<Integer>());
				actCandidatesMap.get(serviceType).get(custRel).get(zone).get(personID).add(peIdx);
			}
		}

		else {

			actCandidatesMap.put(serviceType, new ConcurrentHashMap<String, Map<String, Map<Id<Person>, Set<Integer>>>>());
			actCandidatesMap.get(serviceType).put(custRel, new ConcurrentHashMap<String, Map<Id<Person>, Set<Integer>>>());
			actCandidatesMap.get(serviceType).get(custRel).put(zone, new ConcurrentHashMap<Id<Person>, Set<Integer>>());
			actCandidatesMap.get(serviceType).get(custRel).get(zone).put(personID, new HashSet<Integer>());
			actCandidatesMap.get(serviceType).get(custRel).get(zone).get(personID).add(peIdx);
		}

	}
}
