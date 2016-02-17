package org.matsim.counts;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author thibautd
 */
public class CountsReprojectionIOTest {
	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testInput() {
		final String file = utils.getOutputDirectory()+"/counts.xml";

		final Counts<Link> originalCounts = createDummyCounts();
		new CountsWriter( originalCounts ).write( file );

		final Counts<Link> reprojectedCounts = new Counts();
		new MatsimCountsReader( new Transformation() , reprojectedCounts ).readFile( file );

		assertCountsAreReprojectedCorrectly( originalCounts , reprojectedCounts );
	}

	@Test
	public void testOutput() {
		final String file = utils.getOutputDirectory()+"/counts.xml";

		final Counts<Link> originalCounts = createDummyCounts();
		new CountsWriter( new Transformation() , originalCounts ).write( file );

		final Counts<Link> reprojectedCounts = new Counts();
		new MatsimCountsReader( reprojectedCounts ).readFile( file );

		assertCountsAreReprojectedCorrectly( originalCounts , reprojectedCounts );
	}

	private void assertCountsAreReprojectedCorrectly(
			Counts<Link> originalCounts,
			Counts<Link> reprojectedCounts) {
		Assert.assertEquals(
				"unexpected number of counts",
				originalCounts.getCounts().size(),
				reprojectedCounts.getCounts().size() );

		for ( Id<Link> id : originalCounts.getCounts().keySet() ) {
			final Coord original = originalCounts.getCount( id ).getCoord();
			final Coord transformed = reprojectedCounts.getCount( id ).getCoord();

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
	}

	private Counts<Link> createDummyCounts() {
		final Counts<Link> counts = new Counts<Link>();

		counts.setYear( 1988 );

		for ( double i=0; i < 10; i++ ) {
			for ( double j=0; j < 10; j++ ) {
				final Count<Link> c = counts.createAndAddCount(
						Id.createLinkId( i+"-"+j ),
						i+"-"+j );
				c.setCoord( new Coord( i , j ) );
				c.createVolume( 1 , 0 );
			}
		}

		return counts;
	}

	private static class Transformation implements CoordinateTransformation {
		@Override
		public Coord transform(Coord coord) {
			return new Coord( coord.getX() + 1000 , coord.getY() + 1000 );
		}
	}
}
