package org.tit.matsim;

import java.io.IOException;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

public class Tutorial {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// firstConfig();
		 testHandMadePlan("RandomPlan");
//		 customPlan();
		
	}

	public static void firstConfig() {
		String configFile = "input/config_countsScaleFactor1.xml";
		Config config = ConfigUtils.loadConfig(configFile);
		Controler controler = new Controler(config);
		controler.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		controler.run();
	}

	public static void testHandMadePlan(String name) {
		// set the plan input in config file
		String configFile = "input/config_useHandMadePlan.xml";
		Config config = ConfigUtils.loadConfig(configFile);
		Controler controler = new Controler(config);
		controler.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		controler.run();
		 try {
			 Tools.copyCurrentOutput(name);
			 } catch (IOException e) {
			 // TODO Auto-generated catch block
			 e.printStackTrace();
			 }
	}

	public static void customPlan() {
		Config config = ConfigUtils.createConfig();
		Scenario sc = ScenarioUtils.createScenario(config);
		Network network = sc.getNetwork();
		Population population = sc.getPopulation();
		PopulationFactory populationFactory = population.getFactory();
		//
		
		for (int i = 0; i < 1000; i++) {
			Double rDouble = Math.random()*0.08;
			Person person = populationFactory.createPerson(Id.create("pid" + i, Person.class));

			population.addPerson(person);
			//
			Plan plan = populationFactory.createPlan();
			person.addPlan(plan);
			//
			CoordinateTransformation ct = TransformationFactory
					.getCoordinateTransformation(TransformationFactory.WGS84,
							TransformationFactory.CH1903_LV03);
			// home activity
			Coord homeCoordinates = new Coord(8.55744100, 47.3548407);
			Activity activity1 = populationFactory.createActivityFromCoord(
					"h6", ct.transform(homeCoordinates));
			activity1.setEndTime(21600);
			
			plan.addActivity(activity1);

			plan.addLeg(populationFactory.createLeg("car"));
			// work activity
			Activity activity2 =
					  populationFactory.createActivityFromCoord(
					    "w10",ct.transform(new Coord(8.51774679, 47.3893719))
					  );
			activity2.setEndTime(57600);
			plan.addActivity(activity2);
			plan.addLeg(populationFactory.createLeg("car"));
			
			Activity activity3 = populationFactory.createActivityFromCoord(
					"h6", ct.transform(homeCoordinates));
			plan.addActivity(activity3);
		}
		//
		MatsimWriter popWriter = new PopulationWriter(population, network);
		popWriter.write("input/plans.HandMade.xml");
	}

}