package org.matsim.facilities;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author thibautd
 */
public class FacilitiesReprojectionIOTest {
	private static final String TEST_FILE = "test/scenarios/chessboard/facilities.xml";

	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testReprojectionAtImport() {
		final Scenario originalScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		final Scenario reprojectedScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

		new MatsimFacilitiesReader( originalScenario ).readFile( TEST_FILE );
		new MatsimFacilitiesReader( new Transformation() , reprojectedScenario ).readFile( TEST_FILE );

		assertScenarioReprojectedCorrectly(originalScenario, reprojectedScenario);
	}

	@Test
	public void testReprojectionAtExport() {
		final Scenario originalScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimFacilitiesReader( originalScenario ).readFile( TEST_FILE );

		final String outFile = utils.getOutputDirectory()+"/facilities.xml.gz";

		new FacilitiesWriter( new Transformation() , originalScenario.getActivityFacilities() ).write( outFile );
		final Scenario reprojectedScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimFacilitiesReader( reprojectedScenario ).readFile( outFile );

		assertScenarioReprojectedCorrectly(originalScenario, reprojectedScenario);
	}

	private void assertScenarioReprojectedCorrectly(Scenario originalScenario, Scenario reprojectedScenario) {
		final ActivityFacilities originalFacilities = originalScenario.getActivityFacilities();
		final ActivityFacilities reprojectedFacilities = reprojectedScenario.getActivityFacilities();

		Assert.assertEquals(
				"unexpected size of reprojected facilities",
				originalFacilities.getFacilities().size(),
				reprojectedFacilities.getFacilities().size() );

		for (Id<ActivityFacility> id : originalFacilities.getFacilities().keySet() ) {
			final ActivityFacility originalFacility = originalFacilities.getFacilities().get( id );
			final ActivityFacility reprojectedFacility = reprojectedFacilities.getFacilities().get( id );

			assertReprojectedCorrectly( originalFacility , reprojectedFacility );
		}
	}

	private void assertReprojectedCorrectly(ActivityFacility originalFacility, ActivityFacility reprojectedFacility) {
		final Coord original = originalFacility.getCoord();
		final Coord transformed = reprojectedFacility.getCoord();

		Assert.assertEquals(
				"wrong reprojected X value",
				original.getX() + 1000 ,
				transformed.getX(),
				MatsimTestUtils.EPSILON );
		Assert.assertEquals(
				"wrong reprojected Y value",
				original.getY() + 1000 ,
				transformed.getY(),
				MatsimTestUtils.EPSILON );
	}

	private static class Transformation implements CoordinateTransformation {
		@Override
		public Coord transform(Coord coord) {
			return new Coord( coord.getX() + 1000 , coord.getY() + 1000 );
		}
	}
}
