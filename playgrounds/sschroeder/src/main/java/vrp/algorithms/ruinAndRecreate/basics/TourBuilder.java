package vrp.algorithms.ruinAndRecreate.basics;

import vrp.algorithms.ruinAndRecreate.basics.BestTourBuilder.TourInformation;
import vrp.basics.Tour;

/**
 * 
 * @author stefan schroeder
 *
 */

interface TourBuilder {
	TourInformation buildTour(Tour tour, Shipment shipment);
}