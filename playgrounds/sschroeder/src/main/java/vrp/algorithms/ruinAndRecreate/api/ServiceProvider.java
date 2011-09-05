package vrp.algorithms.ruinAndRecreate.api;

import vrp.algorithms.ruinAndRecreate.RuinAndRecreate.Offer;
import vrp.algorithms.ruinAndRecreate.basics.Shipment;

/**
 * 
 * @author stefan schroeder
 *
 */

public interface ServiceProvider {
	
	public abstract Offer requestService(Shipment shipment, double bestKnownPrice);
	
	public abstract void offerGranted(Shipment shipment);

	public abstract void offerRejected(Offer offer);

}
