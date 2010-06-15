package playground.wrashid.parkingSearch.planLevel.parkingPrice;

import org.matsim.api.core.v01.Id;

/**
 * As there are not that many parking schemes in the network, we can define a mapping from facility to a ParkingPrice(Scheme).
 * 
 * @author wrashid
 *
 */
public interface ParkingPriceMapping {

	public ParkingPrice getParkingPrice(Id facilityId);
	
}
