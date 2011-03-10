/**
 *
 */
package playground.yu.analysis.forMuc;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.roadpricing.RoadPricingReaderXMLv1;
import org.matsim.roadpricing.RoadPricingScheme;
import org.xml.sax.SAXException;

import playground.yu.analysis.LegTravelTimeModalSplit;
import playground.yu.analysis.PlanModeJudger;
import playground.yu.utils.io.SimpleWriter;

/**
 * compute average leg travel Time of Munich network and/or Munich city Region
 *
 * @author yu
 *
 */
public class LegTravelTimeModalSplit4Muc extends LegTravelTimeModalSplit
		implements Analysis4Muc {

	private final double[] rideTravelTimes;
	private final int[] rideArrCount;

	public LegTravelTimeModalSplit4Muc(int binSize, int nofBins,
			Population plans) {
		super(binSize, nofBins, plans);
		rideTravelTimes = new double[nofBins + 1];
		rideArrCount = new int[nofBins + 1];
	}

	public LegTravelTimeModalSplit4Muc(int binSize, Population plans) {
		this(binSize, 30 * 3600 / binSize + 1, plans);
	}

	public LegTravelTimeModalSplit4Muc(Population ppl,
			RoadPricingScheme toll) {
		this(ppl);
		this.toll = toll;
	}

	public LegTravelTimeModalSplit4Muc(Population plans) {
		this(300, plans);
	}

	@Override
	protected void internalCompute(String agentId, double arrTime) {
		Double dptTime = this.tmpDptTimes.remove(agentId);
		if (dptTime != null) {
			int binIdx = getBinIndex(arrTime);
			double travelTime = arrTime - dptTime;
			this.travelTimes[binIdx] += travelTime;
			this.arrCount[binIdx]++;

			Plan selectedplan = plans.getPersons().get(new IdImpl(agentId))
					.getSelectedPlan();
			String mode = PlanModeJudger.getMode(selectedplan);
			if (TransportMode.car.equals(mode)) {
				this.carTravelTimes[binIdx] += travelTime;
				this.carArrCount[binIdx]++;
			} else if (TransportMode.pt.equals(mode)) {
				this.ptTravelTimes[binIdx] += travelTime;
				this.ptArrCount[binIdx]++;
			} else if (TransportMode.walk.equals(mode)) {
				wlkTravelTimes[binIdx] += travelTime;
				wlkArrCount[binIdx]++;
			} else if (TransportMode.bike.equals(mode)) {
				bikeTravelTimes[binIdx] += travelTime;
				bikeArrCount[binIdx]++;
			} else if (TransportMode.ride.equals(mode)) {
				rideTravelTimes[binIdx] += travelTime;
				rideArrCount[binIdx]++;
			} else {
				this.othersTravelTimes[binIdx] += travelTime;
				this.othersArrCount[binIdx]++;
			}
		}
	}

	@Override
	public void write(final String filename) {
		SimpleWriter sw = new SimpleWriter(filename);
		sw
				.writeln("time\ttimeBin"
						+ "\tall_traveltimes [s]\tn._arrivals\tavg. traveltimes [s]"
						+ "\tcar_traveltimes [s]\tcar_n._arrivals\tcar_avg. traveltimes [s]"
						+ "\tpt_traveltimes [s]\tpt_n._arrivals\tpt_avg. traveltimes [s]"
						+ "\twalk_traveltimes [s]\twalk_n._arrivals\twalk_avg. traveltimes [s]"
						+ "\tbike_traveltimes [s]\tbike_n._arrivals\tbike_avg. traveltimes [s]"
						+ "\tride_traveltimes [s]\tride_n._arrivals\tride_avg. traveltimes [s]"
						+ "\tthrough_traveltimes [s]\tthrough_n._arrivals\tthrough_avg. traveltimes [s]"
						+ "\tothers_traveltimes [s]\tothers_n._arrivals\tothers_avg. traveltimes [s]");

		for (int i = 0; i < this.travelTimes.length; i++)
			sw
					.writeln(Time.writeTime(i * this.binSize) + "\t"
							+ i * this.binSize + "\t" + this.travelTimes[i]
							+ "\t" + this.arrCount[i] + "\t"
							+ this.travelTimes[i] / this.arrCount[i] + "\t"
							+ this.carTravelTimes[i] + "\t"
							+ this.carArrCount[i] + "\t"
							+ this.carTravelTimes[i] / this.carArrCount[i]
							+ "\t" + this.ptTravelTimes[i] + "\t"
							+ this.ptArrCount[i] + "\t"
							+ this.ptTravelTimes[i] / this.ptArrCount[i]
							+ this.wlkTravelTimes[i] + "\t"
							+ this.wlkArrCount[i] + "\t"
							+ this.wlkTravelTimes[i] / this.wlkArrCount[i]
							+ this.bikeTravelTimes[i] + "\t"
							+ this.bikeArrCount[i] + "\t"
							+ this.bikeTravelTimes[i] / this.bikeArrCount[i]
							+ this.rideTravelTimes[i] + "\t"
							+ this.rideArrCount[i] + "\t"
							+ this.rideTravelTimes[i] / this.rideArrCount[i]
							+ this.othersTravelTimes[i] + "\t"
							+ this.othersArrCount[i] + "\t"
							+ this.othersTravelTimes[i]
							/ this.othersArrCount[i]);
		sw.write("----------------------------------------\n");
		double ttSum = 0.0, carTtSum = 0.0, ptTtSum = 0.0, wlkTtSum = 0.0, bikeTtSum = 0.0, rideTtSum = 0.0, othersTtSum = 0.0;
		int nTrips = 0, nCarTrips = 0, nPtTrips = 0, nWlkTrips = 0, nBikeTrips = 0, nRideTrips = 0, nOthersTrips = 0;
		for (int i = 0; i < this.travelTimes.length; i++) {
			ttSum += this.travelTimes[i];
			carTtSum += this.carTravelTimes[i];
			ptTtSum += this.ptTravelTimes[i];
			wlkTtSum += this.wlkTravelTimes[i];
			bikeTtSum += this.bikeTravelTimes[i];
			rideTtSum += this.rideTravelTimes[i];
			othersTtSum += othersTravelTimes[i];

			nTrips += this.arrCount[i];
			nCarTrips += this.carArrCount[i];
			nPtTrips += this.ptArrCount[i];
			nWlkTrips += wlkArrCount[i];
			nBikeTrips += this.bikeArrCount[i];
			nRideTrips += this.rideArrCount[i];
			nOthersTrips += othersArrCount[i];
		}
		sw.writeln("the sum of all the traveltimes [s]: " + ttSum
				+ "\nthe number of all the Trips: " + nTrips
				+ "\nthe sum of all the drivers traveltimes [s]: " + carTtSum
				+ "\nthe number of all the drivers Trips: " + nCarTrips
				+ "\nhe sum of all the public transit unsers traveltimes [s]: "
				+ ptTtSum + "\nthe number of all the public users Trips: "
				+ nPtTrips + "\nthe sum of all the walkers traveltimes [s]: "
				+ wlkTtSum + "\nthe number of all the walkers Trips: "
				+ nWlkTrips

				+ "\nthe sum of all the cyclists traveltimes [s]: " + bikeTtSum
				+ "\nthe number of all the cyclists traffic Trips: "
				+ nBikeTrips

				+ "\nthe sum of all the ride traveltimes [s]: " + rideTtSum
				+ "\nthe number of all the ride traffic Trips: " + nRideTrips

				+ "\nthe sum of all the other traffic traveltimes [s]: "
				+ othersTtSum + "\nthe number of all the other traffic Trips: "
				+ nOthersTrips);
		sw.close();
	}

	public static void main(final String[] args) {
		final String netFilename = "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml";
		final String eventsFilename = "../matsimTests/changeLegModeTests/500.events.txt.gz";
		final String plansFilename = "../matsimTests/changeLegModeTests/500.plans.xml.gz";
		String outputFilename = "../matsimTests/changeLegModeTests/500.legTravelTime.txt";
		String chartFilename = "../matsimTests/changeLegModeTests/";
		String tollFilename = "../schweiz-ivtch-SVN/baseCase/roadpricing/KantonZurich/KantonZurich.xml";

		Gbl.startMeasurement();
		// Gbl.createConfig(null);

		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile(netFilename);

		Population population = scenario.getPopulation();
		System.out.println("-->reading plansfile: " + plansFilename);
		new MatsimPopulationReader(scenario).readFile(plansFilename);

		RoadPricingScheme toll = scenario.getRoadPricingScheme();
		RoadPricingReaderXMLv1 tollReader = new RoadPricingReaderXMLv1(toll);
		try {
			tollReader.parse(tollFilename);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		EventsManagerImpl events = new EventsManagerImpl();

		LegTravelTimeModalSplit4Muc lttms = new LegTravelTimeModalSplit4Muc(
				population, toll);
		events.addHandler(lttms);

		System.out.println("-->reading evetsfile: " + eventsFilename);
		new MatsimEventsReader(events).readFile(eventsFilename);

		lttms.write(outputFilename);
		lttms.writeCharts(chartFilename);

		System.out.println("--> Done!");
		Gbl.printElapsedTime();
		System.exit(0);
	}
}
