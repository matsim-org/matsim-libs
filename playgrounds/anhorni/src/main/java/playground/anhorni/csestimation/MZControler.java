/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.anhorni.csestimation;

import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.LegImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.anhorni.analysis.microcensus.planbased.MZ2Plans;
import playground.anhorni.analysis.microcensus.planbased.MZActivityImpl;
import playground.anhorni.analysis.microcensus.planbased.MZPerson;
import playground.anhorni.csestimation.biogeme.ChoiceSetWriter;
import playground.anhorni.csestimation.biogeme.ModFileWriter;

public class MZControler {	
	private TreeMap<Id<Location>, ShopLocation> shops;
	private final static Logger log = Logger.getLogger(MZControler.class);	
	private Population estimationPopulation0510 = 
		((ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig())).getPopulation();
		
	public static void main(String[] args) {
		MZControler c = new MZControler();
		String mzIndir = args[0];
		String universalChoiceSetFile = args[1];
		String outdir = args[2];
		String bzFile = args[3];
		c.run(universalChoiceSetFile, mzIndir, outdir, bzFile);
	}
	
	public void run(String universalChoiceSetFile, String mzIndir, String outdir, String bzFile) {
		Population population = 
			((ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig())).getPopulation();
		Population popMZ;
		MZ2Plans mzCreator = new MZ2Plans();
		try {
			popMZ = mzCreator.createMZ2Plans(mzIndir, outdir);
			
			for (Person p : popMZ.getPersons().values()) {
				MZPerson person = (MZPerson)p;
				
				if (person.getWeight() > 0.0 && person.getAge() > 0.0) {				
					population.addPerson(new EstimationPerson(person));	
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		UniversalChoiceSetReader ucsReader = new UniversalChoiceSetReader();
		this.shops = ucsReader.readUniversalCS(universalChoiceSetFile);	
		this.createShoppingTrips(population);
		
		SurveyCleaner cleaner = new SurveyCleaner();
		this.estimationPopulation0510 = cleaner.removeNonAgeNonIncomePersons(this.estimationPopulation0510);
		
		this.analyze(outdir);
		this.write(outdir, this.estimationPopulation0510, bzFile);
		
		ChoiceSetWriter writer = new ChoiceSetWriter(this.shops, this.estimationPopulation0510);
		writer.write(outdir, bzFile);
		
		ModFileWriter modWriter = new ModFileWriter();
		modWriter.writeModFiles(outdir);
		
		log.info("finished .......................................");
	}
		
	private void createShoppingTrips(Population population) {
		QuadTree<Location> shopQuadTree = Utils.buildLocationQuadTree(this.shops); 	// TODO: coord conversion
		
		int sgCnt = 0;
		int sngCnt = 0;
		
		for (Person p:population.getPersons().values()) {			
			EstimationPerson person = (EstimationPerson)p;
			int actlegIndex = -1;
			Plan plan = person.getSelectedPlan();			
			for (PlanElement pe : plan.getPlanElements()) {
				actlegIndex++;
				if (pe instanceof Activity) {
					MZActivityImpl act = (MZActivityImpl)plan.getPlanElements().get(actlegIndex);
					if (act.getType().startsWith("sg")) {
						sgCnt++;
						ShopLocation shop = (ShopLocation) shopQuadTree.getRectangle(act.getCoord().getX(), act.getCoord().getY());
						ShoppingTrip shoppingTrip = new ShoppingTrip();
						shoppingTrip.setShop(shop);
						LegImpl leg = (LegImpl)plan.getPlanElements().get(actlegIndex - 1);
						shoppingTrip.setMode(leg.getMode());
						MZActivityImpl start = (MZActivityImpl)plan.getPlanElements().get(actlegIndex - 2);
						
						shoppingTrip.setStartCoord(start.getCoord());						
						MZActivityImpl end = (MZActivityImpl)plan.getPlanElements().get(actlegIndex + 2);
						shoppingTrip.setEndCoord(end.getCoord());
						
					
						if (Integer.toString(start.getPlz()).startsWith("80") && 
								Integer.toString(end.getPlz()).startsWith("80") && 
								CoordUtils.calcDistance(shop.getCoord(), act.getCoord()) < 200.0) { // close to a Zurich shop
							
							person.addShoppingTrip(shoppingTrip);
							
							if (this.estimationPopulation0510.getPersons().get(person.getId()) == null) {
								this.estimationPopulation0510.addPerson(person);
							}
						}
					}
					else if (act.getType().startsWith("sng")) {
						sngCnt++;
					}
				}
			}			
		}
		log.info("sg cnt: " + sgCnt + " sng cnt: " + sngCnt);
	}
	
	private void analyze(String outdir) {
		log.info("zh population size: " + this.estimationPopulation0510.getPersons().size());
		SurveyAnalyzer analyzer = new SurveyAnalyzer(this.estimationPopulation0510, outdir);
		analyzer.analyzeMZ();	
	}
	
	private void write(String outdir, Population population, String bzFile) {
		Writer writer = new Writer();
		writer.write(population, shops, outdir + "/persons.csv", outdir + "stores.csv", bzFile);
	}	
}
