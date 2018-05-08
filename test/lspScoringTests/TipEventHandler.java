package lspScoringTests;

import java.util.Random;

import lsp.events.ServiceStartEvent;
import lsp.events.ServiceStartEventHandler;
import lsp.events.ServiceEndEvent;
import lsp.events.ServiceEndEventHandler;

public class TipEventHandler implements ServiceEndEventHandler{

	private double tipSum;
	private Random tipRandom;
	
	public TipEventHandler() {
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

	public double getTip() {
		return tipSum;
	}
}
