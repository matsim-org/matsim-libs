/**
 *
 */
package playground.yu.analysis;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.utils.charts.XYScatterChart;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.yu.utils.io.SimpleWriter;

/**
 * @author yu
 * 
 */
public class DailyEnRouteTimeModalSplit extends AbstractPersonAlgorithm
		implements PlanAlgorithm {
	private double maleCarTime, malePtTime, femaleCarTime, femalePtTime;
	private double ageACarTime, ageBCarTime, ageCCarTime, ageDCarTime,
			ageAPtTime, ageBPtTime, ageCPtTime, ageDPtTime;
	private double withLicenseCarTime, withoutLicenseCarTime,
			withLicensePtTime, withoutLicensePtTime;
	private double isEmployedCarTime, isEmployedPtTime, notEmployedCarTime,
			notEmployedPtTime;
	private double alwaysCarTime, alwaysPtTime, sometimesCarTime,
			sometimesPtTime, neverCarTime, neverPtTime;
	private PersonImpl person = null;

	/**
	 *
	 */
	public DailyEnRouteTimeModalSplit() {
		this.maleCarTime = 0.0;
		this.malePtTime = 0.0;
		this.femaleCarTime = 0.0;
		this.femalePtTime = 0.0;
		this.ageACarTime = 0.0;
		this.ageBCarTime = 0.0;
		this.ageCCarTime = 0.0;
		this.ageDCarTime = 0.0;
		this.ageAPtTime = 0.0;
		this.ageBPtTime = 0.0;
		this.ageCPtTime = 0.0;
		this.ageDPtTime = 0.0;
		this.withLicenseCarTime = 0.0;
		this.withoutLicenseCarTime = 0.0;
		this.withLicensePtTime = 0.0;
		this.withoutLicensePtTime = 0.0;
		this.isEmployedCarTime = 0.0;
		this.isEmployedPtTime = 0.0;
		this.notEmployedCarTime = 0.0;
		this.notEmployedPtTime = 0.0;
		this.alwaysCarTime = 0.0;
		this.alwaysPtTime = 0.0;
		this.sometimesCarTime = 0.0;
		this.sometimesPtTime = 0.0;
		this.neverCarTime = 0.0;
		this.neverPtTime = 0.0;
	}

	@Override
	public void run(final Person person) {
		this.person = (PersonImpl) person;
		run(person.getSelectedPlan());
	}

	public void run(final Plan plan) {
		int age = this.person.getAge();
		String carAvail = this.person.getCarAvail();
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof LegImpl) {
				LegImpl leg = (LegImpl) pe;
				double legTime = leg.getRoute().getTravelTime() / 60.0;
				if (Integer.parseInt(this.person.getId().toString()) < 1000000000
						&& leg.getDepartureTime() < 86400) {
					if (leg.getMode().equals(TransportMode.car)) {
						if (this.person.getSex().equals("m"))
							this.maleCarTime += legTime;
						else
							this.femaleCarTime += legTime;
						if (age < 30)
							this.ageACarTime += legTime;
						else if (age >= 30 && age < 50)
							this.ageBCarTime += legTime;
						else if (age >= 50 && age < 70)
							this.ageCCarTime += legTime;
						else
							this.ageDCarTime += legTime;
						if (this.person.getLicense().equals("yes"))
							this.withLicenseCarTime += legTime;
						else
							this.withoutLicenseCarTime += legTime;
						if (carAvail.equals("always"))
							this.alwaysCarTime += legTime;
						else if (carAvail.equals("sometimes"))
							this.sometimesCarTime += legTime;
						else
							this.neverCarTime += legTime;
						if (this.person.isEmployed())
							this.isEmployedCarTime += legTime;
						else
							this.notEmployedCarTime += legTime;
					} else if (leg.getMode().equals(TransportMode.pt)) {
						if (this.person.getSex().equals("m"))
							this.malePtTime += legTime;
						else
							this.femalePtTime += legTime;
						if (age < 30)
							this.ageAPtTime += legTime;
						else if (age >= 30 && age < 50)
							this.ageBPtTime += legTime;
						else if (age >= 50 && age < 70)
							this.ageCPtTime += legTime;
						else
							this.ageDPtTime += legTime;
						if (this.person.getLicense().equals("yes"))
							this.withLicensePtTime += legTime;
						else
							this.withoutLicensePtTime += legTime;
						if (carAvail.equals("always"))
							this.alwaysPtTime += legTime;
						else if (carAvail.equals("sometimes"))
							this.sometimesPtTime += legTime;
						else
							this.neverPtTime += legTime;
						if (this.person.isEmployed())
							this.isEmployedPtTime += legTime;
						else
							this.notEmployedPtTime += legTime;
					}
				}
			}
		}
	}

	public void write(final String outputFilename) {
		SimpleWriter sw = new SimpleWriter(outputFilename + ".txt");
		sw.writeln("Modal Split -- DailyTime");
		sw.writeln("category\tcar Time [km]\tpt Time [km]");
		sw.writeln("male\t" + this.maleCarTime + "\t" + this.malePtTime);
		sw.writeln("female\t" + this.femaleCarTime + "\t" + this.femalePtTime);
		sw.writeln("age<30\t" + this.ageACarTime + "\t" + this.ageAPtTime);
		sw.writeln("30<=age<50\t" + this.ageBCarTime + "\t" + this.ageBPtTime);
		sw.writeln("50<=age<70\t" + this.ageCCarTime + "\t" + this.ageCPtTime);
		sw.writeln("age>70\t" + this.ageDCarTime + "\t" + this.ageDPtTime);
		sw.writeln("with license\t" + this.withLicenseCarTime + "\t"
				+ this.withLicensePtTime);
		sw.writeln("without license\t" + this.withoutLicenseCarTime + "\t"
				+ this.withoutLicensePtTime);
		sw.writeln("always has a car\t" + this.alwaysCarTime + "\t"
				+ this.alwaysPtTime);
		sw.writeln("sometimes has a car\t" + this.sometimesCarTime + "\t"
				+ this.sometimesPtTime);
		sw.writeln("never has a car\t" + this.neverCarTime + "\t"
				+ this.neverPtTime);
		sw.writeln("employed\t" + this.sometimesCarTime + "\t"
				+ this.sometimesPtTime);
		sw.writeln("not employed\t" + this.notEmployedCarTime + "\t"
				+ this.notEmployedPtTime);
		sw.writeln("-----------------------------");
		sw.close();
		XYScatterChart chart = new XYScatterChart("Modal split -- daily Time",
				"pt fraction", "car fraction");
		chart.addSeries("male", new double[] { this.malePtTime * 100.0
				/ (this.maleCarTime + this.malePtTime) },
				new double[] { this.maleCarTime * 100.0
						/ (this.maleCarTime + this.malePtTime) });
		chart.addSeries("female", new double[] { this.femalePtTime * 100.0
				/ (this.femaleCarTime + this.femalePtTime) },
				new double[] { this.femaleCarTime * 100.0
						/ (this.femaleCarTime + this.femalePtTime) });
		chart.addSeries("age<30", new double[] { this.ageAPtTime * 100.0
				/ (this.ageACarTime + this.ageAPtTime) },
				new double[] { this.ageACarTime * 100.0
						/ (this.ageACarTime + this.ageAPtTime) });
		chart.addSeries("30<=age<50", new double[] { this.ageBPtTime * 100.0
				/ (this.ageBCarTime + this.ageBPtTime) },
				new double[] { this.ageBCarTime * 100.0
						/ (this.ageBCarTime + this.ageBPtTime) });
		chart.addSeries("50<=age<70", new double[] { this.ageCPtTime * 100.0
				/ (this.ageCCarTime + this.ageCPtTime) },
				new double[] { this.ageCCarTime * 100.0
						/ (this.ageCCarTime + this.ageCPtTime) });
		chart.addSeries("age>70", new double[] { this.ageDPtTime * 100.0
				/ (this.ageDCarTime + this.ageDPtTime) },
				new double[] { this.ageDCarTime * 100.0
						/ (this.ageDCarTime + this.ageDPtTime) });
		chart.addSeries("with license", new double[] { this.withLicensePtTime
				* 100.0 / (this.withLicenseCarTime + this.withLicensePtTime) },
				new double[] { this.withLicenseCarTime * 100.0
						/ (this.withLicenseCarTime + this.withLicensePtTime) });
		chart
				.addSeries(
						"without license",
						new double[] { this.withoutLicensePtTime
								* 100.0
								/ (this.withoutLicenseCarTime + this.withoutLicensePtTime) },
						new double[] { this.withoutLicenseCarTime
								* 100.0
								/ (this.withoutLicenseCarTime + this.withoutLicensePtTime) });
		chart.addSeries("always has a car", new double[] { this.alwaysPtTime
				* 100.0 / (this.alwaysCarTime + this.alwaysPtTime) },
				new double[] { this.alwaysCarTime * 100.0
						/ (this.alwaysCarTime + this.alwaysPtTime) });
		chart.addSeries("sometimes has a car",
				new double[] { this.sometimesPtTime * 100.0
						/ (this.sometimesCarTime + this.sometimesPtTime) },
				new double[] { this.sometimesCarTime * 100.0
						/ (this.sometimesCarTime + this.sometimesPtTime) });
		chart.addSeries("never has a car", new double[] { this.neverPtTime
				* 100.0 / (this.neverCarTime + this.neverPtTime) },
				new double[] { this.neverCarTime * 100.0
						/ (this.neverCarTime + this.neverPtTime) });
		chart.addSeries("employed", new double[] { this.isEmployedPtTime
				* 100.0 / (this.isEmployedCarTime + this.isEmployedPtTime) },
				new double[] { this.isEmployedCarTime * 100.0
						/ (this.isEmployedCarTime + this.isEmployedPtTime) });
		chart.addSeries("not employed", new double[] { this.notEmployedPtTime
				* 100.0 / (this.notEmployedCarTime + this.notEmployedPtTime) },
				new double[] { this.notEmployedCarTime * 100.0
						/ (this.notEmployedCarTime + this.notEmployedPtTime) });
		chart.saveAsPng(outputFilename + ".png", 1200, 900);
	}

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		Gbl.startMeasurement();

		final String netFilename = "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml";
		final String plansFilename = "../runs_SVN/run669/it.1000/1000.plans.xml.gz";
		final String outputFilename = "../runs_SVN/run669/it.1000/ModalSplitDailyTime";

		ScenarioImpl scenario = new ScenarioImpl();
		NetworkLayer network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(netFilename);

		PopulationImpl population = scenario.getPopulation();

		System.out.println("-->reading plansfile: " + plansFilename);
		new MatsimPopulationReader(scenario).readFile(plansFilename);

		DailyEnRouteTimeModalSplit ddms = new DailyEnRouteTimeModalSplit();
		ddms.run(population);

		ddms.write(outputFilename);

		System.out.println("--> Done!");
		Gbl.printElapsedTime();
		System.exit(0);
	}

}
