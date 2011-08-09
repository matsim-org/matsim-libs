package freight.vrp;

import java.util.Collection;

import vrp.basics.Tour;

public interface InitialSolutionFactory {
	
	public Collection<Tour> createInitialSolution();

}
