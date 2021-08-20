package org.matsim.mosaic;

import org.eclipse.mosaic.lib.util.scheduling.Event;
import org.eclipse.mosaic.lib.util.scheduling.EventProcessor;
import org.eclipse.mosaic.rti.api.AbstractFederateAmbassador;
import org.eclipse.mosaic.rti.api.InternalFederateException;
import org.eclipse.mosaic.rti.api.parameters.AmbassadorParameter;

public class MATSimAmbassador extends AbstractFederateAmbassador implements EventProcessor {


	protected MATSimAmbassador(AmbassadorParameter ambassadorParameter) {
		super(ambassadorParameter);
	}

	@Override
	public void processEvent(Event event) throws Exception {

	}

	@Override
	public void initialize(long startTime, long endTime) throws InternalFederateException {
		super.initialize(startTime, endTime);
	}

	@Override
	public boolean isTimeConstrained() {
		return true;
	}

	@Override
	public boolean isTimeRegulating() {
		return true;
	}

	@Override
	public boolean canProcessEvent() {
		return true;
	}

}
