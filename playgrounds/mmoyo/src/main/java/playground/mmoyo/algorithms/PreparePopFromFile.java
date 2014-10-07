package playground.mmoyo.algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.population.algorithms.PersonAlgorithm;
import org.matsim.population.algorithms.PlansFilterByLegMode;
import org.matsim.population.algorithms.XY2Links;

import playground.mmoyo.io.PopSecReader;
import playground.mmoyo.utils.DataLoader;

public class PreparePopFromFile implements PersonAlgorithm{

	private PtLegDetector ptLegDetector = new PtLegDetector();
	private XY2Links xy2Links;
	private PopulationCleaner populationCleaner = new PopulationCleaner();
	private Population newPop;
	private AgentInPtRouteZoneFind agentInPtRouteZoneFind;
	
	public PreparePopFromFile (Population newPop, AgentInPtRouteZoneFind agentInPtRouteZoneFind, XY2Links xy2Links){
		this.agentInPtRouteZoneFind = agentInPtRouteZoneFind;
		this.newPop = newPop;
		this.xy2Links = xy2Links;
	}
	
	@Override
	public void run(Person person) {
		//clean 
		this.populationCleaner.run(person);

		//ignore persons outside route area
		if (!this.agentInPtRouteZoneFind.judge(person)){
			return;
		}

		//remove link and facility
		for(Plan plan: person.getPlans()){
			for(PlanElement pe : plan.getPlanElements()){
				if (pe instanceof Activity) {
					ActivityImpl act = (ActivityImpl)pe;
					act.setLinkId(null);
					act.setFacilityId(null);
				}
			}
		}
		
		//apply xy2links with old network
		xy2Links.run(person);
		
		//ignore plans without pt legs
		//if (!this.ptLegDetector.judge(person)){
		//	return;
		//}
		//

		//add to final population
		this.newPop.addPerson(person);
	}
	
	public static void main(String[] args) {
		String popFilePath;
		String ptNetFilePath;
		String mivNetpath;
		String newNetPath;
		
		if (args.length>0){
			popFilePath = args[0];
			ptNetFilePath = args[1];
			mivNetpath = args[2];
			newNetPath = args[3];
		}else{
			popFilePath = "../../input/newDemand/bvg.run190.25pct.100.plans.xml";
			ptNetFilePath = "../../input/newDemand/network.final.xml.gz";   // "../../input/newDemand/multimodalNet.xml.gz";
			mivNetpath = "../../berlin-bvg09/pt/nullfall_berlin_brandenburg/input/network_multimodal.xml.gz";
			newNetPath = ""; 
		}

		//load scn with net 
		DataLoader dataLoader = new DataLoader ();
		ScenarioImpl scn = (ScenarioImpl) dataLoader.createScenario();
		MatsimNetworkReader matsimNetReader = new MatsimNetworkReader(scn);
		matsimNetReader.readFile(newNetPath);  
		
		//load ptNet
		Network ptNet = dataLoader.readNetwork(ptNetFilePath);
		
		//load oldNet
		Network mivNet = dataLoader.readNetwork(mivNetpath);
		
		//create nodeLst
		List<Node> nodeList = new ArrayList<Node>();
		nodeList.add(ptNet.getNodes().get(Id.create("812020", Node.class)));
		nodeList.add(ptNet.getNodes().get(Id.create("812550", Node.class)));
		nodeList.add(ptNet.getNodes().get(Id.create("812030", Node.class)));
		nodeList.add(ptNet.getNodes().get(Id.create("812560", Node.class)));
		nodeList.add(ptNet.getNodes().get(Id.create("812570", Node.class)));
		nodeList.add(ptNet.getNodes().get(Id.create("812013", Node.class)));
		nodeList.add(ptNet.getNodes().get(Id.create("806520", Node.class)));
		nodeList.add(ptNet.getNodes().get(Id.create("806030", Node.class)));
		nodeList.add(ptNet.getNodes().get(Id.create("806010", Node.class)));
		nodeList.add(ptNet.getNodes().get(Id.create("806540", Node.class)));
		nodeList.add(ptNet.getNodes().get(Id.create("804070", Node.class)));
		nodeList.add(ptNet.getNodes().get(Id.create("804060", Node.class)));
		nodeList.add(ptNet.getNodes().get(Id.create("801020", Node.class)));
		nodeList.add(ptNet.getNodes().get(Id.create("801030", Node.class)));
		nodeList.add(ptNet.getNodes().get(Id.create("801530", Node.class)));
		nodeList.add(ptNet.getNodes().get(Id.create("801040", Node.class)));
		nodeList.add(ptNet.getNodes().get(Id.create("792050", Node.class)));
		
		//create empty pop
		Population newPop = dataLoader.createScenario().getPopulation();
		
		//create personAlgorihtms
		AgentInPtRouteZoneFind agentInPtRouteZoneFind = new AgentInPtRouteZoneFind (nodeList, (NetworkImpl) ptNet);
		XY2Links xy2Links = new XY2Links (mivNet);
		
		//run preparation
		PreparePopFromFile preparePopFromFile = new PreparePopFromFile(newPop, agentInPtRouteZoneFind, xy2Links);
		PopSecReader popSecReader = new PopSecReader (scn, preparePopFromFile);
		popSecReader.readFile(popFilePath);

		//car Leg filter
		PlansFilterByLegMode plansFilterByLegMode = new PlansFilterByLegMode( TransportMode.pt, PlansFilterByLegMode.FilterType.keepPlansWithOnlyThisMode);
		plansFilterByLegMode.run(newPop);
		
		//write file
		System.out.println("writing output plan file...");
		PopulationWriter popwriter = new PopulationWriter(newPop, scn.getNetwork());
		popwriter.write(new File(popFilePath).getParent() + "/preparedPopulation.xml.gz") ;
		System.out.println("done");
	}


}
