package org.matsim.mosaic.example;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.application.MATSimApplication;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.builder.Signals;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsDataLoader;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemControllerData;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.mosaic.MosaicSignalController;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Uses signal scenario from corresponding contrib.
 */
public class SignalScenario extends MATSimApplication {

	private static final File PATH = new File("../signals/examples/tutorial/singleCrossingScenario");

	public SignalScenario() {
		super(new File(PATH, "config.xml").toString());
	}


	@Override
	protected List<ConfigGroup> getCustomModules() {
		return List.of(new SignalSystemsConfigGroup());
	}

	@Override
	protected Config prepareConfig(Config config) {

		config.travelTimeCalculator().setSeparateModes(false);

		return config;
	}

	@Override
	protected void prepareScenario(Scenario scenario) {

		SignalsData data = new SignalsDataLoader(scenario.getConfig()).loadSignalsData();

		for (Map.Entry<Id<SignalSystem>, SignalSystemControllerData> e : data.getSignalControlData().getSignalSystemControllerDataBySystemId().entrySet()) {
			e.getValue().setControllerIdentifier(MosaicSignalController.IDENTIFIER);
		}

		scenario.addScenarioElement(SignalsData.ELEMENT_NAME, data);
	}

	@Override
	protected void prepareControler(Controler controler) {


	}
}
