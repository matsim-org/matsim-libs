package playground.fzwick;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.io.NetworkReaderMatsimV1;
import org.matsim.core.scenario.ScenarioUtils;

public class RunExampleTest {
	
	
		
		

		public static void main(String[] args) {
//			new RunExampleTest().run();
		
			
			Config config = ConfigUtils.createConfig();
			config.controler().setOutputDirectory("Z:/Berlin-Netz/");
			config.controler().setLastIteration(5);
			
			Scenario sce = ScenarioUtils.createScenario(config);
			createPopulation(sce);
			
			NetworkReaderMatsimV1 reader = new NetworkReaderMatsimV1(sce.getNetwork());
			reader.readFile("Z:/Berlin-Netz/mergedReducedSpeedSantiagoWay.xml");
			config.network().setInputFile("Z:/Berlin-Netz/mergedReducedSpeedSantiagoWay.xml");
			final Controler controler;
			controler = new Controler(sce);
			controler.run();
			
			new ConfigWriter(config).write("Z:/Berlin-Netz/config.xml");
			
		}
		
//		public void run(){
//			controler.run();
//		}
//
//		public Controler getControler() {
//			return controler;
//		}

		

		private static void createPopulation(Scenario scenario) {
			Population population = scenario.getPopulation();			
		
				for (int i = 0; i >= 2; i++) {
					// create a person
					Person person = population.getFactory().createPerson(Id.createPersonId(i));
					population.addPerson(person);
		
					// create a plan for the person that contains all this
					// information
					Plan plan = population.getFactory().createPlan();
					person.addPlan(plan);
		
					// create a start activity at the from link
					Activity startAct = population.getFactory().createActivityFromLinkId("dummy", Id.createLinkId(27923));
					// distribute agents uniformly during one hour.
					startAct.setEndTime(i);
					plan.addActivity(startAct);
		
					// create a dummy leg
					plan.addLeg(population.getFactory().createLeg(TransportMode.car));
		
					// create a drain activity at the to link
					Activity drainAct = population.getFactory().createActivityFromLinkId("dummy", Id.createLinkId(88278));
					plan.addActivity(drainAct);
				}
			
		}

	}