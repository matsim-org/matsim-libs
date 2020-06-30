package example.lsp.lspScoring;

import java.util.Random;

import lsp.events.ServiceEndEvent;
import lsp.eventhandlers.ServiceEndEventHandler;

/*package-private*/ class TipEventHandler implements ServiceEndEventHandler{

	private double tipSum;
	private Random tipRandom;

	/*package-private*/ TipEventHandler() {
		tipRandom = new Random(1);
		tipSum = 0;
	}
	
	@Override
	public void reset(int iteration) {
		tipSum = 0;	
	}

	@Override
	public void handleEvent(ServiceEndEvent event) {
		double tip = tipRandom.nextDouble() * 5;
		tipSum += tip;
	}

	/*package-private*/ double getTip() {
		return tipSum;
	}
}
