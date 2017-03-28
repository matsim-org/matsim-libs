package playground.fzwick;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.controler.SignalsModule;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsDataLoader;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalControlDataFactoryImpl;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalControlData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalControlDataFactory;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalPlanData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalSystemControllerData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsDataFactory;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsDataFactoryImpl;
import org.matsim.contrib.signals.model.DefaultPlanbasedSignalSystemController;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalPlan;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.contrib.signals.utils.SignalUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.network.io.NetworkReaderMatsimV1;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultSelector;
import org.matsim.core.scenario.ScenarioUtils;

import tutorial.simpleResponsiveSignalEngine.RunSimpleResponsiveSignalExample;
import tutorial.simpleResponsiveSignalEngine.SimpleResponsiveSignal;

public class RunExample {
	
	
		
		private final Controler controler;

		public static void main(String[] args) {
			new RunExample().run();
		}

		public RunExample() {
			Config con = ConfigUtils.createConfig();
			
			Scenario sce = ScenarioUtils.createScenario(con);
			createPopulation(sce);
			
			NetworkReaderMatsimV1 reader = new NetworkReaderMatsimV1(sce.getNetwork());
			reader.readFile("Z:/Berlin-Netz/mergedReducedSpeedSantiagoWay.xml");
			Config config = ConfigUtils.createConfig();
			config.controler().setOutputDirectory("Z:/Berlin-Netz/");
			config.controler().setLastIteration(1);
			controler = new Controler(sce);

			
			
		}
		
		public void run(){
			controler.run();
		}

		public Controler getControler() {
			return controler;
		}

		

		private static void createPopulation(Scenario scenario) {
			Population population = scenario.getPopulation();			
		
				for (int i = 0; i < 1; i++) {
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