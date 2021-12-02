package org.matsim.contrib.drt.extension.alonso_mora.algorithm.function.sequence;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.AlonsoMoraRequest;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.function.sequence.ExtensiveSequenceGenerator;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.mockito.Mockito;

public class ExtensiveSequenceGeneratorTest {
	private DrtRequest createDrtRequestMock() {
		DrtRequest request = Mockito.mock(DrtRequest.class);
		return request;
	}

	private AlonsoMoraRequest createAlonsoMoraRequestMock() {
		AlonsoMoraRequest request = Mockito.mock(AlonsoMoraRequest.class);
		Mockito.when(request.getDrtRequests()).thenReturn(Collections.singleton(createDrtRequestMock()));
		return request;
	}

	@Test
	public void testOneRequests() {
		List<AlonsoMoraRequest> requests = new LinkedList<>();
		requests.add(createAlonsoMoraRequestMock());

		ExtensiveSequenceGenerator generator = new ExtensiveSequenceGenerator(Collections.emptySet(), requests);

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
		List<AlonsoMoraRequest> requests = new LinkedList<>();
		requests.add(createAlonsoMoraRequestMock());
		requests.add(createAlonsoMoraRequestMock());

		ExtensiveSequenceGenerator generator = new ExtensiveSequenceGenerator(Collections.emptySet(), requests);

		int partial = 0;
		int complete = 0;

		while (generator.hasNext()) {
			partial++;

			if (generator.isComplete()) {
				complete++;
			}

			generator.advance();
		}

		Assert.assertEquals(18, partial);
		Assert.assertEquals(6, complete);
	}

	@Test
	public void testOneOnboard() {
		List<AlonsoMoraRequest> onboardRequests = new LinkedList<>();
		onboardRequests.add(createAlonsoMoraRequestMock());

		ExtensiveSequenceGenerator generator = new ExtensiveSequenceGenerator(onboardRequests, Collections.emptySet());

		int partial = 0;
		int complete = 0;

		while (generator.hasNext()) {
			partial++;

			if (generator.isComplete()) {
				complete++;
			}

			generator.advance();
		}

		Assert.assertEquals(1, partial);
		Assert.assertEquals(1, complete);
	}

	@Test
	public void testTwoOnboard() {
		List<AlonsoMoraRequest> onboardRequests = new LinkedList<>();
		onboardRequests.add(createAlonsoMoraRequestMock());
		onboardRequests.add(createAlonsoMoraRequestMock());

		ExtensiveSequenceGenerator generator = new ExtensiveSequenceGenerator(onboardRequests, Collections.emptySet());

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
		Assert.assertEquals(2, complete);
	}

	@Test
	public void testTwoRequestsWithOneOnboard() {
		List<AlonsoMoraRequest> onboardRequests = new LinkedList<>();
		AlonsoMoraRequest onboardRequest = createAlonsoMoraRequestMock();
		onboardRequests.add(onboardRequest);

		List<AlonsoMoraRequest> requests = new LinkedList<>();

		requests.add(createAlonsoMoraRequestMock());
		requests.add(createAlonsoMoraRequestMock());

		ExtensiveSequenceGenerator generator = new ExtensiveSequenceGenerator(onboardRequests, requests);

		int partial = 0;
		int complete = 0;

		while (generator.hasNext()) {
			partial++;

			if (generator.isComplete()) {
				complete++;
			}

			generator.advance();
		}

		Assert.assertEquals(89, partial);
		Assert.assertEquals(30, complete);
	}

	@Test
	public void testFourRequests() {
		List<AlonsoMoraRequest> requests = new LinkedList<>();

		requests.add(createAlonsoMoraRequestMock());
		requests.add(createAlonsoMoraRequestMock());
		requests.add(createAlonsoMoraRequestMock());
		requests.add(createAlonsoMoraRequestMock());

		ExtensiveSequenceGenerator generator = new ExtensiveSequenceGenerator(Collections.emptySet(), requests);

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
	public void testTwoRequestsFirstReject() {
		List<AlonsoMoraRequest> requests = new LinkedList<>();
		requests.add(createAlonsoMoraRequestMock());
		requests.add(createAlonsoMoraRequestMock());

		ExtensiveSequenceGenerator generator = new ExtensiveSequenceGenerator(requests, Collections.emptySet());

		Assert.assertTrue(generator.hasNext());
		Assert.assertEquals(1, generator.get().size());

		generator.abort();

		Assert.assertTrue(generator.hasNext());
		Assert.assertEquals(1, generator.get().size());

		generator.advance();

		Assert.assertTrue(generator.hasNext());
		Assert.assertEquals(2, generator.get().size());

		generator.advance();

		Assert.assertFalse(generator.hasNext());
	}
}
