package commercialtraffic.integration;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.Carrier;

public interface CarrierJSpritIterations {

    int getNrOfJSpritIterationsForCarrier(Id<Carrier> carrierId);
}
