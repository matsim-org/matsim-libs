package commercialtraffic.commercialJob;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.Carrier;

public interface CarrierMode {

    String getCarrierMode(Id<Carrier> carrierId);
}
