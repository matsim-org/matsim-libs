package org.matsim.contrib.drt.optimizer.insertion.extensive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.optimizer.insertion.InsertionDetourTimeCalculator;
import org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator;
import org.matsim.contrib.drt.optimizer.insertion.InsertionWithDetourData;
import org.matsim.contrib.drt.optimizer.insertion.InsertionWithDetourData.InsertionDetourData;
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

		assertThat(result).containsEntry(0, baseCache)
				.containsEntry(1, altCache)
				.hasSize(2);
		verify(pathCalculator).calculatePaths(variants.get(0).request(), List.of(base1, base2));
		verify(pathCalculator).calculatePaths(variants.get(1).request(), List.of(alt));
		verifyNoMoreInteractions(pathCalculator);
	}

	@Test
	void variantsAreKeyedByVariantNotByRequestObject() {
		// two variants that share one DrtRequest instance must still get separate path data
		var request = DrtRequest.newBuilder()
				.fromLink(new FakeLink(Id.createLinkId("from")))
				.toLink(new FakeLink(Id.createLinkId("to")))
				.build();
		var first = new RequestVariant(request, 0, 0, 0);
		var second = new RequestVariant(request, 30, 0, 1);
		var vehicleEntry = mock(VehicleEntry.class);
		var firstInsertion = InsertionTestUtils.insertionFor(first, vehicleEntry, 0, 0, loadType.fromInt(1));
		var secondInsertion = InsertionTestUtils.insertionFor(second, vehicleEntry, 0, 0, loadType.fromInt(1));

		var pathCalculator = mock(MultiInsertionDetourPathCalculator.class);
		when(pathCalculator.calculatePaths(any(), any())).thenReturn(mock(DetourPathDataCache.class));

		ExtensiveInsertionSearch.calculatePathsPerVariant(pathCalculator, List.of(firstInsertion, secondInsertion));

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<InsertionGenerator.Insertion>> insertionsPerCall = ArgumentCaptor.forClass(List.class);
		verify(pathCalculator, times(2)).calculatePaths(eq(request), insertionsPerCall.capture());
		assertThat(insertionsPerCall.getAllValues()).containsExactly(List.of(firstInsertion), List.of(secondInsertion));
	}

	/**
	 * With lazy path creation, the paths of an earlier variant follow the search tree of the last calculated variant.
	 * The paths of a chosen insertion of an earlier variant are therefore calculated again; times are kept.
	 */
	@Test
	void pathsOfAChosenEarlierVariantAreCalculatedAgain() {
		var variants = RequestVariants.of(requestWithAlternativePickup());
		var vehicleEntry = mock(VehicleEntry.class);
		var insertion = InsertionTestUtils.insertionFor(variants.get(0), vehicleEntry, 0, 0, loadType.fromInt(1));
		var staleData = mock(InsertionDetourData.class);
		var timeInfo = mock(InsertionDetourTimeCalculator.DetourTimeInfo.class);
		var best = new InsertionWithDetourData(insertion, staleData, timeInfo);

		var freshData = mock(InsertionDetourData.class);
		var freshCache = mock(DetourPathDataCache.class);
		when(freshCache.createInsertionDetourData(insertion)).thenReturn(freshData);
		var pathCalculator = mock(MultiInsertionDetourPathCalculator.class);
		when(pathCalculator.calculatePaths(variants.get(0).request(), List.of(insertion))).thenReturn(freshCache);

		// variant 1 was calculated last
		var result = ExtensiveInsertionSearch.withPathsOfItsVariant(pathCalculator, best, 1);

		assertThat(result.insertion).isSameAs(insertion);
		assertThat(result.detourData).isSameAs(freshData);
		assertThat(result.detourTimeInfo).isSameAs(timeInfo);
		verify(pathCalculator).calculatePaths(variants.get(0).request(), List.of(insertion));
	}

	@Test
	void pathsOfTheLastCalculatedVariantAreKept() {
		var variants = RequestVariants.of(requestWithAlternativePickup());
		var insertion = InsertionTestUtils.insertionFor(variants.get(1), mock(VehicleEntry.class), 0, 0,
				loadType.fromInt(1));
		var best = new InsertionWithDetourData(insertion, mock(InsertionDetourData.class),
				mock(InsertionDetourTimeCalculator.DetourTimeInfo.class));
		var pathCalculator = mock(MultiInsertionDetourPathCalculator.class);

		// also the case of a request without candidates (single variant 0)
		assertThat(ExtensiveInsertionSearch.withPathsOfItsVariant(pathCalculator, best, 1)).isSameAs(best);
		verifyNoMoreInteractions(pathCalculator);
	}

	private static DrtRequest requestWithAlternativePickup() {
		return DrtRequest.newBuilder()
				.fromLink(new FakeLink(Id.createLinkId("from")))
				.toLink(new FakeLink(Id.createLinkId("to")))
				.stopCandidates(new DrtStopCandidates(0, 0,
						List.of(new DrtStopCandidate(new FakeLink(Id.createLinkId("alt")), 60)), List.of()))
				.build();
	}
}
