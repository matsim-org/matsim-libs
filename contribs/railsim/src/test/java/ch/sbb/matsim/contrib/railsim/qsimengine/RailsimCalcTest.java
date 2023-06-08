package ch.sbb.matsim.contrib.railsim.qsimengine;

import org.assertj.core.data.Offset;
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

	@Test
	public void maxSpeed() {

		double dist = 1000;

		double current = 5;
		double f = 0;

		RailsimCalc.SpeedTarget res = RailsimCalc.calcTargetSpeed(dist, 0.5, 0.5,
			5, 30, 0);

		System.out.println(res);

		double timeDecel = (res.targetSpeed() - f) / 0.5;
		double distDecel = RailsimCalc.calcTraveledDist(res.targetSpeed(), timeDecel, -0.5);

		double timeAccel = (res.targetSpeed() - current) / 0.5;
		double distAccel = RailsimCalc.calcTraveledDist(5, timeAccel, 0.5);

		assertThat(distDecel + distAccel)
			.isCloseTo(dist, Offset.offset(0.001));

	}

	@Test
	public void decel() {

		double d = RailsimCalc.calcTargetDecel(1000, 10);

		assertThat(RailsimCalc.calcTraveledDist(10, -10 / d, d))
			.isCloseTo(1000, Offset.offset(0.001));

	}

	@Test
	public void speedForStop() {

		double v = RailsimCalc.calcTargetSpeedForStop(1000, 0.5, 0.5, 0);

		double accelTime = v / 0.5;

		double d1 = RailsimCalc.calcTraveledDist(0, accelTime, 0.5);
		double d2 = RailsimCalc.calcTraveledDist(v, accelTime, -0.5);

		assertThat(d1 + d2)
			.isCloseTo(1000, Offset.offset(0.0001));

	}
}
