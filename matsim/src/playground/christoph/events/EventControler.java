/* *********************************************************************** *
 * project: org.matsim.*
 * EventControler.java
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

package playground.christoph.events;


import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;

import org.matsim.controler.Controler;
import org.matsim.population.Person;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.router.Dijkstra;
import org.matsim.router.PlansCalcRouteLandmarks;
import org.matsim.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.router.costcalculators.TravelTimeDistanceCostCalculator;
import org.matsim.router.util.PreProcessLandmarks;
import org.matsim.utils.geometry.transformations.AtlantisToWGS84;
import org.matsim.utils.geometry.transformations.CH1903LV03toWGS84;
import org.matsim.utils.geometry.transformations.GK4toWGS84;
import org.matsim.utils.geometry.transformations.WGS84toCH1903LV03;

import playground.christoph.events.algorithms.ParallelInitialReplanner;
import playground.christoph.events.algorithms.ParallelReplanner;
import playground.christoph.knowledge.nodeselection.FileNameCreator;
import playground.christoph.knowledge.KMLPersonWriter;
import playground.christoph.knowledge.nodeselection.ParallelCreateKnownNodesMap;
import playground.christoph.knowledge.nodeselection.SelectNodes;
import playground.christoph.knowledge.nodeselection.SelectNodesCircular;
import playground.christoph.knowledge.nodeselection.SelectNodesDijkstra;
import playground.christoph.knowledge.nodeselection.SelectionReaderMatsim;
import playground.christoph.knowledge.nodeselection.SelectionWriter;
import playground.christoph.mobsim.MyQueueNetwork;
import playground.christoph.mobsim.ReplanningQueueSimulation;
import playground.christoph.router.CompassRoute;
import playground.christoph.router.DijkstraWrapper;
import playground.christoph.router.KnowledgePlansCalcRoute;
import playground.christoph.router.RandomCompassRoute;
import playground.christoph.router.RandomRoute;
import playground.christoph.router.TabuRoute;
import playground.christoph.router.costcalculators.KnowledgeTravelCostCalculator;
import playground.christoph.router.costcalculators.KnowledgeTravelTimeCalculator;


/**
 * The Controler is responsible for complete simulation runs, including
 * the initialization of all required data, running the iterations and
 * the replanning, analyses, etc.
 *
 * @author Christoph Dobler
 */
//mysimulations/census2000_dilZh30km_miv_transitincl_10pct/config.xml
//mysimulations/berlin/config.xml
/* Example for the new entries in a config.xml file to read / write selected nodes from / to files.
<module name="selection">
	<param name="readSelection" value="true"/>
	<param name="inputSelectionFile" value="mysimulations/berlin/selection.xml.gz" />
	<param name="writeSelection" value="true"/>
	<param name="outputSelectionFile" value="./output/berlin/selection.xml.gz" />
	<param name="dtdFile" value="./src/playground/christoph/knowledge/nodeselection/Selection.dtd" />
</module>
*/
public class EventControler extends Controler{

	protected ReplanningQueueSimulation sim;
//	protected TravelTimeDistanceCostCalculator travelCostCalculator;
//	protected KnowledgeTravelTimeCalculator travelTimeCalculator;
	protected ArrayList<PlanAlgorithm> replanners;
	protected ArrayList<SelectNodes> nodeSelectors;
	
	private static final Logger log = Logger.getLogger(EventControler.class);
	

	/**
	 * Initializes a new instance of Controler with the given arguments.
	 *
	 * @param args The arguments to initialize the controler with. <code>args[0]</code> is expected to
	 * 		contain the path to a configuration file, <code>args[1]</code>, if set, is expected to contain
	 * 		the path to a local copy of the DTD file used in the configuration file.
	 */
	public EventControler(String[] args)
	{
		super(args);

		/*
		 * Implement EndActReplanning
		 * If the current Activity of a Persons ends, the route to the next Activity.
		 * At the moment replanning when a link is left can't be handled via events -
		 * there is no event that could be used for that purpose (NO, a LinkLeaveEvent
		 * can NOT be used). Due to this fact replanning when leaving a links is
		 * implemented in the MyQueueNode Class.
		 */
//		events.addHandler(new ActEndReplanner());
	}
	
	/*
	 * New Routers for the Replanning are used instead of using the controler's. 
	 * By doing this every person can use a personalised Router.
	 */
	protected void initReplanningRouter()
	{
		replanners = new ArrayList<PlanAlgorithm>();
		
		KnowledgeTravelTimeCalculator travelTimeCalculator = new KnowledgeTravelTimeCalculator(sim.getQueueNetwork());
		TravelTimeDistanceCostCalculator travelCostCalculator = new TravelTimeDistanceCostCalculator(travelTimeCalculator);
	
		// Dijkstra
		//replanners.add(new PlansCalcRouteDijkstra(network, travelCostCalculator, travelTimeCalculator));

		//AStarLandmarks
		PreProcessLandmarks landmarks = new PreProcessLandmarks(new FreespeedTravelTimeCost());
		landmarks.run(network);
		replanners.add(new PlansCalcRouteLandmarks(network, landmarks, travelCostCalculator, travelTimeCalculator));
		
		// BasicReplanners (Random, Tabu, Compass, ...)
		// each replanner can handle an arbitrary number of persons
		replanners.add(new KnowledgePlansCalcRoute(new RandomRoute(), new RandomRoute()));
		replanners.add(new KnowledgePlansCalcRoute(new TabuRoute(), new TabuRoute()));
		replanners.add(new KnowledgePlansCalcRoute(new CompassRoute(), new CompassRoute()));
		replanners.add(new KnowledgePlansCalcRoute(new RandomCompassRoute(), new RandomCompassRoute()));
		
		// Dijkstra for Replanning
		KnowledgeTravelTimeCalculator travelTime = new KnowledgeTravelTimeCalculator();
		KnowledgeTravelCostCalculator travelCost = new KnowledgeTravelCostCalculator(travelTime);
		Dijkstra dijkstra = new Dijkstra(network, travelCost, travelTime);
		DijkstraWrapper dijkstraWrapper = new DijkstraWrapper(dijkstra, travelCost, travelTime);
		KnowledgePlansCalcRoute dijkstraRouter = new KnowledgePlansCalcRoute(dijkstraWrapper, dijkstraWrapper);
		dijkstraRouter.setQueueNetwork(sim.getQueueNetwork());
		replanners.add(dijkstraRouter);
		
	}
	
	public ArrayList<PlanAlgorithm> getReplanningRouters()
	{
		return replanners;
	}

	/*
	 * Hands over the ArrayList to the ParallelReplanner
	 */
	protected void initParallelReplanningModules()
	{
		ParallelReplanner.init(replanners);
		ParallelReplanner.setNumberOfThreads(2);

	/*
		ParallelLeaveLinkReplanner.init(replanners);
		ParallelLeaveLinkReplanner.setNumberOfThreads(2);
		ParallelActEndReplanner.init(replanners);
		ParallelActEndReplanner.setNumberOfThreads(2);
		
		// more Modules to come...
	 */
	
	}
	
	/*
	 * Initializes the NodeSeletors that are used to create the Activity Spaces of the
	 * Persons of a Population.
	 */
	protected void initNodeSelectors()
	{
		nodeSelectors = new ArrayList<SelectNodes>();
		
		nodeSelectors.add(new SelectNodesCircular(this.network));
		
		SelectNodesDijkstra selectNodesDijkstra = new SelectNodesDijkstra(this.network);
		selectNodesDijkstra.setCostFactor(1.3);
		nodeSelectors.add(selectNodesDijkstra);
	}

	@Override
	protected void runMobSim() 
	{			
		sim = new ReplanningQueueSimulation(this.network, this.population, this.events);
		
		sim.setControler(this);
		
		// CostCalculator entsprechend setzen!
//		setCostCalculator();

		log.info("Read known Nodes Maps from a File");
		readKnownNodesMap();
		
		log.info("Initialize Replanning Routers");
		initReplanningRouter();
		
		log.info("Initialize Parallel Replanning Modules");
		initParallelReplanningModules();
		
		log.info("Set Replanning flags");
		setReplanningFlags();
		
		log.info("Set Replanners for each Person");
		setReplanners();
		
		log.info("Initialize Node Selectors");
		initNodeSelectors();
		
		log.info("Set Node Selectors");
		setNodeSelectors();
		
//		log.info("Create known Nodes Maps");
//		createKnownNodes();

//		log.info("Write known Nodes Maps to a File");
//		writeKownNodesMap();
		
		/* 
		 * Could be done before or after the creation of the activity rooms -
		 * depending on the intention of the simulation.
		 * 
		 * If done before, the new created Route is the base for the activity rooms.
		 * 
		 * If done afterwards, existing routes are the base for the activity rooms and
		 * the replanners have to act within the borders of the already defined rooms.
		 * The existing plans could for example be the results of a relaxed solution of
		 * a standard MATSim simulation.
		 */
		log.info("do initial Replanning");
		doInitialReplanning();
		
		sim.run();
	}
	

	/* Add three boolean variables to each Person.
	 * They are used to indicate, if the plans of this person should be
	 * replanned each time if an activity ends, each time a link is left,
	 * before the simulation starts or never during an iteration.
	 * 
	 * I don't like this way but, it is the best way I know at the moment...
	 * In my opinion these variables should be part of the PersonAgents within
	 * the QueueSimulation - but they can't be accessed from an EventHandler.
	 */
	protected void setReplanningFlags()
	{
		int counter = 0;
		
		Iterator<Person> PersonIterator = this.getPopulation().iterator();
		while (PersonIterator.hasNext())
		{		
			Person p = PersonIterator.next();
			
			counter++;
			if(counter < 100000)
			{
				Map<String,Object> customAttributes = p.getCustomAttributes();
				customAttributes.put("initialReplanning", new Boolean(false));
				customAttributes.put("leaveLinkReplanning", new Boolean(false));
				customAttributes.put("endActivityReplanning", new Boolean(true));
				
				// (de)activate replanning
				MyQueueNetwork.doLeaveLinkReplanning(false);
				MyQueueNetwork.doActEndReplanning(true);
			}
			else
			{
				Map<String,Object> customAttributes = p.getCustomAttributes();
				customAttributes.put("initialReplanning", new Boolean(false));
				customAttributes.put("leaveLinkReplanning", new Boolean(false));
				customAttributes.put("endActivityReplanning", new Boolean(false));
				 
				// deactivate replanning
				MyQueueNetwork.doLeaveLinkReplanning(false);
				MyQueueNetwork.doActEndReplanning(false);
			}
		}
	}

	/*
	 * Assigns a replanner to every Person of the population.
	 * Same problem as above: should be part of the PersonAgents, but only
	 * Persons are available in the replanning modules.
	 * 
	 * At the moment: Replanning Modules are assigned hard coded.
	 * Later: Modules are assigned based on probabilities from config files. 
	 */
	protected void setReplanners()
	{
		Iterator<Person> PersonIterator = this.getPopulation().iterator();
		while (PersonIterator.hasNext())
		{
			Person p = PersonIterator.next();
		
			Map<String,Object> customAttributes = p.getCustomAttributes();
			customAttributes.put("Replanner", replanners.get(5));
		}
	}
	
	/*
	 * Assigns nodeSelectors to every Person of the population, which are
	 * used to create an activity rooms for every Person. It is possible to
	 * assign more than one Selector to each Person.
	 * If non is selected the Person knows every Node of the network.
	 *
	 * At the moment: Selection Modules are assigned hard coded.
	 * Later: Modules are assigned based on probabilities from config files. 
	 */
	protected void setNodeSelectors()
	{
		int counter = 0;
		
		Iterator<Person> PersonIterator = this.getPopulation().iterator();
		while (PersonIterator.hasNext())
		{			
			Person p = PersonIterator.next();
		
			Map<String,Object> customAttributes = p.getCustomAttributes();
			
			ArrayList<SelectNodes> personNodeSelectors = new ArrayList<SelectNodes>();
			
			if (counter++ < 100000) personNodeSelectors.add(nodeSelectors.get(1));
			if (counter++ < 50) personNodeSelectors.add(nodeSelectors.get(1));
			
			customAttributes.put("NodeSelectors", personNodeSelectors);
		}
	}
	
	/*
	 * Read Maps of Nodes that each Agents "knows" from a file that is specified in config.xml.
	 */
	protected void readKnownNodesMap()
	{
		// reading Selection from file
		boolean readSelection = Boolean.valueOf(this.config.getModule("selection").getValue("readSelection"));
		if (readSelection)
		{
			String path = this.config.getModule("selection").getValue("inputSelectionFile");
			log.info("Path: " + path);
			//new SelectionReaderMatsim(this.network, this.population).readFile(path);
			//new SelectionReaderMatsim(this.network, this.population).readMultiFile(path);
			
/*	
			FileNameCreator fileNameCreator = new FileNameCreator();
			fileNameCreator.setBaseFileName(path);
				
			String nextFileName = fileNameCreator.getNextFileName();

			SelectionReaderMatsim test = new SelectionReaderMatsim(this.network, this.population);
			
			while (new File(nextFileName).exists())
			{
				log.info(nextFileName);
				test.readFile(nextFileName);
				nextFileName = fileNameCreator.getNextFileName();
			}
			test = null;
*/			
			
			//reading multiple Files automatically
			new SelectionReaderMatsim(this.network, this.population).readMultiFile(path);
			log.info("Read input selection file!");
		}
	}
	
	/*
	 * Write Maps of Nodes that each Agents "knows" to a file that is specified in config.xml.
	 */
	protected void writeKownNodesMap()
	{
		// writing Selection to file
		boolean writeSelection = Boolean.valueOf(this.config.getModule("selection").getValue("writeSelection"));
		if (writeSelection)
		{
			String outPutFile = this.config.getModule("selection").getValue("outputSelectionFile");
			String dtdFile = "./src/playground/christoph/knowledge/nodeselection/Selection.dtd";
			
			//new SelectionWriter(this.population, outPutFile, dtdFile , "1.0", "dummy").write();
			new SelectionWriter(this.population, outPutFile, dtdFile , "1.0", "dummy").write(5000);
			//new SelectionWriter(this.population, outPutFile, dtdFile , "1.0", "dummy").write(2500);
			
			//new SelectionWriter(this.population, outPutFile, dtdFile, "1.0", "dummy").write();
			new SelectionWriter(this.population, outPutFile, dtdFile, "1.0", "dummy").write(5000);
			//new SelectionWriter(this.population, getOutputFilename("selection.xml.gz"), "1.0", "dummy").write();	
						
			log.info("Path: " + outPutFile);
		}
	}
	
	/*
	 * Creates the Maps of Nodes that each Agents "knows".
	 */
	protected void createKnownNodes()
	{
		// use only one of these two - they should produce the same results... 
		ParallelCreateKnownNodesMap.run(this.population, nodeSelectors, 1);
		
		writePersonKML(this.population.getPerson("100139"));
		
		// non multi-core calculation
		//CreateKnownNodesMap.collectAllSelectedNodes(this.population);

	}	// setNodes()

	
	protected void doInitialReplanning()
	{
		ArrayList<Person> personsToReplan = new ArrayList<Person>();
		
		for (Person person : this.getPopulation().getPersons().values())
		{
			boolean replanning = (Boolean)person.getCustomAttributes().get("initialReplanning");
			
			if (replanning)
			{
				personsToReplan.add(person);
			}
		}
		
		double time = 0.0;
		ParallelInitialReplanner.run(personsToReplan, time);

/*
		for (Person person : this.getPopulation().getPersons().values())
		{			
			boolean replanning = (Boolean)person.getCustomAttributes().get("initialReplanning");
			
			if (replanning)
			{
				KnowledgePlansCalcRoute replanner = (KnowledgePlansCalcRoute)replanners.get(1);
				replanner.setPerson(person);
				replanner.run(person.getSelectedPlan());
			}
		}
*/	
	} //doInitialReplanning
	
	
	protected void writePersonKML(Person person)
	{
		KMLPersonWriter test = new KMLPersonWriter(network, person);
		
		// set CoordinateTransformation
		String coordSys = this.config.global().getCoordinateSystem();
		if(coordSys.equalsIgnoreCase("GK4")) test.setCoordinateTransformation(new GK4toWGS84());
		if(coordSys.equalsIgnoreCase("Atlantis")) test.setCoordinateTransformation(new AtlantisToWGS84());
		if(coordSys.equalsIgnoreCase("CH1903_LV03")) test.setCoordinateTransformation(new CH1903LV03toWGS84());
		
		// waiting for an implementation...
		//new WGS84toCH1903LV03();
		
		String outputDirectory = this.config.controler().getOutputDirectory();
		test.setOutputDirectory(outputDirectory);
		
		test.writeFile();
	}
	
	/* ===================================================================
	 * main
	 * =================================================================== */

	public static void main(final String[] args) {
		if ((args == null) || (args.length == 0)) {
			System.out.println("No argument given!");
			System.out.println("Usage: Controler config-file [dtd-file]");
			System.out.println();
		} else {
			final EventControler controler = new EventControler(args);			
			controler.setOverwriteFiles(true);
			controler.run();
		}
		System.exit(0);
	}

	
}
