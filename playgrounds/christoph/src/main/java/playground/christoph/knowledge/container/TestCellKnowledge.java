package playground.christoph.knowledge.container;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;

import playground.christoph.knowledge.container.dbtools.DBConnectionTool;
import playground.christoph.knowledge.nodeselection.SelectNodesDijkstra;
import playground.christoph.router.costcalculators.OnlyTimeDependentTravelCostCalculator;

public class TestCellKnowledge {

	private final static Logger log = Logger.getLogger(TestCellKnowledge.class);
	
	private NetworkLayer network;
	private PopulationImpl population;
	private Config config;
	private Person person;
	private SelectNodesDijkstra selectNodesDijkstra;
	private Map<Id, Node> nodesMap;
	private CellKnowledge cellKnowledge;
	private CellNetworkMapping cellNetworkMapping;
	private CellKnowledgeCreator createCellKnowledge;

	private final String configFileName = "mysimulations/kt-zurich/config.xml";
	private final String dtdFileName = null;
	private final String networkFile = "mysimulations/kt-zurich/input/network.xml";
	private final String populationFile = "mysimulations/kt-zurich/input/plans.xml";
	//private final String networkFile = "D:/Master_Thesis_HLI/Workspace/TestNetz/network.xml";
	//private final String networkFile = "D:/Master_Thesis_HLI/Workspace/myMATSIM/mysimulations/kt-zurich/networks/ivtch-zh-cut/network.xml";
	
	
	public static void main(String[] args)
	{
		new TestCellKnowledge();
	}
	
	public TestCellKnowledge()
	{	
		DBConnectionTool dbct = new DBConnectionTool();
		dbct.connect();		 
		
		loadNetwork();
		loadPopulation();
		loadConfig();
				
		log.info("Network size: " + network.getLinks().size());
		log.info("Population size: " + population.getPersons().size());
		
		//Person person = population.getPersons().values().iterator().next();
		person = population.getPersons().get(new IdImpl(100000));
		log.info("Person: " + person);
		log.info("ID: " + person.getId());
		
		initNodeSelector();
		createKnownNodes();
		log.info("Found known Nodes: " + nodesMap.size());
		
		cellNetworkMapping = new CellNetworkMapping(network);
		cellNetworkMapping.createMapping();
		
		CreateCellKnowledge();
		
		log.info("included Nodes in CellKnowledge: " + cellKnowledge.getKnownNodes().size());
		cellKnowledge.findFullCells();
		
/*		
		long memUsage;
		memUsage = calculateMemoryUsage(cellKnowledge);
		System.out.println("CellKnowledge took " + memUsage + " bytes of memory");
		
		memUsage = calculateMemoryUsage(nodesMap);
		System.out.println("NodesMap took " + memUsage + " bytes of memory");
*/
	}
	
	private void CreateCellKnowledge()
	{
		createCellKnowledge = new CellKnowledgeCreator(cellNetworkMapping);
		cellKnowledge = createCellKnowledge.createCellKnowledge(nodesMap);
	}
	
	private void createKnownNodes()
	{
		Plan plan = person.getSelectedPlan();
		
		nodesMap = new TreeMap<Id, Node>();
		
		// get all acts of the selected plan
		ArrayList<ActivityImpl> acts = new ArrayList<ActivityImpl>();					
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof ActivityImpl) {
				acts.add((ActivityImpl) pe);
			}
		}
		
		for(int j = 1; j < acts.size(); j++)
		{						
			Node startNode = acts.get(j-1).getLink().getToNode();
			Node endNode = acts.get(j).getLink().getFromNode();
				
			selectNodesDijkstra.setStartNode(startNode);
			selectNodesDijkstra.setEndNode(endNode);

			selectNodesDijkstra.addNodesToMap(nodesMap);
		}
	}
	
	private void initNodeSelector()
	{
		selectNodesDijkstra = new SelectNodesDijkstra((NetworkLayer)this.network);
		selectNodesDijkstra.setCostCalculator(new OnlyTimeDependentTravelCostCalculator(null));
		selectNodesDijkstra.setCostFactor(20.0);
	}
	
	private void loadNetwork()
	{
		network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(networkFile);
		log.info("Loading Network ... done");
	}
	
	private void loadPopulation()
	{
		population = new PopulationImpl();
		new MatsimPopulationReader(population, network).readFile(populationFile);
		log.info("Loading Population ... done");
	}
	
	private void loadConfig()
	{
		this.config = new Config();
		this.config.addCoreModules();
		this.config.checkConsistency();
		try {
			new MatsimConfigReader(this.config).readFile(this.configFileName, this.dtdFileName);
		} catch (IOException e) {
			log.error("Problem loading the configuration file from " + this.configFileName);
			throw new RuntimeException(e);
		}
		Gbl.setConfig(config);
		log.info("Loading Config ... done");
	}
/*	
	private long calculateMemoryUsage(Object object) 
	{
	    System.gc(); System.gc(); System.gc(); System.gc();
		System.gc(); System.gc(); System.gc(); System.gc();
		System.gc(); System.gc(); System.gc(); System.gc();
		System.gc(); System.gc(); System.gc(); System.gc();
		
		long mem1 = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
	    
		object = null;

	    System.gc(); System.gc(); System.gc(); System.gc();
		System.gc(); System.gc(); System.gc(); System.gc();
		System.gc(); System.gc(); System.gc(); System.gc();
		System.gc(); System.gc(); System.gc(); System.gc();
		
		long mem0 = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		
		return mem1 - mem0;
	 }
*/	
}
