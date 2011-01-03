/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.anhorni.scenarios;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.facilities.FacilitiesReaderMatsimV1;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;

import playground.anhorni.scenarios.analysis.PlansAnalyzer;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Vector;


public class Create3TownsDemand {

	private final static Logger log = Logger.getLogger(Create3TownsDemand.class);
	private NetworkImpl network=null;
	private ScenarioImpl scenarioWriteOut = new ScenarioImpl();
	private PopulationImpl staticPopulation = new PopulationImpl(scenarioWriteOut);
	private static int offset = 100000;
	private static int numberOfHomeTowns = 2;
	private static int shopCityLinkIds[] = {21, 23, 25, 27, 29, 31, 33, 35, 37, 39};
	private static String outputFolder="src/main/java/playground/anhorni/scenarios/3towns/input/";
	
	private static String path = "src/main/java/playground/anhorni/scenarios/3towns/";
	
	private int populationSize = -1;
	private int numberOfCityShoppingLocs = -1;
	private double shopShare = 0.0;	
	private int numberOfPersonStrata = -1;	// excluding destination choice
	//expenditure for home towns
	private double [] mu = {	0.0, 	0.0};
	private double [] sigma = {	0.0,	0.0};
				
	private ExpenditureAssigner expenditureAssigner = null;	
	private AgeAssigner ageAssigner = null;
	private SinglePlanGenerator singlePlanGenerator = null;
	ConfigReader configReader = new ConfigReader();
		
	public static void main(final String[] args) {
		String networkfilePath="src/main/java/playground/anhorni/scenarios/3towns/input/network.xml";
		String facilitiesfilePath="src/main/java/playground/anhorni/scenarios/3towns/input/facilities.xml";

		Create3TownsDemand plansCreator=new Create3TownsDemand();
		plansCreator.init(networkfilePath, facilitiesfilePath);
		
		plansCreator.run();			
		log.info("Creation finished -----------------------------------------");
	}
	
	private void init(final String networkfilePath,
			final String facilitiesfilePath) {
		configReader.read();
		this.populationSize = configReader.getPopulationSize();
		this.numberOfCityShoppingLocs = configReader.getNumberOfCityShoppingLocs();
		this.shopShare = configReader.getShopShare();
		this.numberOfPersonStrata = configReader.getNumberOfPersonStrata();
		this.mu = configReader.getMu();
		this.sigma = configReader.getSigma();
		
		this.expenditureAssigner = new ExpenditureAssigner(this.numberOfPersonStrata, this.mu, this.sigma, path);
		this.ageAssigner = new AgeAssigner(this.populationSize, Create3TownsDemand.numberOfHomeTowns);
		this.singlePlanGenerator = new SinglePlanGenerator(Create3TownsDemand.shopCityLinkIds);

		new MatsimNetworkReader(scenarioWriteOut).readFile(networkfilePath);		
		new FacilitiesReaderMatsimV1(scenarioWriteOut).readFile(facilitiesfilePath);
	}

	private void run() {	
		Random rnd = new Random(109876L);
		PlansAnalyzer analyzer = new PlansAnalyzer(path, this.numberOfCityShoppingLocs, Create3TownsDemand.shopCityLinkIds);
		this.generatePopulation();
		
		List<Integer> keyList = new Vector<Integer>();
		for (Id id : this.staticPopulation.getPersons().keySet()) {
			keyList.add(Integer.parseInt(id.toString()));
		}
		Collections.shuffle(keyList, rnd);
		
		this.generateStratifiedPlans(keyList);
		this.write("plans_stratified.xml");
		analyzer.run((PopulationImpl)scenarioWriteOut.getPopulation(), "strat");
		new CreateNetworks().create(scenarioWriteOut.getPopulation().getPersons().size(), false);
		scenarioWriteOut.getPopulation().getPersons().clear();
		
		this.generateSuperPlans(keyList);
		this.write("plans_superposition.xml");
		analyzer.run((PopulationImpl)scenarioWriteOut.getPopulation(), "super");
		new CreateNetworks().create(scenarioWriteOut.getPopulation().getPersons().size(), true);
		scenarioWriteOut.getPopulation().getPersons().clear();
		
		for (int i = 0; i < configReader.getNumberOfRandomRuns(); i++) {		
			Collections.shuffle(keyList, rnd);			
			this.generateRandomPlans(keyList);
			this.write(i + "_plans_random.xml");
			analyzer.run((PopulationImpl)scenarioWriteOut.getPopulation(), "random_" + i);
			scenarioWriteOut.getPopulation().getPersons().clear();
		}
		expenditureAssigner.finalize();
		ageAssigner.finalize();
		analyzer.finalize();	
	}
	
	private void generatePopulation() {		
		for (int i = 0; i < populationSize; i++) {
			PersonImpl p = new PersonImpl(new IdImpl(offset + i));
			
			int townId = 0;
			if (i >= (populationSize / numberOfHomeTowns)) {
				townId = 1;
			}
			p.getCustomAttributes().put("townId", townId);
			ageAssigner.assignAge(p);
			if (this.sigma[0] > 0.000001) {
				expenditureAssigner.assignExpenditureGaussian(p);
			}
			else {
				expenditureAssigner.assignExpenditureFixed(p);	
			}
			this.staticPopulation.addPerson(p);
		}
	}
	
	private void generateSuperPlans(List<Integer> keyList) {			
		int cnt = 0;
		for (Integer id : keyList) {			
			PersonImpl p = (PersonImpl) this.staticPopulation.getPersons().get(new IdImpl(id));
							
			for (int i = 0; i < (int)(1 / this.shopShare); i++) {
				boolean cityShopping = false;
				if (i == 0) {
					cityShopping = true;
				}
				for (int locIndex = 0; locIndex < this.numberOfCityShoppingLocs; locIndex++) {
					// copy person -------
					PersonImpl pTmp = new PersonImpl(new IdImpl(cnt));
					pTmp.createDesires(p.getDesires().getDesc());
					pTmp.setAge(p.getAge());
					pTmp.setEmployed(true);
					// copy person -------
					PlanImpl plan = singlePlanGenerator.generatePlan((Integer)p.getCustomAttributes().get("townId"), cityShopping, locIndex);				
					pTmp.addPlan(plan);
					scenarioWriteOut.getPopulation().addPerson(pTmp);
					cnt++;
				}
			}
		}
	}
		
	private void generateRandomPlans(List<Integer> keyList) {		
		int shopCityCnt = (int)(populationSize * shopShare);
		
		ShopLocationAssigner shopLocationAssigner = new ShopLocationAssigner((int)(populationSize * this.shopShare), numberOfCityShoppingLocs);
		
		int cnt = 0;
		for (Integer id : keyList) {						
			PersonImpl p = (PersonImpl)this.staticPopulation.getPersons().get(new IdImpl(id));
			
			// copy person -------
			PersonImpl pTmp = new PersonImpl(new IdImpl(offset + cnt));
			pTmp.createDesires(p.getDesires().getDesc());
			pTmp.setAge(p.getAge());
			pTmp.setEmployed(true);
			// copy person -------
							
			boolean cityShopping = false;
			if (shopCityCnt > 0.0) {
				cityShopping = true;
				shopCityCnt--;				
			}
			//int shopIndex = shopLocationAssigner.getLocationId();
			int shopIndex = shopLocationAssigner.getRandomLocationId();
			PlanImpl plan = singlePlanGenerator.generatePlan((Integer)p.getCustomAttributes().get("townId"), cityShopping, shopIndex);
			pTmp.addPlan(plan);
			scenarioWriteOut.getPopulation().addPerson(pTmp);
			cnt++;
		}	
	}

	private void generateStratifiedPlans(List<Integer> keyList) {	
		int strataSize = (int)(populationSize /numberOfPersonStrata);
		
		ShopLocationAssigner shopLocationAssigner = new ShopLocationAssigner((int)(populationSize * this.shopShare), numberOfCityShoppingLocs);
		
		//stratum 	0: home town 0	age < 5
		//			1: home town 0	age >= 5
		//			2: home town 1	age < 5
		//			3: home town 1	age >= 5
		int shopCityPerStratum [] = new int [numberOfPersonStrata];
		
		for (int i = 0; i < this.numberOfPersonStrata; i++) {
			shopCityPerStratum[i] = (int) (this.populationSize / this.numberOfPersonStrata * this.shopShare);
		}
	
		int cnt = 0;
		for (Integer id : keyList) {			
			int stratumIndex = (int) Math.floor(id / strataSize);
					
			PersonImpl p = (PersonImpl) this.staticPopulation.getPersons().get(new IdImpl(id));
			if ((Integer)p.getCustomAttributes().get("townId") == 0 && p.getAge() < 5) {stratumIndex = 0;}
			else if ((Integer)p.getCustomAttributes().get("townId") == 0 && p.getAge() >= 5) {stratumIndex = 1;}
			else if ((Integer)p.getCustomAttributes().get("townId") == 1 && p.getAge() < 5) {stratumIndex = 2;}
			else if ((Integer)p.getCustomAttributes().get("townId") == 1 && p.getAge() >= 5) {stratumIndex = 3;}
			
			
			boolean cityShopping = false;
			int shopIndex = -1;
			
			if (shopCityPerStratum[stratumIndex] > 0.0) {
				cityShopping = true;
				shopCityPerStratum[stratumIndex]--;
				//shopIndex = shopLocationAssigner.getLocationId();
				shopIndex = shopLocationAssigner.getRandomLocationId();
			}
			
			PlanImpl plan = singlePlanGenerator.generatePlan((Integer)p.getCustomAttributes().get("townId"), cityShopping, shopIndex);
			// copy person -------
			PersonImpl pTmp = new PersonImpl(new IdImpl(offset + cnt));
			pTmp.createDesires(p.getDesires().getDesc());
			pTmp.setAge(p.getAge());
			pTmp.setEmployed(true);
			// copy person -------
			
			pTmp.addPlan(plan);
			scenarioWriteOut.getPopulation().addPerson(pTmp);
			cnt++;
		}
	}
			
	private void write(String filename) {
		new PopulationWriter(scenarioWriteOut.getPopulation(), this.network).write(Create3TownsDemand.outputFolder + "plans/" + filename);
	}
}
