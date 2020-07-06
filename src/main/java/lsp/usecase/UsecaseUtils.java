package lsp.usecase;

import lsp.LogisticsSolutionElement;
import lsp.resources.Resource;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.Carrier;

import java.util.ArrayList;

public class UsecaseUtils {

	public static CollectionCarrierScheduler createDefaultCollectionCarrierScheduler() {
		return new CollectionCarrierScheduler();
	}

	public static DistributionCarrierScheduler createDefaultDistributionCarrierScheduler() {
		return new DistributionCarrierScheduler();
	}

	public static class CollectionCarrierAdapterBuilder {

		Id<Resource> id;
		Carrier carrier;
		Id<Link> locationLinkId;
		ArrayList<LogisticsSolutionElement> clientElements;
		CollectionCarrierScheduler collectionScheduler;
		Network network;

			public static CollectionCarrierAdapterBuilder newInstance(Id<Resource> id, Network network){
				return new CollectionCarrierAdapterBuilder(id,network);
			}

			private CollectionCarrierAdapterBuilder(Id<Resource> id, Network network){
				this.id = id;
				this.clientElements = new ArrayList <LogisticsSolutionElement>();
				this.network = network;
			}

			public CollectionCarrierAdapterBuilder setLocationLinkId(Id<Link> locationLinkId){
				this.locationLinkId = locationLinkId;
				return this;
			}

			public CollectionCarrierAdapterBuilder setCarrier(Carrier carrier){
				this.carrier = carrier;
				return this;
			}


			public CollectionCarrierAdapterBuilder setCollectionScheduler(CollectionCarrierScheduler collectionHandler){
				this.collectionScheduler = collectionHandler;
				return this;
			}

			public CollectionCarrierAdapter build(){
				return new CollectionCarrierAdapter(this);
			}

		}


	public static class DistributionCarrierAdapterBuilder {

		Id<Resource>id;
		Carrier carrier;
		Id<Link> locationLinkId;
		ArrayList<LogisticsSolutionElement> clientElements;
		DistributionCarrierScheduler distributionHandler;
		Network network;

			public static DistributionCarrierAdapterBuilder newInstance(Id<Resource> id, Network network){
				return new DistributionCarrierAdapterBuilder(id,network);
			}

			private DistributionCarrierAdapterBuilder(Id<Resource> id, Network network){
				this.id = id;
				this.clientElements = new ArrayList <LogisticsSolutionElement>();
				this.network = network;
			}

			public DistributionCarrierAdapterBuilder setLocationLinkId(Id<Link> locationLinkId){
				this.locationLinkId = locationLinkId;
				return this;
			}

			public DistributionCarrierAdapterBuilder setCarrier(Carrier carrier){
				this.carrier = carrier;
				return this;
			}


			public DistributionCarrierAdapterBuilder setDistributionScheduler(DistributionCarrierScheduler distributionHandler){
				this.distributionHandler = distributionHandler;
				return this;
			}

			public DistributionCarrierAdapter build(){
				return new DistributionCarrierAdapter(this);
			}

		}
}
