package org.matsim.contrib.drt.extension.alonso_mora.algorithm.function.sequence;

import java.util.Collection;
import java.util.List;

import org.matsim.contrib.drt.extension.alonso_mora.algorithm.AlonsoMoraRequest;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.AlonsoMoraStop;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.AlonsoMoraVehicle;

/**
 * Combined sequence generator as explained by Alonso-Mora et al.
 * 
 * Below a configurable threshold, an extensive search using {#link
 * ExtensiveSequenceGenerator} is performed, while above that threshold the
 * simpler {#link InsertiveSequenceGenerator} is used.
 * 
 * @author sebhoerl
 */
public class CombinedSequenceGenerator implements SequenceGenerator {
	private final SequenceGenerator delegate;

	public CombinedSequenceGenerator(AlonsoMoraVehicle vehicle, Collection<AlonsoMoraRequest> onboardRequests,
			Collection<AlonsoMoraRequest> requests, int insertionStartOccupancy) {
		if (requests.size() + vehicle.getOnboardRequests().size() >= insertionStartOccupancy) {
			this.delegate = new InsertiveSequenceGenerator(vehicle, onboardRequests, requests);
		} else {
			this.delegate = new ExtensiveSequenceGenerator(onboardRequests, requests);
		}
	}

	@Override
	public void advance() {
		delegate.advance();
	}

	@Override
	public void abort() {
		delegate.abort();
	}

	@Override
	public boolean hasNext() {
		return delegate.hasNext();
	}

	@Override
	public List<AlonsoMoraStop> get() {
		return delegate.get();
	}

	@Override
	public boolean isComplete() {
		return delegate.isComplete();
	}

	static public class Factory implements SequenceGeneratorFactory {
		private final int insertionStartOccupancy;

		public Factory(int insertionStartOccupancy) {
			this.insertionStartOccupancy = insertionStartOccupancy;
		}

		@Override
		public SequenceGenerator createGenerator(AlonsoMoraVehicle vehicle,
				Collection<AlonsoMoraRequest> onboardRequests, Collection<AlonsoMoraRequest> requests, double now) {
			return new CombinedSequenceGenerator(vehicle, onboardRequests, requests, insertionStartOccupancy);
		}
	}
}
