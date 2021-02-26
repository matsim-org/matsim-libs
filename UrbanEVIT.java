package org.matsim.urbanEV;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.ev.EvConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.run.ev.RunUrbanEVExample;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.urbanEV.EVUtils;
import org.matsim.vehicles.*;

import java.net.URL;
import java.util.Arrays;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class UrbanEVIT {

	@Rule
	public MatsimTestUtils matsimTestUtils = new MatsimTestUtils();

	@Test
	public void run() {
		Scenario scenario = CreateUrbanEVTestScenario.createTestScenario();
		///controler with Urban EV module
		Controler controler = RunUrbanEVExample.prepareControler(scenario);
		controler.run();
	}
}