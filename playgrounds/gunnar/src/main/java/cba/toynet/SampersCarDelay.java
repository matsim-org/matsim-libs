package cba.toynet;

import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;

import floetteroed.utilities.Units;
import opdytsintegration.utils.TimeDiscretization;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class SampersCarDelay {

	private final Map<TourSequence.Type, Double> type2delay_h;

	SampersCarDelay(final TimeDiscretization timeDiscr, final TravelTime carTravelTime, final Network network) {
		
		// FREE-FLOW

		final AverageTravelTime freeTT = new AverageTravelTime(timeDiscr, new FreeSpeedTravelTime());
		final double ttFree12 = freeTT.getAvgLinkTravelTime(network.getLinks().get(Id.createLinkId("1_2")), null, null);
		final double ttFree21 = freeTT.getAvgLinkTravelTime(network.getLinks().get(Id.createLinkId("2_1")), null, null);
		final double ttFree14 = freeTT.getAvgLinkTravelTime(network.getLinks().get(Id.createLinkId("1_4")), null, null);
		final double ttFree41 = freeTT.getAvgLinkTravelTime(network.getLinks().get(Id.createLinkId("4_1")), null, null);
		final double ttFree23 = freeTT.getAvgLinkTravelTime(network.getLinks().get(Id.createLinkId("2_3")), null, null);
		final double ttFree32 = freeTT.getAvgLinkTravelTime(network.getLinks().get(Id.createLinkId("3_2")), null, null);
		final double ttFree45 = freeTT.getAvgLinkTravelTime(network.getLinks().get(Id.createLinkId("4_5")), null, null);
		final double ttFree54 = freeTT.getAvgLinkTravelTime(network.getLinks().get(Id.createLinkId("5_4")), null, null);

		// CONGESTED

		final AverageTravelTime avgTT = new AverageTravelTime(timeDiscr, carTravelTime);
		final double ttCong12 = avgTT.getAvgLinkTravelTime(network.getLinks().get(Id.createLinkId("1_2")), null, null);
		final double ttCong21 = avgTT.getAvgLinkTravelTime(network.getLinks().get(Id.createLinkId("2_1")), null, null);
		final double ttCong14 = avgTT.getAvgLinkTravelTime(network.getLinks().get(Id.createLinkId("1_4")), null, null);
		final double ttCong41 = avgTT.getAvgLinkTravelTime(network.getLinks().get(Id.createLinkId("4_1")), null, null);
		final double ttCong23 = avgTT.getAvgLinkTravelTime(network.getLinks().get(Id.createLinkId("2_3")), null, null);
		final double ttCong32 = avgTT.getAvgLinkTravelTime(network.getLinks().get(Id.createLinkId("3_2")), null, null);
		final double ttCong45 = avgTT.getAvgLinkTravelTime(network.getLinks().get(Id.createLinkId("4_5")), null, null);
		final double ttCong54 = avgTT.getAvgLinkTravelTime(network.getLinks().get(Id.createLinkId("5_4")), null, null);

		// FINAL DOUBLE DELAYS
		final double delay12 = (ttCong12 - ttFree12);
		final double delay23 = (ttCong23 - ttFree23);
		final double delay32 = (ttCong32 - ttFree32);
		final double delay21 = (ttCong21 - ttFree21);

		final double delay14 = (ttCong14 - ttFree14);
		final double delay45 = (ttCong45 - ttFree45);
		final double delay54 = (ttCong54 - ttFree54);
		final double delay41 = (ttCong41 - ttFree41);

		// TOUR DELAYS
		final double work_h = Units.H_PER_S * (delay12 + delay23 + delay32 + delay21);
		final double other1_h = work_h;
		final double other2_h = Units.H_PER_S * (delay14 + delay45 + delay54 + delay41);

		// TOUR SEQUENCE DELAYS
		this.type2delay_h = new LinkedHashMap<>();
		this.type2delay_h.put(TourSequence.Type.work_car, work_h);
		this.type2delay_h.put(TourSequence.Type.work_car_other1_car, work_h + other1_h);
		this.type2delay_h.put(TourSequence.Type.work_car_other1_pt, work_h);
		this.type2delay_h.put(TourSequence.Type.work_car_other2_car, work_h + other2_h);
		this.type2delay_h.put(TourSequence.Type.work_pt, 0.0);
		this.type2delay_h.put(TourSequence.Type.work_pt_other1_car, other1_h);
		this.type2delay_h.put(TourSequence.Type.work_pt_other1_pt, 0.0);
		this.type2delay_h.put(TourSequence.Type.work_pt_other2_car, other2_h);
	}

	synchronized double getDelay_h(TourSequence seq) {
		return this.type2delay_h.get(seq.type);
	}

}
