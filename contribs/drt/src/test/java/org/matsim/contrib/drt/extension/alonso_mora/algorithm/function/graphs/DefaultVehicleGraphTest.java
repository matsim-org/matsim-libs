package org.matsim.contrib.drt.extension.alonso_mora.algorithm.function.graphs;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.AlonsoMoraRequest;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.AlonsoMoraTrip;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.AlonsoMoraVehicle;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.function.AlonsoMoraFunction;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.graphs.DefaultVehicleGraph;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.graphs.RequestGraph;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.graphs.VehicleGraph;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.core.utils.collections.Tuple;
import org.mockito.Mockito;

public class DefaultVehicleGraphTest {
	@Test
	public void testSingleRequest() {
		/*-
		 * 
		 * R1
		 * 
		 */

		AlonsoMoraFunction mockFunction = new MockFunction(25.0);
		MockRequest request1 = new MockRequest(10.0);

		MockRequestGraph requestGraph = new MockRequestGraph();

		VehicleGraph graph = new DefaultVehicleGraph(mockFunction, requestGraph, mockVehicle());
		graph.addRequest(request1, 0.0);

		List<AlonsoMoraTrip> trips = graph.stream().collect(Collectors.toList());

		// Only one request, so we only expect one trip
		Assert.assertEquals(1, trips.size());
		Assert.assertEquals(1, trips.stream().filter(t -> t.getLength() == 1).count());
	}

	@Test
	public void testTwoConnectedRequests() {
		/*-
		 * 
		 * R1 - R2
		 * 
		 */

		AlonsoMoraFunction mockFunction = new MockFunction(25.0);
		MockRequest request1 = new MockRequest(10.0);
		MockRequest request2 = new MockRequest(12.0);

		MockRequestGraph requestGraph = new MockRequestGraph();
		requestGraph.add(request1, request2);

		VehicleGraph graph = new DefaultVehicleGraph(mockFunction, requestGraph, mockVehicle());
		graph.addRequest(request1, 0.0);
		graph.addRequest(request2, 0.0);

		List<AlonsoMoraTrip> trips = graph.stream().collect(Collectors.toList());

		// Two connected requests, we have two single rides and one combined
		Assert.assertEquals(3, trips.size());
		Assert.assertEquals(2, trips.stream().filter(t -> t.getLength() == 1).count());
		Assert.assertEquals(1, trips.stream().filter(t -> t.getLength() == 2).count());
	}

	@Test
	public void testTwoUNConnectedRequests() {
		/*-
		 * 
		 * R1       R2
		 * 
		 */

		AlonsoMoraFunction mockFunction = new MockFunction(25.0);
		MockRequest request1 = new MockRequest(10.0);
		MockRequest request2 = new MockRequest(10.0);

		MockRequestGraph requestGraph = new MockRequestGraph();

		VehicleGraph graph = new DefaultVehicleGraph(mockFunction, requestGraph, mockVehicle());
		graph.addRequest(request1, 0.0);
		graph.addRequest(request2, 0.0);

		List<AlonsoMoraTrip> trips = graph.stream().collect(Collectors.toList());

		// Two unconnected requests, we have two single rides
		Assert.assertEquals(2, trips.size());
		Assert.assertEquals(2, trips.stream().filter(t -> t.getLength() == 1).count());
	}

	@Test
	public void testComplexMatching2() {
		/*-
		 * 
		 * R1          R4
		 *  |   \  /   |
		 *  |    R3    |
		 *  |   /  \   |
		 * R2          R5
		 * 
		 */

		AlonsoMoraFunction mockFunction = new MockFunction(25.0);

		MockRequest request1 = new MockRequest(10.1);
		MockRequest request2 = new MockRequest(10.2);
		MockRequest request3 = new MockRequest(10.3);
		MockRequest request4 = new MockRequest(10.4);
		MockRequest request5 = new MockRequest(10.5);

		MockRequestGraph requestGraph = new MockRequestGraph();
		requestGraph.add(request1, request2);
		requestGraph.add(request1, request3);
		requestGraph.add(request2, request3);
		requestGraph.add(request3, request4);
		requestGraph.add(request3, request5);
		requestGraph.add(request4, request5);

		VehicleGraph graph = new DefaultVehicleGraph(mockFunction, requestGraph, mockVehicle());
		graph.addRequest(request1, 0.0);
		graph.addRequest(request2, 0.0);
		graph.addRequest(request3, 0.0);
		graph.addRequest(request4, 0.0);
		graph.addRequest(request5, 0.0);

		List<AlonsoMoraTrip> trips = graph.stream().collect(Collectors.toList());

		// Only two passengers allowed currently (weight 25),
		// we expect 5 single passengers rides and 3 + 3 = 6 two-passengres rides

		Assert.assertEquals(11, trips.size());
		Assert.assertEquals(5, trips.stream().filter(t -> t.getLength() == 1).count());
		Assert.assertEquals(6, trips.stream().filter(t -> t.getLength() == 2).count());
	}

	@Test
	public void testComplexMatching3() {
		/*-
		 * 
		 * R1          R4
		 *  |   \  /   |
		 *  |    R3    |
		 *  |   /  \   |
		 * R2          R5
		 * 
		 */

		AlonsoMoraFunction mockFunction = new MockFunction(100.0);

		MockRequest request1 = new MockRequest(10.1);
		MockRequest request2 = new MockRequest(10.2);
		MockRequest request3 = new MockRequest(10.3);
		MockRequest request4 = new MockRequest(10.4);
		MockRequest request5 = new MockRequest(10.5);

		MockRequestGraph requestGraph = new MockRequestGraph();
		requestGraph.add(request1, request2);
		requestGraph.add(request1, request3);
		requestGraph.add(request2, request3);
		requestGraph.add(request3, request4);
		requestGraph.add(request3, request5);
		requestGraph.add(request4, request5);

		VehicleGraph graph = new DefaultVehicleGraph(mockFunction, requestGraph, mockVehicle());
		graph.addRequest(request1, 0.0);
		graph.addRequest(request2, 0.0);
		graph.addRequest(request3, 0.0);
		graph.addRequest(request4, 0.0);
		graph.addRequest(request5, 0.0);

		List<AlonsoMoraTrip> trips = graph.stream().collect(Collectors.toList());

		// Only two passengers allowed currently (weight 25),
		// we expect 5 single passengers rides and 3 + 3 = 6 two-passengres rides, and 2
		// three-passenger rides

		Assert.assertEquals(13, trips.size());
		Assert.assertEquals(5, trips.stream().filter(t -> t.getLength() == 1).count());
		Assert.assertEquals(6, trips.stream().filter(t -> t.getLength() == 2).count());
		Assert.assertEquals(2, trips.stream().filter(t -> t.getLength() == 3).count());
	}

	@Test
	public void testComplexMatching4() {
		/*-
		 * 
		 * R1          R4
		 *  |   \  /   |  \
		 *  |    R3  - R6  |
		 *  |   /  \   |  /
		 * R2          R5
		 * 
		 */

		AlonsoMoraFunction mockFunction = new MockFunction(100.0);

		MockRequest request1 = new MockRequest(10.1);
		MockRequest request2 = new MockRequest(10.2);
		MockRequest request3 = new MockRequest(10.3);
		MockRequest request4 = new MockRequest(10.4);
		MockRequest request5 = new MockRequest(10.5);
		MockRequest request6 = new MockRequest(10.6);

		MockRequestGraph requestGraph = new MockRequestGraph();
		requestGraph.add(request1, request2);
		requestGraph.add(request1, request3);
		requestGraph.add(request2, request3);
		requestGraph.add(request3, request4);
		requestGraph.add(request3, request5);
		requestGraph.add(request4, request5);

		requestGraph.add(request6, request3);
		requestGraph.add(request6, request4);
		requestGraph.add(request6, request5);

		VehicleGraph graph = new DefaultVehicleGraph(mockFunction, requestGraph, mockVehicle());
		graph.addRequest(request1, 0.0);
		graph.addRequest(request2, 0.0);
		graph.addRequest(request3, 0.0);
		graph.addRequest(request4, 0.0);
		graph.addRequest(request5, 0.0);
		graph.addRequest(request6, 0.0);

		List<AlonsoMoraTrip> trips = graph.stream().collect(Collectors.toList());

		// Only two passengers allowed currently (weight 25),
		// we expect 6 single passengers rides and 3 + 3 + 3 = 9 two-passengres rides,
		// and 4 three-passenger ride, 1 four-passenger-ride

		Assert.assertEquals(21, trips.size());
		Assert.assertEquals(6, trips.stream().filter(t -> t.getLength() == 1).count());
		Assert.assertEquals(9, trips.stream().filter(t -> t.getLength() == 2).count());
		Assert.assertEquals(5, trips.stream().filter(t -> t.getLength() == 3).count());
		Assert.assertEquals(1, trips.stream().filter(t -> t.getLength() == 4).count());
	}

	@Test
	public void testComplexReduction() {
		/*-
		 * 
		 * R1          R4
		 *  |   \  /   |  \
		 *  |    R3  - R6  |
		 *  |   /  \   |  /
		 * R2          R5
		 * 
		 */

		AlonsoMoraFunction mockFunction = new MockFunction(100.0);

		MockRequest request1 = new MockRequest(10.1);
		MockRequest request2 = new MockRequest(10.2);
		MockRequest request3 = new MockRequest(10.3);
		MockRequest request4 = new MockRequest(10.4);
		MockRequest request5 = new MockRequest(10.5);
		MockRequest request6 = new MockRequest(10.6);

		MockRequestGraph requestGraph = new MockRequestGraph();
		requestGraph.add(request1, request2);
		requestGraph.add(request1, request3);
		requestGraph.add(request2, request3);
		requestGraph.add(request3, request4);
		requestGraph.add(request3, request5);
		requestGraph.add(request4, request5);

		requestGraph.add(request6, request3);
		requestGraph.add(request6, request4);
		requestGraph.add(request6, request5);

		VehicleGraph graph = new DefaultVehicleGraph(mockFunction, requestGraph, mockVehicle());
		graph.addRequest(request1, 0.0);
		graph.addRequest(request2, 0.0);
		graph.addRequest(request3, 0.0);
		graph.addRequest(request4, 0.0);
		graph.addRequest(request5, 0.0);
		graph.addRequest(request6, 0.0);

		List<AlonsoMoraTrip> trips = graph.stream().collect(Collectors.toList());

		// Only two passengers allowed currently (weight 25),
		// we expect 6 single passengers rides and 3 + 3 + 3 = 9 two-passengres rides,
		// and 4 three-passenger ride, 1 four-passenger-ride

		Assert.assertEquals(21, trips.size());
		Assert.assertEquals(6, trips.stream().filter(t -> t.getLength() == 1).count());
		Assert.assertEquals(9, trips.stream().filter(t -> t.getLength() == 2).count());
		Assert.assertEquals(5, trips.stream().filter(t -> t.getLength() == 3).count());
		Assert.assertEquals(1, trips.stream().filter(t -> t.getLength() == 4).count());

		// Remove Request 1
		graph.removeRequest(request1);
		trips = graph.stream().collect(Collectors.toList());

		Assert.assertEquals(21 - 4, trips.size());
		Assert.assertEquals(6 - 1, trips.stream().filter(t -> t.getLength() == 1).count());
		Assert.assertEquals(9 - 2, trips.stream().filter(t -> t.getLength() == 2).count());
		Assert.assertEquals(5 - 1, trips.stream().filter(t -> t.getLength() == 3).count());
		Assert.assertEquals(1, trips.stream().filter(t -> t.getLength() == 4).count());

		// Remove Request 3
		graph.removeRequest(request3);
		trips = graph.stream().collect(Collectors.toList());

		Assert.assertEquals(21 - 4 - 9, trips.size());
		Assert.assertEquals(6 - 1 - 1, trips.stream().filter(t -> t.getLength() == 1).count());
		Assert.assertEquals(9 - 2 - 4, trips.stream().filter(t -> t.getLength() == 2).count());
		Assert.assertEquals(5 - 1 - 3, trips.stream().filter(t -> t.getLength() == 3).count());
		Assert.assertEquals(1 - 0 - 1, trips.stream().filter(t -> t.getLength() == 4).count());
	}

	static private class MockFunction implements AlonsoMoraFunction {
		private final double maximumWeight;

		MockFunction(double maximumWeight) {
			this.maximumWeight = maximumWeight;
		}

		@Override
		public boolean checkShareability(AlonsoMoraRequest firstRequest, AlonsoMoraRequest secondRequest, double now) {
			throw new IllegalStateException();
		}

		@Override
		public Optional<Result> calculateRoute(Collection<AlonsoMoraRequest> requests, AlonsoMoraVehicle vehicle,
				double now) {
			int sum = 0;

			for (AlonsoMoraRequest request : requests) {
				sum += ((MockRequest) request).weight;
			}

			if (sum < maximumWeight) {
				return Optional.of(new Result(0.0, Arrays.asList()));
			} else {
				return Optional.empty();
			}
		}

		@Override
		public Optional<Double> checkRelocation(AlonsoMoraVehicle vehicle, Link destination, double now) {
			// TODO Auto-generated method stub
			return null;
		}
	}

	static private class MockRequestGraph implements RequestGraph {
		private List<Tuple<MockRequest, MockRequest>> pairs = new LinkedList<>();

		public void add(MockRequest request1, MockRequest request2) {
			pairs.add(new Tuple<>(request1, request2));
		}

		@Override
		public void addRequest(AlonsoMoraRequest request, double now) {
			throw new IllegalStateException();
		}

		@Override
		public Collection<AlonsoMoraRequest> getShareableRequests(AlonsoMoraRequest request) {
			Set<AlonsoMoraRequest> result = new HashSet<>();

			for (Tuple<MockRequest, MockRequest> pair : pairs) {
				if (pair.getFirst() == request) {
					result.add(pair.getSecond());
				} else if (pair.getSecond() == request) {
					result.add(pair.getFirst());
				}
			}

			return result;
		}

		@Override
		public int getSize() {
			return 0;
		}
	}

	private static class MockRequest implements AlonsoMoraRequest {
		private final double weight;

		MockRequest(double weight) {
			this.weight = weight;
		}

		@Override
		public int compareTo(AlonsoMoraRequest o) {
			return Double.compare(weight, ((MockRequest) o).weight);
		}

		@Override
		public String toString() {
			return String.format("R(%.2f)", weight);
		}

		@Override
		public boolean isPickedUp() {
			throw new IllegalStateException();
		}

		@Override
		public boolean isDroppedOff() {
			throw new IllegalStateException();
		}

		@Override
		public double getLatestAssignmentTime() {
			throw new IllegalStateException();
		}

		@Override
		public void unassign() {
			throw new IllegalStateException();
		}

		@Override
		public void setVehicle(AlonsoMoraVehicle vehicle) {
			throw new IllegalStateException();
		}

		@Override
		public AlonsoMoraVehicle getVehicle() {
			throw new IllegalStateException();
		}

		@Override
		public void setPickupTask(AlonsoMoraVehicle vehicle, DrtStopTask pickupTask) {
			throw new IllegalStateException();
		}

		@Override
		public void setDropoffTask(AlonsoMoraVehicle vehicle, DrtStopTask dropoffTask) {
			throw new IllegalStateException();
		}

		@Override
		public DrtStopTask getPickupTask() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public DrtStopTask getDropoffTask() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public boolean isAssigned() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public Collection<DrtRequest> getDrtRequests() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public int getSize() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public Link getPickupLink() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Link getDropoffLink() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public double getLatestPickupTime() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public double getLatestDropoffTime() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public double getDirectArivalTime() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public double getPlannedPickupTime() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public void setPlannedPickupTime(double plannedPickupTime) {
			// TODO Auto-generated method stub

		}

		@Override
		public double getDirectRideDistance() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public double getEarliestPickupTime() {
			// TODO Auto-generated method stub
			return 0;
		}
	}

	private AlonsoMoraVehicle mockVehicle() {
		DvrpVehicle dvrpVehicle = Mockito.mock(DvrpVehicle.class);
		Mockito.when(dvrpVehicle.getCapacity()).thenReturn(4);

		AlonsoMoraVehicle vehicle = Mockito.mock(AlonsoMoraVehicle.class);
		Mockito.when(vehicle.getVehicle()).thenReturn(dvrpVehicle);

		return vehicle;
	}
}
