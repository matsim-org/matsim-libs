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

import org.matsim.contrib.accidents.computation.AccidentCost30vs50;
import org.matsim.contrib.accidents.computation.AccidentCostComputationBVWP;
import org.matsim.contrib.accidents.computation.AccidentFrequencyComputation;
import org.matsim.contrib.accidents.data.AccidentAreaType;
import org.matsim.contrib.accidents.data.ParkingType;
import org.matsim.contrib.accidents.data.berlin.PlanfreeLinkIDs;
import org.matsim.contrib.accidents.data.berlin.TunnelLinkIDs;

/**
 * @author ikaddoura, mmayobre
 * 
 * 
 */
public class ComputationTest {
		
	@Test
	public void test1() {
		
		ParkingType parkingType = ParkingType.Rarely;
		AccidentAreaType areaType = AccidentAreaType.IndustrialResidentialNeighbourhood;
		double frequency = AccidentFrequencyComputation.computeAccidentFrequency(4000, 13.89, 7, 4, parkingType , areaType );	
		Assert.assertEquals("something is wrong!", 1.03, frequency, 0.01);		
	}
	
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
	public void test3() {
		double actualSpeed = 15.6;
		double costs = AccidentCost30vs50.giveAccidentCostDependingOnActualSpeed(actualSpeed);
		Assert.assertEquals("something is wrong!", 13510., costs, 0.01);		
	}
	
	@Test
	public void test4() {
		double actualSpeed2 = 8.3;
		double costs = AccidentCost30vs50.giveAccidentCostDependingOnActualSpeed(actualSpeed2);
		Assert.assertEquals("something is wrong!", 10425., costs, 0.01);		
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
