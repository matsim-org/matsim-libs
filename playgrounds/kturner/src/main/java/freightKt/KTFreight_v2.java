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
package freightKt;

import jsprit.core.algorithm.VehicleRoutingAlgorithm;
import jsprit.core.algorithm.io.VehicleRoutingAlgorithms;
import jsprit.core.problem.VehicleRoutingProblem;
import jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import jsprit.core.util.Solutions;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.contrib.freight.controler.CarrierModule;
import org.matsim.contrib.freight.jsprit.MatsimJspritFactory;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts.Builder;
import org.matsim.contrib.freight.jsprit.NetworkRouter;
import org.matsim.contrib.freight.jsprit.VehicleTypeDependentRoadPricingCalculator;
import org.matsim.contrib.freight.replanning.CarrierPlanStrategyManagerFactory;
import org.matsim.contrib.freight.scoring.CarrierScoringFunctionFactory;
import org.matsim.contrib.freight.scoring.FreightActivity;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.ActivityDurationInterpretation;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkChangeEvent.ChangeType;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;
import org.matsim.core.network.NetworkChangeEventFactory;
import org.matsim.core.network.NetworkChangeEventFactoryImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.replanning.GenericStrategyManager;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.roadpricing.ControlerDefaultsWithRoadPricingModule;
import org.matsim.roadpricing.RoadPricingConfigGroup;
import org.matsim.roadpricing.RoadPricingReaderXMLv1;
import org.matsim.roadpricing.RoadPricingSchemeImpl;
import org.matsim.vehicles.VehicleType;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * @author kt
 * Variante von KT
 * Es wird nur eine (1) MATSIM-Iteration durchgeführt, um Event-Files zu generieren.
 * Das Planen der Touren selbst erfolgt durch jsprit.
 * Es werden Ausgaben nach Abschluss des Jsprit-Teils ausgegeben (jsprit_plannedCarriers.xml, jsprit_solution.png)
 * 18.12.2014 KT: Strategie wie abgesprochen raus...
 */
public class KTFreight_v2 {

	private static final Logger log = Logger.getLogger(KTFreight_v2.class) ;

	//	//Beginn Namesdefinition KT Für Berlin-Szenario 
	//	private static final String INPUT_DIR = "F:/OneDrive/Dokumente/Masterarbeit/MATSIM/input/Berlin_Szenario/" ;
	//	private static final String OUTPUT_DIR = "F:/OneDrive/Dokumente/Masterarbeit/MATSIM/output/Matsim/Berlin/1carrier/500it/" ;
	//	private static final String TEMP_DIR = "F:/OneDrive/Dokumente/Masterarbeit/MATSIM/output/Temp/";
	//
	//	//Dateinamen ohne XML-Endung
	//	private static final String NETFILE_NAME = "network" ;
	//	private static final String VEHTYPES_NAME = "vehicleTypes" ;
	//	private static final String CARRIERS_NAME = "carrier_kt_1_aldi" ;
	//	private static final String ALGORITHM_NAME = "grid-algorithm" ;
	//	private static final String TOLL_NAME = "toll_distance_test_kt";
	//	//Ende  Namesdefinition Berlin


	//Beginn Namesdefinition KT Für Test-Szenario (Grid)
	private static final String INPUT_DIR = "F:/OneDrive/Dokumente/Masterarbeit/MATSIM/input/Grid_Szenario/" ;
	private static final String OUTPUT_DIR = "F:/OneDrive/Dokumente/Masterarbeit/MATSIM/output/Matsim/Grid/ScoringToll1/" ;
	private static final String TEMP_DIR = "F:/OneDrive/Dokumente/Masterarbeit/MATSIM/output/Temp/" ;	

	//Dateinamen ohne XML-Endung
	private static final String NETFILE_NAME = "grid-network" ;
	private static final String VEHTYPES_NAME = "grid-vehTypes_kt" ;
	private static final String CARRIERS_NAME = "grid-carrier_kt" ;
	private static final String ALGORITHM_NAME = "grid-algorithm" ;
	private static final String TOLL_NAME = "grid-tollCordon";
	//Ende Namesdefinition Grid


	private static final String RUN = "Run_" ;
	private static int runIndex = 0;

	private static final String NETFILE = INPUT_DIR + NETFILE_NAME + ".xml" ;
	private static final String VEHTYPEFILE = INPUT_DIR + VEHTYPES_NAME + ".xml";
	private static final String CARRIERFILE = INPUT_DIR + CARRIERS_NAME + ".xml" ;
	private static final String ALGORITHMFILE = INPUT_DIR + ALGORITHM_NAME + ".xml";
	private static final String TOLLFILE = INPUT_DIR + TOLL_NAME + ".xml";


	// Einstellungen für den Run	

	private static final boolean addingCongestion = false ;  //doesn't work correctly, KT 10.12.2014
	private static final boolean addingToll = true;  //added, kt. 07.08.2014
	private static final boolean runMatsim = true;	 //when false only jsprit run will be performed
	private static final int LAST_MATSIM_ITERATION = 0;  //only one iteration for writing events.
	private static final int LAST_JSPRIT_ITERATION = 100;
	private static final int NU_OF_TOTAL_RUNS = 1;	

	//temporär zum Programieren als Ausgabe
	private static WriteTextToFile textInfofile; 

	private static RoadPricingSchemeImpl rpscheme = new RoadPricingSchemeImpl();


	public static void main(String[] args) {

		for (int i = 1; i<=NU_OF_TOTAL_RUNS; i++) {
			runIndex = i;	
			multipleRun(args);	
		}
		writeRunInfo();	
		System.out.println("#### End of all runs --Y Finished ####");
	}



	//### KT 03.12.2014 multiple run for testing the variety of the jsprit solutions (especially in terms of costs). 
	private static void multipleRun (String[] args){	
		System.out.println("#### Starting Run: " + runIndex + "of: "+ NU_OF_TOTAL_RUNS);
		createDir(new File(OUTPUT_DIR + RUN + runIndex));
		createDir(new File(TEMP_DIR));	
		

		// ### config stuff: ###	
		Config config = createConfig(args);
		//		config.addConfigConsistencyChecker(new VspConfigConsistencyCheckerImpl());
		//		AbstractController.checkConfigConsistencyAndWriteToLog(config, "dump");
		textInfofile = new WriteTextToFile(new File(TEMP_DIR + "#TextInformation.txt"), null);
		
		if ( addingCongestion ) { //erst config vorbereiten....Config ist fix, sobald in Scenario!!!! KT, 11.12.14
			config.network().setTimeVariantNetwork(true);
		}
		
		// ### scenario stuff: ###
		Scenario scenario = ScenarioUtils.loadScenario(config);

		if ( addingCongestion ) {
			configureTimeDependentNetwork(scenario);
		}

		Carriers carriers = jspritRun(config, scenario);		//Building the Carriers with jsprit, incl jspritOutput KT 03.12.2014

		//### additional output for multiple-run analysis: ### KT 03.12.2014

		if ( runMatsim){
			matsimRun(scenario, carriers);	//final Matsim configuarations and start of the Matsim-Run
		}
		finalOutput(config, carriers);	//write some final Output
		moveTempFile(new File(TEMP_DIR), new File(OUTPUT_DIR)); //move of temp-files
	} 

	private static Carriers jspritRun(Config config, Scenario scenario) {
		CarrierVehicleTypes vehicleTypes = createVehicleTypes();

		Carriers carriers = createCarriers(vehicleTypes);
		generateCarrierPlans(scenario.getNetwork(), carriers, vehicleTypes, config);

		new WriteCarrierScoreInfos(carriers, new File(TEMP_DIR + "#JspritCarrierScoreInformation.txt"), runIndex);

		return carriers;
	}


	private static void matsimRun(Scenario scenario, Carriers carriers) {
		final Controler ctrl = new Controler( scenario ) ;

		if (addingToll){		 //Added RoadpricingScheme to MATSIM-Controler Added, KT, 02.12.2014
			ctrl.setModules(new ControlerDefaultsWithRoadPricingModule(rpscheme));
		}

		//		Bringt aktuell (18.nov.14) NullPointerExeption at org.matsim.core.controler.Controler.getLinkTravelTimes(Controler.java:551)
		//		PlanCalcScoreConfigGroup cnScoringGroup = ctrl.getConfig().planCalcScore() ;
		//		TravelTime timeCalculator = ctrl.getLinkTravelTimes() ;
		//		TravelDisutility trDisutil = ctrl.getTravelDisutilityFactory().createTravelDisutility(timeCalculator, cnScoringGroup) ;
		//		

		CarrierScoringFunctionFactory scoringFunctionFactory = createMyScoringFunction2(scenario);
		CarrierPlanStrategyManagerFactory planStrategyManagerFactory =  createMyStrategymanager(); //Benötigt, da listener kein "Null" als StrategyFactory mehr erlaubt, KT 17.04.2015
		

		CarrierModule listener = new CarrierModule(carriers, planStrategyManagerFactory, scoringFunctionFactory) ;
		listener.setPhysicallyEnforceTimeWindowBeginnings(true);
		ctrl.addOverridingModule(listener) ;

		ctrl.run();
	}
	

	private static void finalOutput(Config config, Carriers carriers) {
		// ### some final output: ###
		if (runMatsim){		//makes only sence, when Matsun-Run was performed KT 06.04.15
		new WriteCarrierScoreInfos(carriers, new File(OUTPUT_DIR + "#MatsimCarrierScoreInformation.txt"), runIndex);
		new CarrierPlanXmlWriterV2(carriers).write( OUTPUT_DIR + "matsim_output_carriers.xml") ;
		}		
		new CarrierPlanXmlWriterV2(carriers).write( config.controler().getOutputDirectory() + "/output_carriers.xml") ;
		new CarrierPlanXmlWriterV2(carriers).write( config.controler().getOutputDirectory() + "/output_carriers.xml.gz") ;
	}


	//-----

	//Ergänzung kt: 1.8.2014 Erstellt das OUTPUT-Verzeichnis. Falls es bereits exisitert, geschieht nichts
	// Mit dem doppelten Eintrag für den Run: nicht schön, aber selten.
	private static void createDir(File file) {
		System.out.println("Verzeichnis " + file + "erstellt: "+ file.mkdirs());	
	}

	private static void moveTempFile (File sourceDir, File destDir) {
		File[] files = sourceDir.listFiles();
		File destFile = null;
		destDir.mkdirs();

		try{
			for (int i = 0; i < files.length; i++) {
				destFile = new File(destDir.getAbsolutePath() + System.getProperty("file.separator") + files[i].getName());
				if (files[i].isDirectory()) {
					//copyDir(files[i], newFile);
				}
				else {
					files[i].renameTo(destFile);
					System.out.println("Dateiwurde verschoben: " + files[i].toString() + "nach: " + destFile);
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static Config createConfig(String[] args) {
		Config config = ConfigUtils.createConfig() ;

		if ((args == null) || (args.length == 0)) {
			config.controler().setOutputDirectory(OUTPUT_DIR + RUN + runIndex);
		} else {
			System.out.println( "args[0]:" + args[0] );
			config.controler().setOutputDirectory( args[0]+"/" );
		}

		config.controler().setLastIteration(LAST_MATSIM_ITERATION);	
		config.network().setInputFile(NETFILE);

		//Damit nicht alle um Mitternacht losfahren
		config.plans().setActivityDurationInterpretation(ActivityDurationInterpretation.tryEndTimeThenDuration ); 
		config.vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.WARN);
		return config;
	}  //End createConfig


	private static Carriers createCarriers(CarrierVehicleTypes vehicleTypes) {
		Carriers carriers = new Carriers() ;
		new CarrierPlanXmlReaderV2(carriers).read(CARRIERFILE) ;

		// assign vehicle types to the carriers (who already have their vehicles (??)):
		new CarrierVehicleTypeLoader(carriers).loadVehicleTypes(vehicleTypes) ;
		return carriers;
	}


	private static CarrierVehicleTypes createVehicleTypes() {
		CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes() ;
		new CarrierVehicleTypeReader(vehicleTypes).read(VEHTYPEFILE) ;
		return vehicleTypes;
	}

	/*
	 * Anm.: KT, 23.07.14
	 * 
	 */
	private static void generateCarrierPlans(Network network, Carriers carriers, CarrierVehicleTypes vehicleTypes, Config config) {
		final Builder netBuilder = NetworkBasedTransportCosts.Builder.newInstance( network, vehicleTypes.getVehicleTypes().values() );
		//		netBuilder.setBaseTravelTimeAndDisutility(travelTime, travelDisutility) ;
		netBuilder.setTimeSliceWidth(1800) ; // !!!!, otherwise it will not do anything.


		if (addingToll){		 //Added, KT, 07.08.2014
			generateRoadPricingCalculator(netBuilder, config, carriers);
		}

		final NetworkBasedTransportCosts netBasedCosts = netBuilder.build() ;

		for ( Carrier carrier : carriers.getCarriers().values() ) {
			VehicleRoutingProblem.Builder vrpBuilder = MatsimJspritFactory.createRoutingProblemBuilder( carrier, network ) ;
			vrpBuilder.setRoutingCost(netBasedCosts) ;
			VehicleRoutingProblem vrp = vrpBuilder.build() ;

			VehicleRoutingAlgorithm algorithm = VehicleRoutingAlgorithms.readAndCreateAlgorithm(vrp,ALGORITHMFILE);
			algorithm.setMaxIterations(LAST_JSPRIT_ITERATION);

			VehicleRoutingProblemSolution solution = Solutions.bestOf(algorithm.searchSolutions());
			CarrierPlan newPlan = MatsimJspritFactory.createPlan(carrier, solution) ;

			NetworkRouter.routePlan(newPlan,netBasedCosts) ;
			// (maybe not optimal, but since re-routing is a matsim strategy, 
			// certainly ok as initial solution)

			carrier.setSelectedPlan(newPlan) ;

			/*  JSprit Events. Neu: KT 07.12.2014 --> bringt in Version 1.4.3 noch Fehlermeldung: "gridVehicle_start" .. könnte an den gleichnamigen Vehicle-Ids liegen.. :-(
			 * (AlgorithmEventsRecorder.java:360 und daraus :359, :374): return vehicle.getId() + "_start";
			 */
			//	AlgorithmEventsRecorder.writeSolution(vrp, solution, new File(TEMP_DIR + "jsprit_events_" + RUN + runIndex+".txt"));

			//### Output nach Jsprit Iteration
			new CarrierPlanXmlWriterV2(carriers).write( TEMP_DIR + "jsprit_plannedCarriers_" + RUN + runIndex+".xml") ; //Muss in Temp, da OutputDir leer sein muss // setOverwriteFiles gibt es nicht mehr; kt 05.11.2014
			//Plot der Jsprit-Lösung
			//			Plotter plotter = new Plotter(vrp,solution);
			//			plotter.plot(TEMP_DIR +"jsprit_solution_" +RUN + runIndex+".png", carrier.getId().toString());

			//Ausgabe der Ergebnisse auf der Console
			//			SolutionPrinter.print(vrp,solution,Print.VERBOSE);

		}
	}


	/*
	 * KT, 07.08.2014
	 * Hinzufügen des RoadPricing-Calculators --> Toll wird enabled	
	 */
	static void generateRoadPricingCalculator(final Builder netBuilder, final Config config, final Carriers carriers) {

		ConfigUtils.addOrGetModule(config, RoadPricingConfigGroup.GROUP_NAME, RoadPricingConfigGroup.class);


		//		VehicleTypeDependentRoadPricingCalculator rpCalculator = new VehicleTypeDependentRoadPricingCalculator();
		//				RoadPricingSchemeImpl scheme = new RoadPricingSchemeImpl();
		//				Id<Link> linkIdMaut = Id.createLinkId("i(6,0)");  //bisher nur für den einen Link
		//				scheme.setType("distance");
		//				scheme.addLinkCost(linkIdMaut, 0.0, 24.0*3600, 0.1); //Aktuell noch 0-24 Uhr und extrem hohe Maut!
		//		
		//				rpCalculator.addPricingScheme("gridType", scheme );  //bisher nur für die 26t
		//				netBuilder.setRoadPricingCalculator(rpCalculator);



		final RoadPricingSchemeImpl scheme = new RoadPricingSchemeImpl();
		RoadPricingReaderXMLv1 rpReader = new RoadPricingReaderXMLv1(scheme);
		try {
			RoadPricingConfigGroup rpConfig = (RoadPricingConfigGroup) config.getModule(RoadPricingConfigGroup.GROUP_NAME) ;
			rpConfig.setTollLinksFile(TOLLFILE);
			rpReader.parse(rpConfig.getTollLinksFile());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		VehicleTypeDependentRoadPricingCalculator rpCalculator = new VehicleTypeDependentRoadPricingCalculator();


		//		//Damit alle Carrier die Maut erfahren --> noch unsauber, da auf die einzelnen Vehicles genommen werden --> doppelte Einträge. KT 23.10.14
		//		for(Carrier c : carriers.getCarriers().values()){
		//			for(CarrierVehicle v : c.getCarrierCapabilities().getCarrierVehicles()){
		//				Id typeId = v.getVehicleId();
		//				rpCalculator.addPricingScheme(typeId.toString(), scheme);
		//			}
		//		}
		rpCalculator.addPricingScheme("gridType01", scheme );  //bisher nur für die GridVehicles, KT
		rpCalculator.addPricingScheme("gridType05", scheme );  //bisher nur für die GridVehicles, KT
		rpCalculator.addPricingScheme("gridType10", scheme );  //bisher nur für die GridVehicles, KT
		netBuilder.setRoadPricingCalculator(rpCalculator);
		
		rpscheme = scheme;
		
		for(Id<VehicleType> vehTypId: rpCalculator.getSchemes().keySet()){
			textInfofile.writeTextLineToFile(vehTypId.toString());
			textInfofile.writeTextLineToFile(rpCalculator.getPricingSchemes(vehTypId).toString());
		}
		
	}


	/*
	 * Anm.: KT, 23.07.14
	 * Nur wenn AddingCongestion == true !!
	 * Basiert offenbar auf einem Schwellenwert von 5/3.6 approx. 1.39. [m/s] --> 5 km/h
	 * Auf allen Links, bei denen Freespeed > Schwellenwert, wird von 7-11:30 Uhr [Für Grid auf 1 bis 2 Uhr gesetzt], Kt 10.12.14
	 * die absolute Geschwindigkeit auf 1/10 der Grenze (also 0.139 m/s --> 0.5 km/h) gesetzt.
	 * Somit aktuell nur Abbildung einer morgendlichen Rush-Hour.
	 * 
	 * Änderungsüberlegung, inwiefern dies realistisch ist. Und nicht eher ein Faktor auf den freespeed verwendet werden sollte.
	 * 
	 */
	private static void configureTimeDependentNetwork(Scenario scenario) {
		NetworkChangeEventFactory cef = new NetworkChangeEventFactoryImpl() ;
		for ( Link link : scenario.getNetwork().getLinks().values() ) {
			double speed = link.getFreespeed() ;
			final double threshold = 5./3.6;		//5km/h
//			if (link.getId().toString().startsWith("j(1,")) {  //Nur für bestimmte Links Nordwärts zu Service 1, KT 7.1.86
				if ( speed > threshold ) {
					{
						NetworkChangeEvent event = cef.createNetworkChangeEvent(0*3600.) ;
						event.setFreespeedChange(new ChangeValue( ChangeType.ABSOLUTE,  threshold )); //KT, 7.1.15 vorher threshold/10 hatte leere Lösung
						event.addLink(link);
						((NetworkImpl)scenario.getNetwork()).addNetworkChangeEvent(event);
					}
					{
						NetworkChangeEvent event = cef.createNetworkChangeEvent(24*3600.) ;
						event.setFreespeedChange(new ChangeValue( ChangeType.ABSOLUTE,  speed ));
						event.addLink(link);
						((NetworkImpl)scenario.getNetwork()).addNetworkChangeEvent(event);
					}
				}
//			} // End-Klammer: Bestimmte Links
		}
	}

	//Benötigt, da listener kein "Null" als StrategyFactory mehr erlaubt, KT 17.04.2015
	//Da keine Strategy notwendig hier zunächst eine "leere" Factory
	private static CarrierPlanStrategyManagerFactory createMyStrategymanager(){
		return new CarrierPlanStrategyManagerFactory() {

			@Override
			public GenericStrategyManager<CarrierPlan, Carrier> createStrategyManager() {
				return null;
			}
			
		};

	}
	
	/*
	 * Neue Variante der ScoringFunction KT, 17.04.15
	 * //TODO: Test von  MoneyScoring, ActivityScoring
	 * Activity: Kostensatz mitgeben, damit klar ist, wo er herkommt... oder vlt geht es in dem Konstrukt doch aus den Veh-Eigenschaften?? (KT, 17.04.15)
	 * TODO: Maut fuktoniert nicht (MoneyScoring). Ist jedoch in Events zu sehen.
	 */
	
	private static CarrierScoringFunctionFactoryImpl_KT createMyScoringFunction2 (final Scenario scenario) {

		textInfofile.writeTextLineToFile("createMyScoringFunction2 aufgerufen");
		
		return new CarrierScoringFunctionFactoryImpl_KT(scenario, TEMP_DIR) {
			
			public ScoringFunction createScoringFunction(final Carrier carrier){
			SumScoringFunction sumSf = new SumScoringFunction() ;
			
//			VehicleFixCostScoring fixCost = new VehicleFixCostScoring(carrier);
//			sumSf.addScoringFunction(fixCost);
			
//			LegScoring legScoring = new LegScoring(carrier);
//			sumSf.addScoringFunction(legScoring);
			
//			ActivityScoring actScoring = new ActivityScoring(carrier);
//			sumSf.addScoringFunction(actScoring);
			
			MoneyScoring moneyScoring = new MoneyScoring(carrier);
			sumSf.addScoringFunction(moneyScoring);
			
			return sumSf;
			}
		};
	}

	private static void writeRunInfo() {
		File file = new File(OUTPUT_DIR + "#RunInformation.txt");
		try {
			FileWriter writer = new FileWriter(file);  //Neuer File (überschreibt im Zweifel den alten - der jedoch nicht existieren dürfte!

			writer.write("##Inputfiles:" +System.getProperty("line.separator"));
			writer.write("Net: \t \t" + NETFILE +System.getProperty("line.separator"));
			writer.write("Carrier:  \t" + CARRIERFILE +System.getProperty("line.separator"));
			writer.write("VehType: \t" + VEHTYPEFILE +System.getProperty("line.separator"));
			writer.write("Algorithm: \t" + ALGORITHMFILE +System.getProperty("line.separator"));

			writer.write(System.getProperty("line.separator"));
			writer.write("##Run Settings:" +System.getProperty("line.separator"));
			writer.write("addingCongestion: \t" + addingCongestion +System.getProperty("line.separator"));
			writer.write("addingToll: \t \t" + addingToll +System.getProperty("line.separator"));
			writer.write("runMatsim: \t \t" + runMatsim +System.getProperty("line.separator"));
			writer.write("Last Matsim Iteration: \t" + LAST_MATSIM_ITERATION +System.getProperty("line.separator"));
			writer.write("Max Jsprit Iteration: \t" + LAST_JSPRIT_ITERATION +System.getProperty("line.separator"));
			writer.write("Number of Runs: \t" + NU_OF_TOTAL_RUNS +System.getProperty("line.separator"));

			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Datei: " + file + " geschrieben.");
	}

}

