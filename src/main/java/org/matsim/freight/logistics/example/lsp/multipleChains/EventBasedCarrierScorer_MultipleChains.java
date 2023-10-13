package org.matsim.freight.logistics.example.lsp.multipleChains;

import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.Tour;
import org.matsim.freight.carriers.controler.CarrierScoringFunctionFactory;
import org.matsim.freight.carriers.events.CarrierTourEndEvent;
import org.matsim.freight.carriers.events.CarrierTourStartEvent;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.util.*;

/**
 * @author Kai Martins-Turner (kturner)
 */
class EventBasedCarrierScorer_MultipleChains implements CarrierScoringFunctionFactory {

	@Inject
	private Network network;

	@Inject
	private Scenario scenario;

	private double toll;

	public ScoringFunction createScoringFunction(Carrier carrier) {
		SumScoringFunction sf = new SumScoringFunction();
		sf.addScoringFunction(new EventBasedScoring());
		sf.addScoringFunction(new LinkBasedTollScoring(toll));
		return sf;
	}

	void setToll (double toll) {
		this.toll = toll;
	}

	/**
	 * Calculate the carrier's score based on Events.
	 * Currently, it includes:
	 * - fixed costs (using CarrierTourEndEvent)
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
			log.debug(event.toString());
			if (event instanceof CarrierTourStartEvent freightTourStartEvent) {
				handleEvent(freightTourStartEvent);
			} else if (event instanceof CarrierTourEndEvent freightTourEndEvent) {
				handleEvent(freightTourEndEvent);
			} else if (event instanceof LinkEnterEvent linkEnterEvent) {
				handleEvent(linkEnterEvent);
			}
		}

		private void handleEvent(CarrierTourStartEvent event) {
			// Save time of freight tour start
			tourStartTime.put(event.getTourId(), event.getTime());
		}

		//Fix costs for vehicle usage
		private void handleEvent(CarrierTourEndEvent event) {
			//Fix costs for vehicle usage
			final VehicleType vehicleType = (VehicleUtils.findVehicle(event.getVehicleId(), scenario)).getType();

			double tourDuration = event.getTime() - tourStartTime.get(event.getTourId());

			log.info("Score fixed costs for vehicle type: " + vehicleType.getId().toString());
			score = score - vehicleType.getCostInformation().getFixedCosts();

			// variable costs per time
			score = score - (tourDuration * vehicleType.getCostInformation().getCostsPerSecond());
		}

		private void handleEvent(LinkEnterEvent event) {
			final double distance = network.getLinks().get(event.getLinkId()).getLength();
			final double costPerMeter = (VehicleUtils.findVehicle(event.getVehicleId(), scenario)).getType().getCostInformation().getCostsPerMeter();
			// variable costs per distance
			score = score - (distance * costPerMeter);
		}

	}

	/**
	 * Calculate some toll for driving on a link
	 * This a lazy implementation of a cordon toll.
	 * A vehicle is only tolled once.
	 */
	class LinkBasedTollScoring implements SumScoringFunction.ArbitraryEventScoring {

		final Logger log = LogManager.getLogger(EventBasedScoring.class);

		private final double toll;
		private double score;

//		private final List<String> vehicleTypesToBeTolled = Arrays.asList("large50");
private final List<String> vehicleTypesToBeTolled = List.of("heavy40t");

		private final List<Id<Vehicle>> tolledVehicles = new ArrayList<>();

		public LinkBasedTollScoring(double toll) {
			super();
			this.toll = toll;
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

//		private void handleEvent(LinkEnterEvent event) {
//			List<String> tolledLinkList = Arrays.asList("i(3,4)", "i(3,6)", "i(7,5)R", "i(7,7)R", "j(4,8)R", "j(6,8)R", "j(3,4)", "j(5,4)");
//
//			final Id<VehicleType> vehicleTypeId = (VehicleUtils.findVehicle(event.getVehicleId(), scenario)).getType().getId();
//
//			//toll a vehicle only once.
//			if (!tolledVehicles.contains(event.getVehicleId()))
//				if (vehicleTypesToBeTolled.contains(vehicleTypeId.toString())) {
//					if (tolledLinkList.contains(event.getLinkId().toString())) {
//						log.info("Tolling caused by event: " + event);
//						tolledVehicles.add(event.getVehicleId());
//						score = score - toll;
//					}
//				}
//		}

		private void handleEvent(LinkEnterEvent event) {
			List<String> tolledLinkList = Arrays.asList("70831","14691","49319","70830","17284","65008","65007","62413","17283","144164","144165","4606","118311","4607","15423","53820","15422","138286","69167","138287","17057","74648","74647","113641","10307","10306","51775","155051","51776","150042","150043","150164","90583","96329","19320","132511","19321","64851","144180","34042","124770","34041","74891","144184","124769","35018","35017","77379","35256","108717","113640","157261","142799","157262","52995","934","52996","935","95587","95588","17150","147460","147461","54024","54023","152801","144506","145715","144505","156464","17125","17126","114545","114546","140792","17127","17248","17128","17249","156458","35463","159609","35462","159608","22046","154715","22047","144373","154716","155927","155926","144372","96330","61139","98190","144126","144127","61011","61010","156463","63682","47555","73006","94867","138930","94866","133488","138931","47554","73005","58893","116395","116394","144136","1158","1157","58894","61269","79237","144137","732","149702","733","77854","4785","55946","77855","4786","55945","90018","61264","61263","86201","77738","120646","77739","26507","108414","108415","17115","66841","26506","78255","78254","118561","35447","147535","17116","118560","61270","102480","51917","62494","72973","51918","72972","72050","72051","147027","33258","61169","18419","102479","20863","61170","43048","43049","69459","73037","18420","69458","3255","3254","73036","27017","76094","41429","74241","76095","149583","74240","35426","81688","81689","12686","25848","25849","64459","115416","149592","74374","115417","81474","81475","36983","36984","36985","36986","52917","52918","64460","40311","108695","40310","79385","119212","155909","119213","119334","119335","112023","48277","48278","106946","91853","91854","102288","69129","102287","13607","2985","64482","156612","8983","156613","67517","28548","28549","83543","145734","83542","149536","149537","151175","151174","18159","8994","93250","147370","53001","5918","24153","79875","147369","36147","53002","138543","138542","104212","137699","137698","41960","104211","18160","41723","41724","3505","123744","81389","104205","104206","112065","49320","84772","37107","142803");

			final Id<VehicleType> vehicleTypeId = (VehicleUtils.findVehicle(event.getVehicleId(), scenario)).getType().getId();

			//toll a vehicle only once.
			if (!tolledVehicles.contains(event.getVehicleId()))
				if (vehicleTypesToBeTolled.contains(vehicleTypeId.toString())) {
					if (tolledLinkList.contains(event.getLinkId().toString())) {
						log.info("Tolling caused by event: " + event);
						tolledVehicles.add(event.getVehicleId());
						score = score - toll;
					}
				}
		}
	}
}
