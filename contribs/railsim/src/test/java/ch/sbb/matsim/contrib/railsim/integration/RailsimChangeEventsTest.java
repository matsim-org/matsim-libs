package ch.sbb.matsim.contrib.railsim.integration;

import org.junit.jupiter.api.Test;

import java.io.File;

public class RailsimChangeEventsTest extends AbstractIntegrationTest {


	@Test
	void trackClosure() {

		SimulationResult result = runSimulation(new File(utils.getPackageInputDirectory(), "microJunctionCrossLarge"),
			useEvents("trackClosure"), null);

		// Track is closed until 9:00
		assertThat(result)
			.allTrainsArrived()
			.trainHasLastArrival("train3", "9:18:17")
			.trainHasLastArrival("train4", "9:21:27");

	}

	@Test
	void capacityChangeTrainFollowing() {

		SimulationResult result = runSimulation(new File(utils.getPackageInputDirectory(), "microTrainFollowingFixedVsMovingBlock"),
			useEvents("capacityChange"), null);

		// The second trains can surpass the first ones
		assertThat(result)
			.allTrainsArrived()
			.trainHasLastArrival("t1_train1", 30531)
			.trainHasLastArrival("t1_train2", 30372)
			.trainHasLastArrival("t2_train1", 31101)
			.trainHasLastArrival("t2_train2", 30968);

	}

	@Test
	void speedAdjustment() {

		SimulationResult result = runSimulation(new File(utils.getPackageInputDirectory(), "microJunctionCrossLarge"),
			useEvents("speedAdjustment"), null);


		assertThat(result)
			.allTrainsArrived()
			.trainHasLastArrival("train1", 33422.0)
			.trainHasLastArrival("train2", 31732.0)
			.trainHasLastArrival("train3", 34518.0)
			.trainHasLastArrival("train4", 32319.0);

	}

	@Test
	void movingBlock() {

		SimulationResult result = runSimulation(new File(utils.getPackageInputDirectory(), "microTrainFollowingFixedVsMovingBlock"),
			useEvents("speedAdjustment"), null);

		// Train on the second track arrives later than the one on track 1 (due to speed adjustment)
		assertThat(result)
			.allTrainsArrived()
			.trainHasLastArrival("t1_train1", 30531)
			.trainHasLastArrival("t2_train1", 31260);

	}
}
