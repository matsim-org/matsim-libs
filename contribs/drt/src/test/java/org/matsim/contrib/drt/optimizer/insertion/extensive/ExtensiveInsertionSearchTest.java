package org.matsim.contrib.drt.optimizer.insertion.extensive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.optimizer.insertion.InsertionTestUtils;
import org.matsim.contrib.drt.optimizer.insertion.RequestVariant;
import org.matsim.contrib.drt.optimizer.insertion.RequestVariants;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.passenger.DrtStopCandidate;
import org.matsim.contrib.drt.passenger.DrtStopCandidates;
import org.matsim.contrib.dvrp.load.IntegerLoadType;
import org.matsim.testcases.fakes.FakeLink;

/**
 * Tests that the extensive insertion search calculates detour paths per request variant.
 */
public class ExtensiveInsertionSearchTest {
	private final IntegerLoadType loadType = new IntegerLoadType("passengers");

	@Test
	void pathsAreCalculatedPerVariant() {
		var request = DrtRequest.newBuilder()
				.fromLink(new FakeLink(Id.createLinkId("from")))
				.toLink(new FakeLink(Id.createLinkId("to")))
				.stopCandidates(new DrtStopCandidates(0, 0,
						List.of(new DrtStopCandidate(new FakeLink(Id.createLinkId("alt")), 60)), List.of()))
				.build();
		List<RequestVariant> variants = RequestVariants.of(request);
		var vehicleEntry = mock(VehicleEntry.class);

		var base1 = InsertionTestUtils.insertionFor(variants.get(0), vehicleEntry, 0, 0, loadType.fromInt(1));
		var alt = InsertionTestUtils.insertionFor(variants.get(1), vehicleEntry, 0, 0, loadType.fromInt(1));
		var base2 = InsertionTestUtils.insertionFor(variants.get(0), vehicleEntry, 1, 1, loadType.fromInt(1));

		var pathCalculator = mock(MultiInsertionDetourPathCalculator.class);
		var baseCache = mock(DetourPathDataCache.class);
		var altCache = mock(DetourPathDataCache.class);
		when(pathCalculator.calculatePaths(variants.get(0).request(), List.of(base1, base2))).thenReturn(baseCache);
		when(pathCalculator.calculatePaths(variants.get(1).request(), List.of(alt))).thenReturn(altCache);

		var result = ExtensiveInsertionSearch.calculatePathsPerVariant(pathCalculator, List.of(base1, alt, base2));

		assertThat(result).containsEntry(variants.get(0).request(), baseCache)
				.containsEntry(variants.get(1).request(), altCache)
				.hasSize(2);
		verify(pathCalculator).calculatePaths(variants.get(0).request(), List.of(base1, base2));
		verify(pathCalculator).calculatePaths(variants.get(1).request(), List.of(alt));
		verifyNoMoreInteractions(pathCalculator);
	}
}
