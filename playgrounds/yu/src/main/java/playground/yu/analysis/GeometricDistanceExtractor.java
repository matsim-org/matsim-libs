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
import playground.yu.utils.charts.DoubleBarChart;
import playground.yu.utils.io.SimpleWriter;

/**
 * @author yu
 *
 */
public class GeometricDistanceExtractor extends AbstractPersonAlgorithm
		implements PlanAlgorithm {
	private RoadPricingScheme toll = null;
	private final String outputFilename;// without "."+postfix
	private final SimpleWriter writer;
	private double w2hLinearDist = 0.0, legLinearDist = 0.0;
	private int w2hCnt = 0, legCnt = 0, personCnt = 0;
	/* Map<purpose,Tuple<gdSum,gdCnt>[]> */
	private final Map<String, Tuple<Double, Double>[]> gds = new HashMap<String, Tuple<Double, Double>[]>();
	private final double[] carLegDists = new double[9], ptLegDists = new double[9],
			walkLegDists = new double[9];

	public GeometricDistanceExtractor(final RoadPricingScheme toll,
			final String outputFilename) {
		this.toll = toll;
		this.outputFilename = outputFilename;
		this.writer = new SimpleWriter(outputFilename + ".txt");
		this.writer.writeln("mittlere w_dist_obj2\t7.307566679381953");
		this.writer.writeln("w_dist_obj2Cnt\t18101.31975827781");
		this.writer
				.writeln("------------------------------------------\nlineCnt\t=\t18471.0\tpersonCnt\t=\t5127.0\n-----------------------------------------");
		this.writer
				.writeln("wegezwecke\tmittlere w_dist_obj2(LV)\tmittlere w_dist_obj2(MIV)\tmittlere w_dist_obj2(OeV)\tmittlere w_dist_obj2(Andere)");

		this.writer
				.writeln("Andere\t0.5699637941664182\t6.2757832069950155\t54.111435834469106\t12.966938961723768");
		this.writer
				.writeln("Arbeit\t1.1922130191305493\t9.04248179795842\t10.857171406288717\t18.156876062119938");
		this.writer
				.writeln("Geschaeftliche Taetigkeit und Dienstfahrt\t1.9287975361430263\t14.386517412575252\t33.03721290271486\t44.89536041926112");
		this.writer
				.writeln("Einkauf\t0.7048907318708167\t7.145485757876666\t5.672966627881363\t5.194383866318486");
		this.writer
				.writeln("Rueckkehr nach Hause bzw. auswaertige Unterkunft\t0.23628220556976123\t33.89939347017498\t36.7969730963612\t22.23043650644029");
		this.writer
				.writeln("Service- und Begleitwege\t0.7538780842580616\t4.477992748635797\t9.020955388044248\t3.7");
		this.writer
				.writeln("Freizeit\t0.8075903384505545\t12.197351732261064\t15.037552858176516\t10.778808992470886");
		this.writer
				.writeln("Ausbildung/Schule\t2.1686474725167018\t6.834617991162332\t10.062321375204034\t0.8445631013693445");
		this.writer
				.writeln("total\t1.0401121917578349\t10.162527664979805\t12.191850351016745\t11.763501868608962");
		this.writer
				.writeln("----------------------------------\nMATSim\ntravel purpose\tGeometricDistance [km]");
	}

	@Override
	public void run(final Person person) {
		Plan plan = person.getSelectedPlan();
		if (this.toll == null) {
			this.personCnt++;
			run(plan);
		} else if (TollTools.isInRange(((PlanImpl) plan).getFirstActivity()
				.getLinkId(), this.toll)) {
			this.personCnt++;
			run(plan);
		}
	}

	@Override
	public void run(final Plan p) {
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
					this.w2hCnt++;
					this.w2hLinearDist += geoDist;
				}
				this.legCnt++;
				this.legLinearDist += geoDist;

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
				else
					purpose = "others";

				String mode = leg.getMode();

				Tuple<Double, Double>[] tuple = this.gds.get(purpose);
				if (tuple == null) {
					tuple = new Tuple[4];
					for (int i = 0; i < tuple.length; i++)
						tuple[i] = new Tuple<Double, Double>(0d, 0d);
				}
				if (TransportMode.car.equals(mode)) {
					this.handleTuples(0, tuple, geoDist);
				} else if (TransportMode.pt.equals(mode)) {
					this.handleTuples(1, tuple, geoDist);
				} else if (TransportMode.walk.equals(mode)) {
					this.handleTuples(2, tuple, geoDist);
				} else {
					this.handleTuples(3, tuple, geoDist);
				}
				this.gds.put(purpose, tuple);
			}
		}
	}

	private void handleTuples(final int idx, final Tuple<Double, Double>[] tuple,
			final double legGeoDist) {
		tuple[idx] = new Tuple<Double, Double>(tuple[idx].getFirst()
				+ legGeoDist, tuple[idx].getSecond() + 1);
	}

	private int getPurposeIdx(final String purpose) {
		if (purpose.equals("work"))
			return 0;
		else if (purpose.equals("home"))
			return 1;
		else if (purpose.equals("leisure"))
			return 2;
		else if (purpose.equals("shopping"))
			return 3;
		else if (purpose.equals("education"))
			return 4;
		else
			/* others */return 5;
	}

	private void prepareData4Plot(final int transportModeIndex, final String purpose,
			final double legDist) {
		int purposeIdx = this.getPurposeIdx(purpose);
		switch (transportModeIndex) {
		case 0:
			this.carLegDists[purposeIdx] = legDist;
			return;
		case 1:
			this.ptLegDists[purposeIdx] = legDist;
			return;
		case 2:
			this.walkLegDists[purposeIdx] = legDist;
			return;
		}
	}

	public void write() {
		double avgW2hLD = this.w2hLinearDist / this.w2hCnt;
		double avgLegLD = this.legLinearDist / this.legCnt;
		this.writer.writeln("avg. Work2home\t" + avgW2hLD / 1000d);
		this.writer.writeln("avg. Leg\t" + avgLegLD / 1000d);
		this.writer.writeln("\npersons :\t" + this.personCnt + "\tlegs :\t" + this.legCnt
				+ "\twork2home legs :\t" + this.w2hCnt);

		this.writer.writeln("purpose\tcar\tpt\twalk\tothers");

		double lgDistSum[] = new double[4], lgDistCnt[] = new double[4];

		for (Entry<String, Tuple<Double, Double>[]> entry : this.gds.entrySet()) {
			String purpose = entry.getKey();
			StringBuffer line = new StringBuffer(purpose);
			Tuple<Double, Double>[] tuple = entry.getValue();
			for (int i = 0; i < tuple.length; i++) {
				line.append('\t');
				double sum = tuple[i].getFirst(), cnt = tuple[i].getSecond();

				double avgLegDist = sum / cnt / 1000d;

				line.append(avgLegDist);

				this.prepareData4Plot(i, purpose, avgLegDist);

				lgDistSum[i] += sum;
				lgDistCnt[i] += cnt;
			}
			this.writer.writeln(line);
		}
		this.writer.write("total\t");
		for (int i = 0; i < lgDistSum.length; i++) {
			double totalAvgDist = lgDistSum[i] / lgDistCnt[i] / 1000d;
			this.writer.write(totalAvgDist + "\t");
			switch (i) {
			case 0:
				this.carLegDists[8] = totalAvgDist;
				break;
			case 1:
				this.ptLegDists[8] = totalAvgDist;
				break;
			case 2:
				this.walkLegDists[8] = totalAvgDist;
				break;
			}
		}
		this.writer.close();

		// write chart
		DoubleBarChart dbChart = new DoubleBarChart(
				"Verkehrszwecke/ travel destination",
				new String[] { "Wegdistanz - MZ05",
						"Leg Geometric distance - MATSim",
						"valueAxisLabelLower" },
				new String[] {
						"Arbeit (work)",
						"Rueckkehr nach Hause bzw. auswaertige Unterkunft (home)",
						"Freizeit (leisure)", "Einkauf (shopping)",
						"Ausbildung/Schule (education)", "Andere (others)",
						"Geschaeftliche Taetigkeit und Dienstfahrt",
						"Service- und Begleitwege", "total" }, 2);

		// add data to upper plot
		dbChart.addSeries(0, "MIV (car)", new double[] { 9.04, 33.90, 12.20,
				7.15, 6.83, 6.28, 14.39, 4.48, 10.16 });
		dbChart.addSeries(0, "OeV (pt)", new double[] { 10.86, 36.80, 15.04,
				5.67, 10.06, 54.11, 33.04, 9.02, 12.19 });
		dbChart.addSeries(0, "LV (walk)", new double[] { 1.19, 0.24, 0.81,
				0.70, 2.17, 0.57, 1.93, 0.75, 1.04 });
		dbChart.addSeries(0, "Andere (others)", new double[] { 18.16, 22.23,
				10.78, 5.19, 0.84, 12.97, 44.90, 3.7, 11.76 });

		// add data to middle plot
		dbChart.addSeries(1, "MIV (car)", this.carLegDists);
		dbChart.addSeries(1, "OeV (pt)", this.ptLegDists);
		dbChart.addSeries(1, "LV (walk)", this.walkLegDists);
		dbChart.addSeries(1, "Andere (others)", new double[] { 0d, 0d, 0d, 0d,
				0d, 0d, 0d, 0d, 0d });

		System.out.print("car\t:");
		for (int i = 0; i < 9; i++)
			System.out.print("\t" + i + "\t" + this.carLegDists[i] + ";");

		System.out.println();
		System.out.print("pt\t:");
		for (int i = 0; i < 9; i++)
			System.out.print("\t" + i + "\t" + this.ptLegDists[i] + ";");
		System.out.println();

		System.out.print("walk\t:");
		for (int i = 0; i < 9; i++)
			System.out.print("\t" + i + "\t" + this.walkLegDists[i] + ";");
		System.out.println();

		// save png picture
		dbChart.saveAsPng(this.outputFilename + ".png",
				"MZ05 vs MATSim - mittlere Wegdistanz", 1024, 768);
	}

	public static void main(final String[] args) {
		String netFilename = "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml";
		final String plansFilename = "../runs-svn/run696/it.1500/696.1500.plans.xml.gz";
		final String outputFilename = "../matsimTests/geoDist/696.1500.geoDistKanton";
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
