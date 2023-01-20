package lsp;

import com.google.inject.Inject;
import lsp.shipment.LSPShipment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.utils.objectattributes.AttributeConverter;
import org.matsim.utils.objectattributes.attributable.AttributesXmlReaderDelegate;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.xml.sax.Attributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

class LSPPlanXmlParser extends MatsimXmlParser {

    public static final Logger logger = LogManager.getLogger(LSPPlanXmlParser.class);
    private final LSPs lsPs;
    private final CarrierVehicleTypes carrierVehicleTypes;
    private final AttributesXmlReaderDelegate attributesReader = new AttributesXmlReaderDelegate();

    public LSPPlanXmlParser(LSPs lsPs, CarrierVehicleTypes carrierVehicleTypes) {
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


    public void startTag(String name, org.xml.sax.Attributes atts, Stack<String> context) {
        case "lsp":
        fleetSize = atts.getValue("id");
        if (fleetSize == null) {
            throw new IllegalStateException("carrierId is missing.");
        }

        this.currentCarrier = CarrierUtils.createCarrier(Id.create(fleetSize, Carrier.class));
        break;
    }



}
