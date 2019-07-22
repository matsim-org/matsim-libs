package org.matsim.contrib.ev.example;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.junit.Test;

public class RunEvExampleIT {
	@Test
	public void run() throws MalformedURLException {
		URL configUrl = new File(RunEvExample.DEFAULT_CONFIG_FILE).toURI().toURL();
		new RunEvExample().run(configUrl);
	}
}
