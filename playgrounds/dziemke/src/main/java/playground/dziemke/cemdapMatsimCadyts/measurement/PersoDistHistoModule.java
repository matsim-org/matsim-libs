package playground.dziemke.cemdapMatsimCadyts.measurement;


import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.ReplayEvents;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.events.EventsManagerModule;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioByInstanceModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.EventsToLegs;
import org.matsim.core.scoring.ExperiencedPlanElementsModule;
import org.matsim.core.scoring.PersonExperiencedLeg;
import org.matsim.core.utils.io.UncheckedIOException;

public class PersoDistHistoModule extends AbstractModule {

	public static void main(String[] args) {
		String runDir = args[0];
		Config config = ConfigUtils.createConfig();
		config.controler().setOutputDirectory(runDir);
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		Scenario scenario = ScenarioUtils.createScenario(config);
		int iterationNumber = Integer.parseInt(args[1]);
		new MatsimNetworkReader(scenario.getNetwork()).readFile(runDir + "/output_network.xml.gz");
		new PopulationReader(scenario).readFile(runDir + "/output_plans.xml.gz");
		com.google.inject.Injector injector = Injector.createInjector(config,
				new ReplayEvents.Module(),
				new PersoDistHistoModule(),
				new ExperiencedPlanElementsModule(),
				new ScenarioByInstanceModule(scenario),
				new EventsManagerModule(),
				new AbstractModule() {
					@Override
					public void install() {
						bind(OutputDirectoryHierarchy.class).asEagerSingleton();
					}
				});
				ReplayEvents instance = injector.getInstance(ReplayEvents.class);
		instance.playEventsFile(runDir + "/ITERS/it."+iterationNumber+"/"+iterationNumber+".events.xml.gz", iterationNumber);
	}

	@Override
	public void install() {
		bind(PersoDistHistoControlerListener.class).asEagerSingleton();
		bind(PersoDistHistogram.class).to(PersoDistHistoControlerListener.class);
		addControlerListenerBinding().to(PersoDistHistoControlerListener.class);
	}

	private static class PersoDistHistoControlerListener implements EventsToLegs.LegHandler, StartupListener, BeforeMobsimListener, IterationEndsListener, PersoDistHistogram {

		@Inject Population population;
		@Inject EventsToLegs experiencedPlanElementsService;
		@Inject OutputDirectoryHierarchy controlerIO;
//		@Inject CloneService cloneService;

		private HashMap<Id<Person>, Double> distances;

		@Override
		public void notifyStartup(StartupEvent startupEvent) {
			experiencedPlanElementsService.addLegHandler(this);
		}

		@Override
		public void notifyBeforeMobsim(BeforeMobsimEvent beforeMobsimEvent) {
			distances = new HashMap<>();
			for (Id<Person> personId : population.getPersons().keySet()) {
//				if (cloneService.isActive(personId)) {
					distances.put(personId, 0.0);
//				}
			}
		}

		@Override
		public void handleLeg(PersonExperiencedLeg leg) {
			distances.put(leg.getAgentId(), distances.get(leg.getAgentId()) + leg.getLeg().getRoute().getDistance());
		}

		@Override
		public void notifyIterationEnds(IterationEndsEvent iterationEndsEvent) {
			writeToFile(controlerIO.getIterationPath(iterationEndsEvent.getIteration())+"/perso-dist-histo.txt", pw -> {
				pw.printf("person\tdistance\n");
				for (Map.Entry<Id<Person>, Double> entry : distances.entrySet()) {
					String personId = entry.getKey().toString();
					Double distance = entry.getValue();
					pw.printf("%s\t%.2f\n", personId, distance);
				}
			});
		}

		@Override
		public HashMap<Id<Person>, Double> getDistances() {
			return distances;
		}

	}
	
	public static void writeToFile(String string, StreamingOutput so) {
        try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(string)))) {
            so.write(pw);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
	}

}
