package org.matsim.contrib.discrete_mode_choice.modules.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashSet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.contribs.discrete_mode_choice.components.constraints.LinkAttributeConstraint;
import org.matsim.contribs.discrete_mode_choice.components.constraints.ShapeFileConstraint;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceModel.FallbackBehaviour;
import org.matsim.contribs.discrete_mode_choice.modules.ConstraintModule;
import org.matsim.contribs.discrete_mode_choice.modules.EstimatorModule;
import org.matsim.contribs.discrete_mode_choice.modules.HomeFinderModule;
import org.matsim.contribs.discrete_mode_choice.modules.ModeAvailabilityModule;
import org.matsim.contribs.discrete_mode_choice.modules.ModelModule;
import org.matsim.contribs.discrete_mode_choice.modules.SelectorModule;
import org.matsim.contribs.discrete_mode_choice.modules.TourFinderModule;
import org.matsim.contribs.discrete_mode_choice.modules.config.ActivityTourFinderConfigGroup;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.contribs.discrete_mode_choice.modules.config.LinkAttributeConstraintConfigGroup;
import org.matsim.contribs.discrete_mode_choice.modules.config.MATSimTripScoringConfigGroup;
import org.matsim.contribs.discrete_mode_choice.modules.config.MultinomialLogitSelectorConfigGroup;
import org.matsim.contribs.discrete_mode_choice.modules.config.ShapeFileConstraintConfigGroup;
import org.matsim.contribs.discrete_mode_choice.modules.config.VehicleTourConstraintConfigGroup;
import org.matsim.contribs.discrete_mode_choice.modules.config.VehicleTripConstraintConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigReader;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.testcases.MatsimTestUtils;

public class ConfigTest {
	@RegisterExtension
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testDefaultValuesRoundTrip() {
		// Create config with all default values
		DiscreteModeChoiceConfigGroup dmcConfig = new DiscreteModeChoiceConfigGroup();
		Config config = ConfigUtils.createConfig(dmcConfig);

		// Write to file and read back
		String path = utils.getOutputDirectory() + "/config_default.xml";
		new ConfigWriter(config).write(path);

		DiscreteModeChoiceConfigGroup dmcConfig2 = new DiscreteModeChoiceConfigGroup();
		ConfigUtils.loadConfig(path, dmcConfig2);

		// Verify top-level defaults
		assertEquals(true, dmcConfig2.getPerformReroute());
		assertEquals(false, dmcConfig2.getEnforceSinglePlan());
		assertEquals(true, dmcConfig2.getAccumulateEstimationDelays());
		assertEquals(ModelModule.ModelType.Tour, dmcConfig2.getModelType());
		assertEquals(FallbackBehaviour.EXCEPTION, dmcConfig2.getFallbackBehaviour());
		assertEquals(ModeAvailabilityModule.CAR, dmcConfig2.getModeAvailability());
		assertEquals(TourFinderModule.ACTIVITY_BASED, dmcConfig2.getTourFinder());
		assertEquals(HomeFinderModule.ACTIVITY_BASED, dmcConfig2.getHomeFinder());
		assertEquals(SelectorModule.RANDOM, dmcConfig2.getSelector());
		assertEquals(new HashSet<>(Arrays.asList(ConstraintModule.VEHICLE_CONTINUITY)),
				new HashSet<>(dmcConfig2.getTourConstraints()));
		assertEquals(new HashSet<>(Arrays.asList(ConstraintModule.VEHICLE_CONTINUITY)),
				new HashSet<>(dmcConfig2.getTripConstraints()));
		assertEquals(EstimatorModule.UNIFORM, dmcConfig2.getTourEstimator());
		assertEquals(EstimatorModule.UNIFORM, dmcConfig2.getTripEstimator());
		assertTrue(dmcConfig2.getTourFilters().isEmpty());
		assertTrue(dmcConfig2.getTripFilters().isEmpty());
		assertTrue(dmcConfig2.getCachedModes().isEmpty());
		assertEquals(1, dmcConfig2.getWriteUtilitiesInterval());

		// Verify LinkAttributeConstraintConfigGroup defaults
		LinkAttributeConstraintConfigGroup linkAttr = dmcConfig2.getLinkAttributeConstraintConfigGroup();
		assertEquals(LinkAttributeConstraint.Requirement.BOTH, linkAttr.getRequirement());
		assertNull(linkAttr.getAttributeName());
		assertNull(linkAttr.getAttributeValue());
		assertTrue(linkAttr.getConstrainedModes().isEmpty());

		// Verify ShapeFileConstraintConfigGroup defaults
		ShapeFileConstraintConfigGroup shapeFile = dmcConfig2.getShapeFileConstraintConfigGroup();
		assertEquals(ShapeFileConstraint.Requirement.BOTH, shapeFile.getRequirement());
		assertNull(shapeFile.getPath());
		assertTrue(shapeFile.getConstrainedModes().isEmpty());

		// Verify MultinomialLogitSelectorConfigGroup defaults
		MultinomialLogitSelectorConfigGroup mls = dmcConfig2.getMultinomialLogitSelectorConfig();
		assertEquals(-700.0, mls.getMinimumUtility(), 1e-9);
		assertEquals(700.0, mls.getMaximumUtility(), 1e-9);
		assertEquals(false, mls.getConsiderMinimumUtility());

		// Verify VehicleTourConstraintConfigGroup defaults
		VehicleTourConstraintConfigGroup vehicleTour = dmcConfig2.getVehicleTourConstraintConfig();
		assertEquals(new HashSet<>(Arrays.asList("car", "bike")),
				new HashSet<>(vehicleTour.getRestrictedModes()));

		// Verify VehicleTripConstraintConfigGroup defaults
		VehicleTripConstraintConfigGroup vehicleTrip = dmcConfig2.getVehicleTripConstraintConfig();
		assertEquals(new HashSet<>(Arrays.asList("car", "bike")),
				new HashSet<>(vehicleTrip.getRestrictedModes()));
		assertEquals(true, vehicleTrip.getIsAdvanced());

		// Verify SubtourModeConstraintConfigGroup defaults
		assertTrue(dmcConfig2.getSubtourConstraintConfig().getConstrainedModes().isEmpty());

		// Verify MATSimTripScoringConfigGroup defaults
		MATSimTripScoringConfigGroup matSimScoring = dmcConfig2.getMATSimTripScoringConfigGroup();
		assertEquals(new HashSet<>(Arrays.asList("pt")), new HashSet<>(matSimScoring.getPtLegModes()));

		// Verify TourLengthFilterConfigGroup defaults
		assertEquals(10, dmcConfig2.getTourLengthFilterConfigGroup().getMaximumLength());

		// Verify ActivityTourFinderConfigGroup defaults
		ActivityTourFinderConfigGroup atf = dmcConfig2.getActivityTourFinderConfigGroup();
		assertEquals(new HashSet<>(Arrays.asList("home")), new HashSet<>(atf.getActivityTypes()));
	}

	@Test
	void testNonDefaultValuesRoundTrip() {
		// Create config with non-default values
		DiscreteModeChoiceConfigGroup dmcConfig = new DiscreteModeChoiceConfigGroup();
		Config config = ConfigUtils.createConfig(dmcConfig);

		// Set non-default top-level values
		dmcConfig.setPerformReroute(false);
		dmcConfig.setEnforceSinglePlan(true);
		dmcConfig.setAccumulateEstimationDelays(false);
		dmcConfig.setModelType(ModelModule.ModelType.Trip);
		dmcConfig.setFallbackBehaviour(FallbackBehaviour.IGNORE_AGENT);
		dmcConfig.setModeAvailability("CustomModeAvailability");
		dmcConfig.setTourFinder("CustomTourFinder");
		dmcConfig.setHomeFinder("CustomHomeFinder");
		dmcConfig.setSelector("CustomSelector");
		dmcConfig.setTourConstraints(Arrays.asList("ConstraintA", "ConstraintB"));
		dmcConfig.setTripConstraints(Arrays.asList("ConstraintC", "ConstraintD"));
		dmcConfig.setTourEstimator("CustomTourEstimator");
		dmcConfig.setTripEstimator("CustomTripEstimator");
		dmcConfig.setTourFilters(Arrays.asList("FilterA"));
		dmcConfig.setTripFilters(Arrays.asList("FilterB"));
		dmcConfig.setCachedModes(Arrays.asList("car", "bike"));
		dmcConfig.setWriteUtilitiesInterval(5);

		// Set non-default sub-config values
		MultinomialLogitSelectorConfigGroup mls = dmcConfig.getMultinomialLogitSelectorConfig();
		mls.setMinimumUtility(-500.0);
		mls.setMaximumUtility(500.0);
		mls.setConsiderMinimumUtility(true);

		LinkAttributeConstraintConfigGroup linkAttr = dmcConfig.getLinkAttributeConstraintConfigGroup();
		linkAttr.setRequirement(LinkAttributeConstraint.Requirement.ORIGIN);
		linkAttr.setAttributeName("myAttribute");
		linkAttr.setAttributeValue("myValue");
		linkAttr.setConstrainedModes(Arrays.asList("car"));

		ShapeFileConstraintConfigGroup shapeFile = dmcConfig.getShapeFileConstraintConfigGroup();
		shapeFile.setRequirement(ShapeFileConstraint.Requirement.DESTINATION);
		shapeFile.setPath("/path/to/shapefile.shp");
		shapeFile.setConstrainedModes(Arrays.asList("bike", "car"));

		VehicleTourConstraintConfigGroup vehicleTour = dmcConfig.getVehicleTourConstraintConfig();
		vehicleTour.setRestrictedModes(Arrays.asList("car"));

		VehicleTripConstraintConfigGroup vehicleTrip = dmcConfig.getVehicleTripConstraintConfig();
		vehicleTrip.setRestrictedModes(Arrays.asList("car"));
		vehicleTrip.setIsAdvanced(false);

		ActivityTourFinderConfigGroup atf = dmcConfig.getActivityTourFinderConfigGroup();
		atf.setActivityTypes(Arrays.asList("home", "work"));

		dmcConfig.getMATSimTripScoringConfigGroup().setPtLegModes(Arrays.asList("pt", "bus"));
		dmcConfig.getTourLengthFilterConfigGroup().setMaximumLength(20);

		// Write to file and read back
		String path = utils.getOutputDirectory() + "/config_nondefault.xml";
		new ConfigWriter(config).write(path);

		DiscreteModeChoiceConfigGroup dmcConfig2 = new DiscreteModeChoiceConfigGroup();
		ConfigUtils.loadConfig(path, dmcConfig2);

		// Verify all non-default top-level values
		assertEquals(false, dmcConfig2.getPerformReroute());
		assertEquals(true, dmcConfig2.getEnforceSinglePlan());
		assertEquals(false, dmcConfig2.getAccumulateEstimationDelays());
		assertEquals(ModelModule.ModelType.Trip, dmcConfig2.getModelType());
		assertEquals(FallbackBehaviour.IGNORE_AGENT, dmcConfig2.getFallbackBehaviour());
		assertEquals("CustomModeAvailability", dmcConfig2.getModeAvailability());
		assertEquals("CustomTourFinder", dmcConfig2.getTourFinder());
		assertEquals("CustomHomeFinder", dmcConfig2.getHomeFinder());
		assertEquals("CustomSelector", dmcConfig2.getSelector());
		assertEquals(new HashSet<>(Arrays.asList("ConstraintA", "ConstraintB")),
				new HashSet<>(dmcConfig2.getTourConstraints()));
		assertEquals(new HashSet<>(Arrays.asList("ConstraintC", "ConstraintD")),
				new HashSet<>(dmcConfig2.getTripConstraints()));
		assertEquals("CustomTourEstimator", dmcConfig2.getTourEstimator());
		assertEquals("CustomTripEstimator", dmcConfig2.getTripEstimator());
		assertEquals(new HashSet<>(Arrays.asList("FilterA")), new HashSet<>(dmcConfig2.getTourFilters()));
		assertEquals(new HashSet<>(Arrays.asList("FilterB")), new HashSet<>(dmcConfig2.getTripFilters()));
		assertEquals(new HashSet<>(Arrays.asList("car", "bike")), new HashSet<>(dmcConfig2.getCachedModes()));
		assertEquals(5, dmcConfig2.getWriteUtilitiesInterval());

		// Verify non-default sub-config values
		MultinomialLogitSelectorConfigGroup mls2 = dmcConfig2.getMultinomialLogitSelectorConfig();
		assertEquals(-500.0, mls2.getMinimumUtility(), 1e-9);
		assertEquals(500.0, mls2.getMaximumUtility(), 1e-9);
		assertEquals(true, mls2.getConsiderMinimumUtility());

		LinkAttributeConstraintConfigGroup linkAttr2 = dmcConfig2.getLinkAttributeConstraintConfigGroup();
		assertEquals(LinkAttributeConstraint.Requirement.ORIGIN, linkAttr2.getRequirement());
		assertEquals("myAttribute", linkAttr2.getAttributeName());
		assertEquals("myValue", linkAttr2.getAttributeValue());
		assertEquals(new HashSet<>(Arrays.asList("car")), new HashSet<>(linkAttr2.getConstrainedModes()));

		ShapeFileConstraintConfigGroup shapeFile2 = dmcConfig2.getShapeFileConstraintConfigGroup();
		assertEquals(ShapeFileConstraint.Requirement.DESTINATION, shapeFile2.getRequirement());
		assertEquals("/path/to/shapefile.shp", shapeFile2.getPath());
		assertEquals(new HashSet<>(Arrays.asList("bike", "car")), new HashSet<>(shapeFile2.getConstrainedModes()));

		VehicleTourConstraintConfigGroup vehicleTour2 = dmcConfig2.getVehicleTourConstraintConfig();
		assertEquals(new HashSet<>(Arrays.asList("car")), new HashSet<>(vehicleTour2.getRestrictedModes()));

		VehicleTripConstraintConfigGroup vehicleTrip2 = dmcConfig2.getVehicleTripConstraintConfig();
		assertEquals(new HashSet<>(Arrays.asList("car")), new HashSet<>(vehicleTrip2.getRestrictedModes()));
		assertEquals(false, vehicleTrip2.getIsAdvanced());

		ActivityTourFinderConfigGroup atf2 = dmcConfig2.getActivityTourFinderConfigGroup();
		assertEquals(new HashSet<>(Arrays.asList("home", "work")), new HashSet<>(atf2.getActivityTypes()));

		assertEquals(new HashSet<>(Arrays.asList("pt", "bus")),
				new HashSet<>(dmcConfig2.getMATSimTripScoringConfigGroup().getPtLegModes()));
		assertEquals(20, dmcConfig2.getTourLengthFilterConfigGroup().getMaximumLength());
	}

	@Test
	void testNullableFieldsRoundTrip() {
		// Verify that null fields (like attributeName, attributeValue, path) remain null after round-trip
		DiscreteModeChoiceConfigGroup dmcConfig = new DiscreteModeChoiceConfigGroup();
		Config config = ConfigUtils.createConfig(dmcConfig);

		// Defaults: attributeName/attributeValue/path are null
		assertNull(dmcConfig.getLinkAttributeConstraintConfigGroup().getAttributeName());
		assertNull(dmcConfig.getLinkAttributeConstraintConfigGroup().getAttributeValue());
		assertNull(dmcConfig.getShapeFileConstraintConfigGroup().getPath());

		String path = utils.getOutputDirectory() + "/config_nullable.xml";
		new ConfigWriter(config).write(path);

		DiscreteModeChoiceConfigGroup dmcConfig2 = new DiscreteModeChoiceConfigGroup();
		ConfigUtils.loadConfig(path, dmcConfig2);

		assertNull(dmcConfig2.getLinkAttributeConstraintConfigGroup().getAttributeName());
		assertNull(dmcConfig2.getLinkAttributeConstraintConfigGroup().getAttributeValue());
		assertNull(dmcConfig2.getShapeFileConstraintConfigGroup().getPath());
	}

	@Test
	void testEmptyCollectionRoundTrip() {
		// Test that collections that are empty by default remain empty after round-trip,
		// and that non-default collections can be explicitly emptied and remain empty.
		DiscreteModeChoiceConfigGroup dmcConfig = new DiscreteModeChoiceConfigGroup();
		Config config = ConfigUtils.createConfig(dmcConfig);

		// Explicitly set some non-empty-default collections to empty
		dmcConfig.getVehicleTourConstraintConfig().setRestrictedModes(Arrays.asList());
		dmcConfig.getVehicleTripConstraintConfig().setRestrictedModes(Arrays.asList());
		dmcConfig.getMATSimTripScoringConfigGroup().setPtLegModes(Arrays.asList());

		String path = utils.getOutputDirectory() + "/config_empty.xml";
		new ConfigWriter(config).write(path);

		DiscreteModeChoiceConfigGroup dmcConfig2 = new DiscreteModeChoiceConfigGroup();
		ConfigUtils.loadConfig(path, dmcConfig2);

		// Collections that default to empty should remain empty
		assertTrue(dmcConfig2.getLinkAttributeConstraintConfigGroup().getConstrainedModes().isEmpty());
		assertTrue(dmcConfig2.getShapeFileConstraintConfigGroup().getConstrainedModes().isEmpty());
		assertTrue(dmcConfig2.getSubtourConstraintConfig().getConstrainedModes().isEmpty());

		// Collections explicitly set to empty should remain empty
		assertTrue(dmcConfig2.getVehicleTourConstraintConfig().getRestrictedModes().isEmpty());
		assertTrue(dmcConfig2.getVehicleTripConstraintConfig().getRestrictedModes().isEmpty());
		assertTrue(dmcConfig2.getMATSimTripScoringConfigGroup().getPtLegModes().isEmpty());
	}

	@Test
	void testReadWriteConfig() {
		// Create config
		DiscreteModeChoiceConfigGroup dmcConfig = new DiscreteModeChoiceConfigGroup();
		Config config = ConfigUtils.createConfig(dmcConfig);

		dmcConfig.setSelector("unknown selector");
		dmcConfig.getCarModeAvailabilityConfig().setAvailableModes(Arrays.asList("abc", "def"));

		// Write config
		new ConfigWriter(config).write(utils.getOutputDirectory() + "/test_config.xml");

		// Read in again
		DiscreteModeChoiceConfigGroup dmcConfig2 = new DiscreteModeChoiceConfigGroup();
		ConfigUtils.loadConfig(utils.getOutputDirectory() + "/test_config.xml", dmcConfig2);

		assertEquals("unknown selector", dmcConfig2.getSelector());
		assertEquals(new HashSet<>(dmcConfig.getCarModeAvailabilityConfig().getAvailableModes()),
				new HashSet<>(Arrays.asList("abc", "def")));
	}

	@Test
	void testReadWriteConfigMultipleTimes() throws IOException {
		DiscreteModeChoiceConfigGroup dmcConfig = new DiscreteModeChoiceConfigGroup();
		Config config1 = ConfigUtils.createConfig(dmcConfig);

		new ConfigWriter(config1).write(utils.getOutputDirectory() + "/test_config1.xml");

		Config config2 = ConfigUtils.loadConfig(utils.getOutputDirectory() + "/test_config1.xml",
				new DiscreteModeChoiceConfigGroup());
		new ConfigWriter(config2).write(utils.getOutputDirectory() + "/test_config2.xml");

		Config config3 = ConfigUtils.loadConfig(utils.getOutputDirectory() + "/test_config2.xml",
				new DiscreteModeChoiceConfigGroup());
		new ConfigWriter(config3).write(utils.getOutputDirectory() + "/test_config3.xml");

		BufferedReader reader = new BufferedReader(
				new InputStreamReader(new FileInputStream(new File(utils.getOutputDirectory() + "/test_config3.xml"))));

		String line = null;

		int numberOfMultinomialLogit = 0;
		int numberOfVehicleContinuity = 0;

		while ((line = reader.readLine()) != null) {
			if (line.contains("parameterset") && line.contains("selector:MultinomialLogit")) {
				numberOfMultinomialLogit++;
			}

			if (line.contains("parameterset") && line.contains("tourConstraint:VehicleContinuity")) {
				numberOfVehicleContinuity++;
			}
		}

		reader.close();

		assertEquals(1, numberOfMultinomialLogit);
		assertEquals(1, numberOfVehicleContinuity);
	}

	@Test
	void testEmptyStringParsedAsEmptySet() {
		DiscreteModeChoiceConfigGroup dmcConfig = new DiscreteModeChoiceConfigGroup();

		dmcConfig.setTourFiltersAsString("");
		assertTrue(dmcConfig.getTourFilters().isEmpty(), "tourFilters should be empty for empty string input");

		dmcConfig.setTripFiltersAsString("");
		assertTrue(dmcConfig.getTripFilters().isEmpty(), "tripFilters should be empty for empty string input");

		dmcConfig.setTourConstraintsAsString("");
		assertTrue(dmcConfig.getTourConstraints().isEmpty(), "tourConstraints should be empty for empty string input");

		dmcConfig.setTripConstraintsAsString("");
		assertTrue(dmcConfig.getTripConstraints().isEmpty(), "tripConstraints should be empty for empty string input");

		dmcConfig.setCachedModesAsString("");
		assertTrue(dmcConfig.getCachedModes().isEmpty(), "cachedModes should be empty for empty string input");
	}

	@Test
	void testSetTripConstraints() {
		DiscreteModeChoiceConfigGroup dmcConfig1 = new DiscreteModeChoiceConfigGroup();
		dmcConfig1.setTripConstraints(Arrays.asList("A", "B", "C"));

		Config config1 = ConfigUtils.createConfig(dmcConfig1);
		new ConfigWriter(config1).write(utils.getOutputDirectory() + "/test_config.xml");

		DiscreteModeChoiceConfigGroup dmcConfig2 = new DiscreteModeChoiceConfigGroup();
		Config config2 = ConfigUtils.createConfig(dmcConfig2);
		new ConfigReader(config2).readFile(utils.getOutputDirectory() + "/test_config.xml");

		assertTrue(dmcConfig2.getTripConstraints().contains("A"));
		assertTrue(dmcConfig2.getTripConstraints().contains("B"));
		assertTrue(dmcConfig2.getTripConstraints().contains("C"));
	}
}
