package example.lsp.multipleChains;

import lsp.shipment.LSPShipment;
import lsp.shipment.ShipmentUtils;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierShipment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;

public class MultipleChainsUtils {
	public static RandomLogisticChainShipmentAssigner createRandomLogisticChainShipmentAssigner() {
		return new RandomLogisticChainShipmentAssigner();
	}

	public static RoundRobinLogisticChainShipmentAssigner createRoundRobinLogisticChainShipmentAssigner() {
		return new RoundRobinLogisticChainShipmentAssigner();
	}

	public static PrimaryLogisticChainShipmentAssigner createPrimaryLogisticChainShipmentAssigner() {
		return new PrimaryLogisticChainShipmentAssigner();
	}

	public enum LspPlanTypes {
		SINGLE_ONE_ECHELON_CHAIN("singleOneEchelonChain"),
		SINGLE_TWO_ECHELON_CHAIN("singleTwoEchelonChain"),
		MULTIPLE_ONE_ECHELON_CHAINS("multipleOneEchelonChains"),
		MULTIPLE_TWO_ECHELON_CHAINS("multipleTwoEchelonChains"),
		MULTIPLE_MIXED_ECHELON_CHAINS("multipleMixedEchelonChains");

		private final String label;
		LspPlanTypes(String label) {this.label = label;}

		@Override
		public String toString() {
			return label;
		}

		private static final Map<String, LspPlanTypes > stringToEnum =
				Stream.of(values()).collect(toMap(Object::toString, e -> e));

		public static LspPlanTypes fromString(String label) {
			return stringToEnum.get(label);

		}
	}

	public static Collection<LSPShipment> createLSPShipmentsFromCarrierShipments(Carrier carrier) {
		List<LSPShipment> shipmentList = new ArrayList<>();

		List<CarrierShipment> carrierShipments = carrier.getShipments().values().stream()
				.toList();

		for (CarrierShipment shipment : carrierShipments) {
			ShipmentUtils.LSPShipmentBuilder builder = ShipmentUtils.LSPShipmentBuilder.newInstance(Id.create(shipment.getId().toString(), LSPShipment.class));
			builder.setCapacityDemand(shipment.getSize());
			builder.setFromLinkId(shipment.getFrom());
			builder.setToLinkId(shipment.getTo());
			builder.setStartTimeWindow(shipment.getPickupTimeWindow());
			builder.setEndTimeWindow(shipment.getDeliveryTimeWindow());
			builder.setPickupServiceTime(shipment.getPickupServiceTime());
			builder.setDeliveryServiceTime(shipment.getDeliveryServiceTime());
			shipmentList.add(builder.build());
		}
		return shipmentList;
	}

}
