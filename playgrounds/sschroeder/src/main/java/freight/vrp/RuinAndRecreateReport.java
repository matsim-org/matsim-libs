package freight.vrp;

import vrp.algorithms.ruinAndRecreate.RuinAndRecreateEvent;
import vrp.algorithms.ruinAndRecreate.api.RuinAndRecreateListener;
import vrp.basics.Tour;

import java.util.Collection;

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
		System.out.println("totalCosts="+Math.round(bestResult));
		System.out.println("#tours="+bestSolution.size());
		for(Tour t : bestSolution){
			System.out.println(t);
			System.out.println("tpCosts=" + round(t.costs.generalizedCosts));
			System.out.println("tpDistance=" + round(t.costs.distance));
			System.out.println("tpTime=" + round(t.costs.time));
		}
	}

	private Long round(double time) {
		return Math.round(time);
	}
	
	

}
