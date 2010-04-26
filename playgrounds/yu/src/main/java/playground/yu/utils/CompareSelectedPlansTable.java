package playground.yu.utils;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.utils.charts.XYScatterChart;
import org.matsim.core.utils.io.IOUtils;

import playground.yu.analysis.PlanModeJudger;

/**
 * it is a copy of <class>org.matsim.run.CompareSelectedPlansTable</class>, only
 * some small changes were taken.
 *
 */
public class CompareSelectedPlansTable {

	private Population plans0;
	private Population plans1;
	private static final String HEADER = "personid;sex;age;license;caravail;employed;homex;homey;homelink;"
			+ "score0;score1;s1-s0;relativScoreDiff;"
			+ "plantraveltime0;plantraveltime1;t1-t0;"
			+ "plantraveldistance0;plantraveldistance1;d1-d0;"
			+ "plantype0;plantype1;planTypeChange;"
			+ "departuretime0;departuretime1;dt1-dt0;"
			+ "numberoftrips0;numberoftrips1;n1-n0";
	private NetworkLayer network;

	/**
	 * @param args
	 *            array with 4 entries: {path to plans file 0, path to plans
	 *            file 1, name of output file, path to network file}
	 */
	public static void main(final String[] args) {
		if (args.length < 4) {
			System.out.println("Too few arguments.");
			printUsage();
			System.exit(1);
		}

		Gbl.startMeasurement();
		CompareSelectedPlansTable table = new CompareSelectedPlansTable();
		table.run(args[0], args[1], args[2], args[3]);

		Gbl.printElapsedTime();
	}

	private void init(final String networkPath) {
		System.out.println("  reading the network...");
		ScenarioImpl scenario = new ScenarioImpl();
		this.network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(networkPath);
	}

	private void readFiles(final String plansfilePath0,
			final String plansfilePath1) {
		ScenarioImpl scenario0 = new ScenarioImpl();
		scenario0.setNetwork(this.network);
		this.plans0 = scenario0.getPopulation();
		System.out.println("  reading file " + plansfilePath0);
		PopulationReader plansReader0 = new MatsimPopulationReader(scenario0);
		plansReader0.readFile(plansfilePath0);

		ScenarioImpl scenario1 = new ScenarioImpl();
		scenario1.setNetwork(this.network);
		this.plans1 = scenario1.getPopulation();
		System.out.println("  reading file " + plansfilePath1);
		PopulationReader plansReader1 = new MatsimPopulationReader(scenario1);
		plansReader1.readFile(plansfilePath1);
	}

	private void writeSummaryFile(final String outfile) {
		XYScatterChart chart = new XYScatterChart("Score differences",
				"score0", "score1");
		try {
			BufferedWriter out = IOUtils.getBufferedWriter(outfile);
			out.write(CompareSelectedPlansTable.HEADER);
			out.newLine();
			double[] score0s = new double[this.plans0.getPersons().size()];
			double[] score1s = new double[this.plans0.getPersons().size()];
			double[] absDiffs = new double[this.plans0.getPersons().size()];
			double[] relativeDiffs = new double[this.plans0.getPersons().size()];
			int i = 0;
			for (Id person_id : this.plans0.getPersons().keySet()) {

				// method person.toString() not appropriate
				out.write(person_id.toString() + ";");
				PersonImpl person = (PersonImpl) this.plans0.getPersons().get(person_id);
				out.write(person.getSex() + ";");
				out.write(person.getAge() + ";");
				out.write(person.getLicense() + ";");
				out.write(person.getCarAvail() + ";");
				out.write(person.isEmployed() + ";");

				if (((PlanImpl) person.getSelectedPlan()).getFirstActivity().getType()
						.substring(0, 1).equals("h")) {
					ActivityImpl act = ((PlanImpl) person.getSelectedPlan()).getFirstActivity();
					Coord crd = act.getCoord();
					out.write(crd.getX() + ";");
					out.write(crd.getY() + ";");
					out.write(act.getLinkId() + ";");
				} else {
					// no home activity in the plan -> no home activity in the
					// knowledge
					out.write("-;-;-;");
				}

				double s0 = person.getSelectedPlan().getScore().doubleValue();
				out.write(s0 + ";");
				score0s[i] = s0;
				Person person_comp = this.plans1.getPersons().get(person_id);
				double s1 = person_comp.getSelectedPlan().getScore()
						.doubleValue();
				out.write(s1 + ";");
				score1s[i] = s1;

				out.write(Double.toString(s1 - s0) + ";");
				absDiffs[i] = s1 - s0;
				out.write(Double.toString((s1 - s0) / Math.abs(s0)) + ";");
				relativeDiffs[i] = (s1 - s0) / Math.abs(s0);

				double t0 = CompareSelectedPlansTable.getTravelTime(person);
				out.write(t0 + ";");
				double t1 = CompareSelectedPlansTable
						.getTravelTime(person_comp);
				out.write(t1 + ";");
				out.write(Double.toString(t1 - t0) + ";");

				double d0 = CompareSelectedPlansTable.getTravelDist(person);
				out.write(d0 + ";");
				double d1 = CompareSelectedPlansTable
						.getTravelDist(person_comp);
				out.write(d1 + ";");
				out.write(Double.toString(d1 - d0) + ";");

				TransportMode mode0 = PlanModeJudger.getMode(person
						.getSelectedPlan());
				String tp0 = ((PlanModeJudger.useCar(person.getSelectedPlan()) || PlanModeJudger
						.usePt(person.getSelectedPlan())) ? mode0 : "-")
						.toString();
				out.write(tp0 + ";");
				TransportMode mode1 = PlanModeJudger.getMode(person_comp
						.getSelectedPlan());
				String tp1 = ((PlanModeJudger.useCar(person_comp
						.getSelectedPlan()) || PlanModeJudger.usePt(person_comp
						.getSelectedPlan())) ? mode1 : "-").toString();
				out.write(tp1 + ";");
				out.write(tp0 + "->" + tp1 + ";");

				ActivityImpl fa0 = ((PlanImpl) person.getSelectedPlan()).getFirstActivity();
				double dpt0 = fa0.getEndTime();
				boolean hact0 = fa0.getType().startsWith("h");
				out.write((hact0 ? dpt0 : 0.0) + ";");
				ActivityImpl fa1 = ((PlanImpl) person_comp.getSelectedPlan()).getFirstActivity();
				double dpt1 = fa1.getEndTime();
				boolean hact1 = fa1.getType().startsWith("h");
				out.write((hact1 ? dpt1 : 0.0) + ";");
				out.write(((hact0 && hact1) ? (dpt1 - dpt0) : 0.0) + ";");

				int n0 = CompareSelectedPlansTable.getNumberOfTrips(person);
				out.write(n0 + ";");
				int n1 = CompareSelectedPlansTable
						.getNumberOfTrips(person_comp);
				out.write(n1 + ";");
				out.write(Integer.toString(n1 - n0));

				out.newLine();
				out.flush();

				i++;
			}
			out.close();
			chart.addSeries("score0<->score1", score0s, score1s);
			chart.addSeries("score0<->absolute score differences", score0s,
					absDiffs);
			chart.addSeries("score0<->relative score differences", score0s,
					relativeDiffs);
			chart.saveAsPng(outfile + "s0-s1.png", 1024, 768);
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
	}

	/*
	 * TODO: Put the next three methods into a "stats" class and use traveltime,
	 * numberOfTrips and traveldist as attributes. At the moment it is "nicer"
	 * to have everything in one single class
	 */

	protected static double getTravelTime(final Person person) {

		double travelTime = 0.0;
		for (PlanElement pe : person.getSelectedPlan().getPlanElements()) {
			if (pe instanceof LegImpl) {
				LegImpl leg = (LegImpl) pe;
				travelTime += leg.getTravelTime();
			}
		}
		return travelTime;
	}

	protected static double getTravelDist(final Person person) {

		double travelDist = 0.0;
		for (PlanElement pe : person.getSelectedPlan().getPlanElements()) {
			if (pe instanceof LegImpl) {
				LegImpl leg = (LegImpl) pe;
				travelDist += leg.getRoute().getDistance();
			}
		}
		return travelDist;
	}

	protected static int getNumberOfTrips(final Person person) {

		int numberOfLegs = 0;
		for (PlanElement pe : person.getSelectedPlan().getPlanElements()) {
			if (pe instanceof LegImpl) {
				numberOfLegs++;
			}
		}
		return numberOfLegs;
	}

	// --------------------------------------------------------------------------

	private void run(final String plansfilePath0, final String plansfilePath1,
			final String outfile, final String networkPath) {
		this.init(networkPath);
		readFiles(plansfilePath0, plansfilePath1);
		writeSummaryFile(outfile);
		System.out.println("finished");
	}

	private static void printUsage() {
		System.out.println();
		System.out.println("CompareSelectedPlansTable:");
		System.out.println();
		System.out
				.println("Creates an agent-based table including all agent \n"
						+ "attributes, the selected plan score and the total travel time");
		System.out.println();
		System.out.println("usage: CompareSelectedPlansTable args");
		System.out.println(" arg 0: path to plans file 0 (required)");
		System.out.println(" arg 1: path to plans file 1 (required)");
		System.out.println(" arg 2: name of output file (required)");
		System.out.println(" arg 3: path to network file (required)");

		System.out.println("----------------");
		System.out.println("2008, matsim.org");
		System.out.println();
	}

}
