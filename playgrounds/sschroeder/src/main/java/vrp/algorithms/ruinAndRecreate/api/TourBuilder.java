package vrp.algorithms.ruinAndRecreate.api;

import vrp.algorithms.ruinAndRecreate.basics.Shipment;
import vrp.basics.Tour;

/**
 * 
 * @author stefan schroeder
 *
 */

public interface TourBuilder {
	Tour addShipmentAndGetTour(Tour tour, Shipment shipment);
}