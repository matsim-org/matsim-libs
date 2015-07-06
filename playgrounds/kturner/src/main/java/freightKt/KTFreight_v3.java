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

import jsprit.analysis.toolbox.Plotter;
import jsprit.core.algorithm.VehicleRoutingAlgorithm;
import jsprit.core.algorithm.io.VehicleRoutingAlgorithms;
import jsprit.core.problem.VehicleRoutingProblem;
import jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import jsprit.core.util.Solutions;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.contrib.freight.carrier.Tour.ServiceActivity;
import org.matsim.contrib.freight.carrier.Tour.TourElement;
import org.matsim.contrib.freight.controler.CarrierModule;
import org.matsim.contrib.freight.jsprit.MatsimJspritFactory;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts.Builder;
import org.matsim.contrib.freight.jsprit.NetworkRouter;
import org.matsim.contrib.freight.jsprit.VehicleTypeDependentRoadPricingCalculator;
import org.matsim.contrib.freight.replanning.CarrierPlanStrategyManagerFactory;
import org.matsim.contrib.freight.scoring.CarrierScoringFunctionFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.consistency.VspConfigConsistencyCheckerImpl;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.PlansConfigGroup.ActivityDurationInterpretation;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.VspDefaultsCheckingLevel;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.ControlerUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkChangeEvent.ChangeType;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;
import org.matsim.core.network.NetworkChangeEventFactory;
import org.matsim.core.network.NetworkChangeEventFactoryImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.replanning.GenericStrategyManager;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.roadpricing.ControlerDefaultsWithRoadPricingModule;
import org.matsim.roadpricing.RoadPricingConfigGroup;
import org.matsim.roadpricing.RoadPricingReaderXMLv1;
import org.matsim.roadpricing.RoadPricingSchemeImpl;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author kt
 * Variante von KT
 * Unterschied zu v_2: Es wurde die funktionalität der Umschlagplätze eingebaut werden.
 * Dazu wird benötigt, 
 * a) Orte der UCCs "unrban Consolidation centers" [s. Ehmke 2012 p. 15] (über Definition des Carriers)
 * b) welche Services, über die UCC abgewickelt werden (Hier die innerhalb der Mautgrenzen. Die Touren müssen dann von 
 * 		Jsprit entsprechend geplant werden
 * c) somit können Services (mit je demand=1) von den normalen DC zu den Ucc erstellt werden und bei deren Tourenplanung 
 * 		mit berücksichtigt werden. Aus Realitätsgründen ist es durchaus möglich dass die UCC von verschiedenen DCs 
 * 		beliefert werden. Werde hier zunächst dann die "normalen" Carrier mitnutzen. 
 * 		Können dann (z.Bsp. zu Beginn) ihrer Tour, Ware zu den UCC bringen.
 * d) demand=1, damit bei der Tourenplanung die optimalen Fzg und Tourenauswahl/Zusammensetzung gewählt werden kann. Jsprit
 * 		kann zusammenfassen aber nicht auf mehrere Fzg. auftrennen. Dementsprehcend auch eine kurze duration wählen.
 * e) Anlieferungszeit bei UCC werden so angepasst, dass die endet, bevor diese zur Auslieferung öffnen.
 */
public class KTFreight_v3 {

	private static final Logger log = Logger.getLogger(KTFreight_v3.class) ;

		//Beginn Namesdefinition KT Für Berlin-Szenario 
		private static final String INPUT_DIR = "C:/Users/kturner/workspace/projects_freight/studies/MA_Turner-Kai/input/Berlin_Szenario/" ;
		private static final String OUTPUT_DIR = "Z:/WinHome/Docs/runs/Berlin/aldi/Base/" ;
		private static final String TEMP_DIR = "Z:/WinHome/Docs/runs/Temp/";
	
		//Dateinamen ohne XML-Endung
		private static final String NETFILE_NAME = "network" ;
		private static final String VEHTYPES_NAME = "vehicleTypes" ;
		private static final String CARRIERS_NAME = "carrierLEH_v2_withFleet" ;
		private static final String ALGORITHM_NAME = "mdvrp_algorithmConfig_2" ;
		private static final String TOLL_NAME = "toll_cordon20";		//Zur Mautberechnung
		private static final String LEZ_NAME = "toll_area";  //Zonendefinition (Links) für anhand eines Maut-Files
		
		//Prefix mit denen UCC-CarrierIds beginnen (Rest identisch mit CarrierId).
		private static final String uccC_prefix = "UCC-";	

		//All retailer/carrier to handle in UCC-Case. (begin of CarrierId); null if all should be used.
		private static final ArrayList<String> retailerNames = 
				new ArrayList<String>(Arrays.asList("aldi")); 
		//Location of UCC
		private static final ArrayList<String> uccDepotsLinkIdsString = 
				new ArrayList<String>(Arrays.asList("6874", "3058", "5468")); 
		// VehicleTypes die vom Maut betroffen seien sollen. null, wenn alle (ohne Einschränkung) bemautet werden sollen
		private static final ArrayList<String> onlyTollVehTypes = 
				new ArrayList<String>(Arrays.asList("heavy40t", "heavy26t", "heavy26t_frozen", "medium18t", "light8t", "light8t_frozen")); 
		//Ende  Namesdefinition Berlin


////Beginn Namesdefinition KT Für Test-Szenario (Grid)
//	private static final String INPUT_DIR = "F:/OneDrive/Dokumente/Masterarbeit/MATSIM/input/Grid_Szenario/" ;
//	private static final String OUTPUT_DIR = "F:/OneDrive/Dokumente/Masterarbeit/MATSIM/output/JSprit/Grid/UCC_2/" ;
//	private static final String TEMP_DIR = "F:/OneDrive/Dokumente/Masterarbeit/MATSIM/output/Temp/" ;	
//
//	//Dateinamen ohne XML-Endung
//	private static final String NETFILE_NAME = "grid-network" ;
//	private static final String VEHTYPES_NAME = "grid-vehTypes_kt" ;
//	private static final String CARRIERS_NAME = "grid-carrier_kt2" ;
//	private static final String ALGORITHM_NAME = "mdvrp_algorithmConfig_2" ;
//	private static final String TOLL_NAME = "grid-tollDistance";
//	private static final String LEZ_NAME = "grid-tollArea"; 
//	//Prefix mit denen UCC-CarrierIds beginnen (Rest identisch mit CarrierId). Vermeide "_", 
//	//um die Analyse der MATSIMEvents einfacher zu gestalten (Dort ist "_" als Trennzeichen verwendet.
//	private static final String uccC_prefix = "UCC-";		
//	// All retailer/carrier to handle in UCC-Case. (begin of CarrierId); null if all should be used.
//	//TODO: add Warning, if Name does not exists in carrier-File
//	private static final ArrayList<String> retailerNames = null ;
////			new ArrayList<String>(Arrays.asList("gridCarrier3"));
////		= new ArrayList<String>("gridCarrier", "gridCarrier1", "gridCarrier2", "gridCarrier3"); 
//	//Location of UCC
//	private static final ArrayList<String> uccDepotsLinkIdsString =
//		new ArrayList<String>(Arrays.asList("j(0,5)", "j(10,5)")); 
//	// VehicleTypes die vom Maut betroffen seien sollen. null, wenn alle (ohne Einschränkung) bemautet werden sollen
//	//TODO: add Warning, if Name does not exists in carrier-File and /or vehType-File (oder andere klare Restriktion)
//	private static final ArrayList<String> onlyTollVehTypes =  null;
////		new ArrayList<String>(Arrays.asList("gridType01", "gridType03", "gridType05", "gridType10")); 
////	//Ende Namesdefinition Grid

	
	private static final String RUN = "Run_" ;
	private static int runIndex = 0;

	private static final String NETFILE = INPUT_DIR + NETFILE_NAME + ".xml" ;
	private static final String VEHTYPEFILE = INPUT_DIR + VEHTYPES_NAME + ".xml";
	private static final String CARRIERFILE = INPUT_DIR + CARRIERS_NAME + ".xml" ;
	private static final String ALGORITHMFILE = INPUT_DIR + ALGORITHM_NAME + ".xml";
	private static final String TOLLFILE = INPUT_DIR + TOLL_NAME + ".xml";
	private static final String ZONEFILE = INPUT_DIR + LEZ_NAME + ".xml";


	// Einstellungen für den Run	
	private static final boolean addingCongestion = true ;  //doesn't work correctly, KT 10.12.2014
	private static final boolean addingToll = false;  //added, kt. 07.08.2014
	private static final boolean usingUCC = false;	 //Using Transshipment-Center, added kt 30.04.2015
	private static final boolean runMatsim = true;	 //when false only jsprit run will be performed
	private static final int LAST_MATSIM_ITERATION = 0;  //only one iteration for writing events.
	private static final int MAX_JSPRIT_ITERATION = 4000;
	private static final int NU_OF_TOTAL_RUNS = 100;	

	//temporär zum Programmieren als Ausgabe
	private static WriteTextToFile textInfofile; 

	//da immer wieder benutzt.
	private static RoadPricingSchemeImpl rpscheme = new RoadPricingSchemeImpl();
	private static VehicleTypeDependentRoadPricingCalculator rpCalculator = 
			new VehicleTypeDependentRoadPricingCalculator();


	public static void main(String[] args) {
		for (int i = 1; i<=NU_OF_TOTAL_RUNS; i++) {
			//Damit jeweils neu besetzt wird; sonst würde es sich aufkumulieren.
			rpscheme = new RoadPricingSchemeImpl();		
			rpCalculator = new VehicleTypeDependentRoadPricingCalculator();	
			
			runIndex = i;	
			multipleRun(args);	
		}
		writeRunInfo();	
		moveTempFiles(new File(TEMP_DIR), new File(OUTPUT_DIR)); //move of temp-files
		System.out.println("#### End of all runs --Y Finished ####");
	}


	//### KT 03.12.2014 multiple run for testing the variety of the jsprit solutions (especially in terms of costs). 
	private static void multipleRun (String[] args){	
		System.out.println("#### Starting Run: " + runIndex + " of: "+ NU_OF_TOTAL_RUNS);
		createDir(new File(OUTPUT_DIR + RUN + runIndex));
		createDir(new File(TEMP_DIR + RUN + runIndex));	
		

		// ### config stuff: ###	
		Config config = createConfig(args);
				config.addConfigConsistencyChecker(new VspConfigConsistencyCheckerImpl());
				config.controler().setOverwriteFileSetting(OverwriteFileSetting.failIfDirectoryExists);
				ControlerUtils.checkConfigConsistencyAndWriteToLog(config, "dump");
		textInfofile = new WriteTextToFile(new File(TEMP_DIR + "#TextInformation.txt"), null);
		
		if ( addingCongestion ) { //erst config vorbereiten....Config ist fix, sobald in Scenario!!!! KT, 11.12.14
			config.network().setTimeVariantNetwork(true);
		}
		
		// ### scenario stuff: ###
		Scenario scenario = ScenarioUtils.loadScenario(config);

		if ( addingCongestion ) {
			configureTimeDependentNetwork(scenario);
		}

		//Building the Carriers with jsprit, incl jspritOutput KT 03.12.2014
		Carriers carriers = jspritRun(config, scenario);			
		
		if ( runMatsim){
			matsimRun(scenario, carriers);	//final MATSim configurations and start of the MATSim-Run
		}
		finalOutput(config, carriers);	//write some final Output
	} 

	private static Carriers jspritRun(Config config, Scenario scenario) {
		CarrierVehicleTypes vehicleTypes = createVehicleTypes();

		Carriers carriers = createCarriers(vehicleTypes);
	
		carriers = new UccCarrierCreator().extractCarriers(carriers, retailerNames);

		/*
		* Wenn UCC verwendent werden, dann muss das Problem geteilt werden.
		* Es erfolgt eien seperate Berechnung der Touren für die (neuen
		* UCC-Carrier, welche innerhalb der Umweltzone liefern und den
		* bisherigen Carriern, die die anderen Services außerhalb der Zone 
		* übernehmen. Hinzu kommt noch der Transport der Güter für die UCC-Carrier
		* von den Depots, welcher ebenfalls von den bisherigen Carriern im Rahmen
		* ihrer Tour mit übernommen wird.	
		*/
		if (usingUCC) {		
			//TODO: Tests ob uccDepotsLinkIdsString != null und ob LinkIds element des Netzwerkes.
			ArrayList<Id<Link>> uccDepotsLinkIds = new ArrayList<Id<Link>>();	//Location of UCC
			for (String linkId : uccDepotsLinkIdsString){
				uccDepotsLinkIds.add(Id.createLinkId(linkId));
			}
			
			UccCarrierCreator uccCarrierCreator = new UccCarrierCreator(carriers, vehicleTypes, ZONEFILE, uccC_prefix, retailerNames, uccDepotsLinkIds, 0.0, 0.0 );
			uccCarrierCreator.createSplittedUccCarrriers();
			carriers = uccCarrierCreator.getSplittedCarriers();
			
			Carriers uccCarriers = new Carriers();
			Carriers nonUccCarriers = new Carriers();
			for (Carrier c : carriers.getCarriers().values()){
				if (c.getId().toString().startsWith(uccC_prefix)){		//Wenn Carrier ID mit UCC beginnt.
					uccCarriers.addCarrier(c);
				} else {
					nonUccCarriers.addCarrier(c);
				};
			}
			
			generateCarrierPlans(scenario.getNetwork(), uccCarriers, vehicleTypes, config); // Hier erfolgt Lösung des VRPs für die UCC-Carriers

			//TODO: DepotÖffnungszeiten der normalen Carrier anpassen, damit UCC rechtzeitig beliefert werden können. -> für künftige Erweiterung
			nonUccCarriers = uccCarrierCreator.createServicesToUCC(uccCarriers, nonUccCarriers); // Nachfrage den der UCCC ausliefert muss an die Umschlagpunkte geliefert werden. 
			generateCarrierPlans(scenario.getNetwork(), nonUccCarriers, vehicleTypes, config); // Hier erfolgt Lösung des VRPs für die NonUCC-Carriers


		} else {  // ohne UCCs 
			carriers = new UccCarrierCreator().extractCarriers(carriers, retailerNames);
			carriers = new UccCarrierCreator().renameVehId(carriers);
			generateCarrierPlans(scenario.getNetwork(), carriers, vehicleTypes, config); // Hier erfolgt Lösung des VRPs
		}
			
		checkServiceAssignment(carriers);
		
		//### Output nach Jsprit Iteration
		new CarrierPlanXmlWriterV2(carriers).write( TEMP_DIR +  RUN + runIndex + "/jsprit_plannedCarriers.xml") ; //Muss in Temp, da OutputDir leer sein muss // setOverwriteFiles gibt es nicht mehr; kt 05.11.2014

		new WriteCarrierScoreInfos(carriers, new File(TEMP_DIR +  "#JspritCarrierScoreInformation.txt"), runIndex);

		return carriers;
	}

	/**
	 * Prüft ob alle Services auch in den geplanten Touren vorkommen, d.h., ob sie auhc tatsächslcih geplant wurden.
	 * Falls nicht: log.Error und Ausgabe einer Datei: "#UnassignedServices.txt" mit den Service-Ids.
	 * @param carriers
	 */
	//TODO: Ausgabe der Assigned Services in Run-Verzeichnis und dafür in der Übersicht nur eine Nennung der Anzahl unassignedServices je Run 
	//TODO: multiassigned analog.
	private static void checkServiceAssignment(Carriers carriers) {
		for (Carrier c :carriers.getCarriers().values()){
			ArrayList<CarrierService> assignedServices = new ArrayList<CarrierService>();
			ArrayList<CarrierService> multiassignedServices = new ArrayList<CarrierService>();
			ArrayList<CarrierService> unassignedServices = new ArrayList<CarrierService>();

			System.out.println("### Carrier: " +c.getId());
			//Erfasse alle einer Tour zugehörigen (-> stattfindenden) Services 
			for (ScheduledTour tour : c.getSelectedPlan().getScheduledTours()){
				for (TourElement te : tour.getTour().getTourElements()){
					if (te instanceof  ServiceActivity){
						CarrierService assignedService = ((ServiceActivity) te).getService();
						if (!assignedServices.contains(assignedService)){
							assignedServices.add(assignedService);
							System.out.println("Assigned Service: " +assignedServices.toString());
						} else {
							multiassignedServices.add(assignedService);
							log.error("Service wurde von dem Carrier " + c.getId().toString() + " bereits angefahren: " + assignedService.getId().toString() );
						}
					}
				}
			}
			
			//Nun prüfe, ob alle definierten Services zugeordnet wurden
			for (CarrierService service : c.getServices()){
				System.out.println("Service to Check: " + service.toString());
				if (!assignedServices.contains(service)){
					System.out.println("Service not assigned: " +service.toString());
					unassignedServices.add(service);
					log.error("Service wird von Carrier " + c.getId().toString() + " NICHT bedient: " + service.getId().toString() );
				} else {
					System.out.println("Service was assigned: " +service.toString());
				}
			}
			
			//Schreibe die mehrfach eingeplanten Services in Datei
			if (!multiassignedServices.isEmpty()){
				try {
					FileWriter writer = new FileWriter(new File(TEMP_DIR + "#MultiAssignedServices.txt"), true);
					writer.write("#### Multi-assigned Services of Carrier: " + c.getId().toString() + System.getProperty("line.separator"));
					for (CarrierService s : multiassignedServices){
						writer.write(s.getId().toString() + System.getProperty("line.separator"));
					}
					writer.flush();
					writer.close();
				} catch (IOException e) {
					e.printStackTrace();
				} 
			}
			
			//Schreibe die nicht eingeplanten Services in Datei
			if (!unassignedServices.isEmpty()){
				try {
					FileWriter writer = new FileWriter(new File(TEMP_DIR + "#UnassignedServices.txt"), true);
					writer.write("#### Unassigned Services of Carrier: " + c.getId().toString() + System.getProperty("line.separator"));
					for (CarrierService s : unassignedServices){
						writer.write(s.getId().toString() + System.getProperty("line.separator"));
					}
					writer.flush();
					writer.close();
				} catch (IOException e) {
					e.printStackTrace();
				} 
			}
			
		}//for(carriers)

	}


	private static void matsimRun(Scenario scenario, Carriers carriers) {
		final Controler controler = new Controler( scenario ) ;

		if (addingToll){		 //Added RoadpricingScheme to MATSIM-Controler Added, KT, 02.12.2014
			controler.setModules(new ControlerDefaultsWithRoadPricingModule(rpscheme));
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
		controler.addOverridingModule(listener) ;

		controler.run();
	}
	
	//TODO: Klarmachen, wann wlechen Output schreiben. Im Moment wird der ketzte immer in RUN-Verzeichnis geschrieben, 
	//das ist mal jsprit und mal Matsim, je nachdem was durchgeführt zuletzt wurde... -> nicht einheitlich -> nicht gut :(
	private static void finalOutput(Config config, Carriers carriers) {
		// ### some final output: ###
		if (runMatsim){		//makes only sence, when Matsun-Run was performed KT 06.04.15
		new WriteCarrierScoreInfos(carriers, new File(OUTPUT_DIR + "#MatsimCarrierScoreInformation.txt"), runIndex);
//		new CarrierPlanXmlWriterV2(carriers).write( OUTPUT_DIR + "matsim_output_carriers.xml") ; //das letzte Ergebnis in oberstes AusgabeSit schreiben
		}		
		new CarrierPlanXmlWriterV2(carriers).write( config.controler().getOutputDirectory() + "/output_carriers.xml") ;
		new CarrierPlanXmlWriterV2(carriers).write( config.controler().getOutputDirectory() + "/output_carriers.xml.gz") ;
	}


	//-----

	//Ergänzung kt: 1.8.2014 Erstellt das OUTPUT-Verzeichnis. Falls es bereits exisitert, geschieht nichts
	// Mit dem doppelten Eintrag für den Run: nicht schön, aber selten.
	private static void createDir(File file) {
		System.out.println("Verzeichnis " + file + " erstellt: "+ file.mkdirs());	
	}

	private static void moveTempFiles (File sourceDir, File destDir) {
		File[] files = sourceDir.listFiles();
		File destFile = null;
		destDir.mkdirs();

		try{
			for (int i = 0; i < files.length; i++) {
				destFile = new File(destDir.getAbsolutePath() + System.getProperty("file.separator") + files[i].getName());
				if (files[i].isDirectory()) {
					FileUtils.copyDirectory(files[i], destFile);
					FileUtils.deleteDirectory(files[i]);
				}
				else {
					files[i].renameTo(destFile);
					System.out.println("Datei wurde verschoben: " + files[i].toString() + " nach: " + destFile);
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
		config.plans().setActivityDurationInterpretation(PlansConfigGroup.ActivityDurationInterpretation.tryEndTimeThenDuration ); 
		config.vspExperimental().setVspDefaultsCheckingLevel(VspDefaultsCheckingLevel.warn);
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
		Builder netBuilder = NetworkBasedTransportCosts.Builder.newInstance( network, vehicleTypes.getVehicleTypes().values() );
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
			algorithm.setMaxIterations(MAX_JSPRIT_ITERATION);

			VehicleRoutingProblemSolution solution = Solutions.bestOf(algorithm.searchSolutions());
			CarrierPlan newPlan = MatsimJspritFactory.createPlan(carrier, solution) ;

			NetworkRouter.routePlan(newPlan,netBasedCosts) ;
			// (maybe not optimal, but since re-routing is a matsim strategy, 
			// certainly ok as initial solution)

			carrier.setSelectedPlan(newPlan) ;

			//Plot der Jsprit-Lösung
			Plotter plotter = new Plotter(vrp,solution);
//			if (carrier.getId().toString().startsWith(uccC_prefix)){
//				plotter.plot(TEMP_DIR +"jsprit_solution_" +RUN + runIndex+"_UccC.png", carrier.getId().toString());
//			} else{
//				plotter.plot(TEMP_DIR +"jsprit_solution_" +RUN + runIndex+".png", carrier.getId().toString());
//			}
			plotter.plot(TEMP_DIR + RUN + runIndex + "/jsprit_solution_" + carrier.getId().toString() +".png", carrier.getId().toString());
			
			//Ausgabe der Ergebnisse auf der Console
			//SolutionPrinter.print(vrp,solution,Print.VERBOSE);
			
		}
	}


	/*
	 * KT, 07.08.2014
	 * Hinzufügen des RoadPricing-Calculators --> Toll wird enabled	
	 */
	static void generateRoadPricingCalculator(final Builder netBuilder, final Config config, final Carriers carriers) {

		ConfigUtils.addOrGetModule(config, RoadPricingConfigGroup.GROUP_NAME, RoadPricingConfigGroup.class);

		final RoadPricingSchemeImpl scheme = new RoadPricingSchemeImpl();
		RoadPricingReaderXMLv1 rpReader = new RoadPricingReaderXMLv1(scheme);
		try {
			RoadPricingConfigGroup rpConfig = (RoadPricingConfigGroup) config.getModule(RoadPricingConfigGroup.GROUP_NAME) ;
			rpConfig.setTollLinksFile(TOLLFILE);
			rpReader.parse(rpConfig.getTollLinksFile());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		
		Collection<Id<VehicleType>> vehTypesAddedToRPS = new ArrayList<Id<VehicleType>>();
		//keine Einschränkung eingegeben -> alle bemauten
		if (onlyTollVehTypes == null) {
			for(Carrier c : carriers.getCarriers().values()){
				for(CarrierVehicle v : c.getCarrierCapabilities().getCarrierVehicles()){
					Id<VehicleType> typeId = v.getVehicleType().getId();
					if (!vehTypesAddedToRPS.contains(typeId)) {
						vehTypesAddedToRPS.add(typeId);
						rpCalculator.addPricingScheme(typeId, scheme);
					}
				}
			}
		} else {
			for(Carrier c : carriers.getCarriers().values()){
				for(CarrierVehicle v : c.getCarrierCapabilities().getCarrierVehicles()){
					Id<VehicleType> typeId = v.getVehicleType().getId();
					if (onlyTollVehTypes.contains(typeId.toString()) & !vehTypesAddedToRPS.contains(typeId)){
						vehTypesAddedToRPS.add(typeId);
						rpCalculator.addPricingScheme(typeId, scheme);
					}
				}
			}
		}

		netBuilder.setRoadPricingCalculator(rpCalculator);
		
		rpscheme = scheme;
		
		//Writing Info
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
//			final double threshold = 5./3.6;		//5km/h
//				if ( speed > threshold ) {
//					{
//						NetworkChangeEvent event = cef.createNetworkChangeEvent(0*3600.) ;
//						event.setFreespeedChange(new ChangeValue( ChangeType.ABSOLUTE,  threshold )); //KT, 7.1.15 vorher threshold/10 hatte leere Lösung
//						event.addLink(link);
//						((NetworkImpl)scenario.getNetwork()).addNetworkChangeEvent(event);
//					}
//					{
//						NetworkChangeEvent event = cef.createNetworkChangeEvent(24*3600.) ;
//						event.setFreespeedChange(new ChangeValue( ChangeType.ABSOLUTE,  speed ));
//						event.addLink(link);
//						((NetworkImpl)scenario.getNetwork()).addNetworkChangeEvent(event);
//					}
//				}
				
			//Berufsverkehr von 7-10 Uhr und 16:30 bis 19 Uhr; Für alle Kanten mit fressped > 25 km/h wird dieser auf 50% reduziert
				final double threshold = 25./3.6;		//25km/h
				if ( speed > threshold ) {
					{
						NetworkChangeEvent event = cef.createNetworkChangeEvent(7*3600.) ;
						event.setFreespeedChange(new ChangeValue( ChangeType.FACTOR,  0.5 )); 
						event.addLink(link);
						((NetworkImpl)scenario.getNetwork()).addNetworkChangeEvent(event);
					}
					{
						NetworkChangeEvent event = cef.createNetworkChangeEvent(10*3600.) ;
						event.setFreespeedChange(new ChangeValue( ChangeType.ABSOLUTE,  speed ));
						event.addLink(link);
						((NetworkImpl)scenario.getNetwork()).addNetworkChangeEvent(event);
					}
					{
						NetworkChangeEvent event = cef.createNetworkChangeEvent(16.5*3600.) ;
						event.setFreespeedChange(new ChangeValue( ChangeType.FACTOR,  0.5 )); 
						event.addLink(link);
						((NetworkImpl)scenario.getNetwork()).addNetworkChangeEvent(event);
					}
					{
						NetworkChangeEvent event = cef.createNetworkChangeEvent(19*3600.) ;
						event.setFreespeedChange(new ChangeValue( ChangeType.ABSOLUTE,  speed ));
						event.addLink(link);
						((NetworkImpl)scenario.getNetwork()).addNetworkChangeEvent(event);
					}
				}
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
	 * TODO:
	 * Activity: Kostensatz mitgeben, damit klar ist, wo er herkommt... oder vlt geht es in dem Konstrukt doch aus den Veh-Eigenschaften?? (KT, 17.04.15)
	 */
	
	private static CarrierScoringFunctionFactoryImpl_KT createMyScoringFunction2 (final Scenario scenario) {

		textInfofile.writeTextLineToFile("createMyScoringFunction2 aufgerufen");
		
		return new CarrierScoringFunctionFactoryImpl_KT(scenario, scenario.getConfig().controler().getOutputDirectory()) {
			
			public ScoringFunction createScoringFunction(final Carrier carrier){
			SumScoringFunction sumSf = new SumScoringFunction() ;
			
			VehicleFixCostScoring fixCost = new VehicleFixCostScoring(carrier);
			sumSf.addScoringFunction(fixCost);
			
			LegScoring legScoring = new LegScoring(carrier);
			sumSf.addScoringFunction(legScoring);
			
			//Score Activity w/o Correction of WaitingTime @ 1st Service.
//			ActivityScoring actScoring = new ActivityScoring(carrier);
//			sumSf.addScoringFunction(actScoring);
			
			//Score Activity with Correction of WaitingTime @ 1st Service.
			ActivityScoringWithCorrection actScoring = new ActivityScoringWithCorrection(carrier);
			sumSf.addScoringFunction(actScoring);
			
			//TollScoring - liefert nur leider keine Amounts und somit auch keine Kosten :( -> use tollScoring
//			MoneyScoring moneyScoring = new MoneyScoring(carrier);
//			sumSf.addScoringFunction(moneyScoring);
			
			TollScoring tollScoring = new TollScoring(carrier, scenario.getNetwork(), rpCalculator) ;
			sumSf.addScoringFunction(tollScoring);
			
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
			writer.write("Toll: \t" + TOLL_NAME +System.getProperty("line.separator"));
			writer.write("LowEmissionZone: \t" + LEZ_NAME +System.getProperty("line.separator"));

			writer.write(System.getProperty("line.separator"));
			writer.write("##Run Settings:" +System.getProperty("line.separator"));
			writer.write("addingCongestion: \t" + addingCongestion +System.getProperty("line.separator"));
			writer.write("addingToll: \t \t" + addingToll +System.getProperty("line.separator"));
			writer.write("usingUCC: \t \t" + usingUCC +System.getProperty("line.separator"));
			writer.write("runMatsim: \t \t" + runMatsim +System.getProperty("line.separator"));
			writer.write("Last Matsim Iteration: \t" + LAST_MATSIM_ITERATION +System.getProperty("line.separator"));
			writer.write("Max Jsprit Iteration: \t" + MAX_JSPRIT_ITERATION +System.getProperty("line.separator"));
			writer.write("Number of Runs: \t" + NU_OF_TOTAL_RUNS +System.getProperty("line.separator"));

			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Datei: " + file + " geschrieben.");
	}

}

