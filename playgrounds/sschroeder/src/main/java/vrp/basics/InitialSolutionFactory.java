package vrp.basics;

import java.util.Collection;

import vrp.api.Customer;
import vrp.api.VRP;


public interface InitialSolutionFactory {
	
	public Collection<Tour> createInitialSolution(VRP vrp);
	
	public Tour createRoundTour(VRP vrp, Customer from, Customer to);

	public Vehicle createVehicle(VRP vrp, Tour tour);

}
