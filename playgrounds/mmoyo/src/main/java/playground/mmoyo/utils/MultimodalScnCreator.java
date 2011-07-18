package playground.mmoyo.utils;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.population.ActivityImpl;
//import playground.mrieser.pt.utils.MergeNetworks;

import playground.mmoyo.algorithms.PopulationCleaner;

public class MultimodalScnCreator {
	NetworkImpl mivNet;
	NetworkImpl ptNet;
	Population pop;
	final static String MIV = "miv_";
	
	public MultimodalScnCreator (final NetworkImpl mivNet, final NetworkImpl ptNet, final Population pop){
		this.mivNet = mivNet;
		this.ptNet = ptNet;
		this.pop= pop;
	}
	
	private void createMultimodalNet(final String outDir){
		NetworkImpl multiModalNet = (NetworkImpl) new DataLoader().createScenario().getNetwork();
		//MergeNetworks.merge(this.mivNet, MIV, this.ptNet, null, multiModalNet);
		
		//add MIVS nodes and links
		for (Node node : this.mivNet.getNodes().values()){
			Id newId = new IdImpl(MIV + node.getId());
			multiModalNet.createAndAddNode(newId, node.getCoord());
		}
		
		for (Link l : this.mivNet.getLinks().values()){
			Id newId = new IdImpl(MIV + l.getId());
			Id fromNodeId = new IdImpl(MIV + l.getFromNode().getId()); 
			Node fromNode = multiModalNet.getNodes().get(fromNodeId); 
			Id toNodeId = new IdImpl(MIV + l.getToNode().getId()); 
			Node toNode = multiModalNet.getNodes().get(toNodeId);
			multiModalNet.createAndAddLink(newId, fromNode, toNode, l.getLength(), l.getFreespeed(), l.getCapacity(), l.getNumberOfLanes(), ((LinkImpl)l).getOrigId(), ((LinkImpl)l).getType());
			LinkImpl newLink = (LinkImpl) multiModalNet.getLinks().get(newId);
			newLink.setAllowedModes(l.getAllowedModes());
		}
		new NetworkWriter(multiModalNet).write(outDir + "mivNetWithSuffix.xml.gz");
		System.out.println("done writting:" + outDir + "mivNetWithSuffix.xml.gz");		

		/*
		//add pt nodes and links
		for (Node node : this.ptNet.getNodes().values()){
			multiModalNet.createAndAddNode(node.getId(), node.getCoord());
		}
		
		for (Link l : this.ptNet.getLinks().values()){
			Node fromNode = multiModalNet.getNodes().get(l.getFromNode().getId()); 
			Node toNode = multiModalNet.getNodes().get(l.getToNode().getId());
			multiModalNet.createAndAddLink(l.getId(), fromNode, toNode, l.getLength(), l.getFreespeed(), l.getCapacity(), l.getNumberOfLanes(), ((LinkImpl)l).getOrigId(), ((LinkImpl)l).getType());
			LinkImpl newLink = (LinkImpl) multiModalNet.getLinks().get(l.getId());
			newLink.setAllowedModes(l.getAllowedModes());
		}
			
		new NetworkWriter(multiModalNet).write(outDir + "multimodalNet.xml.gz");
		System.out.println("done writting:" + outDir + "multimodalNet.xml.gz");
		*/				
	}
	
	private void preparePop(Population pop){
		new PopulationCleaner().run(pop);   //<-- caution!
		/*
		for (Person person: pop.getPersons().values()){
			for (Plan plan : person.getPlans()){
				for (PlanElement pe : plan.getPlanElements()){
					if ((pe instanceof Activity)) {
						ActivityImpl act = (ActivityImpl)pe;
						act.setLinkId(linkId)
					}
				}
			}
		}*/
	}
	
	
	
	public static void main(String[] args) {
		String mivNetPath;
		String ptNetPath;
		String popfilePath;
		
		if (args.length>0){
			mivNetPath = args[0];
			ptNetPath = args[1];
			popfilePath = args[2];
		}else{
			mivNetPath = "../../input/newDemand/network.final.xml.gz";
			ptNetPath = "../../berlin-bvg09/pt/nullfall_berlin_brandenburg/input/pt_network.xml.gz";
			popfilePath = "../../input/newDemand/bvg.run190.25pct.100.plans.xml";
		}

		DataLoader dLoader = new DataLoader();
		NetworkImpl mivNet = dLoader.readNetwork(mivNetPath);
		NetworkImpl ptNet = dLoader.readNetwork(ptNetPath);
		//Population pop = dLoader.readPopulation(popfilePath);
		
		new MultimodalScnCreator(mivNet, ptNet, null).createMultimodalNet("../../input/newDemand/");
	}
}
