package org.matsim.mosaic.example;

import com.google.common.io.Resources;
import org.junit.Test;
import org.matsim.mosaic.RunMATSimMosaic;

import java.io.File;
import java.net.URISyntaxException;

public class    RunMATSimMosaicTest {

	private static String resource(String path) throws URISyntaxException {
		return new File(Resources.getResource(path).toURI()).toString();
	}

	@Test
	public void runExample() throws URISyntaxException {

		RunMATSimMosaic.main(new String[]{
				"--config", resource("Example/example_config.json"),
				"--runtime", resource("etc/runtime.json"),
				"--logger", resource("etc/logback.xml")
		});

	}


	@Test
	public void runSignal() throws URISyntaxException {

		RunMATSimMosaic.main(new String[]{
				"--config", resource("SignalExample/signal_config.json"),
				"--runtime", resource("etc/runtime.json"),
				"--logger", resource("etc/logback.xml")
		});

	}
}
