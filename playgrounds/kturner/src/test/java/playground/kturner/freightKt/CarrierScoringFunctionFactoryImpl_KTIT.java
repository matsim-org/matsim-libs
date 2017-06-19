package playground.kturner.freightKt;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.junit.*;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlReaderV2;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeLoader;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeReader;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypes;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.controler.CarrierModule;
import org.matsim.contrib.freight.jsprit.VehicleTypeDependentRoadPricingCalculator;
import org.matsim.contrib.freight.replanning.CarrierPlanStrategyManagerFactory;
import org.matsim.contrib.freight.scoring.CarrierScoringFunctionFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.GenericStrategyManager;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.roadpricing.RoadPricingConfigGroup;
import org.matsim.roadpricing.RoadPricingReaderXMLv1;
import org.matsim.roadpricing.RoadPricingSchemeImpl;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.VehicleType;

import playground.kturner.freightKt.CarrierScoringFunctionFactoryImpl_KT;

public class CarrierScoringFunctionFactoryImpl_KTIT {
	
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	@Test //Fixkosten
	public void fixCosts() throws IOException {
		final String CARRIERS_FILE = utils.getClassInputDirectory() + "jsprit_plannedCarriers.xml";
		final String VEHTYPES_FILE = utils.getClassInputDirectory() + "vehTypes.xml";
		final String NETWORK_FILE = utils.getClassInputDirectory() + "network.xml";
	
    	CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes() ;
    	new CarrierVehicleTypeReader(vehicleTypes).readFile(VEHTYPES_FILE) ;

    	Carriers carriers = new Carriers() ;
    	new CarrierPlanXmlReaderV2(carriers).readFile(CARRIERS_FILE) ;
    	// assign vehicle types to the carriers
    	new CarrierVehicleTypeLoader(carriers).loadVehicleTypes(vehicleTypes) ;
    	
    	Config config = ConfigUtils.createConfig() ;
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.controler().setLastIteration(0);	
		config.network().setInputFile(NETWORK_FILE);

		//Damit nicht alle um Mitternacht losfahren
		config.plans().setActivityDurationInterpretation(PlansConfigGroup.ActivityDurationInterpretation.tryEndTimeThenDuration );
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		final Controler controler = new Controler( scenario ) ;
		CarrierScoringFunctionFactory scoringFunctionFactory = new CarrierScoringFunctionFactoryImpl_KT(scenario, scenario.getConfig().controler().getOutputDirectory()) {

			public ScoringFunction createScoringFunction(final Carrier carrier){
				SumScoringFunction sumSf = new SumScoringFunction() ;

				VehicleFixCostScoring fixCost = new VehicleFixCostScoring(carrier);
				sumSf.addScoringFunction(fixCost);

				return sumSf;
			}
		};
		
		CarrierPlanStrategyManagerFactory planStrategyManagerFactory =  createMyStrategymanager(); //Benötigt, da listener kein "Null" als StrategyFactory mehr erlaubt, KT 17.04.2015

		CarrierModule listener = new CarrierModule(carriers, planStrategyManagerFactory, scoringFunctionFactory) ;
		listener.setPhysicallyEnforceTimeWindowBeginnings(true);
		controler.addOverridingModule(listener) ;

		controler.run();
		
		Carrier gridCarrier3 = carriers.getCarriers().get(Id.create("gridCarrier3", Carrier.class));
		System.out.println(gridCarrier3.getSelectedPlan().getScore());
		//Fixkosten: 130.00 EUR
		Assert.assertEquals("FixCostsScoring not correct", 130.0 , -gridCarrier3.getSelectedPlan().getScore(), MatsimTestUtils.EPSILON);
		FileUtils.forceDelete(new File(scenario.getConfig().controler().getOutputDirectory()));
	}
	
	@Test //LegScoring
	public void legCosts() throws IOException {
		final String CARRIERS_FILE = utils.getClassInputDirectory() + "jsprit_plannedCarriers.xml";
		final String VEHTYPES_FILE = utils.getClassInputDirectory() + "vehTypes.xml";
		final String NETWORK_FILE = utils.getClassInputDirectory() + "network.xml";
	
    	CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes() ;
    	new CarrierVehicleTypeReader(vehicleTypes).readFile(VEHTYPES_FILE) ;

    	Carriers carriers = new Carriers() ;
    	new CarrierPlanXmlReaderV2(carriers).readFile(CARRIERS_FILE) ;
    	// assign vehicle types to the carriers
    	new CarrierVehicleTypeLoader(carriers).loadVehicleTypes(vehicleTypes) ;
    	
    	Config config = ConfigUtils.createConfig() ;
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.controler().setLastIteration(0);	
		config.network().setInputFile(NETWORK_FILE);

		//Damit nicht alle um Mitternacht losfahren
		config.plans().setActivityDurationInterpretation(PlansConfigGroup.ActivityDurationInterpretation.tryEndTimeThenDuration );
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		final Controler controler = new Controler( scenario ) ;
		CarrierScoringFunctionFactory scoringFunctionFactory = new CarrierScoringFunctionFactoryImpl_KT(scenario, scenario.getConfig().controler().getOutputDirectory()) {

			public ScoringFunction createScoringFunction(final Carrier carrier){
				SumScoringFunction sumSf = new SumScoringFunction() ;

				LegScoring legScoring = new LegScoring(carrier);
				sumSf.addScoringFunction(legScoring);

				return sumSf;
			}
		};
		
		CarrierPlanStrategyManagerFactory planStrategyManagerFactory =  createMyStrategymanager(); //Benötigt, da listener kein "Null" als StrategyFactory mehr erlaubt, KT 17.04.2015

		CarrierModule listener = new CarrierModule(carriers, planStrategyManagerFactory, scoringFunctionFactory) ;
		listener.setPhysicallyEnforceTimeWindowBeginnings(true);
		controler.addOverridingModule(listener) ;

		controler.run();
		
		Carrier gridCarrier3 = carriers.getCarriers().get(Id.create("gridCarrier3", Carrier.class));
		System.out.println(gridCarrier3.getSelectedPlan().getScore());
		//Wegkosten (Entfernung): 12000 m *0.001 EUR/m = 12,00 EUR
		//Wegkosten (Zeit): 1212 s *0,01 EUR/s = 12,12 EUR  (12 Kanten a 100s + 12 Knoten a 1 s = 1212s)
		Assert.assertEquals("LegScoring not correct.", 12 + 12.12 , -gridCarrier3.getSelectedPlan().getScore(),  MatsimTestUtils.EPSILON);
		FileUtils.forceDelete(new File(scenario.getConfig().controler().getOutputDirectory()));
	}
	
	@Test //Aktivität ohne Wartezeitkorrektur
	public void aktivityCostsNoCorrection() throws IOException {
		final String CARRIERS_FILE = utils.getClassInputDirectory() + "jsprit_plannedCarriers.xml";
		final String VEHTYPES_FILE = utils.getClassInputDirectory() + "vehTypes.xml";
		final String NETWORK_FILE = utils.getClassInputDirectory() + "network.xml";
	
    	CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes() ;
    	new CarrierVehicleTypeReader(vehicleTypes).readFile(VEHTYPES_FILE) ;

    	Carriers carriers = new Carriers() ;
    	new CarrierPlanXmlReaderV2(carriers).readFile(CARRIERS_FILE) ;
    	// assign vehicle types to the carriers
    	new CarrierVehicleTypeLoader(carriers).loadVehicleTypes(vehicleTypes) ;
    	
    	Config config = ConfigUtils.createConfig() ;
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.controler().setLastIteration(0);	
		config.network().setInputFile(NETWORK_FILE);

		//Damit nicht alle um Mitternacht losfahren
		config.plans().setActivityDurationInterpretation(PlansConfigGroup.ActivityDurationInterpretation.tryEndTimeThenDuration );
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		final Controler controler = new Controler( scenario ) ;
		CarrierScoringFunctionFactory scoringFunctionFactory = new CarrierScoringFunctionFactoryImpl_KT(scenario, scenario.getConfig().controler().getOutputDirectory()) {

			public ScoringFunction createScoringFunction(final Carrier carrier){
				SumScoringFunction sumSf = new SumScoringFunction() ;
				
				//Score Activity w/o correction of waitingTime @ 1st Service.
				ActivityScoring actScoring = new ActivityScoring(carrier);
				sumSf.addScoringFunction(actScoring);

				return sumSf;
			}
		};
		
		CarrierPlanStrategyManagerFactory planStrategyManagerFactory =  createMyStrategymanager(); //Benötigt, da listener kein "Null" als StrategyFactory mehr erlaubt, KT 17.04.2015

		CarrierModule listener = new CarrierModule(carriers, planStrategyManagerFactory, scoringFunctionFactory) ;
		listener.setPhysicallyEnforceTimeWindowBeginnings(true);
		controler.addOverridingModule(listener) ;

		controler.run();
		
		Carrier gridCarrier3 = carriers.getCarriers().get(Id.create("gridCarrier3", Carrier.class));
		System.out.println(gridCarrier3.getSelectedPlan().getScore());
		//Fixkosten: 130.00 EUR
		//Wegkosten (Entfernung): 12000 m *0.001 EUR/m = 12,00 EUR
		//Wegkosten (Zeit): 1212 s *0,01 EUR/s = 12,12 EUR  (12 Kanten a 100s + 12 Knoten a 1 s = 1212s)
		//Aktivität: 600 s * 0.008 EUR/s =  4.80 EUR
		//Wartezeit: 33594 s * 0.008 EUR / s = 268.752 EUR (Wartezeit: 9.5 h - 606 s Fahrzeit (Hinweg))
		Assert.assertEquals("ActivityScoring w/o correction of WaitingTime not correct.",  4.80 + 268.752 , -gridCarrier3.getSelectedPlan().getScore(), MatsimTestUtils.EPSILON);
		FileUtils.forceDelete(new File(scenario.getConfig().controler().getOutputDirectory()));
	}
	
	@Test //Aktivität mit Wartezeitkorrektur
	public void aktivityCostsWithCorrection() throws IOException {
		final String CARRIERS_FILE = utils.getClassInputDirectory() + "jsprit_plannedCarriers.xml";
		final String VEHTYPES_FILE = utils.getClassInputDirectory() + "vehTypes.xml";
		final String NETWORK_FILE = utils.getClassInputDirectory() + "network.xml";
	
    	CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes() ;
    	new CarrierVehicleTypeReader(vehicleTypes).readFile(VEHTYPES_FILE) ;

    	Carriers carriers = new Carriers() ;
    	new CarrierPlanXmlReaderV2(carriers).readFile(CARRIERS_FILE) ;
    	// assign vehicle types to the carriers
    	new CarrierVehicleTypeLoader(carriers).loadVehicleTypes(vehicleTypes) ;
    	
    	Config config = ConfigUtils.createConfig() ;
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.controler().setLastIteration(0);	
		config.network().setInputFile(NETWORK_FILE);

		//Damit nicht alle um Mitternacht losfahren
		config.plans().setActivityDurationInterpretation(PlansConfigGroup.ActivityDurationInterpretation.tryEndTimeThenDuration );
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		final Controler controler = new Controler( scenario ) ;
		CarrierScoringFunctionFactory scoringFunctionFactory = new CarrierScoringFunctionFactoryImpl_KT(scenario, scenario.getConfig().controler().getOutputDirectory()) {

			public ScoringFunction createScoringFunction(final Carrier carrier){
				SumScoringFunction sumSf = new SumScoringFunction() ;

				//Score Activity with correction of waitingTime @ 1st Service.
				ActivityScoringWithCorrection actScoring = new ActivityScoringWithCorrection(carrier);
				sumSf.addScoringFunction(actScoring);

				return sumSf;
			}
		};
		
		CarrierPlanStrategyManagerFactory planStrategyManagerFactory =  createMyStrategymanager(); //Benötigt, da listener kein "Null" als StrategyFactory mehr erlaubt, KT 17.04.2015

		CarrierModule listener = new CarrierModule(carriers, planStrategyManagerFactory, scoringFunctionFactory) ;
		listener.setPhysicallyEnforceTimeWindowBeginnings(true);
		controler.addOverridingModule(listener) ;

		controler.run();
		
		Carrier gridCarrier3 = carriers.getCarriers().get(Id.create("gridCarrier3", Carrier.class));
		System.out.println(gridCarrier3.getSelectedPlan().getScore());
		//Aktivität: 600 s * 0.008 EUR/s =  4.80 EUR
		Assert.assertEquals("ActivityScoring with correction of WaitingTime not correct.", -gridCarrier3.getSelectedPlan().getScore(),  4.80  , MatsimTestUtils.EPSILON);
		FileUtils.forceDelete(new File(scenario.getConfig().controler().getOutputDirectory()));
	}
	
	@Test //tollScoring 
	public void tollScoring() throws IOException {
		final String CARRIERS_FILE = utils.getClassInputDirectory() + "jsprit_plannedCarriers.xml";
		final String VEHTYPES_FILE = utils.getClassInputDirectory() + "vehTypes.xml";
		final String NETWORK_FILE = utils.getClassInputDirectory() + "network.xml";
		final String TOLL_FILE = utils.getClassInputDirectory() + "grid-tollCordon.xml";
	
    	CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes() ;
    	new CarrierVehicleTypeReader(vehicleTypes).readFile(VEHTYPES_FILE) ;

    	Carriers carriers = new Carriers() ;
    	new CarrierPlanXmlReaderV2(carriers).readFile(CARRIERS_FILE) ;
    	// assign vehicle types to the carriers
    	new CarrierVehicleTypeLoader(carriers).loadVehicleTypes(vehicleTypes) ;
    	
    	Config config = ConfigUtils.createConfig() ;
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.controler().setLastIteration(0);	
		config.network().setInputFile(NETWORK_FILE);

		//Damit nicht alle um Mitternacht losfahren
		config.plans().setActivityDurationInterpretation(PlansConfigGroup.ActivityDurationInterpretation.tryEndTimeThenDuration );
		
		final Scenario scenario = ScenarioUtils.loadScenario(config);
		final Controler controler = new Controler( scenario ) ;
		
		ConfigUtils.addOrGetModule(config, RoadPricingConfigGroup.GROUP_NAME, RoadPricingConfigGroup.class);

		//Füge Mautschmema hinzu
		final VehicleTypeDependentRoadPricingCalculator rpCalculator = 
				new VehicleTypeDependentRoadPricingCalculator();
		final RoadPricingSchemeImpl scheme = new RoadPricingSchemeImpl();
		RoadPricingReaderXMLv1 rpReader = new RoadPricingReaderXMLv1(scheme);
		try {
			RoadPricingConfigGroup rpConfig = (RoadPricingConfigGroup) config.getModule(RoadPricingConfigGroup.GROUP_NAME) ;
			rpConfig.setTollLinksFile(TOLL_FILE);
			rpReader.readFile(rpConfig.getTollLinksFile());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		//Mautschema für alle zur Verfügung stehenden FZG-Typen zuweisen
		Collection<Id<VehicleType>> vehTypesAddedToRPS = new ArrayList<Id<VehicleType>>();
		for(Carrier c : carriers.getCarriers().values()){
			for(CarrierVehicle v : c.getCarrierCapabilities().getCarrierVehicles()){
				Id<VehicleType> typeId = v.getVehicleType().getId();
				if (!vehTypesAddedToRPS.contains(typeId)) {
					vehTypesAddedToRPS.add(typeId);
					rpCalculator.addPricingScheme(typeId, scheme);
				}
			}
		}
		
		CarrierScoringFunctionFactory scoringFunctionFactory = new CarrierScoringFunctionFactoryImpl_KT(scenario, scenario.getConfig().controler().getOutputDirectory()) {

			public ScoringFunction createScoringFunction(final Carrier carrier){
				SumScoringFunction sumSf = new SumScoringFunction() ;
				
					TollScoring tollScoring = new TollScoring(carrier, scenario.getNetwork(), rpCalculator) ;
						sumSf.addScoringFunction(tollScoring);
						
				return sumSf;
			}
		};
		
		CarrierPlanStrategyManagerFactory planStrategyManagerFactory =  createMyStrategymanager(); //Benötigt, da listener kein "Null" als StrategyFactory mehr erlaubt, KT 17.04.2015

		CarrierModule listener = new CarrierModule(carriers, planStrategyManagerFactory, scoringFunctionFactory) ;
		listener.setPhysicallyEnforceTimeWindowBeginnings(true);
		controler.addOverridingModule(listener) ;

		controler.run();
		
		Carrier gridCarrier3 = carriers.getCarriers().get(Id.create("gridCarrier3", Carrier.class));
		System.out.println(gridCarrier3.getSelectedPlan().getScore());
		//Maut: 10.00 EUR
		Assert.assertEquals("TollScoring (Cordon) not correct.", 10.0 , -gridCarrier3.getSelectedPlan().getScore() , MatsimTestUtils.EPSILON);
		FileUtils.forceDelete(new File(scenario.getConfig().controler().getOutputDirectory()));
	}
	
	@Test //Aktivität mit Wartezeitkorrektur
	public void totalCostsWithCorrection() throws IOException {
		final String CARRIERS_FILE = utils.getClassInputDirectory() + "jsprit_plannedCarriers.xml";
		final String VEHTYPES_FILE = utils.getClassInputDirectory() + "vehTypes.xml";
		final String NETWORK_FILE = utils.getClassInputDirectory() + "network.xml";
		final String TOLL_FILE = utils.getClassInputDirectory() + "grid-tollCordon.xml";
	
    	CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes() ;
    	new CarrierVehicleTypeReader(vehicleTypes).readFile(VEHTYPES_FILE) ;

    	Carriers carriers = new Carriers() ;
    	new CarrierPlanXmlReaderV2(carriers).readFile(CARRIERS_FILE) ;
    	// assign vehicle types to the carriers
    	new CarrierVehicleTypeLoader(carriers).loadVehicleTypes(vehicleTypes) ;
    	
    	Config config = ConfigUtils.createConfig() ;
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.controler().setLastIteration(0);	
		config.network().setInputFile(NETWORK_FILE);

		//Damit nicht alle um Mitternacht losfahren
		config.plans().setActivityDurationInterpretation(PlansConfigGroup.ActivityDurationInterpretation.tryEndTimeThenDuration );
		
		final Scenario scenario = ScenarioUtils.loadScenario(config);
		
		ConfigUtils.addOrGetModule(config, RoadPricingConfigGroup.GROUP_NAME, RoadPricingConfigGroup.class);
		//Füge Mautschmema hinzu
				final VehicleTypeDependentRoadPricingCalculator rpCalculator = 
						new VehicleTypeDependentRoadPricingCalculator();
				final RoadPricingSchemeImpl scheme = new RoadPricingSchemeImpl();
				RoadPricingReaderXMLv1 rpReader = new RoadPricingReaderXMLv1(scheme);
				try {
					RoadPricingConfigGroup rpConfig = (RoadPricingConfigGroup) config.getModule(RoadPricingConfigGroup.GROUP_NAME) ;
					rpConfig.setTollLinksFile(TOLL_FILE);
					rpReader.readFile(rpConfig.getTollLinksFile());
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
				//Mautschema für alle zur Verfügung stehenden FZG-Typen zuweisen
				Collection<Id<VehicleType>> vehTypesAddedToRPS = new ArrayList<Id<VehicleType>>();
				for(Carrier c : carriers.getCarriers().values()){
					for(CarrierVehicle v : c.getCarrierCapabilities().getCarrierVehicles()){
						Id<VehicleType> typeId = v.getVehicleType().getId();
						if (!vehTypesAddedToRPS.contains(typeId)) {
							vehTypesAddedToRPS.add(typeId);
							rpCalculator.addPricingScheme(typeId, scheme);
						}
					}
				}
				
		final Controler controler = new Controler( scenario ) ;
		CarrierScoringFunctionFactory scoringFunctionFactory = new CarrierScoringFunctionFactoryImpl_KT(scenario, scenario.getConfig().controler().getOutputDirectory()) {

			public ScoringFunction createScoringFunction(final Carrier carrier){
				SumScoringFunction sumSf = new SumScoringFunction() ;
				
				VehicleFixCostScoring fixCost = new VehicleFixCostScoring(carrier);
				sumSf.addScoringFunction(fixCost);

				LegScoring legScoring = new LegScoring(carrier);
				sumSf.addScoringFunction(legScoring);

				//Score Activity with correction of waitingTime @ 1st Service.
				ActivityScoringWithCorrection actScoring = new ActivityScoringWithCorrection(carrier);
				sumSf.addScoringFunction(actScoring);
				
				TollScoring tollScoring = new TollScoring(carrier, scenario.getNetwork(), rpCalculator) ;
				sumSf.addScoringFunction(tollScoring);

				return sumSf;
			}
		};
		
		CarrierPlanStrategyManagerFactory planStrategyManagerFactory =  createMyStrategymanager(); //Benötigt, da listener kein "Null" als StrategyFactory mehr erlaubt, KT 17.04.2015

		CarrierModule listener = new CarrierModule(carriers, planStrategyManagerFactory, scoringFunctionFactory) ;
		listener.setPhysicallyEnforceTimeWindowBeginnings(true);
		controler.addOverridingModule(listener) ;

		controler.run();
		
		Carrier gridCarrier3 = carriers.getCarriers().get(Id.create("gridCarrier3", Carrier.class));
		System.out.println(gridCarrier3.getSelectedPlan().getScore());
		//Fixkosten: 130.00 EUR
		//Wegkosten (Entfernung): 12000 m *0.001 EUR/m = 12,00 EUR
		//Wegkosten (Zeit): 1212 s *0,01 EUR/s = 12,12 EUR  (12 Kanten a 100s + 12 Knoten a 1 s = 1212s)
		//Aktivität: 600 s * 0.008 EUR/s =  4.80 EUR
		//Maut: 10.00 EUR
		Assert.assertEquals("TotalScoring not correct.", 130 + 12 + 12.12 + 4.80 + 10 ,  -gridCarrier3.getSelectedPlan().getScore() , MatsimTestUtils.EPSILON);
		FileUtils.forceDelete(new File(scenario.getConfig().controler().getOutputDirectory()));
	}
	
	
	//Benötigt, da listener kein "Null" als StrategyFactory mehr erlaubt, KT 17.04.2015
	//Da keine Strategy notwendig, hier zunächst eine "leere" Factory
	private CarrierPlanStrategyManagerFactory createMyStrategymanager(){
		return new CarrierPlanStrategyManagerFactory() {
			@Override
			public GenericStrategyManager<CarrierPlan, Carrier> createStrategyManager() {
				return null;
			}
		};
	}

}
