package org.matsim.contrib.ev.example;

import org.junit.Test;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

public class RunEvExampleWithLTHConsumptionModelIT {

	@Test
	public void run() throws MalformedURLException {
		URL configUrl = new File(RunEvExampleWithLTHConsumptionModel.DEFAULT_CONFIG_FILE).toURI().toURL();
		new RunEvExampleWithLTHConsumptionModel().run(configUrl);

	}
}
