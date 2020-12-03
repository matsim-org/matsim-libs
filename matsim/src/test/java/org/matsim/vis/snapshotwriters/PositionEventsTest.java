package org.matsim.vis.snapshotwriters;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

import javax.inject.Inject;
import javax.inject.Provider;
import java.net.MalformedURLException;
import java.net.URISyntaxException;

public class PositionEventsTest {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void test1() throws URISyntaxException, MalformedURLException {

		var twoRoutesScenario = ExamplesUtils.getTestScenarioURL("two-routes");

		var config = ConfigUtils.loadConfig(twoRoutesScenario.toURI().resolve("config.xml").toURL());
		//config.controler().setSnapshotFormat(List.of(ControlerConfigGroup.SnapshotFormat.positionevents));
		config.qsim().setSnapshotPeriod(10);
		config.controler().setWriteSnapshotsInterval(0);
		config.controler().setLastIteration(0);
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		config.controler().setOutputDirectory("C:\\Users\\Janekdererste\\Desktop\\test-output");

		var scenario = ScenarioUtils.loadScenario(config);

		var controler = new Controler(scenario);

		controler.addOverridingModule(
				new AbstractModule() {
					@Override
					public void install() {
						addMobsimListenerBinding().toProvider(new Provider<>() {

							@Inject
							ReplanningContext context;
							@Inject
							OutputDirectoryHierarchy hierarchy;
							@Inject
							private Config config;

							@Override
							public MobsimListener get() {
								if (context.getIteration() == config.controler().getLastIteration()) {

									var manager = new SnapshotWriterManager(config);
									var writer = new SnapshotWriterBla(hierarchy.getOutputFilename("position_events.xml.gz"));
									manager.addSnapshotWriter(writer);
									return manager;
								}
								return new MobsimListener() {
								};
							}
						});
					}
				}
		);

		controler.run();
	}

	private static class SnapshotWriterBla implements SnapshotWriter {

		private final EventWriterXML writer;
		private double now = 0;

		SnapshotWriterBla(String outfile) {
			this.writer = new EventWriterXML(outfile);
		}

		@Override
		public void beginSnapshot(double time) {
			now = time;
		}

		@Override
		public void endSnapshot() {

		}

		@Override
		public void addAgent(AgentSnapshotInfo position) {

			if (position.getAgentState().equals(AgentSnapshotInfo.AgentState.PERSON_DRIVING_CAR))
				writer.handleEvent(new PositionEvent(now, position));
		}

		@Override
		public void finish() {

		}
	}
}
