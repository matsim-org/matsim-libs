package org.matsim.contrib.accidents;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import org.matsim.contrib.accidents.computation.AccidentCostComputationBVWP;
import org.matsim.contrib.accidents.runExample.PlanfreeLinkIDs;
import org.matsim.contrib.accidents.runExample.TunnelLinkIDs;

/**
 * @author ikaddoura, mmayobre
 * 
 * 
 */
public class ComputationTest {
		
	@Test
	public void test2() {
		//example linkID: 52761861_259225122	
		ArrayList<Integer> list = new ArrayList<>();
		list.add(0, 0); //Planfrei
		list.add(1, 0); //KFZ, außerhalb
		list.add(2, 2); //2 Lanes
		double costs = AccidentCostComputationBVWP.computeAccidentCosts(4820, 15871.5137629840428417082875967025757, list);
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
