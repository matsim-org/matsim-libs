package playground.mmoyo.algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.population.algorithms.PersonAlgorithm;
import org.matsim.population.algorithms.PlansFilterByLegMode;
import org.matsim.population.algorithms.XY2Links;

import playground.mmoyo.algorithms.AgentInPtRouteZoneFind;
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
		NetworkImpl ptNet = dataLoader.readNetwork(ptNetFilePath);
		
		//load oldNet
		NetworkImpl mivNet = dataLoader.readNetwork(mivNetpath);
		
		//create nodeLst
		List<Node> nodeList = new ArrayList<Node>();
		nodeList.add(ptNet.getNodes().get(new IdImpl("812020")));
		nodeList.add(ptNet.getNodes().get(new IdImpl("812550")));
		nodeList.add(ptNet.getNodes().get(new IdImpl("812030")));
		nodeList.add(ptNet.getNodes().get(new IdImpl("812560")));
		nodeList.add(ptNet.getNodes().get(new IdImpl("812570")));
		nodeList.add(ptNet.getNodes().get(new IdImpl("812013")));
		nodeList.add(ptNet.getNodes().get(new IdImpl("806520")));
		nodeList.add(ptNet.getNodes().get(new IdImpl("806030")));
		nodeList.add(ptNet.getNodes().get(new IdImpl("806010")));
		nodeList.add(ptNet.getNodes().get(new IdImpl("806540")));
		nodeList.add(ptNet.getNodes().get(new IdImpl("804070")));
		nodeList.add(ptNet.getNodes().get(new IdImpl("804060")));
		nodeList.add(ptNet.getNodes().get(new IdImpl("801020")));
		nodeList.add(ptNet.getNodes().get(new IdImpl("801030")));
		nodeList.add(ptNet.getNodes().get(new IdImpl("801530")));
		nodeList.add(ptNet.getNodes().get(new IdImpl("801040")));
		nodeList.add(ptNet.getNodes().get(new IdImpl("792050")));
		
		//create empty pop
		Population newPop = dataLoader.createScenario().getPopulation();
		
		//create personAlgorihtms
		AgentInPtRouteZoneFind agentInPtRouteZoneFind = new AgentInPtRouteZoneFind (nodeList, ptNet);
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
