package org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents;


import org.matsim.contrib.freight.vrp.basics.Job;
import org.matsim.contrib.freight.vrp.basics.Tour;
import org.matsim.contrib.freight.vrp.basics.Vehicle;

/**
 * offerMaker are basically marginal cost calculators. they determine the locations within
 * a tour resulting in least costs. 
 * calculating mc can be quite expensive, thus the calculator is adaptapted to the problem type.
 * 
 * @author schroeder
 *
 */
public interface OfferMaker {

	OfferData makeOffer(Vehicle vehicle, Tour tour, Job job, double bestKnownPrice);

}
