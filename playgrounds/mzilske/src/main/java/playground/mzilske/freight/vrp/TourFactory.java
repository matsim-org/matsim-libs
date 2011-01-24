/**
 * 
 */
package playground.mzilske.freight.vrp;

import org.matsim.api.core.v01.Id;

import playground.mzilske.freight.Tour;

/**
 * @author schroeder
 * 
 */
public class TourFactory {
	public Tour createNewTour(Id depot, Id i, Double demand_i, Id j,
			Double demand_j) {
		TourImpl tour = new TourImpl(depot);
		tour.addDemandPointAtIndex(i, demand_i, 1);
		tour.addDemandPointAtIndex(j, demand_j, 2);
		return tour;
	}

	public Tour createNewTour(Id depot, Id i, Double demand_i) {
		TourImpl tour = new TourImpl(depot);
		tour.addDemandPointAtIndex(i, demand_i, 1);
		return tour;
	}

	public Tour createTourCopy(Tour tour) {
		Tour tourCopy = new TourImpl(tour);
		return tourCopy;
	}

	public Tour createNewTour(Node depotNode, Node iNode, Node jNode) {
		
		return null;
	}
}
