package ch.sbb.matsim.contrib.railsim.prototype.analysis;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.api.experimental.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.Vehicle;

import java.io.BufferedWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Ihab Kaddoura
 */
public class TransitEventHandler implements VehicleArrivesAtFacilityEventHandler, VehicleDepartsAtFacilityEventHandler {
	private static final Logger log = LogManager.getLogger(TransitEventHandler.class);

	private final Map<String, Tuple<Double, Double>> vehicleFacilityDeparture2time2delay = new HashMap<>();
	private final Map<String, Tuple<Double, Double>> vehicleFacilityArrival2time2delay = new HashMap<>();
	private final Map<Id<Vehicle>, Double> vehicle2totalTraveltime = new HashMap<>();
	private final Map<Id<Vehicle>, Double> vehicle2previousDepartureTime = new HashMap<>();

	@Override
	public void handleEvent(VehicleDepartsAtFacilityEvent event) {
		String vehicleFacility = event.getVehicleId().toString() + "---" + event.getFacilityId().toString();
		this.vehicleFacilityDeparture2time2delay.put(vehicleFacility, new Tuple<>(event.getTime(), event.getDelay()));
		this.vehicle2previousDepartureTime.put(event.getVehicleId(), event.getTime());
	}

	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		String vehicleFacility = event.getVehicleId().toString() + "---" + event.getFacilityId().toString();
		this.vehicleFacilityArrival2time2delay.put(vehicleFacility, new Tuple<>(event.getTime(), event.getDelay()));

		double tt = event.getTime() - this.vehicle2previousDepartureTime.getOrDefault(event.getVehicleId(), 0.);
		double ttSoFar = this.vehicle2totalTraveltime.getOrDefault(event.getVehicleId(), 0.);
		this.vehicle2totalTraveltime.put(event.getVehicleId(), ttSoFar + tt);
	}

	/**
	 * @return the vehicleFacilityDeparture2time2delay
	 */
	public Map<String, Tuple<Double, Double>> getVehicleFacilityDeparture2time2delay() {
		return vehicleFacilityDeparture2time2delay;
	}

	/**
	 * @return the vehicleFacilityArrival2time2delay
	 */
	public Map<String, Tuple<Double, Double>> getVehicleFacilityArrival2time2delay() {
		return vehicleFacilityArrival2time2delay;
	}

	/**
	 * @param outputDirectory
	 * @param runId
	 */
	public void printResults(String outputDirectory, String runId) {
		final String name = "transitAnalysis";

		if (!outputDirectory.endsWith("/")) outputDirectory = outputDirectory + "/";

		String outputFile = outputDirectory + runId + "." + name + ".csv";

		BufferedWriter writer = IOUtils.getBufferedWriter(outputFile);

		try {
			writer.write("vehicle;facility;type;time;delay");
			writer.newLine();

			for (String vehicleFacility : vehicleFacilityDeparture2time2delay.keySet()) {
				writer.write(vehicleFacility.split("---")[0] + ";" + vehicleFacility.split("---")[1] + ";" + "departure;" + vehicleFacilityDeparture2time2delay.get(vehicleFacility).getFirst() + ";" + vehicleFacilityDeparture2time2delay.get(vehicleFacility).getSecond());
				writer.newLine();
			}
			for (String vehicleFacility : vehicleFacilityArrival2time2delay.keySet()) {
				writer.write(vehicleFacility.split("---")[0] + ";" + vehicleFacility.split("---")[1] + ";" + "arrival;" + vehicleFacilityArrival2time2delay.get(vehicleFacility).getFirst() + ";" + vehicleFacilityArrival2time2delay.get(vehicleFacility).getSecond());
				writer.newLine();
			}
			writer.close();

			log.info("Text info written to file.");
		} catch (Exception e) {
			log.warn("Text info not written to file.");
		}
	}

	/**
	 * @return the vehicle2totalTraveltime
	 */
	public Map<Id<Vehicle>, Double> getVehicle2totalTraveltime() {
		return vehicle2totalTraveltime;
	}

}
