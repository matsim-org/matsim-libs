package playground.mzilske.teach.tasks2012;

import com.google.inject.Provider;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.vis.otfvis.OTFFileWriter;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo.AgentState;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfoFactory;

public class OwnMobsim {

	private static class MyMobsim implements Mobsim {

		private EventsManager eventsManager;
		private Scenario scenario;
		private AgentSnapshotInfoFactory agentSnapshotInfoFactory = new AgentSnapshotInfoFactory(null);

		public MyMobsim(Scenario sc, EventsManager eventsManager) {
			this.scenario = sc;
			this.eventsManager = eventsManager;
		}

		@Override
		public void run() {
			OTFFileWriter writer = new OTFFileWriter(scenario, "output/movie.mvi");
			Node start = scenario.getNetwork().getNodes().get(Id.create("1", Node.class));
			Node end = scenario.getNetwork().getNodes().get(Id.create("14", Node.class));
			for (double i=0; i<1000; i++) {
				writer.beginSnapshot(i);
				CoordImpl pos = new CoordImpl(start.getCoord().getX() + ( (end.getCoord().getX() - start.getCoord().getX()) / 1000.0) * i,  
						start.getCoord().getY() + ( (end.getCoord().getY() - start.getCoord().getY()) / 1000.0)* i);
				AgentSnapshotInfo agentSnapshotInfo = agentSnapshotInfoFactory.createAgentSnapshotInfo(Id.create("1", Person.class), pos.getX(), pos.getY(), 0.0, 0.0);
				System.out.println(pos);
				agentSnapshotInfo.setAgentState(AgentState.PERSON_DRIVING_CAR);
				writer.addAgent(agentSnapshotInfo);
				writer.endSnapshot();			
			}
			writer.finish();
		}

	}

	private static class MyMobsimFactory implements MobsimFactory {

		@Override
		public Mobsim createMobsim(Scenario sc, EventsManager eventsManager) {
			return new MyMobsim(sc, eventsManager);
		}

	}

	public static void main(String[] args) {
		Config config = ConfigUtils.loadConfig("examples/equil/config.xml");
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(1);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		final Controler controler = new Controler(scenario);
		controler.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		final MobsimFactory mobsimFactory = new MyMobsimFactory();
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bindMobsim().toProvider(new Provider<Mobsim>() {
					@Override
					public Mobsim get() {
						return mobsimFactory.createMobsim(controler.getScenario(), controler.getEvents());
					}
				});
			}
		});
		controler.run();
		OTFVis.playMVI("output/movie.mvi");
	}

}
