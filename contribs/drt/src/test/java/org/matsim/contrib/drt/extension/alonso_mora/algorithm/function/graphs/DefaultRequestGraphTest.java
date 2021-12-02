package org.matsim.contrib.drt.extension.alonso_mora.algorithm.function.graphs;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.AlonsoMoraRequest;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.AlonsoMoraVehicle;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.function.AlonsoMoraFunction;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.graphs.DefaultRequestGraph;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.graphs.RequestGraph;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.schedule.DrtStopTask;

import com.google.common.base.VerifyException;

public class DefaultRequestGraphTest {
	@Test
	public void testBasicMatching() {
		MockRequest request1 = new MockRequest(1);
		MockRequest request2 = new MockRequest(2);

		AlonsoMoraFunction function = new MockFunction();
		RequestGraph graph = new DefaultRequestGraph(function, new ForkJoinPool(1));

		graph.addRequest(request1, 0.0);

		Assert.assertEquals(0, graph.getShareableRequests(request1).size());
		Assert.assertEquals(0, graph.getShareableRequests(request2).size());

		graph.addRequest(request2, 0.0);

		Assert.assertEquals(1, graph.getShareableRequests(request1).size());
		Assert.assertEquals(1, graph.getShareableRequests(request2).size());

		Assert.assertTrue(graph.getShareableRequests(request1).contains(request2));
		Assert.assertTrue(graph.getShareableRequests(request2).contains(request1));
	}

	@Test(expected = VerifyException.class)
	public void testAddTwice() {
		MockRequest request1 = new MockRequest(1);

		AlonsoMoraFunction function = new MockFunction();
		RequestGraph graph = new DefaultRequestGraph(function, new ForkJoinPool(1));

		graph.addRequest(request1, 0.0);
		graph.addRequest(request1, 0.0);
	}

	@Test
	public void testSelectiveMatching() {
		MockRequest request1 = new MockRequest(1);
		MockRequest request7 = new MockRequest(7);
		MockRequest request8 = new MockRequest(8);

		// 1 and 2 can be matched (weight < 10)
		// 1 and 7 can be matched (weight < 10)
		// 7 and 8 cannot be matched (weight >= 10)

		AlonsoMoraFunction function = new MockFunction();
		RequestGraph graph = new DefaultRequestGraph(function, new ForkJoinPool(1));

		graph.addRequest(request1, 0.0);
		graph.addRequest(request7, 0.0);
		graph.addRequest(request8, 0.0);

		Assert.assertEquals(2, graph.getShareableRequests(request1).size());
		Assert.assertEquals(1, graph.getShareableRequests(request7).size());
		Assert.assertEquals(1, graph.getShareableRequests(request8).size());

		Assert.assertTrue(graph.getShareableRequests(request1).contains(request7));
		Assert.assertTrue(graph.getShareableRequests(request1).contains(request8));
		Assert.assertTrue(graph.getShareableRequests(request7).contains(request1));
		Assert.assertTrue(graph.getShareableRequests(request8).contains(request1));
	}

	private static class MockRequest implements AlonsoMoraRequest {
		private final int id;

		MockRequest(int id) {
			this.id = id;
		}

		@Override
		public int compareTo(AlonsoMoraRequest o) {
			return Integer.compare(id, ((MockRequest) o).id);
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

	private static class MockFunction implements AlonsoMoraFunction {
		@Override
		public boolean checkShareability(AlonsoMoraRequest firstRequest, AlonsoMoraRequest secondRequest, double now) {
			return ((MockRequest) firstRequest).id + ((MockRequest) secondRequest).id < 10;
		}

		@Override
		public Optional<Result> calculateRoute(Collection<AlonsoMoraRequest> requests, AlonsoMoraVehicle vehicle,
				double now) {
			throw new IllegalStateException();
		}

		@Override
		public Optional<Double> checkRelocation(AlonsoMoraVehicle vehicle, Link destination, double now) {
			// TODO Auto-generated method stub
			return null;
		}
	}
}
