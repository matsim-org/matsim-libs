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
	public void run() throws URISyntaxException {

		RunMATSimMosaic.main(new String[]{
				"--config", resource("Example/scenario_config.json"),
				"--runtime", resource("etc/runtime.json"),
				"--logger", resource("etc/logback.xml")
		});

	}
}
