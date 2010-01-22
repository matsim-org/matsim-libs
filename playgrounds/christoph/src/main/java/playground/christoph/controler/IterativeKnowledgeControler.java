package playground.christoph.controler;

import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.TravelTime;

import playground.christoph.events.EventControler;
import playground.christoph.knowledge.container.MapKnowledgeDB;
import playground.christoph.network.MyLinkFactoryImpl;
import playground.christoph.replanning.MyStrategyManagerConfigLoader;
import playground.christoph.router.KnowledgePlansCalcRoute;
import playground.christoph.router.costcalculators.KnowledgeTravelCostWrapper;
import playground.christoph.router.costcalculators.OnlyTimeDependentTravelCostCalculator;
import playground.christoph.router.util.DijkstraWrapperFactory;
import playground.christoph.scoring.OnlyTimeDependentScoringFunctionFactory;

public class IterativeKnowledgeControler extends Controler{

	private static final Logger log = Logger.getLogger(EventControler.class);

	boolean knowledgeLoaded = false;
	
	/*
	 * Select which size the known Areas should have in the Simulation.
	 * The needed Parameter is the Name of the Table in the MYSQL Database.
	 */
	private static final String tableName = "BatchTable1_5";
	
	public IterativeKnowledgeControler(String[] args) 
	{
		super(args);

		setConstructorParameters();
	}
	
	private void setConstructorParameters()
	{
		// Use MyLinkImpl. They can carry some additional Information like their
		// TravelTime or VehicleCount.
		this.getNetwork().getFactory().setLinkFactory(new MyLinkFactoryImpl());
	}
	
	private void setScoringFunction()
	{
		// Use a Scoring Function, that only scores the travel times!
		this.setScoringFunctionFactory(new OnlyTimeDependentScoringFunctionFactory());
	}
	
	private void setCalculators()
	{
		// Use knowledge when Re-Routing
		OnlyTimeDependentTravelCostCalculator travelCost = new OnlyTimeDependentTravelCostCalculator(this.getTravelTimeCalculator());
		KnowledgeTravelCostWrapper travelCostWrapper = new KnowledgeTravelCostWrapper(travelCost);
		travelCostWrapper.checkNodeKnowledge(true);
		
		this.setTravelCostCalculator(travelCostWrapper);
//.		this.setTravelCostCalculator(travelCost);
	}
	
	private void initialReplanning()
	{
		log.info("Do initial Replanning");
		
		// Use a Wrapper - by doing this, already available MATSim
		// CostCalculators can be used
		TravelTime travelTime = new FreespeedTravelTimeCost();
		OnlyTimeDependentTravelCostCalculator travelCost = new OnlyTimeDependentTravelCostCalculator(this.getTravelTimeCalculator());
		KnowledgeTravelCostWrapper travelCostWrapper = new KnowledgeTravelCostWrapper(travelCost);
		travelCostWrapper.checkNodeKnowledge(true);

//		Dijkstra dijkstra = new Dijkstra(network, travelCostWrapper, travelTime);
//		DijkstraWrapper dijkstraWrapper = new DijkstraWrapper(dijkstra, travelCostWrapper, travelTime, network);	
//		KnowledgePlansCalcRoute dijkstraRouter = new KnowledgePlansCalcRoute(network, dijkstraWrapper, dijkstraWrapper);
		KnowledgePlansCalcRoute dijkstraRouter = new KnowledgePlansCalcRoute(new PlansCalcRouteConfigGroup(), network, travelCostWrapper, travelTime, new DijkstraWrapperFactory());
		
//		FreespeedTravelTimeCost test = new FreespeedTravelTimeCost();
//		KnowledgeTravelCostWrapper travelCostWrapper = new KnowledgeTravelCostWrapper(test);
//		travelCostWrapper.checkNodeKnowledge(false);
//		travelCostWrapper.useLookupTable(false);
//		
//		Dijkstra dijkstra = new Dijkstra(network, travelCostWrapper, test);
//		DijkstraWrapper dijkstraWrapper = new DijkstraWrapper(dijkstra, travelCostWrapper, test, network);
//		KnowledgePlansCalcRoute dijkstraRouter = new KnowledgePlansCalcRoute(network, dijkstraWrapper, dijkstraWrapper);
		
		for (Person person : this.getPopulation().getPersons().values())
		{
			dijkstraRouter.run(person.getSelectedPlan());
		}
	}
	
	private void setKnowledgeStorageHandler() 
	{
		log.info("Loading Knowledge from Database");
		
		for (Person person : this.getPopulation().getPersons().values()) 
		{
			Map<String, Object> customAttributes = person.getCustomAttributes();

			customAttributes.put("NodeKnowledgeStorageType", MapKnowledgeDB.class.getName());

			MapKnowledgeDB mapKnowledgeDB = new MapKnowledgeDB();
			mapKnowledgeDB.setPerson(person);
			mapKnowledgeDB.setNetwork(network);
			mapKnowledgeDB.setTableName(tableName);
			
			mapKnowledgeDB.readFromDB();

			customAttributes.put("NodeKnowledge", mapKnowledgeDB);
		}
	}
	
	@Override
	protected void setUp() {
		super.setUp();
		
		setScoringFunction();
		setCalculators();
	}
	
	@Override
	protected StrategyManager loadStrategyManager()
	{
		StrategyManager manager = new StrategyManager();
		MyStrategyManagerConfigLoader.load(this, this.config, manager);
		return manager;
	}
	
	@Override
	protected void runMobSim()
	{
		if (!knowledgeLoaded)
		{
			setKnowledgeStorageHandler();
//			initialReplanning();
			knowledgeLoaded = true;
		}
		
		super.runMobSim();
	}
	
	public static void main(final String[] args) 
	{
		if ((args == null) || (args.length == 0)) 
		{
			System.out.println("No argument given!");
			System.out.println("Usage: Controler config-file [dtd-file]");
			System.out.println();
		} 
		else 
		{
			final IterativeKnowledgeControler controler = new IterativeKnowledgeControler(args);
			controler.setOverwriteFiles(true);
			controler.run();
		}
		System.exit(0);
	}
}
