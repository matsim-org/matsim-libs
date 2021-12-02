package org.matsim.contrib.drt.extension.alonso_mora.algorithm.function.sequence;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.AlonsoMoraRequest;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.function.sequence.InsertiveSequenceGenerator;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.function.sequence.InsertiveSequenceGenerator.IndexCalculator;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.mockito.Mockito;

public class InsertiveSequenceGeneratorTest {
	@Test
	public void testDefaultDelegation() {
		DrtRequest drtRequest = Mockito.mock(DrtRequest.class);

		AlonsoMoraRequest request1 = Mockito.mock(AlonsoMoraRequest.class);
		Mockito.when(request1.getDrtRequests()).thenReturn(Collections.singleton(drtRequest));

		AlonsoMoraRequest request2 = Mockito.mock(AlonsoMoraRequest.class);
		Mockito.when(request2.getDrtRequests()).thenReturn(Collections.singleton(drtRequest));

		AlonsoMoraRequest request3 = Mockito.mock(AlonsoMoraRequest.class);
		Mockito.when(request3.getDrtRequests()).thenReturn(Collections.singleton(drtRequest));

		AlonsoMoraRequest request4 = Mockito.mock(AlonsoMoraRequest.class);
		Mockito.when(request4.getDrtRequests()).thenReturn(Collections.singleton(drtRequest));

		List<AlonsoMoraRequest> requests = new LinkedList<>();

		requests.add(request1);
		requests.add(request2);
		requests.add(request3);
		requests.add(request4);

		IndexCalculator calculator = Mockito.mock(IndexCalculator.class);
		Mockito.when(calculator.getPickupIndex(Mockito.any())).thenReturn(null);
		Mockito.when(calculator.getDropoffIndex(Mockito.any())).thenReturn(null);

		InsertiveSequenceGenerator generator = new InsertiveSequenceGenerator(calculator, Collections.emptySet(),
				requests);

		int partial = 0;
		int complete = 0;

		while (generator.hasNext()) {
			partial++;

			if (generator.isComplete()) {
				complete++;
			}

			generator.advance();
		}

		Assert.assertEquals(7364, partial);
		Assert.assertEquals(2520, complete);
	}

	@Test
	public void testKeepingOrder() {
		DrtRequest drtRequest = Mockito.mock(DrtRequest.class);

		AlonsoMoraRequest request1 = Mockito.mock(AlonsoMoraRequest.class);
		Mockito.when(request1.getDrtRequests()).thenReturn(Collections.singleton(drtRequest));

		AlonsoMoraRequest request2 = Mockito.mock(AlonsoMoraRequest.class);
		Mockito.when(request2.getDrtRequests()).thenReturn(Collections.singleton(drtRequest));

		AlonsoMoraRequest request3 = Mockito.mock(AlonsoMoraRequest.class);
		Mockito.when(request3.getDrtRequests()).thenReturn(Collections.singleton(drtRequest));

		AlonsoMoraRequest request4 = Mockito.mock(AlonsoMoraRequest.class);
		Mockito.when(request4.getDrtRequests()).thenReturn(Collections.singleton(drtRequest));

		List<AlonsoMoraRequest> requests = new LinkedList<>();

		requests.add(request1);
		requests.add(request2);
		requests.add(request3);
		requests.add(request4);

		IndexCalculator calculator = Mockito.mock(IndexCalculator.class);
		Mockito.when(calculator.getPickupIndex(Mockito.any())).thenReturn(null);
		Mockito.when(calculator.getDropoffIndex(Mockito.any())).thenReturn(null);

		// Request 1 + 2 are already on board (2 comes after 1)
		Mockito.when(calculator.getPickupIndex(Mockito.eq(request1))).thenReturn(0);
		Mockito.when(calculator.getDropoffIndex(Mockito.eq(request1))).thenReturn(1);
		Mockito.when(calculator.getPickupIndex(Mockito.eq(request2))).thenReturn(2);
		Mockito.when(calculator.getDropoffIndex(Mockito.eq(request2))).thenReturn(3);

		InsertiveSequenceGenerator generator = new InsertiveSequenceGenerator(calculator, Collections.emptySet(),
				requests);

		int partial = 0;
		int complete = 0;

		while (generator.hasNext()) {
			partial++;

			if (generator.isComplete()) {
				complete++;
			}

			generator.advance();
		}

		Assert.assertEquals(1440, partial);
		Assert.assertEquals(420, complete);
	}
}
