package org.matsim.contrib.drt.extension.services;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.extension.services.run.DrtServicesControlerCreator;
import org.matsim.contrib.drt.extension.services.services.params.DrtServiceParams;
import org.matsim.contrib.drt.extension.services.services.params.DrtServicesParams;
import org.matsim.contrib.drt.extension.services.services.params.TimeOfDayReachedTriggerParam;
import org.matsim.contrib.drt.extension.services.trackers.ServiceTracker;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.testcases.MatsimTestUtils;

public class RunDrtWithServicesScenarioIT {
	@RegisterExtension
	private final MatsimTestUtils utils = new MatsimTestUtils();

    @Test
    void test() {
		final String outputDirectory = utils.getOutputDirectory();
		final Config config = ServicesTestUtils.configure(outputDirectory, false);
		var multiModeDrtConfigGroup = ConfigUtils.addOrGetModule(config, MultiModeDrtConfigGroup.class);
		var drtConfigGroup = multiModeDrtConfigGroup.getModalElements().stream().findFirst().orElseThrow();

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

		Controler controler = DrtServicesControlerCreator.createControler(config, false);

		ServiceTracker serviceTracker = new ServiceTracker();

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addEventHandlerBinding().toInstance(serviceTracker);
			}
		});


        controler.run();


		// Check vehicles have been cleaned
		for ( var entrySet: serviceTracker.serviceTracker.entrySet())
		{
			Id<DvrpVehicle> vehicleId = entrySet.getKey();
			int nCleaning = (int) serviceTracker.serviceTracker.get(vehicleId).stream().filter(s -> s.getServiceType().equals("clean")).count();
			Assertions.assertEquals(1,nCleaning);
		}

		// Check vehicles have been deep cleaned
		for ( var entrySet: serviceTracker.serviceTracker.entrySet())
		{
			Id<DvrpVehicle> vehicleId = entrySet.getKey();
			int nCleaning = (int) serviceTracker.serviceTracker.get(vehicleId).stream().filter(s -> s.getServiceType().equals("deep clean")).count();
			Assertions.assertEquals(1,nCleaning);
		}
    }

}
