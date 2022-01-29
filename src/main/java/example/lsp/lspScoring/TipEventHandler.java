package example.lsp.lspScoring;

import java.util.Random;

import org.matsim.contrib.freight.events.LSPServiceEndEvent;
import org.matsim.contrib.freight.events.eventhandler.LSPServiceEndEventHandler;

/*package-private*/ class TipEventHandler implements LSPServiceEndEventHandler {

	private double tipSum;
	private final Random tipRandom;

	/*package-private*/ TipEventHandler() {
		tipRandom = new Random(1);
		tipSum = 0;
	}
	
	@Override
	public void reset(int iteration) {
		tipSum = 0;	
	}

	@Override
	public void handleEvent(LSPServiceEndEvent event) {
		double tip = tipRandom.nextDouble() * 5;
		tipSum += tip;
	}

	/*package-private*/ double getTip() {
		return tipSum;
	}
}
