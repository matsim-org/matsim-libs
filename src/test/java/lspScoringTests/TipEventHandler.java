package lspScoringTests;

import java.util.Random;

import lsp.events.LSPServiceEndEvent;
import lsp.eventhandlers.LSPServiceEndEventHandler;

public class TipEventHandler implements LSPServiceEndEventHandler {

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
	public void handleEvent(LSPServiceEndEvent event) {
		double tip = tipRandom.nextDouble() * 5;
		tipSum += tip;
	}

	public double getTip() {
		return tipSum;
	}
}
