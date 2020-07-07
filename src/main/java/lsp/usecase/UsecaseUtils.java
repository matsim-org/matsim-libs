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

	public static MainRunCarrierScheduler createDefaultMainRunCarrierScheduler() {
		return UsecaseUtils.createDefaultMainRunCarrierScheduler();
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

	public static class MainRunCarrierAdapterBuilder {

		private Id<Resource>id;
		private Carrier carrier;
		private Id<Link> fromLinkId;
		private Id<Link> toLinkId;
		private ArrayList<LogisticsSolutionElement> clientElements;
		private MainRunCarrierScheduler mainRunScheduler;
		private Network network;

			public static MainRunCarrierAdapterBuilder newInstance(Id<Resource> id, Network network){
				return new MainRunCarrierAdapterBuilder(id,network);
			}

			private MainRunCarrierAdapterBuilder(Id<Resource> id, Network network){
				this.id = id;
				this.clientElements = new ArrayList <LogisticsSolutionElement>();
				this.network = network;
			}

			public MainRunCarrierAdapterBuilder setFromLinkId(Id<Link> fromLinkId){
				this.fromLinkId = fromLinkId;
				return this;
			}

			public MainRunCarrierAdapterBuilder setToLinkId(Id<Link> toLinkId){
				this.toLinkId = toLinkId;
				return this;
			}

			public MainRunCarrierAdapterBuilder setCarrier(Carrier carrier){
				this.carrier = carrier;
				return this;
			}

			public MainRunCarrierAdapterBuilder setMainRunCarrierScheduler(MainRunCarrierScheduler mainRunScheduler){
				this.mainRunScheduler = mainRunScheduler;
				return this;
			}

			public MainRunCarrierAdapter build(){
				return new MainRunCarrierAdapter(this);
			}

		//--- Getter ---
		Id<Resource> getId() {
			return id;
		}

		Carrier getCarrier() {
			return carrier;
		}

		Id<Link> getFromLinkId() {
			return fromLinkId;
		}

		Id<Link> getToLinkId() {
			return toLinkId;
		}

		ArrayList<LogisticsSolutionElement> getClientElements() {
			return clientElements;
		}

		MainRunCarrierScheduler getMainRunScheduler() {
			return mainRunScheduler;
		}

		Network getNetwork() {
			return network;
		}
		}

	public static class ReloadingPointSchedulerBuilder {
		private double capacityNeedLinear;
		private double capacityNeedFixed;

		private ReloadingPointSchedulerBuilder(){
		}

		public static ReloadingPointSchedulerBuilder newInstance(){
			return new ReloadingPointSchedulerBuilder();
		}


		public ReloadingPointSchedulerBuilder setCapacityNeedLinear(double capacityNeedLinear){
			this.capacityNeedLinear = capacityNeedLinear;
			return this;
		}

		public ReloadingPointSchedulerBuilder setCapacityNeedFixed(double capacityNeedFixed){
			this.capacityNeedFixed = capacityNeedFixed;
			return this;
		}

		public ReloadingPointScheduler build(){
			return new ReloadingPointScheduler(this);
		}

		//--- Getter ---
		double getCapacityNeedLinear() {
			return capacityNeedLinear;
		}

		double getCapacityNeedFixed() {
			return capacityNeedFixed;
		}
	}
}
