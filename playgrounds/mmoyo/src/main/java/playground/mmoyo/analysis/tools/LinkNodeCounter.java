package playground.mmoyo.analysis.tools;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;

import playground.mmoyo.utils.TransScenarioLoader;

/**counts transit and mivs links-nodes of a multimodal-network**/
public class LinkNodeCounter {

	public static void main(String[] args) {
		String conf = "../playgrounds/mmoyo/output/20/config_20plans.xml";
		ScenarioImpl scenario  = new TransScenarioLoader().loadScenario(conf);
		
		String strMiv = "miv";
		int intMiv = 0;
		int other =0;
		
		for (Link link : scenario.getNetwork().getLinks().values()){
			if (link.getId().toString().startsWith(strMiv)){
				intMiv++;
			}else{
				other++;
			}	
		}
		int total = intMiv + other;
		System.out.println("links Miv: " + intMiv);
		System.out.println("links other: " + other);	
		System.out.println("links total: " + total);
		System.out.println("number of links:" + scenario.getNetwork().getLinks().size());
		
		
		int intNodesMiv=0;
		int intNodesOther=0;
		for (Node node : scenario.getNetwork().getNodes().values()){
			if (node.getId().toString().startsWith(strMiv)){
				intNodesMiv++;
			}else{
				intNodesOther++;
			}	
		}
		
		int totalNodes = intNodesMiv + intNodesOther;
		System.out.println("\n nodes Miv: " + intNodesMiv);
		System.out.println("nodes other: " + intNodesOther);	
		System.out.println("total: " + totalNodes);
		System.out.println("number of nodes:" + scenario.getNetwork().getNodes().size());

	}

}
