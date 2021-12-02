package org.matsim.contrib.drt.extension.alonso_mora.algorithm.assignment;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.AlonsoMoraRequest;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.AlonsoMoraTrip;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.AlonsoMoraVehicle;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.assignment.AssignmentSolver;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.assignment.GlpkMpsAssignmentSolver;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.function.AlonsoMoraFunction.Result;
import org.mockito.Mockito;

public class GlpkMpsAssignmentSolverTest {
	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Before
	public void checkSolver() {
		Assume.assumeTrue("Checking for availability of GLPK solver", GlpkMpsAssignmentSolver.checkAvailability());
	}

	private AlonsoMoraRequest mockRequest() {
		return Mockito.mock(AlonsoMoraRequest.class);
	}

	private AlonsoMoraVehicle mockVehicle() {
		return Mockito.mock(AlonsoMoraVehicle.class);
	}

	private AlonsoMoraTrip mockTrip(AlonsoMoraVehicle vehicle, double cost, AlonsoMoraRequest... requests) {
		AlonsoMoraTrip trip = Mockito.mock(AlonsoMoraTrip.class);
		Mockito.when(trip.getVehicle()).thenReturn(vehicle);
		Mockito.when(trip.getRequests()).thenReturn(Arrays.asList(requests));

		Result result = Mockito.mock(Result.class);
		Mockito.when(trip.getResult()).thenReturn(result);

		Mockito.when(result.getCost()).thenReturn(cost);

		return trip;
	}

	@Test
	public void testOneVehicleOneRequestExample() throws IOException {
		AssignmentSolver solver = new GlpkMpsAssignmentSolver(9000.0, 9000.0, 1000, temporaryFolder.newFile("problem"),
				temporaryFolder.newFile("solution"));

		AlonsoMoraVehicle vehicle = mockVehicle();
		AlonsoMoraRequest request = mockRequest();
		AlonsoMoraTrip trip = mockTrip(vehicle, 100.0, request);

		List<AlonsoMoraTrip> candidates = Arrays.asList(trip);
		Collection<AlonsoMoraTrip> selection = solver.solve(candidates.stream()).trips;

		Assert.assertEquals(1, selection.size());
		Assert.assertTrue(selection.contains(trip));
	}

	@Test
	public void testTwoIndependentRequests() throws IOException {
		AssignmentSolver solver = new GlpkMpsAssignmentSolver(9000.0, 9000.0, 1000, temporaryFolder.newFile("problem"),
				temporaryFolder.newFile("solution"));

		AlonsoMoraVehicle vehicle1 = mockVehicle();
		AlonsoMoraRequest request1 = mockRequest();
		AlonsoMoraTrip trip1 = mockTrip(vehicle1, 100.0, request1);

		AlonsoMoraVehicle vehicle2 = mockVehicle();
		AlonsoMoraRequest request2 = mockRequest();
		AlonsoMoraTrip trip2 = mockTrip(vehicle2, 200.0, request2);

		List<AlonsoMoraTrip> candidates = Arrays.asList(trip1, trip2);
		Collection<AlonsoMoraTrip> selection = solver.solve(candidates.stream()).trips;

		Assert.assertEquals(2, selection.size());
		Assert.assertTrue(selection.contains(trip1));
		Assert.assertTrue(selection.contains(trip2));
	}

	@Test
	public void testTwoRequestsWithOneVehicle() throws IOException {
		AssignmentSolver solver = new GlpkMpsAssignmentSolver(9000.0, 9000.0, 1000, temporaryFolder.newFile("problem"),
				temporaryFolder.newFile("solution"));

		AlonsoMoraVehicle vehicle = mockVehicle();
		AlonsoMoraRequest request1 = mockRequest();
		AlonsoMoraRequest request2 = mockRequest();

		{
			AlonsoMoraTrip trip1 = mockTrip(vehicle, 100.0, request1);
			AlonsoMoraTrip trip2 = mockTrip(vehicle, 200.0, request2);
			AlonsoMoraTrip trip3 = mockTrip(vehicle, 300.0, request1, request2);

			// Must take trip 3 as the first two are not independent, but penalty leads us
			// to assign two requests

			List<AlonsoMoraTrip> candidates = Arrays.asList(trip1, trip2, trip3);
			Collection<AlonsoMoraTrip> selection = solver.solve(candidates.stream()).trips;

			Assert.assertEquals(1, selection.size());
			Assert.assertTrue(selection.contains(trip3));
		}
	}

	@Test
	public void testTwoRequestsWithOneVehicleLowPenalty() throws IOException {
		AssignmentSolver solver = new GlpkMpsAssignmentSolver(250.0, 250.0, 1000, temporaryFolder.newFile("problem"),
				temporaryFolder.newFile("solution"));

		AlonsoMoraVehicle vehicle = mockVehicle();
		AlonsoMoraRequest request1 = mockRequest();
		AlonsoMoraRequest request2 = mockRequest();

		{
			AlonsoMoraTrip trip1 = mockTrip(vehicle, 100.0, request1);
			AlonsoMoraTrip trip2 = mockTrip(vehicle, 200.0, request2);
			AlonsoMoraTrip trip3 = mockTrip(vehicle, 600.0, request1, request2);

			// Must take trip 1 as trip3 is higher than the penalty.

			List<AlonsoMoraTrip> candidates = Arrays.asList(trip1, trip2, trip3);
			Collection<AlonsoMoraTrip> selection = solver.solve(candidates.stream()).trips;

			Assert.assertEquals(1, selection.size());
			Assert.assertTrue(selection.contains(trip1));
		}
	}
}
