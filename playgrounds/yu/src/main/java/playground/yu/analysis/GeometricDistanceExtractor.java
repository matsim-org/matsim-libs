/**
 *
 */
package playground.yu.analysis;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.roadpricing.RoadPricingReaderXMLv1;
import org.matsim.roadpricing.RoadPricingScheme;
import org.xml.sax.SAXException;

import playground.yu.utils.TollTools;
import playground.yu.utils.io.SimpleWriter;

/**
 * @author yu
 * 
 */
public class GeometricDistanceExtractor extends AbstractPersonAlgorithm
		implements PlanAlgorithm {
	private RoadPricingScheme toll = null;
	private final SimpleWriter writer;
	private double w2hLinearDist = 0.0, legLinearDist = 0.0;
	private int w2hCnt = 0, legCnt = 0, personCnt = 0;
	/** Map<purpose,Tuple<gdSum,gdCnt>[]> */
	private Map<String, Tuple<Double, Double>[]> gds = new HashMap<String, Tuple<Double, Double>[]>();

	public GeometricDistanceExtractor(RoadPricingScheme toll,
			String outputFilename) {
		this.toll = toll;
		writer = new SimpleWriter(outputFilename);
		writer.writeln("GeometricDistance\tGeometricDistance [km]");
	}

	@Override
	public void run(Person person) {
		Plan plan = person.getSelectedPlan();
		if (toll == null) {
			personCnt++;
			run(plan);
		} else if (TollTools.isInRange(((PlanImpl) plan).getFirstActivity()
				.getLinkId(), toll)) {
			personCnt++;
			run(plan);
		}
	}

	public void run(Plan p) {
		PlanImpl plan = (PlanImpl) p;
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Leg) {
				Leg leg = (Leg) pe;
				Activity previousAct = plan.getPreviousActivity(leg);
				Activity nextAct = plan.getNextActivity(leg);

				double geoDist = CoordUtils.calcDistance(
						previousAct.getCoord(), nextAct.getCoord());

				if (Double.isNaN(geoDist))
					throw new RuntimeException("geoDist=NaN, leg\t" + leg
							+ "\npreAct coord\t" + previousAct.getCoord()
							+ "\tnextAct coord\t" + nextAct.getCoord());

				if (previousAct.getType().startsWith("w")
						&& nextAct.getType().startsWith("h")) {
					w2hCnt++;
					w2hLinearDist += geoDist;
				}
				legCnt++;
				legLinearDist += geoDist;

				String purpose = nextAct.getType().substring(0, 1);
				if (purpose.equals("h"))
					purpose = "home";
				else if (purpose.equals("w"))
					purpose = "work";
				else if (purpose.equals("s"))
					purpose = "shopping";
				else if (purpose.equals("e"))
					purpose = "education";
				else if (purpose.equals("l"))
					purpose = "leisure";
				
				TransportMode mode = leg.getMode();

				Tuple<Double, Double>[] tuple = this.gds.get(purpose);
				if (tuple == null) {
					tuple = new Tuple[4];
					for (int i = 0; i < tuple.length; i++)
						tuple[i] = new Tuple<Double, Double>(0d, 0d);
				}
				switch (mode) {
				case car:
					this.handleTuples(0, tuple, geoDist);
					break;
				case pt:
					this.handleTuples(1, tuple, geoDist);
					break;
				case walk:
					this.handleTuples(2, tuple, geoDist);
					break;
				default:
					this.handleTuples(3, tuple, geoDist);
				}
				this.gds.put(purpose, tuple);
			}
		}
	}

	private void handleTuples(int idx, Tuple<Double, Double>[] tuple,
			double legGeoDist) {
		tuple[idx] = new Tuple<Double, Double>(tuple[idx].getFirst()
				+ legGeoDist, tuple[idx].getSecond() + 1);
	}

	public void write() {
		double avgW2hLD = w2hLinearDist / w2hCnt;
		double avgLegLD = legLinearDist / legCnt;
		writer.writeln("avg. Work2home\t" + avgW2hLD / 1000d);
		writer.writeln("avg. Leg\t" + avgLegLD / 1000d);
		writer.writeln("\npersons :\t" + personCnt + "\tlegs :\t" + legCnt
				+ "\twork2home legs :\t" + w2hCnt);

		writer.writeln("purpose\tcar\tpt\twalk\tothers");
		for (Entry<String, Tuple<Double, Double>[]> entry : this.gds.entrySet()) {
			StringBuffer line = new StringBuffer(entry.getKey());
			Tuple<Double, Double>[] tuple = entry.getValue();
			for (int i = 0; i < tuple.length; i++) {
				line.append('\t');
				line.append(tuple[i].getFirst() / tuple[i].getSecond() / 1000d);
			}
			writer.writeln(line);
		}

		writer.close();
	}

	public static void main(String[] args) {
		String netFilename = "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml";
		final String plansFilename = "../runs-svn/run697/it.1500/697.1500.plans.xml.gz";
		final String outputFilename = "../matsimTests/geoDist/697.1500.geoDistKanton.txt";
		String tollFilename = "../schweiz-ivtch-SVN/baseCase/roadpricing/KantonZurich/KantonZurich.xml";

		ScenarioImpl scenario = new ScenarioImpl();
		scenario.getConfig().scenario().setUseRoadpricing(true);

		new MatsimNetworkReader(scenario).readFile(netFilename);

		Population population = scenario.getPopulation();
		System.out.println("-->reading plansfile: " + plansFilename);
		new MatsimPopulationReader(scenario).readFile(plansFilename);

		RoadPricingScheme scheme = scenario.getRoadPricingScheme();
		try {
			new RoadPricingReaderXMLv1(scheme).parse(tollFilename);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		GeometricDistanceExtractor lde = new GeometricDistanceExtractor(scheme,
				outputFilename);
		lde.run(population);
		lde.write();

		System.out.println("--> Done!");
		System.exit(0);
	}
}
