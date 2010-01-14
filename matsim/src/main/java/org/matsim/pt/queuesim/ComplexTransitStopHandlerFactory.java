package org.matsim.pt.queuesim;

public class ComplexTransitStopHandlerFactory implements TransitStopHandlerFactory {

	@Override
	public TransitStopHandler createTransitStopHandler() {
		return new ComplexTransitStopHandler();
	}

}
