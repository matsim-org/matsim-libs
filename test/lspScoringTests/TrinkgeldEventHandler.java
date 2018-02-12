package lspScoringTests;

import java.util.Random;

import events.ServiceCompletedEvent;
import events.ServiceCompletedEventHandler;

public class TrinkgeldEventHandler implements ServiceCompletedEventHandler{

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
	public void handleEvent(ServiceCompletedEvent event) {
		double trinkgeld = trinkgeldRandom.nextDouble() * 5;
		trinkgeldSum += trinkgeld;
	}

	public double getTrinkgeld() {
		return trinkgeldSum;
	}
}
