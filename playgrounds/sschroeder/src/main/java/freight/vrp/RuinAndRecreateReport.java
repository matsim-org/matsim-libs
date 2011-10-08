package freight.vrp;

import java.util.Collection;

import vrp.algorithms.ruinAndRecreate.RuinAndRecreateEvent;
import vrp.algorithms.ruinAndRecreate.api.RuinAndRecreateListener;
import vrp.basics.Tour;

public class RuinAndRecreateReport implements RuinAndRecreateListener{

	private int nOfIteration = 0;
	
	private double bestResult;
	
	private Collection<Tour> bestSolution;
	
	@Override
	public void inform(RuinAndRecreateEvent event) {
		nOfIteration++;
		bestResult = event.getCurrentResult();
		bestSolution = event.getCurrentSolution();
	}

	@Override
	public void finish() {
		System.out.println("totDistance="+bestResult);
		System.out.println("#tours="+bestSolution.size());
	}
	
	

}
