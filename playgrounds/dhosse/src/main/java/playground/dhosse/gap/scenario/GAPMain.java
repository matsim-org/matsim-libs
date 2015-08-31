package playground.dhosse.gap.scenario;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.ControlerConfigGroup.RoutingAlgorithmType;
import org.matsim.core.config.groups.CountsConfigGroup;
import org.matsim.core.config.groups.FacilitiesConfigGroup;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.config.groups.ParallelEventHandlingConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.LinkDynamics;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.config.groups.QSimConfigGroup.StarttimeInterpretation;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.config.groups.QSimConfigGroup.VehicleBehavior;
import org.matsim.core.config.groups.QSimConfigGroup.VehiclesSource;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.config.TransitConfigGroup;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.vehicles.VehicleWriterV1;

import playground.agarwalamit.munich.inputs.AddingActivitiesInPlans;
import playground.dhosse.gap.Global;
import playground.dhosse.gap.scenario.population.Municipalities;


public class GAPMain {
	
//	Main class for the generation of the Garmisch-Partenkirchen scenario.
	
	//84948
	
	public static double n = 0;
	public static final int N = 86336;
	
	final double factor2005_2009 = 0.9883802132;

	public static final Random random = MatsimRandom.getRandom();
	
	static final String fromCrs = "EPSG:4326";
	
	public static final String toCrs = "EPSG:32632";
	public final String GK4 = TransformationFactory.DHDN_GK4;
	
	public static final CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(fromCrs, toCrs);
	static final CoordinateTransformation reverseCt = TransformationFactory.getCoordinateTransformation(toCrs, fromCrs);
	static final CoordinateTransformation gk4ToUTM32N = TransformationFactory.getCoordinateTransformation(TransformationFactory.DHDN_GK4, toCrs);
	static final CoordinateTransformation UTM32NtoGK4 = TransformationFactory.getCoordinateTransformation(toCrs, TransformationFactory.DHDN_GK4);
	
	static final String smbDir = "/run/user/1009/gvfs/smb-share:server=innoz-dc01,share=innoz/";
	static final String adminBordersDir = smbDir + "3_Allgemein/Geoinformation/Administrative_Grenzen/"; //gemeinden_2009.shp
	static final String projectDir = smbDir + "2_MediengestützteMobilität/10_Projekte/eGAP/";
	static final String dataDir = projectDir + "20_Datengrundlage/";
	static final String networkDataDir = dataDir + "Netzwerk/";
	static final String matsimDir = projectDir + "30_Modellierung/";
	public static final String matsimInputDir = matsimDir + "INPUT/";
	public static final String matsimOutputDir = matsimDir + "OUTPUT/" + Global.runID + "/output";
	
	public static void main(String[] args) {
		
		MatsimRandom.reset(4711);
		
		Config config = ConfigUtils.createConfig();
		
//		configureConfig(config);
		
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		new MatsimNetworkReader(scenario).readFile(matsimInputDir + "Netzwerk/network.xml");
		
//		createTransit(scenario);
		
		//create network
//		Network network = GAPScenarioBuilder.createNetwork(scenario, networkDataDir + "merged-network.osm");
//		new NetworkWriter(network).write(matsimInputDir + "Netzwerk/network_final.xml");
		
		double fractionOfStudents = 0.614213;
		Municipalities.addEntry("09180112", (int) (446*fractionOfStudents), 1559, 526);						//Bad Kohlgrub
		Municipalities.addEntry("09180113", (int) (169*fractionOfStudents), 772, 223);						//Bad Bayersoien
		Municipalities.addEntry("09180114", (int) (269*fractionOfStudents), 908, 368);						//Eschenlohe
		Municipalities.addEntry("09180115", (int) (103*fractionOfStudents), 534, 368);						//Ettal
		Municipalities.addEntry("09180116", (int) (636*fractionOfStudents), 2288, 759);						//Farchant
		Municipalities.addEntry("09180117", (int) (3744*fractionOfStudents), 15016, 7308);					//Garmisch-Partenkirchen
		Municipalities.addEntry("09180118", (int) (501*fractionOfStudents), 2193, 841);						//Grainau
		Municipalities.addEntry("09180119", (int) (281*fractionOfStudents), 895, 280);						//Großweil
		Municipalities.addEntry("09180122", (int) (283*fractionOfStudents), 1193, 446);						//Krün
		Municipalities.addEntry("09180123", (int) (978*fractionOfStudents), 4622, 1834);					//Mittenwald
		Municipalities.addEntry("09180124", (int) (2003*fractionOfStudents), 7508, 2751);					//Murnau a. Staffelsee
		Municipalities.addEntry("09180125", (int) (890*fractionOfStudents), 3085, 1253);					//Oberammergau
		Municipalities.addEntry("09180126", (int) (528*fractionOfStudents), 1787, 691);						//Oberau
		Municipalities.addEntry("09180127", (int) (596*fractionOfStudents), 1982, 632);						//Ohlstadt
		Municipalities.addEntry("09180128", (int) (206*fractionOfStudents), 683, 266);						//Riegsee
		Municipalities.addEntry("09180129", (int) (313*fractionOfStudents), 1034, 301);						//Saulgrub
		Municipalities.addEntry("09180131", (int) (111*fractionOfStudents), 335, 155);						//Schwaigen
		Municipalities.addEntry("09180132", (int) (486*fractionOfStudents), 1415, 600);						//Seehausen a. Staffelsee
		Municipalities.addEntry("09180133", (int) (151*fractionOfStudents), 491, 123);						//Spatzenhausen
		Municipalities.addEntry("09180134", (int) (582*fractionOfStudents), 1813, 583);						//Uffing a. Staffelsee
		Municipalities.addEntry("09180135", (int) (294*fractionOfStudents), 857, 315);						//Unterammergau
		Municipalities.addEntry("09180136", (int) (265*fractionOfStudents), 823, 306);						//Wallgau
		
		GAPScenarioBuilder.initAmenities(scenario);
		
		//create population
		Population population = GAPScenarioBuilder.createPlans(scenario, matsimInputDir + "Argentur_für_Arbeit/Garmisch_Einpendler.csv", matsimInputDir + "Argentur_für_Arbeit/Garmisch_Auspendler.csv", false);
		GAPMain.n = GAPScenarioBuilder.nPersons;
		
//		for(Person p : population.getPersons().values()){
//			for(PlanElement pe : p.getSelectedPlan().getPlanElements()){
//				if(pe instanceof Activity){
//					
//					Activity act = (Activity)pe;
//					
//					if(act.getStartTime() != Time.UNDEFINED_TIME && act.getEndTime() != Time.UNDEFINED_TIME){
//						
//						if(act.getEndTime() - act.getStartTime() <= 0){
//							System.out.println(p.getId().toString());
//						}
//						
//					}
//					
//				}
//			}
//		}
		
		AddingActivitiesInPlans aaip = new AddingActivitiesInPlans(scenario);
		aaip.run();
		
		SortedMap<String, Tuple<Double, Double>> acts = aaip.getActivityType2TypicalAndMinimalDuration();
		
		for(String act : acts.keySet()){
			
			ActivityParams params = new ActivityParams();
			params.setActivityType(act);
			params.setTypicalDuration(acts.get(act).getFirst());
			params.setMinimalDuration(acts.get(act).getSecond());
			params.setClosingTime(Time.UNDEFINED_TIME);
			params.setEarliestEndTime(Time.UNDEFINED_TIME);
			params.setLatestStartTime(Time.UNDEFINED_TIME);
			params.setOpeningTime(Time.UNDEFINED_TIME);
			config.planCalcScore().addActivityParams(params);
			
		}
		
		new PopulationWriter(aaip.getOutPop()).write(matsimInputDir + "Pläne/plans_mid.xml.gz");
		
		//TODO create counting stations
//		Counts counts = GAPScenarioBuilder.createCountingStations(network);
//		new CountsWriter(counts).write(matsimInputDir + "Counts/counts.xml");
		
		//configure flow / storage cap factor, counts scale factor
//		configureCountsConfigGroup(config.counts());
//		configureQSimConfigGroup(config.qsim());
		
		new ConfigWriter(config).write(matsimInputDir + "config.xml");
		
	}

	private static void createTransit(Scenario scenario) {
		
		new TransitScheduleReader(scenario).readFile(matsimInputDir + "transit/schedule_stopsOnly.xml");
		
		TransitScheduleCSVReader reader = new TransitScheduleCSVReader(scenario);
		reader.readFileAndAddLines("/home/dhosse/workspace/MAS/input_data/Fahrpläne_zum_Einlesen/9606.csv", "bus");
		reader.readFileAndAddLines("/home/dhosse/workspace/MAS/input_data/Fahrpläne_zum_Einlesen/9606_2.csv", "bus");
		reader.readFileAndAddLines("/home/dhosse/workspace/MAS/input_data/Fahrpläne_zum_Einlesen/9608.csv", "bus");
		reader.readFileAndAddLines("/home/dhosse/workspace/MAS/input_data/Fahrpläne_zum_Einlesen/9608_2.csv", "bus");
		reader.readFileAndAddLines("/home/dhosse/workspace/MAS/input_data/Fahrpläne_zum_Einlesen/Linie_1_1.csv", "bus");
		reader.readFileAndAddLines("/home/dhosse/workspace/MAS/input_data/Fahrpläne_zum_Einlesen/Linie_1_2.csv", "bus");
		reader.readFileAndAddLines("/home/dhosse/workspace/MAS/input_data/Fahrpläne_zum_Einlesen/Linie_2_1.csv", "bus");
		reader.readFileAndAddLines("/home/dhosse/workspace/MAS/input_data/Fahrpläne_zum_Einlesen/Linie_2_2.csv", "bus");
		reader.readFileAndAddLines("/home/dhosse/workspace/MAS/input_data/Fahrpläne_zum_Einlesen/Linie_3.csv", "bus");
		reader.readFileAndAddLines("/home/dhosse/workspace/MAS/input_data/Fahrpläne_zum_Einlesen/Linie_3_2.csv", "bus");
		reader.readFileAndAddLines("/home/dhosse/workspace/MAS/input_data/Fahrpläne_zum_Einlesen/Linie_4.csv", "bus");
		reader.readFileAndAddLines("/home/dhosse/workspace/MAS/input_data/Fahrpläne_zum_Einlesen/Linie_5.csv", "bus");
		
		new TransitScheduleWriter(scenario.getTransitSchedule()).writeFile(matsimInputDir + "transit/scheduleComplete.xml");
		new VehicleWriterV1(scenario.getTransitVehicles()).writeFile(matsimInputDir + "transit/transitVehicles.xml");
		
	}
	
}
//run osmosis to filter ways from the osm file
//maybe better to be run from shell
//try {
//
//	Process process = Runtime.getRuntime().exec(workingDir + "network.sh");
//	process.waitFor();
//	InputStream is = process.getInputStream();
//	BufferedReader reader = new BufferedReader(new InputStreamReader(is));	
//	
//	String line = null;
//	while( (line = reader.readLine()) != null) {
//		System.out.println(line);
//	}
//
//} catch (IOException e) {
//	
//	e.printStackTrace();
//	
//} catch (InterruptedException e) {
//	
//	e.printStackTrace();
//	
//}