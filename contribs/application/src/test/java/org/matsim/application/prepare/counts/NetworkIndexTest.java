package org.matsim.application.prepare.counts;

import org.assertj.core.data.Offset;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;

import static org.assertj.core.api.Assertions.assertThat;

public class NetworkIndexTest {

	@Test
	void angle() {

		GeometryFactory f = JTSFactoryFinder.getGeometryFactory();

		double angle = NetworkIndex.angle(
			f.createLineString(new Coordinate[]{new Coordinate(0, 0), new Coordinate(1, 1)}),
			f.createLineString(new Coordinate[]{new Coordinate(1, 1), new Coordinate(0, 0)})
		);

		assertThat(angle)
			.isEqualTo(Math.PI);

		angle = NetworkIndex.angle(
			f.createLineString(new Coordinate[]{new Coordinate(1, 1), new Coordinate(2, 2)}),
			f.createLineString(new Coordinate[]{new Coordinate(1, 1), new Coordinate(2, 2)})
		);

		assertThat(angle)
			.isEqualTo(0);

		angle = NetworkIndex.angle(
			f.createLineString(new Coordinate[]{new Coordinate(0, 0), new Coordinate(1, 1)}),
			f.createLineString(new Coordinate[]{new Coordinate(1, 1), new Coordinate(2, 0)})
		);

		assertThat(angle)
			.isEqualTo(-Math.PI / 2);

		angle = NetworkIndex.angle(
			f.createLineString(new Coordinate[]{new Coordinate(0, 0), new Coordinate(1, 0)}),
			f.createLineString(new Coordinate[]{new Coordinate(0, 0.1), new Coordinate(1, 0)})
		);

		assertThat(angle)
			.isCloseTo(-0.1, Offset.offset(0.001));

		angle = NetworkIndex.angle(
			f.createLineString(new Coordinate[]{new Coordinate(0, 0), new Coordinate(1, 0)}),
			f.createLineString(new Coordinate[]{new Coordinate(0, -0.1), new Coordinate(1, 0)})
		);

		assertThat(angle)
			.isCloseTo(0.1, Offset.offset(0.001));


		angle = NetworkIndex.angle(
			f.createLineString(new Coordinate[]{new Coordinate(0, 0), new Coordinate(1, 0)}),
			f.createLineString(new Coordinate[]{new Coordinate(1, 0), new Coordinate(0, -0.1)})
		);

		assertThat(angle)
			.isCloseTo(-Math.PI + 0.1, Offset.offset(0.001));


	}
}
