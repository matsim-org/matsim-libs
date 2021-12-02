package org.matsim.contrib.drt.extension.alonso_mora.algorithm.function.sequence;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.AlonsoMoraRequest;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.function.sequence.EuclideanSequenceGenerator;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.mockito.Mockito;

public class EuclideanSequenceGeneratorTest {
	private AlonsoMoraRequest mockRequest(double fromLocation, double toLocation, String id) {
		Link fromLink = Mockito.mock(Link.class);
		Mockito.when(fromLink.getCoord()).thenReturn(new Coord(fromLocation, 0.0));

		Link toLink = Mockito.mock(Link.class);
		Mockito.when(toLink.getCoord()).thenReturn(new Coord(toLocation, 0.0));

		DrtRequest drtRequest = Mockito.mock(DrtRequest.class);
		Mockito.when(drtRequest.getId()).thenReturn(Id.create(id, Request.class));

		AlonsoMoraRequest request = Mockito.mock(AlonsoMoraRequest.class);
		Mockito.when(request.getPickupLink()).thenReturn(fromLink);
		Mockito.when(request.getDropoffLink()).thenReturn(toLink);
		Mockito.when(request.getDrtRequests()).thenReturn(Collections.singleton(drtRequest));

		return request;
	}

	@Test
	public void testOneRequests() {
		List<AlonsoMoraRequest> requests = Arrays.asList( //
				mockRequest(200.0, 100.0, "R1") //
		);

		Link vehicleLink = Mockito.mock(Link.class);
		Mockito.when(vehicleLink.getCoord()).thenReturn(new Coord(0.0, 0.0));

		EuclideanSequenceGenerator generator = new EuclideanSequenceGenerator( //
				vehicleLink, Collections.emptySet(), requests);

		int partial = 0;
		int complete = 0;

		while (generator.hasNext()) {
			partial++;

			if (generator.isComplete()) {
				complete++;
			}

			generator.advance();
		}

		Assert.assertEquals(2, partial);
		Assert.assertEquals(1, complete);
	}

	@Test
	public void testTwoRequests() {
		List<AlonsoMoraRequest> requests = Arrays.asList( //
				mockRequest(200.0, 100.0, "R1"), //
				mockRequest(200.0, 600.0, "R2") //
		);

		Link vehicleLink = Mockito.mock(Link.class);
		Mockito.when(vehicleLink.getCoord()).thenReturn(new Coord(0.0, 0.0));

		EuclideanSequenceGenerator generator = new EuclideanSequenceGenerator( //
				vehicleLink, Collections.emptySet(), requests);

		int partial = 0;
		int complete = 0;

		while (generator.hasNext()) {
			partial++;

			if (generator.isComplete()) {
				complete++;
			}

			generator.advance();
		}

		Assert.assertEquals(4, partial);
		Assert.assertEquals(1, complete);
	}

	@Test
	public void testTwoRequestsWithAbort() {
		List<AlonsoMoraRequest> requests = Arrays.asList( //
				mockRequest(200.0, 100.0, "R1"), //
				mockRequest(50.0, 600.0, "R2") //
		);

		Link vehicleLink = Mockito.mock(Link.class);
		Mockito.when(vehicleLink.getCoord()).thenReturn(new Coord(0.0, 0.0));

		EuclideanSequenceGenerator generator = new EuclideanSequenceGenerator( //
				vehicleLink, Collections.emptySet(), requests);

		int partial = 0;
		int complete = 0;

		int callIndex = 0;

		while (generator.hasNext()) {
			partial++;

			if (generator.isComplete()) {
				complete++;
			}

			// System.out.println(generator.get().stream().map(String::valueOf).collect(Collectors.joining(",")));

			if (callIndex == 1) {
				generator.abort();
			} else {
				generator.advance();
			}

			callIndex++;
		}

		Assert.assertEquals(5, partial);
		Assert.assertEquals(1, complete);
	}

	@Test
	public void testAbortAll() {
		List<AlonsoMoraRequest> requests = Arrays.asList( //
				mockRequest(200.0, 100.0, "R1"), //
				mockRequest(50.0, 600.0, "R2") //
		);

		Link vehicleLink = Mockito.mock(Link.class);
		Mockito.when(vehicleLink.getCoord()).thenReturn(new Coord(0.0, 0.0));

		EuclideanSequenceGenerator generator = new EuclideanSequenceGenerator( //
				vehicleLink, Collections.emptySet(), requests);

		while (generator.hasNext()) {
			generator.abort();
		}
	}
}
