package ch.sbb.matsim.contrib.railsim.qsimengine;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RailsimCalcTest {

	@Test
	public void calc() {

		assertThat(RailsimCalc.calcTraveledDist(5, 2, 0))
			.isEqualTo(10);

		assertThat(RailsimCalc.solveTraveledDist(5, 15, 0))
			.isEqualTo(3);

		double d = RailsimCalc.calcTraveledDist(5, 3, 1);

		assertThat(d)
			.isEqualTo(19.5);

		assertThat(RailsimCalc.solveTraveledDist(5, 19.5, 1))
			.isEqualTo(3);


	}

	@Test
	public void negative() {

		double d = RailsimCalc.calcTraveledDist(5, 5, -1);

		assertThat(d).isEqualTo(12.5);

		assertThat(RailsimCalc.calcTraveledDist(5, 10, -1))
			.isEqualTo(0);

		double t = RailsimCalc.solveTraveledDist(5, 12.5, -1);

		assertThat(t)
			.isEqualTo(5);

	}
}
