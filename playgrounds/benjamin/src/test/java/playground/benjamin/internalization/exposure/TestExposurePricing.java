/* *********************************************************************** *
 * project: org.matsim.*
 * RunEmissionToolOnline.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package playground.benjamin.internalization.exposure;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.testcases.MatsimTestUtils;

import playground.benjamin.internalization.EquilTestSetUp;
import playground.benjamin.scenarios.munich.exposure.EmissionResponsibilityCostModule;
import playground.benjamin.scenarios.munich.exposure.EmissionResponsibilityTravelDisutilityCalculatorFactory;
import playground.benjamin.scenarios.munich.exposure.GridTools;
import playground.benjamin.scenarios.munich.exposure.InternalizeEmissionResponsibilityControlerListener;
import playground.benjamin.scenarios.munich.exposure.ResponsibilityGridTools;


/**
 * @author julia, benjamin, amit
 *
 */
@RunWith(Parameterized.class)
public class TestExposurePricing {

	private String inputPath = "../../../../repos/shared-svn/projects/detailedEval/emissions/testScenario/input/";

	private String emissionInputPath = "../../../../repos/shared-svn/projects/detailedEval/emissions/hbefaForMatsim/";
	private String roadTypeMappingFile = emissionInputPath + "roadTypeMapping.txt";
	private String emissionVehicleFile = inputPath + "emissionVehicles_1pct.xml.gz";

	private String averageFleetWarmEmissionFactorsFile = emissionInputPath + "EFA_HOT_vehcat_2005average.txt";
	private String averageFleetColdEmissionFactorsFile = emissionInputPath + "EFA_ColdStart_vehcat_2005average.txt";

	private boolean isUsingDetailedEmissionCalculation = true;
	private String detailedWarmEmissionFactorsFile = emissionInputPath + "EFA_HOT_SubSegm_2005detailed.txt";
	private String detailedColdEmissionFactorsFile = emissionInputPath + "EFA_ColdStart_SubSegm_2005detailed.txt";

	private Logger logger = Logger.getLogger(TestExposurePricing.class);

	private Double xMin = 0.0;

	private Double xMax = 20000.;

	private Double yMin = 0.0;

	private Double yMax = 12500.;

	private Integer noOfXCells = 32;

	private Integer noOfYCells = 20;

	private int numberOfIterations = 21;

	@Rule
	public MatsimTestUtils helper = new MatsimTestUtils(); 
	
	private DecimalFormat df = new DecimalFormat("#.###");
	
	private boolean isConsideringCO2Costs;

	private int noOfTimeBins;

	public TestExposurePricing(boolean isConsideringCO2Costs, int noOfTimeBins) {
		this.isConsideringCO2Costs = isConsideringCO2Costs;
		this.noOfTimeBins = noOfTimeBins;
	}

	@Parameters
	public static Collection<Object[]> considerCO2 () {
		Object[][] object = new Object [][] { 
				{true, 24},
				{false, 24},
				{true, 1},
				{false, 1}
				};
		return Arrays.asList(object);
	}

	@Test
	public void priceExposure() {
		EquilTestSetUp equilTestSetUp = new EquilTestSetUp();
		Scenario sc = equilTestSetUp.createConfig();
		// TODO : I have used link speed as 100/3.6 m/s instead of 100 m/s thus check the difference in the result
		equilTestSetUp.createNetwork(sc);
		equilTestSetUp.createActiveAgents(sc);
		equilTestSetUp.createPassiveAgents(sc);

		Config config = sc.getConfig();
		EmissionsConfigGroup ecg = new EmissionsConfigGroup() ;
		ecg.setEmissionRoadTypeMappingFile(roadTypeMappingFile);
		ecg.setEmissionVehicleFile(emissionVehicleFile);

		ecg.setAverageWarmEmissionFactorsFile(averageFleetWarmEmissionFactorsFile);
		ecg.setAverageColdEmissionFactorsFile(averageFleetColdEmissionFactorsFile);

		ecg.setUsingDetailedEmissionCalculation(isUsingDetailedEmissionCalculation);
		ecg.setDetailedWarmEmissionFactorsFile(detailedWarmEmissionFactorsFile);
		ecg.setDetailedColdEmissionFactorsFile(detailedColdEmissionFactorsFile);

		config.addModule(ecg);

		config.controler().setLastIteration(numberOfIterations);

		Controler controler = new Controler(sc);
		String outputDirectory = helper.getOutputDirectory() + "/exposureToll/" + (isConsideringCO2Costs ? "considerCO2Costs/" : "notConsiderCO2Costs/");
		sc.getConfig().controler().setOutputDirectory(outputDirectory);

		EmissionModule emissionModule = new EmissionModule(sc);
		emissionModule.setEmissionEfficiencyFactor( 1.0 );
		emissionModule.createLookupTables();
		emissionModule.createEmissionHandler();

		GridTools gt = new GridTools(sc.getNetwork().getLinks(), xMin, xMax, yMin, yMax);
		Map<Id<Link>, Integer> links2xCells = gt.mapLinks2Xcells(noOfXCells);
		Map<Id<Link>, Integer> links2yCells = gt.mapLinks2Ycells(noOfYCells);

		Double timeBinSize = new Double (controler.getScenario().getConfig().qsim().getEndTime() / this.noOfTimeBins );
		
		ResponsibilityGridTools rgt = new ResponsibilityGridTools(timeBinSize, noOfTimeBins, links2xCells, links2yCells, noOfXCells, noOfYCells);
		EmissionResponsibilityCostModule emissionCostModule = new EmissionResponsibilityCostModule( 1.0, isConsideringCO2Costs, rgt, links2xCells, links2yCells);
		final EmissionResponsibilityTravelDisutilityCalculatorFactory emfac = new EmissionResponsibilityTravelDisutilityCalculatorFactory(emissionModule, emissionCostModule);

		controler.addOverridingModule(new AbstractModule() {

			@Override
			public void install() {
				bindCarTravelDisutilityFactory().toInstance(emfac);
			}
		});

		controler.addControlerListener(new InternalizeEmissionResponsibilityControlerListener(emissionModule, emissionCostModule, rgt, links2xCells, links2yCells));

		//		EmissionControlerListener ecl = new EmissionControlerListener(controler);
		//		controler.addControlerListener(ecl);
		//		controler.setScoringFunctionFactory(new ResponsibilityScoringFunctionFactory(config, controler.getNetwork(), ecl));
		//controler.setTravelDisutilityFactory(new ResDisFactory(ecl, ecl.emissionModule, new EmissionCostModule(1.0)));
		
		MyPersonMoneyEventHandler personMoneyEventHandler = new MyPersonMoneyEventHandler();
		controler.getEvents().addHandler(personMoneyEventHandler);
		
		controler.run();
		// checks
		Person activeAgent = sc.getPopulation().getPersons().get(Id.create("567417.1#12424", Person.class));
		Plan selectedPlan = activeAgent.getSelectedPlan();

		// check with the first leg
		LinkNetworkRouteImpl route = (LinkNetworkRouteImpl) ( (Leg) selectedPlan.getPlanElements().get(1) ).getRoute() ;
		// Agent should take longer route to avoid exposure toll
		Assert.assertTrue("Wrong route is selected. Agent should have used route with link 38 (longer) instead.", route.getLinkIds().contains(Id.create("38", Link.class)));
		//TODO : not sure if exposure toll forces active agent to avoid route with link 29 after 0th iteration.

		// I don't think, following is necessary. amit Oct'15
		Double scoreOfSelectedPlan;

		// check selected plan 
		scoreOfSelectedPlan = selectedPlan.getScore();
		for(PlanElement pe: selectedPlan.getPlanElements()){
			if(pe instanceof Leg){
				Leg leg = (Leg)pe;
				LinkNetworkRouteImpl lnri = (LinkNetworkRouteImpl) leg.getRoute();
				if(lnri.getLinkIds().contains(Id.create("39", Link.class))){
					logger.info("Selected route should not use link 39."); //System.out.println("39 contained");
				}else{
					if(lnri.getLinkIds().contains(Id.create("38", Link.class))){
						logger.info("Selected route avoids node 9 as it is supposed to. " +
								"It's score is " + selectedPlan.getScore());
					}
				}
			}
		}

		// check not selected plans - score should be worse if link 39 is used

		for(Plan p: activeAgent.getPlans()){			
			for(PlanElement pe: p.getPlanElements()){
				if(pe instanceof Leg){
					Leg leg = (Leg)pe;
					LinkNetworkRouteImpl lnri = (LinkNetworkRouteImpl) leg.getRoute();
					if(lnri.getLinkIds().contains(Id.create("39", Link.class))){
						logger.info("This plan uses node 9 and has score " + p.getScore()+ ". Selected = " + p.isSelected());
					}
					if(lnri.getLinkIds().contains(Id.create("38", Link.class))){
						logger.info("This plan uses node 8 and has score " + p.getScore() + ". Selected = " + p.isSelected());
					}
				}
			}

		}

		// check links
		//		for(Id linkId: scenario.getNetwork().getLinks().keySet()){
		//			logger.info("link id " + linkId.toString() + " cell " + links2xCells.get(linkId) + " , " + links2yCells.get(linkId));
		//		}
		//		logger.info("epsilon"+epsilon);

		
		//**************************************************************//
		
		if (noOfTimeBins != 1) return; //TODO : yet to get the manual calculation for this.
		
		/*
		 * Manual calculation
		 * 20 passive agents and 1 active agent will be exposed due to emission of 1 active agent on the link 39.
		 * All passive agent perform home activity for 86399 whereas active agent perform 6*3600+86400-51148 (=56852) home activity 
		 * and 51000-22107 (=28893) work activity. 	Total cells = 32*20 = 640.
		 * Emission costs (only warm emission since cold emission exists only upto 2 km on departure link) on link 39 = 0.027613043 
		 * without considering co2 costs (see previous emissionTollTest().)
		 * cell of Link 39 = 13,14; Thus only cells with 9 < x < 17 and 10 < y < 18 matters to get the dispersion weight.
		 * 15,13; 15,12; 15,11; 14,13; 14,12; 14,11; 13,13; 13,12; 13,11; 12,13; 12,12; 12,11; 11,13; 11,12; 11,11; 
		 * noOfCells with zero distance (13,14;) =0; noOfCells with 1 distance (13,13;) =1;
		 * noOfCells with 2 distance (13,12; 12,13; 14,13 ) =3; noOfCells with 3 distance (13,11; 12,12; 11,13; 14,12; 15,13) =5;
		 * avg Exposed duration = (20*86399+(56852+28893))/640 = 2833.945313; 
		 * Thus total relavant duration = (0.132+3*0.029+5*0.002)*86398 = 19785.142
		 * Thus total toll in iteration 1 on link 39 = 19785.142 * 0.027613043 / 2699.9375 = 0.19278
		 */

		if (! isConsideringCO2Costs ) {

			if(personMoneyEventHandler.link39LeaveTime == Double.POSITIVE_INFINITY) {
				throw new RuntimeException("Active agent does not pass through link 39.");
			}

			for (PersonMoneyEvent e : personMoneyEventHandler.events){
				if (e.getTime() == personMoneyEventHandler.link39LeaveTime) {
					//					TODO fix this test. Current value shows -0.18909
					Assert.assertEquals( "Exposure toll on link 39 from Manual calculation does not match from money event.", df.format( -0.19278 ), df.format( e.getAmount() ) );
				}
			}
		} else {
			// TODO: this is completly wrong. Include flat CO2 costs not distributive costs.
		}
		//**************************************************************//		
	}
	class MyPersonMoneyEventHandler implements PersonMoneyEventHandler, LinkLeaveEventHandler  {

		List<PersonMoneyEvent> events = new ArrayList<PersonMoneyEvent>();
		double link39LeaveTime = Double.POSITIVE_INFINITY;

		@Override
		public void reset(int iteration) {
			events.clear();
		}

		@Override
		public void handleEvent(PersonMoneyEvent event) {
			events.add(event);
		}

		@Override
		public void handleEvent(LinkLeaveEvent event) {
			if(event.getLinkId().toString().equals("39")) link39LeaveTime = event.getTime();
		}
	}
}