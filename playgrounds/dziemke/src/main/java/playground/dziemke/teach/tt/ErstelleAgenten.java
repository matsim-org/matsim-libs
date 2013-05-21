package playground.dziemke.teach.tt;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

public class ErstelleAgenten {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		
		//weil wir noch keine Strategien zum Umplanen definiert haben, koennen wir keine weiteren Iterationen durchlaufen lassen
		config.controler().setLastIteration(0);
		
		// neu
		config.controler().setOutputDirectory("D:/Workspace/container/potsdam-tut/output");
		
		//erzeuge Aktivitaetentypen, damit diese in Plan bekannt sind und Nutzenfunktion zugeordnet werden kann
		ActivityParams home = new ActivityParams("home");
		home.setTypicalDuration(16*60*60); //in sekunden -> 16h (Rest des Tages neben work)
		config.planCalcScore().addActivityParams(home);
		ActivityParams work = new ActivityParams("work");
		work.setTypicalDuration(8*60*60); //in sekunden -> 8h
		config.planCalcScore().addActivityParams(work);
		
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		//lese Netzwerk ein
		new MatsimNetworkReader(scenario).readFile("D:/Workspace/container/potsdam-tut/brandenburg-potsdam-merged.xml");
		
		//erzeuge Population mit Person und Plan
		Population population = fillScenario(scenario);
		
		//erzeuge controler
		Controler controler = new Controler(scenario);
		controler.setOverwriteFiles(true);
		controler.run();
		
//		//population in xml schreiben (tut controler.run() auch)
//		new PopulationWriter(population, scenario.getNetwork()).write("D:/Workspace/container/potsdam-tut/population.xml");
	}

	private static Population fillScenario(Scenario scenario) {
		Population population = scenario.getPopulation();
		
		Person person = population.getFactory().createPerson(scenario.createId("1"));
		
		Plan plan = population.getFactory().createPlan();
		//bisher noch nichts im plan enthalten, au�er dass er selected ist
		
		Coord homeCoordinate = scenario.createCoord(1449842, 6854385);
		Activity homeActivity = population.getFactory().createActivityFromCoord("home", homeCoordinate); //activity type und coord
		homeActivity.setEndTime(9*60*60); //in sekunden -> 9 Uhr
		plan.addActivity(homeActivity);
		
		Leg hinweg = population.getFactory().createLeg("car");
		plan.addLeg(hinweg);
		
		Coord workCoordinate = scenario.createCoord(1457842, 6839385);
		Activity workActivity = population.getFactory().createActivityFromCoord("work", workCoordinate); //activity type und coord
		workActivity.setEndTime(17*60*60); //in sekunden -> 17 Uhr
		plan.addActivity(workActivity);
		
		Leg rueckweg = population.getFactory().createLeg("car");
		plan.addLeg(rueckweg);
		
		//koennen nicht nochmal homeActivity hinzufuegen/nehmen, weil diese schon die endzeit 9 uhr hat
		//aber diese muss gleiche Coordinaten haben wie erste homeActivity (sonst warning)
		Activity homeActivity2 = population.getFactory().createActivityFromCoord("home", homeCoordinate);
		plan.addActivity(homeActivity2);
		
		person.addPlan(plan);
		
		population.addPerson(person);
		return population;
	}

}
