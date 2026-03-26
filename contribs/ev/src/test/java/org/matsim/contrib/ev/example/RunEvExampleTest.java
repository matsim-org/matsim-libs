package org.matsim.contrib.ev.example;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.common.zones.systems.grid.square.SquareGridZoneSystemParams;
import org.matsim.contrib.ev.EvConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.PopulationComparison;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.eventsfilecomparison.ComparisonResult;

import com.google.common.io.Files;

public class RunEvExampleTest {

	private static final Logger log = LogManager.getLogger(RunEvExample.class);

	@RegisterExtension
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void runTest() {
		try {
			String[] args = {RunEvExample.DEFAULT_CONFIG_FILE,
				"--config:controler.outputDirectory", utils.getOutputDirectory()
			};

			new RunEvExample().run(args, config -> {
				EvConfigGroup.get(config).setWriteVehicleTrajectoriesInterval(1);
				EvConfigGroup.get(config).setWriteZonalEnergyDemandInterval(1);
				EvConfigGroup.get(config).setWriteVehicleSocInterval(1);
				EvConfigGroup.get(config).setWriteChargingActivitiesInterval(1);
				EvConfigGroup.get(config).setWriteChargersInterval(1);
				config.global().setCoordinateSystem("EPSG:2154");

				SquareGridZoneSystemParams params = new SquareGridZoneSystemParams();
				params.setCellSize(10000);
				EvConfigGroup.get(config).addParameterSet(params);
			});

			{
				Population expected = PopulationUtils.createPopulation(ConfigUtils.createConfig());
				PopulationUtils.readPopulation(expected, utils.getInputDirectory() + "/output_plans.xml.gz");

				Population actual = PopulationUtils.createPopulation(ConfigUtils.createConfig());
				PopulationUtils.readPopulation(actual, utils.getOutputDirectory() + "/output_plans.xml.gz");

				PopulationComparison.Result result = PopulationComparison.compare(expected, actual);
				Assertions.assertEquals(PopulationComparison.Result.equal, result);
			}
			{
				String expected = utils.getInputDirectory() + "/output_events.xml.gz";
				String actual = utils.getOutputDirectory() + "/output_events.xml.gz";
				ComparisonResult result = EventsUtils.compareEventsFiles(expected, actual);
				Assertions.assertEquals(ComparisonResult.FILES_ARE_EQUAL, result);
			}
			{
				assertTrue(new File(utils.getOutputDirectory() + "/ev_analysis_zones.shp").exists());
				
				assertTrue(new File(utils.getOutputDirectory() + "/ITERS/it.0/0.ev_vehicle_trajectories.csv.gz").exists());
				assertTrue(new File(utils.getOutputDirectory() + "/ev_vehicle_trajectories.csv.gz").exists());

				assertTrue(new File(utils.getOutputDirectory() + "/ITERS/it.0/0.ev_zonal_energy_demand.csv.gz").exists());
				assertTrue(new File(utils.getOutputDirectory() + "/ev_zonal_energy_demand.csv.gz").exists());
			
				assertTrue(new File(utils.getOutputDirectory() + "/ITERS/it.0/0.ev_socs.csv.gz").exists());
				assertTrue(new File(utils.getOutputDirectory() + "/ev_socs.csv.gz").exists());

				assertTrue(new File(utils.getOutputDirectory() + "/ITERS/it.0/0.ev_charging_activities.csv.gz").exists());
				assertTrue(new File(utils.getOutputDirectory() + "/ev_charging_activities.csv.gz").exists());
				
				assertTrue(new File(utils.getOutputDirectory() + "/ITERS/it.0/0.chargers.xml.gz").exists());
				assertTrue(new File(utils.getOutputDirectory() + "/chargers.xml.gz").exists());
			}

		} catch (Exception ee) {
			log.fatal("there was an exception: \n" + ee);

			// if one catches an exception, then one needs to explicitly fail the test:
			Assertions.fail();
		}

	}

	@Test
	public void runTestWithChargingDurationEnforcement() {
		try {
			String[] args = {RunEvExample.DEFAULT_CONFIG_FILE,
				"--config:controler.outputDirectory", utils.getOutputDirectory(),
				"--config:ev.enforceChargingInteractionDuration", "true",
			};

			new RunEvExample().run(args);
			{
				Population expected = PopulationUtils.createPopulation(ConfigUtils.createConfig());
				PopulationUtils.readPopulation(expected, utils.getInputDirectory() + "/output_plans.xml.gz");

				Population actual = PopulationUtils.createPopulation(ConfigUtils.createConfig());
				PopulationUtils.readPopulation(actual, utils.getOutputDirectory() + "/output_plans.xml.gz");

				PopulationComparison.Result result = PopulationComparison.compare(expected, actual);
				Assertions.assertEquals(PopulationComparison.Result.equal, result);
			}
			{
				String expected = utils.getInputDirectory() + "/output_events.xml.gz";
				String actual = utils.getOutputDirectory() + "/output_events.xml.gz";
				ComparisonResult result = EventsUtils.compareEventsFiles(expected, actual);
				Assertions.assertEquals(ComparisonResult.FILES_ARE_EQUAL, result);
			}

		} catch (Exception ee) {
			log.fatal("there was an exception: \n" + ee);

			// if one catches an exception, then one needs to explicitly fail the test:
			Assertions.fail();
		}

	}

}
