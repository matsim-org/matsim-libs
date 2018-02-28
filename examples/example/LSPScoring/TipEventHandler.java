package example.LSPScoring;

import java.util.Random;

import lsp.events.ServiceBeginsEvent;
import lsp.events.ServiceBeginsEventHandler;
import lsp.events.ServiceCompletedEvent;
import lsp.events.ServiceCompletedEventHandler;

public class TipEventHandler implements ServiceCompletedEventHandler{

	private double trinkgeldSum;
	private Random trinkgeldRandom;
	
	public TipEventHandler() {
		trinkgeldRandom = new Random(1);
		trinkgeldSum = 0;
	}
	
	@Override
	public void reset(int iteration) {
		trinkgeldSum = 0;	
	}

	@Override
	public void handleEvent(ServiceCompletedEvent event) {
		double trinkgeld = trinkgeldRandom.nextDouble() * 5;
		System.out.println("ServiceEvent " + trinkgeld);
		trinkgeldSum += trinkgeld;
	}

	public double getTrinkgeld() {
		return trinkgeldSum;
	}
}
