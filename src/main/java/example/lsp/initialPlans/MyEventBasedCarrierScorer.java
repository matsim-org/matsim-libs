package example.lsp.initialPlans;

import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.carrier.Tour;
import org.matsim.contrib.freight.controler.CarrierScoringFunctionFactory;
import org.matsim.contrib.freight.events.FreightTourEndEvent;
import org.matsim.contrib.freight.events.FreightTourStartEvent;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Kai Martins-Turner (kturner)
 */
class MyEventBasedCarrierScorer implements CarrierScoringFunctionFactory {

	@Inject
	private Network network;

	@Inject
	private Scenario scenario;

	private double toll;

	public ScoringFunction createScoringFunction(Carrier carrier) {
		SumScoringFunction sf = new SumScoringFunction();
		sf.addScoringFunction(new EventBasedScoring());
		sf.addScoringFunction(new LinkBasedTollScoring(carrier, toll));
		return sf;
	}

	void setToll (double toll) {
		this.toll = toll;
	}

	/**
	 * Calculate the carrier's score based on Events.
	 * Currently, it includes:
	 * - fixed costs (using FreightTourEndEvent)
	 * - time-dependent costs (using FreightTourStart- and -EndEvent)
	 * - distance-dependent costs (using LinkEnterEvent)
	 */
	private class EventBasedScoring implements SumScoringFunction.ArbitraryEventScoring {

		final Logger log = LogManager.getLogger(EventBasedScoring.class);
		private double score;
		private final double MAX_SHIFT_DURATION = 8 * 3600;
		private final Map<VehicleType, Double>  vehicleType2TourDuration = new LinkedHashMap<>();
		private final Map<VehicleType, Integer>  vehicleType2ScoredFixCosts = new LinkedHashMap<>();

		private final Map<Id<Tour>, Double> tourStartTime = new LinkedHashMap<>();

		public EventBasedScoring() {
			super();
		}

		@Override public void finish() {
		}

		@Override public double getScore() {
			return score;
		}

		@Override public void handleEvent(Event event) {
			log.warn(event.toString());
			if (event instanceof FreightTourStartEvent freightTourStartEvent) {
				handleEvent(freightTourStartEvent);
			} else if (event instanceof FreightTourEndEvent freightTourEndEvent) {
				handleEvent(freightTourEndEvent);
			} else if (event instanceof LinkEnterEvent linkEnterEvent) {
				handleEvent(linkEnterEvent);
			}
		}

		private void handleEvent(FreightTourStartEvent event) {
			// Save time of freight tour start
			tourStartTime.put(event.getTourId(), event.getTime());
		}

		//Fix costs for vehicle usage
		private void handleEvent(FreightTourEndEvent event) {
			//Fix costs for vehicle usage
			final VehicleType vehicleType = (VehicleUtils.findVehicle(event.getVehicleId(), scenario)).getType();

			double tourDuration = event.getTime() - tourStartTime.get(event.getTourId());
			{ //limit fixexd costs scoring
				if (tourDuration > MAX_SHIFT_DURATION) {
					throw new RuntimeException("Duration of tour is longer than max shift defined in scoring fct, caused by event:"
							+ event + " tourDuration: " + tourDuration + " max shift duration:  " + MAX_SHIFT_DURATION);
				}

				//sum up tour durations
				if (vehicleType2TourDuration.containsKey(vehicleType)) {
					vehicleType2TourDuration.put(vehicleType, vehicleType2TourDuration.get(vehicleType) + tourDuration);
				} else {
					vehicleType2TourDuration.put(vehicleType, tourDuration);
				}

				//scoring needed?
				final double currentNuOfVehiclesNeeded = Math.ceil(vehicleType2TourDuration.get(vehicleType) / MAX_SHIFT_DURATION);
				final Integer nuAlreadyScored = vehicleType2ScoredFixCosts.get(vehicleType);
				if (nuAlreadyScored == null ) {
					log.info("Score fixed costs for vehicle type: " + vehicleType.getId().toString());
					score = score - vehicleType.getCostInformation().getFixedCosts();
					vehicleType2ScoredFixCosts.put(vehicleType, 1);
				} else if (currentNuOfVehiclesNeeded > nuAlreadyScored) {
					log.info("Score fixed costs for vehicle type: " + vehicleType.getId().toString());
					score = score - vehicleType.getCostInformation().getFixedCosts();
					vehicleType2ScoredFixCosts.put(vehicleType, vehicleType2ScoredFixCosts.get(vehicleType) + 1);
				}
			}

			// variable costs per Time
			score = score - (tourDuration * vehicleType.getCostInformation().getCostsPerSecond());
		}

		private void handleEvent(LinkEnterEvent event) {
			final double distance = network.getLinks().get(event.getLinkId()).getLength();
			final VehicleType vehicleType = (VehicleUtils.findVehicle(event.getVehicleId(), scenario)).getType();
			final double costPerMeter = vehicleType.getCostInformation().getCostsPerMeter();
			score = score - (distance * costPerMeter);
		}

	}

	/**
	 * Calculate some toll for drinving on a link
	 * This a lazy implementation of a cordon toll.
	 */
	class LinkBasedTollScoring implements SumScoringFunction.ArbitraryEventScoring {

		final Logger log = LogManager.getLogger(EventBasedScoring.class);
		private final double toll;
		private double score;

		private double tollingCounter;
		private double maxNumberOfTollings; //Begrenze Anzahl der Bemautungen
		private final List<String> vehicleTypesToBeTolled = Arrays.asList("large50");

		public LinkBasedTollScoring(Carrier carrier, double toll) {
			super();
			this.toll = toll;
			for (ScheduledTour scheduledTour : carrier.getSelectedPlan().getScheduledTours()) {
				//Das ist noch nicht Perfekt, weil es besser wäre, dass nur jedes Fahrzeuig wirklich einmal bemautet wird.
				//Aber es begrenzt immerhin auf die Anzahl der Fahrzeuge des Types, die unterwegs sind.
				//Siehe auch untern bei der Bemautung selbst
				//kmt nov '22
				if (vehicleTypesToBeTolled.contains(scheduledTour.getVehicle().getType().getId().toString())) {
					this.maxNumberOfTollings++;
				}
			}
		}

		@Override public void finish() {}

		@Override public double getScore() {
			return score;
		}

		@Override public void handleEvent(Event event) {
			if (event instanceof LinkEnterEvent linkEnterEvent) {
				handleEvent(linkEnterEvent);
			}
		}

		private void handleEvent(LinkEnterEvent event) {
//			List<String> tolledLinkList = Arrays.asList("i(5,5)R");
			List<String> tolledLinkList = Arrays.asList("i(3,4)", "i(3,6)", "i(7,5)R", "i(7,7)R", "j(4,8)R", "j(6,8)R", "j(3,4)", "j(5,4)");

			final VehicleType vehicleType = (VehicleUtils.findVehicle(event.getVehicleId(), scenario)).getType();

			if (tollingCounter < maxNumberOfTollings) {
				if (vehicleTypesToBeTolled.contains(vehicleType.getId().toString())) {
					if (tolledLinkList.contains(event.getLinkId().toString())) {
						log.info("Tolling caused by event: " + event);
						tollingCounter++;
						score = score - toll;
					}
				}
			}
		}
	}
}
