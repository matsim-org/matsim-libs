package org.matsim.core.mobsim.qsim.qnetsimengine;

import javax.inject.Inject;
import javax.inject.Provider;

class QNetsimEngineDepartureHandlerProvider implements Provider<VehicularDepartureHandler> {
	// yyyyyy should return an interface.  Otherwise one must inherit from the implementation, which we don't like! kai, may'18

	@Inject
	QNetsimEngine qNetsimEngine;

	@Override
	public VehicularDepartureHandler get() {
		return qNetsimEngine.getDepartureHandler();
	}
}
