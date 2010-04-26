/**
 *
 */
package playground.yu.analysis.forZrh;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.utils.misc.Time;
import org.matsim.roadpricing.RoadPricingScheme;

import playground.yu.analysis.LegTravelTimeModalSplit;
import playground.yu.analysis.PlanModeJudger;
import playground.yu.utils.io.SimpleWriter;

/**
 * compute average leg travel Time of Zurich and Kanton Zurich respectively with
 * through traffic
 *
 * @author yu
 *
 */
public class LegTravelTimeModalSplit4Zrh extends LegTravelTimeModalSplit {

	private final double[] throughTravelTimes;
	private final int[] throughArrCount;

	public LegTravelTimeModalSplit4Zrh(int binSize, int nofBins,
			Population plans) {
		super(binSize, nofBins, plans);
		throughTravelTimes = new double[nofBins + 1];
		throughArrCount = new int[nofBins + 1];
	}

	public LegTravelTimeModalSplit4Zrh(int binSize, Population plans) {
		this(binSize, 30 * 3600 / binSize + 1, plans);
	}

	public LegTravelTimeModalSplit4Zrh(Population ppl, RoadPricingScheme toll) {
		this(ppl);
		this.toll = toll;
	}

	public LegTravelTimeModalSplit4Zrh(Population plans) {
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
			if (Integer.parseInt(agentId) < 1000000000) {
				if (PlanModeJudger.useCar(selectedplan)) {
					this.carTravelTimes[binIdx] += travelTime;
					this.carArrCount[binIdx]++;
				} else if (PlanModeJudger.usePt(selectedplan)) {
					this.ptTravelTimes[binIdx] += travelTime;
					this.ptArrCount[binIdx]++;
				} else if (PlanModeJudger.useWalk(selectedplan)) {
					wlkTravelTimes[binIdx] += travelTime;
					wlkArrCount[binIdx]++;
				}
			} else {
				this.throughTravelTimes[binIdx] += travelTime;
				this.throughArrCount[binIdx]++;
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
						+ "\tthrough_traveltimes [s]\tthrough_n._arrivals\tthrough_avg. traveltimes [s]");
		for (int i = 0; i < this.travelTimes.length; i++)
			sw.writeln(Time.writeTime(i * this.binSize) + "\t"
					+ i * this.binSize + "\t" + this.travelTimes[i] + "\t"
					+ this.arrCount[i] + "\t"
					+ this.travelTimes[i] / this.arrCount[i] + "\t"
					+ this.carTravelTimes[i] + "\t" + this.carArrCount[i]
					+ "\t"
					+ this.carTravelTimes[i] / this.carArrCount[i]
					+ "\t" + this.ptTravelTimes[i] + "\t" + this.ptArrCount[i]
					+ "\t"
					+ this.ptTravelTimes[i] / this.ptArrCount[i]
					+ this.wlkTravelTimes[i] + "\t" + this.wlkArrCount[i]
					+ "\t" + this.wlkTravelTimes[i]
					/ this.wlkArrCount[i] + this.throughTravelTimes[i]
					+ "\t" + this.throughArrCount[i] + "\t"
					+ this.throughTravelTimes[i]
					/ this.throughArrCount[i]);
		sw.write("----------------------------------------\n");
		double ttSum = 0.0, carTtSum = 0.0, ptTtSum = 0.0, wlkTtSum = 0.0, otherTtSum = 0.0;
		int nTrips = 0, nCarTrips = 0, nPtTrips = 0, nWlkTrips = 0, nOtherTrips = 0;
		for (int i = 0; i < this.travelTimes.length; i++) {
			ttSum += this.travelTimes[i];
			carTtSum += this.carTravelTimes[i];
			ptTtSum += this.ptTravelTimes[i];
			wlkTtSum += this.wlkTravelTimes[i];
			otherTtSum += throughTravelTimes[i];

			nTrips += this.arrCount[i];
			nCarTrips += this.carArrCount[i];
			nPtTrips += this.ptArrCount[i];
			nWlkTrips += wlkArrCount[i];
			nOtherTrips += throughArrCount[i];
		}
		sw
				.writeln("the sum of all the traveltimes [s]: "
						+ ttSum
						+ "\n"
						+ "the number of all the Trips: "
						+ nTrips
						+ "\n"
						+ "the sum of all the drivers traveltimes [s]: "
						+ carTtSum
						+ "\n"
						+ "the number of all the drivers Trips: "
						+ nCarTrips
						+ "\n"
						+ "the sum of all the public transit unsers traveltimes [s]: "
						+ ptTtSum
						+ "\n"
						+ "the number of all the public users Trips: "
						+ nPtTrips
						+ "\n"
						+ "the sum of all the walkers traveltimes [s]: "
						+ wlkTtSum
						+ "\n"
						+ "the number of all the walkers Trips: "
						+ nWlkTrips
						+ "\n"
						+ "the sum of all the through-traffic traveltimes [s]: "
						+ otherTtSum + "\n"
						+ "the number of all the through-traffic Trips: "
						+ nOtherTrips);
		sw.close();
	}

	public static void main(final String[] args) {
		final String netFilename = "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml";
		final String eventsFilename = "../matsimTests/changeLegModeTests/500.events.txt.gz";
		final String plansFilename = "../matsimTests/changeLegModeTests/500.plans.xml.gz";
		String outputFilename = "../matsimTests/changeLegModeTests/500.legTravelTime.txt";
		String chartFilename = "../matsimTests/changeLegModeTests/";
		// String tollFilename =
		// "../schweiz-ivtch-SVN/baseCase/roadpricing/KantonZurich/KantonZurich.xml";

		Gbl.startMeasurement();
		// Gbl.createConfig(null);

		ScenarioImpl scenario = new ScenarioImpl();
		NetworkLayer network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(netFilename);

		Population population = scenario.getPopulation();
		System.out.println("-->reading plansfile: " + plansFilename);
		new MatsimPopulationReader(scenario).readFile(plansFilename);

		// RoadPricingReaderXMLv1 tollReader = new
		// RoadPricingReaderXMLv1(network);
		// try {
		// tollReader.parse(tollFilename);
		// } catch (SAXException e) {
		// e.printStackTrace();
		// } catch (ParserConfigurationException e) {
		// e.printStackTrace();
		// } catch (IOException e) {
		// e.printStackTrace();
		// }

		EventsManagerImpl events = new EventsManagerImpl();

		LegTravelTimeModalSplit lttms = new LegTravelTimeModalSplit(population
		// ,tollReader.getScheme()
				, null);
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
