package lsp.usecase;

import lsp.LogisticsSolutionElement;
import lsp.resources.LSPResource;
import lsp.resources.LSPResourceScheduler;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.Carrier;

import java.util.ArrayList;
import java.util.List;

public class UsecaseUtils {

	public static CollectionCarrierScheduler createDefaultCollectionCarrierScheduler() {
		return new CollectionCarrierScheduler();
	}

	public static DistributionCarrierScheduler createDefaultDistributionCarrierScheduler() {
		return new DistributionCarrierScheduler();
	}

	public static MainRunCarrierScheduler createDefaultMainRunCarrierScheduler() {
		return new MainRunCarrierScheduler();
	}

	public static SimpleForwardSolutionScheduler createDefaultSimpleForwardSolutionScheduler( List<LSPResource> resources ) {
		return new SimpleForwardSolutionScheduler(resources);
	}

	public static DeterministicShipmentAssigner createDeterministicShipmentAssigner() {
		return new DeterministicShipmentAssigner();
	}

	public static class CollectionCarrierAdapterBuilder {

		final Id<LSPResource> id;
		Carrier carrier;
		Id<Link> locationLinkId;
		final ArrayList<LogisticsSolutionElement> clientElements;
		CollectionCarrierScheduler collectionScheduler;
		final Network network;

			public static CollectionCarrierAdapterBuilder newInstance(Id<LSPResource> id, Network network){
				return new CollectionCarrierAdapterBuilder(id,network);
			}

			private CollectionCarrierAdapterBuilder(Id<LSPResource> id, Network network){
				this.id = id;
				this.clientElements = new ArrayList<>();
				this.network = network;
			}

			public void setLocationLinkId(Id<Link> locationLinkId){
				this.locationLinkId = locationLinkId;
			}

			public void setCarrier(Carrier carrier){
				this.carrier = carrier;
			}


			public void setCollectionScheduler(CollectionCarrierScheduler collectionCarrierScheduler){
				this.collectionScheduler = collectionCarrierScheduler;
			}

			public CollectionCarrierAdapter build(){
				return new CollectionCarrierAdapter(this);
			}

		}


	public static class DistributionCarrierAdapterBuilder {

		final Id<LSPResource>id;
		Carrier carrier;
		Id<Link> locationLinkId;
		final ArrayList<LogisticsSolutionElement> clientElements;
		DistributionCarrierScheduler distributionHandler;
		final Network network;

			public static DistributionCarrierAdapterBuilder newInstance(Id<LSPResource> id, Network network){
				return new DistributionCarrierAdapterBuilder(id,network);
			}

			private DistributionCarrierAdapterBuilder(Id<LSPResource> id, Network network){
				this.id = id;
				this.clientElements = new ArrayList<>();
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


			public DistributionCarrierAdapterBuilder setDistributionScheduler(DistributionCarrierScheduler distributionCarrierScheduler){
				this.distributionHandler = distributionCarrierScheduler;
				return this;
			}

			public DistributionCarrierAdapter build(){
				return new DistributionCarrierAdapter(this);
			}

		}

	public static class MainRunCarrierAdapterBuilder {

		private final Id<LSPResource>id;
		private Carrier carrier;
		private Id<Link> fromLinkId;
		private Id<Link> toLinkId;
		private final ArrayList<LogisticsSolutionElement> clientElements;
		private MainRunCarrierScheduler mainRunScheduler;
		private final Network network;

			public static MainRunCarrierAdapterBuilder newInstance(Id<LSPResource> id, Network network){
				return new MainRunCarrierAdapterBuilder(id,network);
			}

			private MainRunCarrierAdapterBuilder(Id<LSPResource> id, Network network){
				this.id = id;
				this.clientElements = new ArrayList<>();
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
		Id<LSPResource> getId() {
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

		//--- Getters ---
		double getCapacityNeedLinear() {
			return capacityNeedLinear;
		}

		double getCapacityNeedFixed() {
			return capacityNeedFixed;
		}
	}

	public static class ReloadingPointBuilder {

		private final Id<LSPResource> id;
		private final Id<Link> locationLinkId;
		private ReloadingPointScheduler reloadingScheduler;
		private final ArrayList <LogisticsSolutionElement> clientElements;

		public static ReloadingPointBuilder newInstance(Id<LSPResource> id, Id<Link> locationLinkId){
			return new ReloadingPointBuilder(id,locationLinkId);
		}

		private ReloadingPointBuilder(Id<LSPResource> id, Id<Link> locationLinkId){
			this.id = id;
			this.clientElements = new ArrayList<>();
			this.locationLinkId = locationLinkId;
		}

		public ReloadingPointBuilder setReloadingScheduler( LSPResourceScheduler reloadingPointScheduler){
			this.reloadingScheduler = (ReloadingPointScheduler) reloadingPointScheduler;
			return this;
		}

		public ReloadingPoint build(){
			return new ReloadingPoint(this);
		}

		//--- Getters ---
		Id<LSPResource> getId() {
			return id;
		}

		Id<Link> getLocationLinkId() {
			return locationLinkId;
		}

		ReloadingPointScheduler getReloadingScheduler() {
			return reloadingScheduler;
		}

		ArrayList<LogisticsSolutionElement> getClientElements() {
			return clientElements;
		}

	}
}
