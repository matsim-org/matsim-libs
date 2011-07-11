package vrp.algorithms.ruinAndRecreate.api;

import vrp.algorithms.ruinAndRecreate.basics.BestTourBuilder.TourResult;
import vrp.algorithms.ruinAndRecreate.basics.Shipment;
import vrp.basics.Tour;

/**
 * 
 * @author stefan schroeder
 *
 */

public interface TourBuilder {
	TourResult buildTour(Tour tour, Shipment shipment);
}