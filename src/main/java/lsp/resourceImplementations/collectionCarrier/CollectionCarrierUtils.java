package lsp.resourceImplementations.collectionCarrier;

import lsp.LSPResource;
import lsp.LogisticChainElement;
import lsp.resourceImplementations.ResourceImplementationUtils;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.freight.carriers.Carrier;

import java.util.ArrayList;

/**
 * @author Kai Martins-Turner (kturner)
 */
public class CollectionCarrierUtils {
    public static CollectionCarrierScheduler createDefaultCollectionCarrierScheduler() {
        return new CollectionCarrierScheduler();
    }

    public static class CollectionCarrierResourceBuilder {

        final Id<LSPResource> id;
        final ArrayList<LogisticChainElement> clientElements;
        final Network network;
        Carrier carrier;
        Id<Link> locationLinkId;
        CollectionCarrierScheduler collectionScheduler;

        private CollectionCarrierResourceBuilder(Carrier carrier, Network network) {
            this.id = Id.create(carrier.getId().toString(), LSPResource.class);
            ResourceImplementationUtils.setCarrierType(carrier, ResourceImplementationUtils.CARRIER_TYPE.collectionCarrier);
            this.carrier = carrier;
            this.clientElements = new ArrayList<>();
            this.network = network;
        }

        public static CollectionCarrierResourceBuilder newInstance(Carrier carrier, Network network) {
            return new CollectionCarrierResourceBuilder(carrier, network);
        }

        public CollectionCarrierResourceBuilder setLocationLinkId(Id<Link> locationLinkId) {
            this.locationLinkId = locationLinkId;
            return this;
        }

        public CollectionCarrierResourceBuilder setCollectionScheduler(CollectionCarrierScheduler collectionCarrierScheduler) {
            this.collectionScheduler = collectionCarrierScheduler;
            return this;
        }

        public CollectionCarrierResource build() {
            return new CollectionCarrierResource(this);
        }
    }
}
