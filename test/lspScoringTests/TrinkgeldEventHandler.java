package lspScoringTests;

import java.util.Random;

import lsp.events.ServiceStartEvent;
import lsp.events.ServiceStartEventHandler;
import lsp.events.ServiceEndEvent;
import lsp.events.ServiceEndEventHandler;

public class TrinkgeldEventHandler implements ServiceEndEventHandler{

	private double trinkgeldSum;
	private Random trinkgeldRandom;
	
	public TrinkgeldEventHandler() {
		trinkgeldRandom = new Random(1);
		trinkgeldSum = 0;
	}
	
	@Override
	public void reset(int iteration) {
		trinkgeldSum = 0;	
	}

	@Override
	public void handleEvent(ServiceEndEvent event) {
		double trinkgeld = trinkgeldRandom.nextDouble() * 5;
		System.out.println("ServiceEvent " + trinkgeld);
		trinkgeldSum += trinkgeld;
	}

	public double getTrinkgeld() {
		return trinkgeldSum;
	}
}
