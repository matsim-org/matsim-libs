package org.matsim.freight.logistics.resourceImplementations.distributionCarrier;

import java.util.ArrayList;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.logistics.LSPResource;
import org.matsim.freight.logistics.LogisticChainElement;
import org.matsim.freight.logistics.resourceImplementations.ResourceImplementationUtils;

/**
 * @author Kai Martins-Turner (kturner)
 */
public class DistributionCarrierUtils {
    public static DistributionCarrierScheduler createDefaultDistributionCarrierScheduler() {
        return new DistributionCarrierScheduler();
    }

    public static class DistributionCarrierResourceBuilder {

        final Id<LSPResource> id;
        final ArrayList<LogisticChainElement> clientElements;
        final Network network;
        Carrier carrier;
        Id<Link> locationLinkId;
        DistributionCarrierScheduler distributionHandler;

        private DistributionCarrierResourceBuilder(Carrier carrier, Network network) {
            this.id = Id.create(carrier.getId().toString(), LSPResource.class);
            ResourceImplementationUtils.setCarrierType(carrier, ResourceImplementationUtils.CARRIER_TYPE.distributionCarrier);
            this.carrier = carrier;
            this.clientElements = new ArrayList<>();
            this.network = network;
        }

        public static DistributionCarrierResourceBuilder newInstance(Carrier carrier, Network network) {
            return new DistributionCarrierResourceBuilder(carrier, network);
        }

        public DistributionCarrierResourceBuilder setLocationLinkId(Id<Link> locationLinkId) {
            this.locationLinkId = locationLinkId;
            return this;
        }

        public DistributionCarrierResourceBuilder setDistributionScheduler(DistributionCarrierScheduler distributionCarrierScheduler) {
            this.distributionHandler = distributionCarrierScheduler;
            return this;
        }

        public DistributionCarrierResource build() {
            return new DistributionCarrierResource(this);
        }

    }
}
