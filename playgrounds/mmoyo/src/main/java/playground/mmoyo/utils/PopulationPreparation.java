package playground.mmoyo.utils;

import java.io.File;

import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.api.core.v01.population.Population;
import playground.mmoyo.algorithms.PassengerTracker2;
import playground.mmoyo.cadyts_integration.Z_Launcher;
import playground.mmoyo.utils.calibration.OverDemandPlan_router;

public class PopulationPreparation {

	public static void main(String[] args) {
		String configFile= null;
		String[] valuesArray = null;
		int numHomePlans;
		int numClons;
		String allPop= null;
		
		if (args.length>0){
			configFile = args[0];	  //config comb1 comb2 comb3 clones homPlans
			valuesArray = new String[3];
			valuesArray[0]=args[1];
			valuesArray[1]=args[2];
			valuesArray[2]=args[3];
			numClons = Integer.valueOf(args[4]);
			numHomePlans = Integer.valueOf(args[5]);
			//allPop = args[6];
		}else{
			configFile = "../../berlin-bvg09/ptManuel/calibration/100plans_bestValues_config.xml";
			valuesArray = new String[3];
			valuesArray[0] = "6_0.0_1200";
			valuesArray[1] = "10_0.0_240";
			valuesArray[2] = "8_0.5_720";
		
			numClons = 0;
			numHomePlans = 1;
			//allPop = "";
		}
		DataLoader dataLoader = new DataLoader();

		//do here whatever you want to the population before the routing and calibration///////////////
		//////////////////////////////////////////////////////////////////////////////////////////////*
		
		//Config config = dataLoader.readConfig(configFile);
		//Population pop = dataLoader.readPopulation(config.plans().getInputFile());
		
		//ScenarioImpl scn = dataLoader.loadScenario(configFile); 
		//Population pop = scn.getPopulation();
		
		//get ride of cars plans
		/*
		PlansFilterByLegMode plansFilter = new PlansFilterByLegMode( TransportMode.car, PlansFilterByLegMode.FilterType.removeAllPlansWithMode) ;
		plansFilter.run(pop);
		plansFilter = null;
		*/
		
		//re-mutate expanded persons whose original agent is still present
		/*
		Population popAllAgents = dataLoader.readPopulation(allPop);
		MutationReset mutationReset = new MutationReset();
		mutationReset.run(popAllAgents, pop);
		allPop = null;
		popAllAgents= null;
		mutationReset = null;
		System.gc();
		ClonMutator2 mutator2= new ClonMutator2();
		mutator2.mutateClons(pop, Integer.valueOf(config.getParam("TimeAllocationMutator", "mutationRange")));
		mutator2= null;
		*/
		
		//maybe it is not necessary to write the config ¿?
		/*
		Network net = dataLoader.readNetwork(config.network().getInputFile());   //maybe it is not necessary to write the config ¿?
		PopulationWriter popwriter = new PopulationWriter(pop, net);
		File file = new File(config.controler().getOutputDirectory());
		if (!file.exists()){file.mkdir();}
		popwriter.write(config.controler().getOutputDirectory() + "remutatedpop.xml.gz");
		String oldOutDir = config.controler().getOutputDirectory();
		String newOutDir = oldOutDir + "outRouted/";
		config.plans().setInputFile(oldOutDir + "remutatedpop.xml.gz");
		config.controler().setOutputDirectory(newOutDir);
		ConfigWriter configWriter = new ConfigWriter(config);
		String configRemutatedFile = oldOutDir + "configRemutatedPlan.xml";
		configWriter.write(configRemutatedFile);
		file = new File(newOutDir);
		if (!file.exists()){file.mkdir();}
		oldOutDir= null;
		net= null;
		popwriter = null;
		config= null;
		configWriter = null;
		file = null;
		System.gc();
		*/
		
		// route to get a plan
		ScenarioImpl scn = dataLoader.loadScenario(configFile);
		//scn.setPopulation(pop);
		String overDemandConfigFile = new OverDemandPlan_router(scn).run(valuesArray, numHomePlans, numClons);
		scn = new DataLoader().loadScenario(overDemandConfigFile);
		Population pop = scn.getPopulation();
		
		// get as population only persons who travel along the transit Route
		/*
		TransitRoute route = dataLoader.getTransitRoute("B-M44.101.901.H", scn.getTransitSchedule());
		String routedPopFilePath = scn.getConfig().plans().getInputFile();
		PassengerTracker2 passengerTracker2 = new PassengerTracker2();
		passengerTracker2.setNetFile(scn.getConfig().network().getInputFile());
		Population pop = passengerTracker2.run(new String[]{routedPopFilePath}, scn.getTransitSchedule(), scn.getNetwork(), route, route.getStops());
		routedPopFilePath = null;
		*/
		
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//write plan in config-output directory
		File file = new File(scn.getConfig().controler().getOutputDirectory());
		if (!file.exists()){file.mkdir();}
		String modifiedPopFile = scn.getConfig().controler().getOutputDirectory() + "poponlyM44.xml.gz";		
		System.out.println("writing output cloned plan file..." +  modifiedPopFile);
		PopulationWriter popwriter = new PopulationWriter(pop, scn.getNetwork());
		popwriter.write(modifiedPopFile);
		
		//create the new config file
		String oldOutdir= scn.getConfig().controler().getOutputDirectory();
		String newOuputdirPath = scn.getConfig().controler().getOutputDirectory() + "outputCal/";
		scn.getConfig().setParam("plans", "inputPlansFile", modifiedPopFile );
		scn.getConfig().setParam("controler", "outputDirectory", newOuputdirPath);
		ConfigWriter configWriter = new ConfigWriter(scn.getConfig());
		String configClonedFile = oldOutdir + "configOverEstimatedDemandPlans2.xml";
		configWriter.write(configClonedFile);
		
		configFile= null;
		valuesArray = null;
		popwriter= null;
		pop= null;
		//route= null;
		//plansFilter = null;
		oldOutdir = null;
		scn= null;
		configWriter= null;
		newOuputdirPath = null;
		modifiedPopFile = null;
		//passengerTracker2 = null;
		System.gc();
		
		//calibrate
		Z_Launcher.main(new String[]{configClonedFile});
	}
	
}
