package playground.mmoyo.analysis.tools;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;

import playground.mmoyo.utils.DataLoader;

/**counts transit and mivs links-nodes of a multimodal-network**/
public class LinkNodeCounter {

	public static void main(String[] args) {
		String conf = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/calibration/100plans_bestValues_config.xml";
		ScenarioImpl scenario  = new DataLoader().loadScenario(conf);
		
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
