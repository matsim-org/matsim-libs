package org.matsim.pt.queuesim;

public class SimpleTransitStopHandlerFactory implements TransitStopHandlerFactory {

	@Override
	public TransitStopHandler createTransitStopHandler() {
		return new SimpleTransitStopHandler();
	}

}
