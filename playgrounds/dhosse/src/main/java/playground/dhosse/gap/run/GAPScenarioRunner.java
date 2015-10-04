package playground.dhosse.gap.run;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.carsharing.config.CarsharingConfigGroup;
import org.matsim.contrib.carsharing.config.FreeFloatingConfigGroup;
import org.matsim.contrib.carsharing.config.OneWayCarsharingConfigGroup;
import org.matsim.contrib.carsharing.config.TwoWayCarsharingConfigGroup;
import org.matsim.contrib.carsharing.control.listeners.CarsharingListener;
import org.matsim.contrib.carsharing.qsim.CarsharingQsimFactory;
import org.matsim.contrib.carsharing.replanning.CarsharingSubtourModeChoiceStrategy;
import org.matsim.contrib.carsharing.replanning.RandomTripToCarsharingStrategy;
import org.matsim.contrib.carsharing.runExample.CarsharingUtils;
import org.matsim.contrib.carsharing.runExample.RunCarsharing;
import org.matsim.contrib.carsharing.scoring.CarsharingScoringFunctionFactory;
import org.matsim.contrib.locationchoice.DestinationChoiceConfigGroup;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceBestResponseContext;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceInitializer;
import org.matsim.contrib.locationchoice.bestresponse.scoring.DCScoringFunctionFactory;
import org.matsim.contrib.locationchoice.facilityload.FacilitiesLoadCalculator;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl.Builder;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.modules.SubtourModeChoice;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.population.algorithms.XY2Links;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

import playground.dhosse.gap.Global;
import playground.dhosse.gap.analysis.SpatialAnalysis;

public class GAPScenarioRunner {

	private static final String inputPath = Global.matsimDir + "INPUT/";
	private static final String simInputPath = Global.matsimDir + "OUTPUT/" + Global.runID +"/input/";
	private static final String outputPath = "/run/user/1009/gvfs/smb-share:server=innoz-dc01,share=innoz/2_MediengestützteMobilität/10_Projekte/eGAP/30_Modellierung/OUTPUT/" + Global.runID + "/ouput_/";
	
	//configure innovative strategies you want to use
	private static final boolean addModeChoice = true;
	private static final boolean addTimeChoice = false;
	private static final boolean addLocationChoice = false;
	
	private static final boolean addCarsharing = true;
	
	/**
	 * edit the static method executed in the main method
	 * to run a different case
	 * 
	 * @param args
	 */
	public static void main(String args[]){
		
//		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
//		new MatsimPopulationReader(scenario).readFile(simInputPath + "plansV3.xml.gz");
//		new ObjectAttributesXmlReader(scenario.getPopulation().getPersonAttributes()).parse(simInputPath + "demographicAtts.xml");
//		
//		ObjectAttributes atts = new ObjectAttributes();
//		
//		for(Person p : scenario.getPopulation().getPersons().values()){
//			
//			if(p.getId().toString().startsWith("09180")){
//				
//				if(scenario.getPopulation().getPersonAttributes().getAttribute(p.getId().toString(), Global.LICENSE) != null){
//				
//					if(scenario.getPopulation().getPersonAttributes().getAttribute(p.getId().toString(), Global.LICENSE).equals("true")){
//						
//						if(scenario.getPopulation().getPersonAttributes().getAttribute(p.getId().toString(), Global.CAR_AVAIL).equals("true")){
//							
//							atts.putAttribute(p.getId().toString(), Global.USER_GROUP, Global.GP_CAR);
//							
//						} else {
//							
//							atts.putAttribute(p.getId().toString(), Global.USER_GROUP, Global.LICENSE_OWNER);
//							
//						}
//						
//					}
//					
//				}
//				
//			}
//			
//		}
//		
//		new ObjectAttributesXmlWriter(atts).writeFile(simInputPath + "subpopAttributes.xml.gz");
		
		run();
//		runAnalysis();
		
	}

	/**
	 * Runs the GP scenario.
	 */
	private static void run(){
		
		//create a new config and a new scenario and load it
		final Config config = ConfigUtils.createConfig();
		ConfigUtils.loadConfig(config, simInputPath + "config.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		//create a second scenario containing only the cleaned (road only) network
		//in order to map agents on car links
		Scenario scenario2 = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario2).readFile(config.network().getInputFile());
		new NetworkCleaner().run(scenario2.getNetwork());
		
		XY2Links xy2links = new XY2Links(scenario2);
		for(Person person : scenario.getPopulation().getPersons().values()){
			xy2links.run(person);
		}
		
		//create a new controler
		final Controler controler = new Controler(scenario);
		
		//by default, route choice is the only innovative strategy.
		//additional strategies can be switched on/off via boolean members (see above)
		
		if(addModeChoice){
			
			addModeChoice(controler);
			
		}
		
		if(addTimeChoice){
			
			addTimeChoice(controler);
			
		}
		
		if(addLocationChoice){
			
			addLocationChoice(controler);
			
		}
		
		if(addCarsharing){
			addCarsharing(controler);
		}
		
		//finally, add controler listeners and event handlers
		controler.getEvents().addHandler(new ZugspitzbahnFareHandler(controler));
		
		//start of the simulation
		controler.run();
		
	}
	
	/**
	 * Adds time choice as additional replanning strategy.
	 * The method creates separate strategy settings for each subpopulation.
	 * 
	 * @param controler
	 */
	private static void addTimeChoice(final Controler controler){
		
		//New strategy settings are created
		//Specify a name, a subpopulation and a weight for the new strategy and
		//add if to the existing ones in the strategy config group
		StrategySettings tam = new StrategySettings();
		tam.setStrategyName("TimeAllocationMutator");
		tam.setSubpopulation(null);
		tam.setWeight(0.1);
		controler.getConfig().strategy().addStrategySettings(tam);
		
		StrategySettings car = new StrategySettings();
		car.setStrategyName("TimeAllocationMutator");
		car.setSubpopulation(Global.GP_CAR);
		car.setWeight(0.1);
		controler.getConfig().strategy().addStrategySettings(car);
		
		StrategySettings license = new StrategySettings();
		license.setStrategyName("TimeAllocationMutator");
		license.setSubpopulation(Global.LICENSE_OWNER);
		license.setWeight(0.1);
		controler.getConfig().strategy().addStrategySettings(license);
		
		StrategySettings commuter = new StrategySettings();
		commuter.setStrategyName("TimeAllocationMutator");
		commuter.setSubpopulation(Global.COMMUTER);
		commuter.setWeight(0.1);
		controler.getConfig().strategy().addStrategySettings(commuter);
		
	}

	/**
	 * Adds destination choice as innovative strategy.
	 * @param controler
	 */
	private static void addLocationChoice(final Controler controler) {
		
		DestinationChoiceConfigGroup dccg = new DestinationChoiceConfigGroup();
		
		StringBuffer sb = new StringBuffer();
		StringBuffer epsilons = new StringBuffer();
		for(ActivityParams params : controler.getConfig().planCalcScore().getActivityParams()){
			
			if(params.getActivityType().contains("shop") || params.getActivityType().contains("othe") ||
					params.getActivityType().contains("leis")){
				if(sb.length() < 1){
					sb.append(params.getActivityType());
					epsilons.append("1.0");
				} else{
					sb.append(", " + params.getActivityType());
					epsilons.append(", 1.0");
				}
			}
			
		}
		
		dccg.setFlexibleTypes(sb.toString());
		dccg.setEpsilonScaleFactors(epsilons.toString());
		dccg.setpkValuesFile("/home/danielhosse/run9a/personsKValues.xml");
		dccg.setfkValuesFile("/home/danielhosse/run9a/facilitiesKValues.xml");
		dccg.setScaleFactor("1");
		
		controler.getConfig().addModule(dccg);
		DestinationChoiceBestResponseContext dcbr = new DestinationChoiceBestResponseContext(controler.getScenario());
		dcbr.init();
		DCScoringFunctionFactory scFactory = new DCScoringFunctionFactory(controler.getScenario(), dcbr);
		scFactory.setUsingConfigParamsForScoring(true);
		controler.addControlerListener(new DestinationChoiceInitializer(dcbr));
		
		if (Double.parseDouble(controler.getConfig().findParam("locationchoice", "restraintFcnExp")) > 0.0 &&
		        Double.parseDouble(controler.getConfig().findParam("locationchoice", "restraintFcnFactor")) > 0.0) {
		    controler.addControlerListener(new FacilitiesLoadCalculator(dcbr.getFacilityPenalties()));
		}
		
		controler.setScoringFunctionFactory(scFactory);
		
		StrategySettings dc = new StrategySettings();
		dc.setStrategyName("org.matsim.contrib.locationchoice.BestReplyLocationChoicePlanStrategy");
		dc.setSubpopulation(null);
		dc.setWeight(0.1);
		controler.getConfig().strategy().addStrategySettings(dc);
		
	}
	
	/**
	 * Adds subtour mode choice strategy settings to the controler.
	 * These strategies are configured for different types of subpopulations (persons with car and license, commuters, persons without license).
	 * 
	 * @param controler
	 */
	private static void addModeChoice(final Controler controler) {

		StrategySettings carAvail = new StrategySettings();
		carAvail.setStrategyName("SubtourModeChoice_".concat(Global.GP_CAR));
		carAvail.setSubpopulation(Global.GP_CAR);
		carAvail.setWeight(0.1);
		controler.getConfig().strategy().addStrategySettings(carAvail);
		
		StrategySettings license = new StrategySettings();
		license.setStrategyName("SubtourModeChoice_".concat(Global.LICENSE_OWNER));
		license.setSubpopulation(Global.LICENSE_OWNER);
		license.setWeight(0.1);
		controler.getConfig().strategy().addStrategySettings(license);
		
		StrategySettings nonCarAvail = new StrategySettings();
		nonCarAvail.setStrategyName("SubtourModeChoice_".concat("NO_CAR"));
		nonCarAvail.setSubpopulation(null);
		nonCarAvail.setWeight(0.1);
		controler.getConfig().strategy().addStrategySettings(nonCarAvail);
		
		StrategySettings commuter = new StrategySettings();
		commuter.setStrategyName("SubtourModeChoice_".concat(Global.COMMUTER));
		commuter.setSubpopulation(Global.COMMUTER);
		commuter.setWeight(0.1);
		controler.getConfig().strategy().addStrategySettings(commuter);
		
		controler.addOverridingModule(new AbstractModule() {
			
			@Override
			public void install() {
				addPlanStrategyBinding("SubtourModeChoice_".concat(Global.GP_CAR)).toProvider(new javax.inject.Provider<PlanStrategy>() {
					String[] availableModes = {TransportMode.car, TransportMode.pt, TransportMode.bike, TransportMode.walk};
					String[] chainBasedModes = {TransportMode.car, TransportMode.bike};

					@Override
					public PlanStrategy get() {
						final Builder builder = new Builder(new RandomPlanSelector<Plan, Person>());
						builder.addStrategyModule(new SubtourModeChoice(controler.getConfig().global().getNumberOfThreads(), availableModes, chainBasedModes, false));
						builder.addStrategyModule(new ReRoute(controler.getScenario()));
						return builder.build();
					}
				});
				
			}
		});
		
		controler.addOverridingModule(new AbstractModule() {
			
			@Override
			public void install() {
				addPlanStrategyBinding("SubtourModeChoice_".concat("NO_CAR")).toProvider(new javax.inject.Provider<PlanStrategy>() {
					String[] availableModes = {TransportMode.pt, TransportMode.bike, TransportMode.walk};
					String[] chainBasedModes = {TransportMode.bike};

					@Override
					public PlanStrategy get() {
						final Builder builder = new Builder(new RandomPlanSelector<Plan, Person>());
						builder.addStrategyModule(new SubtourModeChoice(controler.getConfig().global().getNumberOfThreads(), availableModes, chainBasedModes, false));
						builder.addStrategyModule(new ReRoute(controler.getScenario()));
						return builder.build();
					}
				});
				
			}
		});
		
		controler.addOverridingModule(new AbstractModule() {
			
			@Override
			public void install() {
				addPlanStrategyBinding("SubtourModeChoice_".concat(Global.COMMUTER)).toProvider(new javax.inject.Provider<PlanStrategy>() {
					String[] availableModes = {TransportMode.car, TransportMode.pt};
					String[] chainBasedModes = {TransportMode.car, TransportMode.bike};

					@Override
					public PlanStrategy get() {
						final Builder builder = new Builder(new RandomPlanSelector<Plan, Person>());
						builder.addStrategyModule(new SubtourModeChoice(controler.getConfig().global().getNumberOfThreads(), availableModes, chainBasedModes, false));
						builder.addStrategyModule(new ReRoute(controler.getScenario()));
						return builder.build();
					}
				});
				
			}
		});
		
		controler.addOverridingModule(new AbstractModule() {
			
			@Override
			public void install() {
				addPlanStrategyBinding("SubtourModeChoice_".concat(Global.LICENSE_OWNER)).toProvider(new javax.inject.Provider<PlanStrategy>() {
					String[] availableModes = {TransportMode.pt, TransportMode.bike, TransportMode.walk, "onewaycarsharing"};
					String[] chainBasedModes = {TransportMode.bike};

					@Override
					public PlanStrategy get() {
						final Builder builder = new Builder(new RandomPlanSelector<Plan, Person>());
						builder.addStrategyModule(new SubtourModeChoice(controler.getConfig().global().getNumberOfThreads(), availableModes, chainBasedModes, false));
						builder.addStrategyModule(new ReRoute(controler.getScenario()));
						return builder.build();
					}
				});
				
			}
		});
		
	}
	
	private static void addCarsharing(final Controler controler){
		
		OneWayCarsharingConfigGroup ow = new OneWayCarsharingConfigGroup();
		ow.setConstantOneWayCarsharing("0");
		ow.setDistanceFeeOneWayCarsharing("0");
		ow.setRentalPriceTimeOneWayCarsharing("0");
		ow.setsearchDistance("2000");
		ow.setTimeFeeOneWayCarsharing("-1");
		ow.setTimeParkingFeeOneWayCarsharing("-0.1");
		ow.setUtilityOfTravelling("-6");
		ow.setUseOneWayCarsharing(true);
		ow.setvehiclelocations(simInputPath + "stations.txt");
		controler.getConfig().addModule(ow);
		
		TwoWayCarsharingConfigGroup tw = new TwoWayCarsharingConfigGroup();
		tw.setUseTwoWayCarsharing(false);
		controler.getConfig().addModule(tw);
		
		FreeFloatingConfigGroup ff = new FreeFloatingConfigGroup();
		ff.setUseFeeFreeFloating(false);
		controler.getConfig().addModule(ff);
		
		CarsharingConfigGroup cs = new CarsharingConfigGroup();
		cs.setStatsWriterFrequency("10");
		controler.getConfig().addModule(cs);
		
		RunCarsharing.installCarSharing(controler);
		
	}
	
	/**
	 * An entry point for some analysis methods...
	 */
	private static void runAnalysis() {
	
//		PersonAnalysis.createLegModeDistanceDistribution("/home/dhosse/plansV3_cleaned.xml.gz", "/home/danielhosse/Dokumente/lmdd/");
		SpatialAnalysis.writePopulationToShape("/home/danielhosse/plansV4.xml", "/home/danielhosse/Dokumente/01_eGAP/popV4.shp");
	
	}
	
}
