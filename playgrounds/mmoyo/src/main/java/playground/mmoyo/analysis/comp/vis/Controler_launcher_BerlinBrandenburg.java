package playground.mmoyo.analysis.comp.vis;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NodeImpl;

import playground.mmoyo.analysis.TransScenarioLoader;
import playground.mzilske.bvg09.TransitControler;

public class Controler_launcher_BerlinBrandenburg {
	
	public static void main(String[] args) {
		String conf;
		
		//configs for fragmented plans
		
		//parameterized
		conf = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/BerlinBrandenburg/routed_1x_subset_xy2links_ptplansonly/fragmented/config/config_routedPlans_MoyoParameterized.xml";
		
		//time
		//conf = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/BerlinBrandenburg/routed_1x_subset_xy2links_ptplansonly/fragmented/config/config_routedPlans_MoyoTime.xml";
		
		//marginalUtility
		//conf = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/BerlinBrandenburg/routed_1x_subset_xy2links_ptplansonly/fragmented/config/config_routedPlans.xml";

		//repeated plans 200 agents
		//conf = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/BerlinBrandenburg/routed_1x_subset_xy2links_ptplansonly/repeated/config/config_repeatedPlans200.xml";
		//TransitControler.main(new String []{conf});

		//repeated plans 20 agents
		//conf = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/BerlinBrandenburg/routed_1x_subset_xy2links_ptplansonly/repeated/config/config_repeatedPlans20.xml";
		//TransitControler.main(new String []{conf});
	
		//conf = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/BerlinBrandenburg/routed_1x_subset_xy2links_ptplansonly/fragmented/detoured/configDetouredPopulation.xml";
		//TransitControler.main(new String []{conf});
		
		
		conf = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/BerlinBrandenburg/routed_1x_subset_xy2links_ptplansonly/no_fragmented/config/config_routedPlans_MoyoParameterized.xml";
		ScenarioImpl scenario  = new TransScenarioLoader().loadScenario(conf);
		NetworkImpl net = scenario.getNetwork();
		
		String coma = ",";
		String strMiv = "miv";
		int intMiv = 0;
		int other =0;
		
		for (Link link : net.getLinks().values()){
			if (link.getId().toString().startsWith(strMiv)){
				intMiv++;
			}else{
				other++;
			}	
		}
		
		int total = intMiv + other;
		
		System.out.println("intMiv: " + intMiv);
		System.out.println("other: " + other);	
		System.out.println("total: " + total);
		System.out.println("number of links:" + net.getLinks().size());
		
		
		
		int intNodesMiv=0;
		int intNodesOther=0;
		
		for (Node node : net.getNodes().values()){
			if (node.getId().toString().startsWith(strMiv)){
				intNodesMiv++;
			}else{
				intNodesOther++;
			}	
		}
		
		int totalNodes = intNodesMiv + intNodesOther;
		
		System.out.println("");
		System.out.println("intNodesMiv: " + intNodesMiv);
		System.out.println("intNodesOther: " + intNodesOther);	
		System.out.println("total: " + totalNodes);
		System.out.println("number of links:" + net.getNodes().size());
		
		
		/*
		System.out.println("");
		
		for (NodeImpl node : net.getNodes().values()){
			System.out.print(coma + node.getId());
		}
		
		System.out.println("");
		System.out.println("persons :" + scenario.getPopulation().getPersons().size() );	
		*/
	}
}
