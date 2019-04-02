package org.matsim.contrib.freight.carrier;

import java.io.BufferedWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.contrib.freight.carrier.CarrierVehicleType.VehicleCostInformation;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.matsim.vehicles.EngineInformation;

/**
 * A writer that writes carriers and their plans in an xml-file.
 * 
 * @author sschroeder
 *
 */
public class CarrierVehicleTypeWriter extends MatsimXmlWriter {

	private static Logger logger = Logger.getLogger(CarrierVehicleTypeWriter.class);

	private CarrierVehicleTypes vehicleTypes;

	
	public CarrierVehicleTypeWriter(CarrierVehicleTypes carrierVehicleTypes) {
		super();
		this.vehicleTypes = carrierVehicleTypes;
	}

	
	public void write(String filename) {
		logger.info("write vehicle-types");
		try {
			openFile(filename);
			writeXmlHead();
			writeTypes(this.writer);
			close();
			logger.info("done");
		} catch (IOException e) {
			e.printStackTrace();
			logger.error(e);
			System.exit(1);
		}
	}

	private void writeTypes(BufferedWriter writer)throws IOException {
		writer.write("\t<vehicleTypes>\n");
		for(CarrierVehicleType type : vehicleTypes.getVehicleTypes().values()){
			writer.write("\t\t<vehicleType id=\"" + type.getId() + "\">\n");
			writer.write("\t\t\t<description>" + type.getDescription() + "</description>\n");
			EngineInformation engineInformation = type.getEngineInformation();
			if(engineInformation != null) writer.write("\t\t\t<engineInformation fuelType=\"" + engineInformation.getFuelType().toString() + "\" gasConsumption=\"" + engineInformation.getFuelConsumption() + "\"/>\n");
			writer.write("\t\t\t<capacity>" + type.getCarrierVehicleCapacity() + "</capacity>\n");
			VehicleCostInformation vehicleCostInformation = type.getVehicleCostInformation();
			if(vehicleCostInformation == null) throw new IllegalStateException("vehicleCostInformation is missing.");
			writer.write("\t\t\t<costInformation fix=\"" + vehicleCostInformation.getFix() + "\" perMeter=\"" + vehicleCostInformation.getPerDistanceUnit() + 
					"\" perSecond=\"" + vehicleCostInformation.getPerTimeUnit() + "\"/>\n");
			writer.write("\t\t</vehicleType>\n");
		}
		writer.write("\t</vehicleTypes>\n\n");
	}

	
}
