package org.matsim.contrib.freight.analysis;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.Tour;
import org.matsim.contrib.freight.events.FreightTourEndEvent;
import org.matsim.contrib.freight.events.FreightTourStartEvent;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Kai Martins-Turner (kturner)
 */
public class FreightTimeAndDistanceAnalysisEventsHandler implements BasicEventHandler {

	private final static Logger log = LogManager.getLogger(FreightTimeAndDistanceAnalysisEventsHandler.class);

	private final Scenario scenario;


	public Map<Id<Vehicle>, Double> getVehicle2TourDuration() {
		return vehicle2TourDuration;
	}

	public Map<Id<Vehicle>, Double> getVehicle2TourLength() {
		return vehicle2TourLength;
	}

	public Map<Id<Vehicle>, Id<Carrier>> getVehicleId2CarrierId() {
		return vehicleId2CarrierId;
	}

	public Map<Id<Vehicle>, Id<Tour>> getVehicleId2TourId() {
		return vehicleId2TourId;
	}

	private final Map<Id<Vehicle>, Double> vehicle2TourDuration = new LinkedHashMap<>();
	private final Map<Id<Vehicle>, Double> vehicle2TourLength = new LinkedHashMap<>();

	private final Map<String, Double> tourStartTime = new LinkedHashMap<>();

	private final Map<Id<Vehicle>, Id<Carrier>> vehicleId2CarrierId = new LinkedHashMap<>();
	private final Map<Id<Vehicle>, Id<Tour>> vehicleId2TourId = new LinkedHashMap<>();

	public FreightTimeAndDistanceAnalysisEventsHandler(Scenario scenario) {
		this.scenario = scenario;
	}

	private void handleEvent(FreightTourStartEvent event) {
		// Save time of freight tour start
		final String key = event.getCarrierId().toString() + "_" + event.getTourId().toString();
		tourStartTime.put(key, event.getTime());
	}

	//Fix costs for vehicle usage
	private void handleEvent(FreightTourEndEvent event) {
		final String key = event.getCarrierId().toString() + "_" + event.getTourId().toString();
		double tourDuration = event.getTime() - tourStartTime.get(key);
		vehicle2TourDuration.put(event.getVehicleId(), tourDuration);

		vehicleId2CarrierId.put(event.getVehicleId(), event.getCarrierId());
		vehicleId2TourId.put(event.getVehicleId(), event.getTourId());
	}

	private void handleEvent(LinkEnterEvent event) {
		final double distance = scenario.getNetwork().getLinks().get(event.getLinkId()).getLength();
		vehicle2TourLength.merge(event.getVehicleId(), distance, Double::sum);
	}


	@Override public void handleEvent(Event event) {
//		log.info(event + " ; " +event.getEventType());
		if (event instanceof FreightTourStartEvent freightTourStartEvent) {
			handleEvent(freightTourStartEvent);
		} else if (event instanceof FreightTourEndEvent freightTourEndEvent) {
			handleEvent(freightTourEndEvent);
		} else if (event instanceof LinkEnterEvent linkEnterEvent) {
			handleEvent(linkEnterEvent);
		}
	}


	static void writeTravelTimeAndDistance(String analysisOutputDirectory, Scenario scenario, FreightTimeAndDistanceAnalysisEventsHandler freightTimeAndDistanceAnalysisEventsHandler) throws IOException {
		log.info("Writing out Time & Distance & Costs ...");
		//Travel time and distance per vehicle
		String fileName = analysisOutputDirectory + "TimeDistance_perVehicle.tsv";

		BufferedWriter bw1 = new BufferedWriter(new FileWriter(fileName));

		//Write headline:
		bw1.write("vehicleId \t carrierId \t vehicleTypeId \t tourId \t tourDuration[s] \t travelDistance[m] \t " +
				"costPerSecond[EUR/s] \t costPerMeter[EUR/m] \t fixedCosts[EUR] \t varCostsTime[EUR] \t varCostsDist[EUR] \t totalCosts[EUR]");
		bw1.newLine();

		var vehicle2Duration = freightTimeAndDistanceAnalysisEventsHandler.getVehicle2TourDuration();
		var vehicle2Distance = freightTimeAndDistanceAnalysisEventsHandler.getVehicle2TourLength();
		var vehicleId2CarrierId = freightTimeAndDistanceAnalysisEventsHandler.getVehicleId2CarrierId();
		var vehicleId2TourId = freightTimeAndDistanceAnalysisEventsHandler.getVehicleId2TourId();

		for (Id<Vehicle> vehicleId : vehicle2Duration.keySet()) {

			final Double durationInSeconds = vehicle2Duration.get(vehicleId);
			final Double distanceInMeters = vehicle2Distance.get(vehicleId);

			final VehicleType vehicleType = VehicleUtils.findVehicle(vehicleId, scenario).getType();
			final Double costsPerSecond = vehicleType.getCostInformation().getCostsPerSecond();
			final Double costsPerMeter = vehicleType.getCostInformation().getCostsPerMeter();
			final Double fixedCost = vehicleType.getCostInformation().getFixedCosts();

			final double varCostsTime = durationInSeconds * costsPerSecond;
			final double varCostsDist = distanceInMeters * costsPerMeter;
			final double totalVehCosts = fixedCost + varCostsTime + varCostsDist;

			bw1.write(vehicleId.toString());
			bw1.write("\t" + vehicleId2CarrierId.get(vehicleId));
			bw1.write("\t" + vehicleType.getId().toString());
			bw1.write("\t" + vehicleId2TourId.get(vehicleId));

			bw1.write("\t" + durationInSeconds);
			bw1.write("\t" + distanceInMeters);
			bw1.write("\t" + costsPerSecond);
			bw1.write("\t" + costsPerMeter);
			bw1.write("\t" + fixedCost);
			bw1.write("\t" + varCostsTime);
			bw1.write("\t" + varCostsDist);
			bw1.write("\t" + totalVehCosts);

			bw1.newLine();
		}

		bw1.close();
		log.info("Output written to " + fileName);
	}
}
