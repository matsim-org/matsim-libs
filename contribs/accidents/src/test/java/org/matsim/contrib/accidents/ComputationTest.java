package org.matsim.contrib.accidents;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;

import org.matsim.contrib.accidents.computation.AccidentCostComputationBVWP;
import org.matsim.contrib.accidents.runExample.PlanfreeLinkIDs;
import org.matsim.contrib.accidents.runExample.TunnelLinkIDs;
import org.matsim.contrib.accidents.data.AccidentLinkInfo;;

/**
 * @author ikaddoura, mmayobre
 * 
 * 
 */
public class ComputationTest {
		
	@Test
	public void test2() {
		//TODO modify this test to be consistent with the new form to compute costs (hugo)
		//example linkID: 52761861_259225122
		Network network = NetworkUtils.createNetwork();
		NetworkFactory factory = network.getFactory();
		
		Node n0 = factory.createNode(Id.createNodeId(0), new Coord(0, 1));
		network.addNode(n0);
		Node n1 = factory.createNode(Id.createNodeId(1), new Coord(1, 0));
		network.addNode(n1);
		
		Link link1 = factory.createLink(Id.createLinkId(0), n0, n1);
		link1.setLength(15871.5137629840428417082875967025757);
		AccidentLinkInfo info = new AccidentLinkInfo(link1.getId()); 
		
		ArrayList<Integer> list = new ArrayList<>();
		list.add(0, 0); //Planfrei
		list.add(1, 0); //KFZ, außerhalb
		list.add(2, 2); //2 Lanes
		
		info.setRoadTypeBVWP(list);
		
		
		double costs = AccidentCostComputationBVWP.computeAccidentCosts(4820, link1, list);
		Assert.assertEquals("something is wrong!", 1772.13863066011, costs, 0.01);
	}
	
	@Test
	public void test5() {
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new AccidentsModule(scenario));
		
		AccidentsConfigGroup accidentSettings = (AccidentsConfigGroup) scenario.getConfig().getModules().get(AccidentsConfigGroup.GROUP_NAME);
		accidentSettings.setTunnelLinksArray(TunnelLinkIDs.getTunnelLinkIDs());
		String tunnelLinkProbe = "267428802_21508808";
		boolean linkIsTunnel;
		if (Arrays.asList(accidentSettings.getTunnelLinksArray()).contains(tunnelLinkProbe)){
			linkIsTunnel = true;
		} else linkIsTunnel = false;
		assertEquals(true, linkIsTunnel);
	}
	
	@Test
	public void test6() {		
		String planfreeLinkProbe = "36562081_30113567";
		boolean linkIsPlanfree;
		String[] planfreeLinkIDs = PlanfreeLinkIDs.getPlanfreeLinkIDs();
		if (Arrays.asList(planfreeLinkIDs).contains(planfreeLinkProbe)){
			linkIsPlanfree = true;
		} else linkIsPlanfree = false;
		assertEquals(true, linkIsPlanfree);
	}
}
