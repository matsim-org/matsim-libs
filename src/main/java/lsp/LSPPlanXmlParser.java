package lsp;

import com.google.inject.Inject;
import lsp.shipment.LSPShipment;
import lsp.shipment.ShipmentUtils;
import lsp.usecase.UsecaseUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.misc.Time;
import org.matsim.utils.objectattributes.AttributeConverter;
import org.matsim.utils.objectattributes.attributable.AttributesXmlReaderDelegate;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import java.util.Map;
import java.util.Stack;

import static lsp.LSPConstants.*;

class LSPPlanXmlParser extends MatsimXmlParser {

    public static final Logger logger = LogManager.getLogger(LSPPlanXmlParser.class);

    private LSP currentLsp = null;
    private Carrier currentCarrier = null;
    private LSPShipment currentShipment = null;
    private final LSPs lsPs;
    private final CarrierVehicleTypes carrierVehicleTypes;
    private CarrierCapabilities.Builder capabilityBuilder;

    private final AttributesXmlReaderDelegate attributesReader = new AttributesXmlReaderDelegate();
    private org.matsim.utils.objectattributes.attributable.Attributes currAttributes =
            new org.matsim.utils.objectattributes.attributable.AttributesImpl();

    public LSPPlanXmlParser(LSPs lsPs, CarrierVehicleTypes carrierVehicleTypes) {
        super();
        this.lsPs = lsPs;
        this.carrierVehicleTypes = carrierVehicleTypes;
    }

    public void putAttributeConverter(Class<?> clazz, AttributeConverter<?> converter) {
        this.attributesReader.putAttributeConverter(clazz, converter);
    }

    @Inject
    public void putAttributeConverters(Map<Class<?>, AttributeConverter<?>> converters) {
        this.attributesReader.putAttributeConverters(converters);
    }

    @Override
    public void startTag(String name, org.xml.sax.Attributes atts, Stack<String> context) {
        switch (name) {
            case LSP: {
                String lspId = atts.getValue(ID);
                Gbl.assertNotNull(lspId);
                currentLsp = LSPUtils.LSPBuilder.getInstance(Id.create(lspId, LSP.class)).build();
                break;
            }

            case CARRIER: {
                String carrierId = atts.getValue(ID);
                Gbl.assertNotNull(carrierId);
                currentCarrier = CarrierUtils.createCarrier(Id.create(carrierId, Carrier.class));
                break;
            }

            case ATTRIBUTE: {
                currAttributes = currentCarrier.getAttributes();
                Gbl.assertNotNull(currAttributes);
                attributesReader.startTag(name, atts, context, currAttributes);
                break;
            }

            case VEHICLE: {
                String vehicleId = atts.getValue(ID);
                Gbl.assertNotNull(vehicleId);

                String depotLinkId = atts.getValue(DEPOT_LINK_ID);
                Gbl.assertNotNull(depotLinkId);

                String typeId = atts.getValue(TYPE_ID);
                Gbl.assertNotNull(typeId);
                VehicleType vehicleType = this.carrierVehicleTypes.getVehicleTypes().get(Id.create(typeId, VehicleType.class));
                Gbl.assertNotNull(vehicleType);

                CarrierVehicle.Builder vehicleBuilder = CarrierVehicle.Builder.newInstance(Id.create(vehicleId, Vehicle.class), Id.create(depotLinkId, Link.class), vehicleType);
                String startTime = atts.getValue(VEHICLE_EARLIEST_START);
                if (startTime != null) vehicleBuilder.setEarliestStart(parseTimeToDouble(startTime));
                String endTime = atts.getValue(VEHICLE_LATEST_END);
                if (endTime != null) vehicleBuilder.setLatestEnd(parseTimeToDouble(endTime));

                CarrierVehicle vehicle = vehicleBuilder.build();
                capabilityBuilder.addVehicle(vehicle);
                break;
            }

            case HUB: {
                String hubId = atts.getValue(ID);
                Gbl.assertNotNull(hubId);
                String location = atts.getValue(LOCATION);
                Gbl.assertNotNull(location);
                String fixedCost = atts.getValue(FIXED_COST);
                Gbl.assertNotNull(fixedCost);
                LSPResource hubResource = UsecaseUtils.TransshipmentHubBuilder.newInstance(id, locationLinkId, scenario)
                        .build();
                LSPUtils.setFixedCost(hubResource, Double.valueOf(fixedCost));
                break;
            }


            case CAPABILITIES: {
                String fleetSize = atts.getValue(FLEET_SIZE);
                Gbl.assertNotNull(fleetSize);
                this.capabilityBuilder = CarrierCapabilities.Builder.newInstance();
                if (fleetSize.toUpperCase().equals(CarrierCapabilities.FleetSize.FINITE.toString())) {
                    this.capabilityBuilder.setFleetSize(CarrierCapabilities.FleetSize.FINITE);
                } else {
                    this.capabilityBuilder.setFleetSize(CarrierCapabilities.FleetSize.INFINITE);
                }
//                currentCarrier.getCarrierCapabilities().setFleetSize(CarrierCapabilities.FleetSize.valueOf(fleetSize));
            }


            case SHIPMENT: {
                String shipmentId = atts.getValue(ID);
                Gbl.assertNotNull(shipmentId);
                Id<LSPShipment> id = Id.create(shipmentId, LSPShipment.class);

                String from = atts.getValue(FROM);
                Gbl.assertNotNull(from);
                String to = atts.getValue(TO);
                Gbl.assertNotNull(to);
                String sizeString = atts.getValue(SIZE);
                Gbl.assertNotNull(sizeString);
                int size = getInt(sizeString);
                ShipmentUtils.LSPShipmentBuilder shipmentBuilder = ShipmentUtils.LSPShipmentBuilder.newInstance(id);

                shipmentBuilder.setFromLinkId(Id.createLinkId(from));
                shipmentBuilder.setToLinkId(Id.createLinkId(to));
                shipmentBuilder.setCapacityDemand(size);

                String startPickup = atts.getValue(START_PICKUP);
                String endPickup = atts.getValue(END_PICKUP);
                String startDelivery = atts.getValue(START_DELIVERY);
                String endDelivery = atts.getValue(END_DELIVERY);
                String pickupServiceTime = atts.getValue(PICKUP_SERVICE_TIME);
                String deliveryServiceTime = atts.getValue(DELIVERY_SERVICE_TIME);

                if (startPickup != null && endPickup != null)
                    shipmentBuilder.setStartTimeWindow(TimeWindow.newInstance(parseTimeToDouble(startPickup), parseTimeToDouble(endPickup)));
                if (startDelivery != null && endDelivery != null)
                    shipmentBuilder.setEndTimeWindow(TimeWindow.newInstance(parseTimeToDouble(startDelivery), parseTimeToDouble(endDelivery)));
                if (pickupServiceTime != null)
                    shipmentBuilder.setPickupServiceTime(parseTimeToDouble(pickupServiceTime));
                if (deliveryServiceTime != null)
                    shipmentBuilder.setDeliveryServiceTime(parseTimeToDouble(deliveryServiceTime));

                currentShipment = shipmentBuilder.build();
                currentLsp.assignShipmentToLSP(currentShipment);
                break;
            }
        }
    }


        private double parseTimeToDouble (String timeString){
            if (timeString.contains(":")) {
                return Time.parseTime(timeString);
            } else {
                return Double.parseDouble(timeString);
            }
        }

        private int getInt (String value){
            return Integer.parseInt(value);
        }


    }
}