package org.matsim.contrib.drt.extension.services;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.contrib.drt.extension.DrtWithExtensionsConfigGroup;
import org.matsim.contrib.drt.extension.services.services.params.DrtServiceParams;
import org.matsim.contrib.drt.extension.services.services.params.DrtServicesParams;
import org.matsim.contrib.drt.extension.services.services.params.TimeOfDayReachedTriggerParam;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.nio.file.Path;

/**
 * @author steffenaxer
 */
public class DrtServicesParamsIOTest {
	@RegisterExtension
	private final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testServiceParamsIO()
	{
		final String outputDirectory = utils.getOutputDirectory();
		Path outputFile = Path.of(outputDirectory).resolve("config.xml");
		Config outputConfig = ConfigUtils.createConfig();
		DrtWithExtensionsConfigGroup drtConfigGroup = new DrtWithExtensionsConfigGroup();
		outputConfig.addModule(drtConfigGroup);

		DrtServicesParams drtServicesParams = new DrtServicesParams();

		{
			DrtServiceParams clean = new DrtServiceParams("clean");
			clean.executionLimit = 1;
			clean.duration = 900;
			var condition1 = new TimeOfDayReachedTriggerParam();
			condition1.executionTime = 53205;
			clean.addParameterSet(condition1);
			drtServicesParams.addParameterSet(clean);
		}

		{
			DrtServiceParams deepClean = new DrtServiceParams("deep clean");
			deepClean.executionLimit = 1;
			deepClean.duration = 1800;
			var condition1 = new TimeOfDayReachedTriggerParam();
			condition1.executionTime = 53205;
			deepClean.addParameterSet(condition1);
			drtServicesParams.addParameterSet(deepClean);
		}

		drtConfigGroup.addParameterSet(drtServicesParams);
		ConfigUtils.writeMinimalConfig(outputConfig,outputFile.toString());


		Config inputConfig = ConfigUtils.loadConfig(outputFile.toString());
		var drtConfigGroup2 = ConfigUtils.addOrGetModule(inputConfig,DrtWithExtensionsConfigGroup.class);
		var servicesParams = drtConfigGroup2.getServicesParams().orElseThrow();
		var services = servicesParams.getServices();
		var service = servicesParams.getService("deep clean").orElseThrow();

		Assertions.assertEquals(1, service.executionLimit);
		Assertions.assertEquals(2, services.size());
		Assertions.assertTrue(drtConfigGroup2.getServicesParams().isPresent());


	}


}
