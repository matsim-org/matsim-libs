package playground.mmoyo.algorithms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.population.filters.AbstractPersonFilter;

import playground.mmoyo.utils.DataLoader;

public class AgentInPtRouteZoneFind extends AbstractPersonFilter {
	private NetworkImpl ptNet;
	private List <Node> nodeList;
	
	public AgentInPtRouteZoneFind(List <Node>  nodeList, NetworkImpl ptNet){
		this.nodeList = nodeList;
		this.ptNet = ptNet;
	}
	
	@Override
	public boolean judge(final Person person) {
		boolean found = false;
		Activity act;
		Activity lastAct= null;
		int i=0;
		do{
			PlanElement pe = person.getSelectedPlan().getPlanElements().get(i++);
			if (pe instanceof Activity) {
				act = (Activity)pe; 
				if (lastAct!=null){

					Coord center = CoordUtils.getCenter(lastAct.getCoord(), act.getCoord());
					double radius = CoordUtils.calcDistance(center, act.getCoord());
					Collection<Node> stopsinBetween = this.ptNet.getNearestNodes(center, radius + 2000);
					int j= 0;
					do {
						Node node = this.nodeList.get(j++);
						found = stopsinBetween.contains(node) || found;
					} while (j< this.nodeList.size() && found == false);
				}
				lastAct = act; 
			}
		}while(i< person.getSelectedPlan().getPlanElements().size() && found == false);
		return found;
	}

	public static void main(String[] args) {
		String netFilePath;
		String popFilePath;
		
		if (args.length>0){
			popFilePath = args[0];
			netFilePath = args[1];
			
		}else{
			popFilePath = "../../input/newDemand/bvg.run190.25pct.100.plans.xml";
			netFilePath = "../../input/newDemand/network.final.xml.gz";   // "../../input/newDemand/multimodalNet.xml.gz";
		}

		DataLoader dataLoader = new DataLoader ();
		ScenarioImpl scn = (ScenarioImpl) dataLoader.createScenario();
		MatsimNetworkReader matsimNetReader = new MatsimNetworkReader(scn);
		matsimNetReader.readFile(netFilePath);  
		
		List<Node> nodeList = new ArrayList<Node>();
		nodeList.add(scn.getNetwork().getNodes().get(new IdImpl("10000000")));
		nodeList.add(scn.getNetwork().getNodes().get(new IdImpl("10000001")));
		nodeList.add(scn.getNetwork().getNodes().get(new IdImpl("8400060")));
		nodeList.add(scn.getNetwork().getNodes().get(new IdImpl("8338")));
		nodeList.add(scn.getNetwork().getNodes().get(new IdImpl("7782")));
		
		//AgentInPtRouteZoneFind agentInPtRouteZoneFind = new AgentInPtRouteZoneFind(nodeList, private );
		//PopSecReader popSecReader = new PopSecReader (scn, agentInPtRouteZoneFind);
		//popSecReader.readFile(popFilePath);
	}
	
}
