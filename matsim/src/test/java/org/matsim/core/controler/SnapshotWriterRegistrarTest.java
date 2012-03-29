package org.matsim.core.controler;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.vis.snapshotwriters.SnapshotWriter;
import org.matsim.vis.snapshotwriters.SnapshotWriterFactory;

public class SnapshotWriterRegistrarTest extends MatsimTestCase {

	public void testGivesInstanceForKMLSnapshotWriter() {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		SnapshotWriterRegistrar registrar = new SnapshotWriterRegistrar();
		SnapshotWriterFactoryRegister register = registrar.getFactoryRegister();
		SnapshotWriterFactory factory = register.getInstance("googleearth");
		SnapshotWriter snapshotWriter = factory.createSnapshotWriter(getOutputDirectory() + factory.getPreferredBaseFilename(), scenario);
		snapshotWriter.finish();
	}

	public void testGivesInstanceForTransimsSnapshotWriter() {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		SnapshotWriterRegistrar registrar = new SnapshotWriterRegistrar();
		SnapshotWriterFactoryRegister register = registrar.getFactoryRegister();
		SnapshotWriterFactory factory = register.getInstance("transims");
		SnapshotWriter snapshotWriter = factory.createSnapshotWriter(getOutputDirectory() + factory.getPreferredBaseFilename(), scenario);
		snapshotWriter.finish();
	}

}
