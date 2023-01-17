package lsp;

import lsp.shipment.LSPShipment;
import lsp.usecase.TransshipmentHub;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.matsim.core.utils.misc.Time;
import org.matsim.utils.objectattributes.attributable.AttributesXmlWriterDelegate;
import org.matsim.vehicles.VehicleType;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Collection;

public class LSPPlanWriter extends MatsimXmlWriter {

    private static final  Logger logger = LogManager.getLogger(LSPPlanWriter.class);
    private final Collection<LSP> lsPs;
    private final AttributesXmlWriterDelegate attributesWriter = new AttributesXmlWriterDelegate();

    public LSPPlanWriter(LSPs lsPs) {
        super();
        this.lsPs = lsPs.getLSPs().values();
    }


    public void write(String filename) {
        logger.info("write lsp");
        try {
            openFile(filename);
            writeXmlHead();

            startLSPs(this.writer);
			for (LSP lsp : lsPs) {
                startLSP(lsp, this.writer);
				writeResources(lsp, this.writer);
                writeShipments(lsp, this.writer);
                writePlans(lsp, this.writer);
                endLSP(this.writer);
			}
            endLSPs(this.writer);

            close();
            logger.info("done");
        } catch ( IOException e) {
            e.printStackTrace();
            logger.error(e);
            System.exit(1);
        }
    }

    private void startLSPs(BufferedWriter writer) throws IOException {
        writer.write("\t<LSPs>\n");
    }

    private void startLSP(LSP lsp, BufferedWriter writer)
            throws IOException {
        writer.write("\t\t<lsp id=\"" + lsp.getId() + "\">\n");
    }


    private void writeResources(LSP lsp, BufferedWriter writer )throws IOException {
        if (lsp.getResources().isEmpty()) return;
        writer.write("\t\t\t<resources>\n");
        for (LSPResource resource : lsp.getResources()) {
            if (resource instanceof TransshipmentHub hub) {
                writer.write("\t\t\t\t<hub ");
                writer.write("id=\"" + hub.getId() + "\" ");
                writer.write("location=\"" + hub.getStartLinkId() + "\" ");
                writer.write("fixedCost=\"" + hub.getAttributes().getAttribute("fixedCost") + "\"/>\n");
            }
            if (resource instanceof LSPCarrierResource carrierResource) {
                writer.write("\t\t\t\t<carrier ");
                writer.write("id=\"" + carrierResource.getId() + "\"/>\n");
                attributesWriter.writeAttributes("\t\t\t\t\t", writer, carrierResource.getCarrier().getAttributes());
                writer.write("\t\t\t\t\t<capabilities fleetSize=\""+ carrierResource.getCarrier().getCarrierCapabilities().getFleetSize() + "\">\n");
                writer.write("\t\t\t\t\t\t<vehicles>\n");
                for (CarrierVehicle v : carrierResource.getCarrier().getCarrierCapabilities().getCarrierVehicles().values()) {
                    Id<VehicleType> vehicleTypeId = v.getVehicleTypeId();
                    if(vehicleTypeId == null) vehicleTypeId = v.getType() == null ? null : v.getType().getId();
                    if(vehicleTypeId == null) throw new IllegalStateException("vehicleTypeId is missing.");
                    writer.write("\t\t\t\t\t\t\t<vehicle id=\"" + v.getId()
                            + "\" depotLinkId=\"" + v.getLinkId()
                            + "\" typeId=\"" + vehicleTypeId
                            + "\" earliestStart=\"" + getTime(v.getEarliestStartTime())
                            + "\" latestEnd=\"" + getTime(v.getLatestEndTime())
                            + "\"/>\n");
                }
                writer.write("\t\t\t\t\t\t</vehicles>\n");
                writer.write("\t\t\t\t\t</capabilities>\n");
                }
            }
        writer.write("\t\t\t</resources>\n\n");
    }

    private void writeShipments(LSP lsp, BufferedWriter writer )throws IOException {
        if(lsp.getShipments().isEmpty()) return;
        writer.write("\t\t\t<shipments>\n");
        for (LSPShipment shipment: lsp.getShipments()) {
            writer.write("\t\t\t\t<shipment ");
            writer.write("id=\"" + shipment.getId() + "\" ");
            writer.write("from=\"" + shipment.getFrom() + "\" ");
            writer.write("to=\"" + shipment.getTo() + "\" ");
            writer.write("size=\"" + shipment.getSize() + "\" ");
            writer.write("startPickup=\"" + shipment.getPickupTimeWindow().getStart() + "\" ");
            writer.write("EndPickup=\"" + shipment.getPickupTimeWindow().getEnd() + "\" ");
            writer.write("StartDelivery=\"" + shipment.getDeliveryTimeWindow().getStart() + "\" ");
            writer.write("EndDelivery=\"" + shipment.getDeliveryTimeWindow().getEnd() + "\" ");
            writer.write("pickupServiceTime=\"" + shipment.getPickupServiceTime() + "\" ");
            writer.write("deliveryServiceTime=\"" + shipment.getDeliveryServiceTime());
            if (shipment.getAttributes().isEmpty()) {
                writer.write("\"/>\n");
            } else {
                writer.write("\">\n");
                this.attributesWriter.writeAttributes("\t\t\t\t\t", writer, shipment.getAttributes());
                writer.write("\t\t\t\t</shipment>\n");
            }
        }
        writer.write("\t\t\t</shipments>\n\n");
    }

    private void writePlans(LSP lsp, BufferedWriter writer)throws IOException {
        if (lsp.getPlans().isEmpty()) return;
        writer.write("\t\t\t<plans>\n");

        for (LSPPlan plan : lsp.getPlans()) {
            writer.write("\t\t\t\t<plan");
            if (plan.getScore() != null) {
                writer.write(" score=\"" + plan.getScore() + "\"");
            }
            if (lsp.getSelectedPlan() != null) {
                if (plan == lsp.getSelectedPlan()) {
                    writer.write(" selected=\"true\"");
                } else {
                    writer.write(" selected=\"false\"");
                }
            } else {
                writer.write(" selected=\"false\"");
            }
            writer.write(">\n");

            for (var logisticChain : plan.getLogisticChain()) {
                for (var chainElement : logisticChain.getLogisticChainElements()) {
                    writer.write("\t\t\t\t\t<resource id=\"" +  chainElement.getResource().getId() + "\"/>\n");
                    writer.write("\t\t\t\t\t\t<shipmentPlans>\n");
                    for (var shipment : logisticChain.getShipments()) {
                        writer.write("\t\t\t\t\t\t\t<shipmentPlan shipmentId=\"" +  shipment.getId() + "\">\n");
                        for (var element : shipment.getShipmentPlan().getPlanElements().values()) {
                            writer.write("\t\t\t\t\t\t\t\telementType=\"" + element.getElementType() + "\" ");
                            writer.write("startTime=\"" + element.getStartTime() + "\" ");
                            writer.write("endTime=\"" + element.getEndTime() +  "\"/>\n");
                        }
                        writer.write("\t\t\t\t\t\t\t</shipmentPlan>\n");
                    }
                    writer.write("\t\t\t\t\t\t</shipmentPlans>\n");
                }
            }
            writer.write("\t\t\t\t</plan>\n");
        }
        writer.write("\t\t\t</plans>\n\n");
    }

    private String getTime(double time) {
        return Time.writeTime(time);
    }

    private void endLSP(BufferedWriter writer) throws IOException {
        writer.write("\t\t</lsp>\n");
    }

    private void endLSPs(BufferedWriter writer) throws IOException {
        writer.write("\t</LSPs>\n");
    }

}