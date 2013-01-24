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
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.transformations.WGS84toCH1903LV03;

import playground.anhorni.analysis.microcensus.planbased.MZ2Plans;
import playground.anhorni.analysis.microcensus.planbased.MZActivityImpl;
import playground.anhorni.analysis.microcensus.planbased.MZPerson;

public class MZControler {	
	private TreeMap<Id, ShopLocation> shops;
	private final static Logger log = Logger.getLogger(MZControler.class);	
	private Population estimationPopulation0510 = 
		((ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig())).getPopulation();
	
	private WGS84toCH1903LV03 trafo = new WGS84toCH1903LV03();
	
	public static void main(String[] args) {
		MZControler c = new MZControler();
		String mzIndir = args[0];
		String universalChoiceSetFile = args[1];
		String outdir = args[2];
		c.run(universalChoiceSetFile, mzIndir, outdir);
	}
	
	public void run(String universalChoiceSetFile, String mzIndir, String outdir) {
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
		this.write(outdir, this.estimationPopulation0510);
		
		log.info("finished .......................................");
	}
		
	private void createShoppingTrips(Population population) {
		QuadTree<Location> shopQuadTree = Utils.buildLocationQuadTree(this.shops); 	// TODO: coord conversion
		
		for (Person p:population.getPersons().values()) {			
			EstimationPerson person = (EstimationPerson)p;
			int actlegIndex = -1;
			Plan plan = person.getSelectedPlan();
			for (PlanElement pe : plan.getPlanElements()) {
				actlegIndex++;
				if (pe instanceof Activity) {
					MZActivityImpl act = (MZActivityImpl)plan.getPlanElements().get(actlegIndex);
					if (act.getType().startsWith("sg")) {
						ShopLocation shop = (ShopLocation) shopQuadTree.get(act.getCoord().getX(), act.getCoord().getY());	
						ShoppingTrip shoppingTrip = new ShoppingTrip();
						shoppingTrip.setShop(shop);
						MZActivityImpl start = (MZActivityImpl)plan.getPlanElements().get(actlegIndex - 2);
						
						shoppingTrip.setStart(start.getCoord());						
						MZActivityImpl end = (MZActivityImpl)plan.getPlanElements().get(actlegIndex + 2);
						shoppingTrip.setEnd(end.getCoord());
						
						Coord sCoord = this.trafo.transform(shop.getCoord());
						Coord aCoord = this.trafo.transform(act.getCoord());						
						if (Integer.toString(start.getPlz()).startsWith("80") && 
								Integer.toString(end.getPlz()).startsWith("80") && 
								CoordUtils.calcDistance(sCoord, aCoord) < 200.0) {
							
							person.addShoppingTrip(shoppingTrip);
							
							if (this.estimationPopulation0510.getPersons().get(person.getId()) == null) {
								this.estimationPopulation0510.addPerson(person);
							}
						}
					}
				}
			}			
		}
	}
	
	private void analyze(String outdir) {
		log.info("zh population size: " + this.estimationPopulation0510.getPersons().size());
		SurveyAnalyzer analyzer = new SurveyAnalyzer(this.estimationPopulation0510, outdir);
		analyzer.analyzeMZ();	
	}
	
	private void write(String outdir, Population population) {
		Writer writer = new Writer();
		writer.write(population, shops, outdir + "/persons.csv", outdir + "stores.csv");
	}	
}
