package org.matsim.contrib.drt.estimator;

import com.google.inject.Inject;
import org.matsim.contrib.dvrp.router.DvrpRoutingModule;
import org.matsim.core.modal.ModalInjector;

/**
 * Class to estimate access and egress locations when using a drt service.
 */
public final class DrtAccessEgressEstimator {

	private final DvrpRoutingModule.AccessEgressFacilityFinder finder;

	@Inject
	public DrtAccessEgressEstimator(ModalInjector injector) {

		finder = injector.getModal(DvrpRoutingModule.AccessEgressFacilityFinder.class);

	}

}
