/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2022 by the members listed in the COPYING,        *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package lsp.usecase;

import lsp.*;
import lsp.shipment.LSPShipment;
import lsp.shipment.ShipmentPlanElement;
import lsp.shipment.ShipmentUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.VehicleType;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

public class UsecaseUtils {

	private static final Logger log = LogManager.getLogger(UsecaseUtils.class);

	public static CollectionCarrierScheduler createDefaultCollectionCarrierScheduler() {
		return new CollectionCarrierScheduler();
	}

	public static DistributionCarrierScheduler createDefaultDistributionCarrierScheduler() {
		return new DistributionCarrierScheduler();
	}

	public static MainRunCarrierScheduler createDefaultMainRunCarrierScheduler() {
		return new MainRunCarrierScheduler();
	}

	public static SimpleForwardLogisticChainScheduler createDefaultSimpleForwardLogisticChainScheduler(List<LSPResource> resources) {
		return new SimpleForwardLogisticChainScheduler(resources);
	}

	public static SingleLogisticChainShipmentAssigner createSingleLogisticChainShipmentAssigner() {
		return new SingleLogisticChainShipmentAssigner();
	}

	/**
	 * Collects all the vehicleTyps from the different Vehicle of the carrier.
	 * This is needed since we do not use carrier.getCarrierCapabilities().getVehicleTypes() any more as second field to safe vehicleTypes ...
	 * TODO: Maybe move to CarrierUtils in MATSim-libs / freight contrib.
	 * <p>
	 * KMT/Jul22
	 *
	 * @param carrier the carrier
	 * @return Collection of VehicleTypes
	 */
	static Collection<VehicleType> getVehicleTypeCollection(Carrier carrier) {
		Set<VehicleType> vehicleTypeCollection = new HashSet<>();
		for (CarrierVehicle carrierVehicle : carrier.getCarrierCapabilities().getCarrierVehicles().values()) {
			vehicleTypeCollection.add(carrierVehicle.getType());
		}
		return vehicleTypeCollection;
	}

	public static void printShipmentsOfLSP(String outputDir, LSP lsp) {
		System.out.println("Writing out shipments of LSP");
		try (BufferedWriter writer = IOUtils.getBufferedWriter(outputDir + "/" + lsp.getId().toString() + "_shipments.tsv")) {
			final String str0 = "LSP: " + lsp.getId();
			System.out.println(str0);
			writer.write(str0 + "\n");
			for (LSPShipment shipment : lsp.getShipments()) {
				final String str1 = "Shipment: " + shipment ;
				System.out.println(str1);
				writer.write(str1 + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void printResults_shipmentPlan(String outputDir, LSP lsp) {
		System.out.println("Writing out shipmentPlan for LSP");
		try (BufferedWriter writer = IOUtils.getBufferedWriter(outputDir + "/" + lsp.getId().toString() + "_schedules.tsv")) {
			final String str0 = "LSP: " + lsp.getId();
			System.out.println(str0);
			writer.write(str0 + "\n");
			for (LSPShipment shipment : lsp.getShipments()) {
				ArrayList<ShipmentPlanElement> elementList = new ArrayList<>(shipment.getShipmentPlan().getPlanElements().values());
				elementList.sort(ShipmentUtils.createShipmentPlanElementComparator());
				writeShipmentWithPlanElements(writer, shipment, elementList);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void writeShipmentWithPlanElements(BufferedWriter writer, LSPShipment shipment, ArrayList<ShipmentPlanElement> elementList) throws IOException {
		final String str1 = "Shipment: " + shipment ;
		System.out.println(str1);
		writer.write(str1 + "\n");
		for (ShipmentPlanElement element : elementList) {
			final String str2 = element.getLogisticChainElement().getId() + "\t\t" + element.getResourceId() + "\t\t" + element.getElementType() + "\t\t" + element.getStartTime() + "\t\t" + element.getEndTime();
			System.out.println(str2);
			writer.write(str2 + "\n");
		}
		System.out.println();
		writer.write("\n");
	}

	/**
	 * Prints out the log of the shipment - this is not the shipment's plan
	 * Maybe the log will get removed soon. kmt sep/oct'22
	 *
	 * @param outputDir path, defining the location for the results
	 * @param lsp the LSP, for which the results should be written out.
	 */
	public static void printResults_shipmentLog(String outputDir, LSP lsp) {
		System.out.println("Writing out shipmentLog for LSP");
		try (BufferedWriter writer = IOUtils.getBufferedWriter(outputDir + "/" + lsp.getId().toString() + "_shipmentLogs.tsv")) {
			final String str0 = "LSP: " + lsp.getId() ;
			System.out.println(str0);
			writer.write(str0 + "\n");
			for (LSPShipment shipment : lsp.getShipments()) {
				ArrayList<ShipmentPlanElement> elementList = new ArrayList<>(shipment.getLog().getPlanElements().values());
				elementList.sort(ShipmentUtils.createShipmentPlanElementComparator());
				writeShipmentWithPlanElements(writer, shipment, elementList);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void printScores(String outputDir, LSP lsp) {
		System.out.println("Writing out scores for LSP");
		try (BufferedWriter writer = IOUtils.getBufferedWriter(outputDir + "/" + lsp.getId().toString() + "_scores.tsv")) {
			final String str0 = "LSP: " + lsp.getId() ;
			System.out.println(str0);
			writer.write(str0 + "\n");
			final String str1 = "The LSP `` " + lsp.getId() + " ´´ has the following number of plans: " + lsp.getPlans().size()  + "\n The scores are: ";
			System.out.println(str1);
			writer.write(str1 + "\n");
			for (LSPPlan plan : lsp.getPlans()) {
				final String str2 = "Score: " + plan.getScore().toString() ;
				System.out.println(str2);
				writer.write(str2 + "\n");
			}
			final String str3 = "The selected plan has the score: " + lsp.getSelectedPlan().getScore() ;
			System.out.println(str3);
			writer.write(str3 + "\n");
			System.out.println("###");
			writer.write("### \n");

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public enum VehicleReturn {returnToFromLink, endAtToLink}

	public static class CollectionCarrierResourceBuilder {

		final Id<LSPResource> id;
		final ArrayList<LogisticChainElement> clientElements;
		final Network network;
		Carrier carrier;
		Id<Link> locationLinkId;
		CollectionCarrierScheduler collectionScheduler;

		private CollectionCarrierResourceBuilder(Carrier carrier, Network network) {
			this.id = Id.create(carrier.getId().toString(), LSPResource.class);
			setCarrierType(carrier, CARRIER_TYPE.collectionCarrier);
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


	public static class DistributionCarrierResourceBuilder {

		final Id<LSPResource> id;
		final ArrayList<LogisticChainElement> clientElements;
		final Network network;
		Carrier carrier;
		Id<Link> locationLinkId;
		DistributionCarrierScheduler distributionHandler;

		private DistributionCarrierResourceBuilder(Carrier carrier, Network network) {
			this.id = Id.create(carrier.getId().toString(), LSPResource.class);
			setCarrierType(carrier, CARRIER_TYPE.distributionCarrier);
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

	public static class MainRunCarrierResourceBuilder {

		private final Id<LSPResource> id;
		private final ArrayList<LogisticChainElement> clientElements;
		private final Network network;
		private Carrier carrier;
		private Id<Link> fromLinkId;
		private Id<Link> toLinkId;
		private MainRunCarrierScheduler mainRunScheduler;
		private VehicleReturn vehicleReturn;

		private MainRunCarrierResourceBuilder(Id<LSPResource> id, Network network) {
			this.id = id;
			this.clientElements = new ArrayList<>();
			this.network = network;
		}

		public static MainRunCarrierResourceBuilder newInstance(Id<LSPResource> id, Network network) {
			return new MainRunCarrierResourceBuilder(id, network);
		}

		public MainRunCarrierResourceBuilder setCarrier(Carrier carrier) {
			setCarrierType(carrier, CARRIER_TYPE.mainRunCarrier);
			this.carrier = carrier;
			return this;
		}

		public MainRunCarrierResourceBuilder setFromLinkId(Id<Link> fromLinkId) {
			this.fromLinkId = fromLinkId;
			return this;
		}

		public MainRunCarrierResourceBuilder setToLinkId(Id<Link> toLinkId) {
			this.toLinkId = toLinkId;
			return this;
		}
		public MainRunCarrierResourceBuilder setMainRunCarrierScheduler(MainRunCarrierScheduler mainRunScheduler) {
			this.mainRunScheduler = mainRunScheduler;
			return this;
		}

		public MainRunCarrierResourceBuilder setVehicleReturn(VehicleReturn vehicleReturn){
			this.vehicleReturn = vehicleReturn;
			return this;
		}

		public MainRunCarrierResource build() {
			return new MainRunCarrierResource(this);
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

		ArrayList<LogisticChainElement> getClientElements() {
			return clientElements;
		}

		MainRunCarrierScheduler getMainRunScheduler() {
			return mainRunScheduler;
		}

		Network getNetwork() {
			return network;
		}

		VehicleReturn getVehicleReturn() {
			return vehicleReturn;
		}
	}

	public static class TranshipmentHubSchedulerBuilder {
		private double capacityNeedLinear;
		private double capacityNeedFixed;

		private TranshipmentHubSchedulerBuilder() {
		}

		public static TranshipmentHubSchedulerBuilder newInstance() {
			return new TranshipmentHubSchedulerBuilder();
		}

		public TranshipmentHubSchedulerBuilder setCapacityNeedLinear(double capacityNeedLinear) {
			this.capacityNeedLinear = capacityNeedLinear;
			return this;
		}

		public TranshipmentHubSchedulerBuilder setCapacityNeedFixed(double capacityNeedFixed) {
			this.capacityNeedFixed = capacityNeedFixed;
			return this;
		}
		public TransshipmentHubScheduler build() {
			return new TransshipmentHubScheduler(this);
		}

		//--- Getters ---

		double getCapacityNeedLinear() {
			return capacityNeedLinear;
		}

		double getCapacityNeedFixed() {
			return capacityNeedFixed;
		}
	}

	public static final class TransshipmentHubBuilder {

		private final Id<LSPResource> id;
		private final Id<Link> locationLinkId;
		private final ArrayList<LogisticChainElement> clientElements;
		private TransshipmentHubScheduler transshipmentHubScheduler;
		private final Scenario scenario;

		private TransshipmentHubBuilder(Id<LSPResource> id, Id<Link> locationLinkId, Scenario scenario) {
			this.id = id;
			this.clientElements = new ArrayList<>();
			this.locationLinkId = locationLinkId;
			this.scenario = scenario;
		}

		public static TransshipmentHubBuilder newInstance(Id<LSPResource> id, Id<Link> locationLinkId, Scenario scenario) {
			return new TransshipmentHubBuilder(id, locationLinkId, scenario);
		}

		public TransshipmentHubBuilder setTransshipmentHubScheduler(LSPResourceScheduler TranshipmentHubScheduler) {
			this.transshipmentHubScheduler = (TransshipmentHubScheduler) TranshipmentHubScheduler;
			return this;
		}

		public TransshipmentHub build() {
			return new TransshipmentHub(this, scenario);
		}
		//--- Getters ---

		Id<LSPResource> getId() {
			return id;
		}

		Id<Link> getLocationLinkId() {
			return locationLinkId;
		}

		TransshipmentHubScheduler getTransshipmentHubScheduler() {
			return transshipmentHubScheduler;
		}

		ArrayList<LogisticChainElement> getClientElements() {
			return clientElements;
		}

	}

	public enum CARRIER_TYPE {collectionCarrier, mainRunCarrier, distributionCarrier, undefined}

	private static final String CARRIER_TYPE_ATTR = "carrierType" ;

	public static CARRIER_TYPE getCarrierType(Carrier carrier) {
		if (carrier.getAttributes().getAttribute(CARRIER_TYPE_ATTR) instanceof CARRIER_TYPE carrierType) {
			return carrierType;
		} else {
			String result = (String) carrier.getAttributes().getAttribute(CARRIER_TYPE_ATTR);
			if (result == null){
				log.warn("Requested attribute " + CARRIER_TYPE_ATTR + " does not exists. Will return " + CARRIER_TYPE.undefined );
				return CARRIER_TYPE.undefined;
			} else {
				return CARRIER_TYPE.valueOf(result);
			}
		}

	}
	private static void setCarrierType( Carrier carrier,  CARRIER_TYPE carrierType ) {
		carrier.getAttributes().putAttribute(CARRIER_TYPE_ATTR, carrierType ) ;
	}

}
