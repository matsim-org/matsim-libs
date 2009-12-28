/**
 *
 */
package playground.yu.analysis;

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
public class DailyDistanceModalSplit extends AbstractPersonAlgorithm implements
		PlanAlgorithm {
	private double maleCarDist, malePtDist, femaleCarDist, femalePtDist;
	private double ageACarDist, ageBCarDist, ageCCarDist, ageDCarDist,
			ageAPtDist, ageBPtDist, ageCPtDist, ageDPtDist;
	private double withLicenseCarDist, withoutLicenseCarDist,
			withLicensePtDist, withoutLicensePtDist;
	private double isEmployedCarDist, isEmployedPtDist, notEmployedCarDist,
			notEmployedPtDist;
	private double alwaysCarDist, alwaysPtDist, sometimesCarDist,
			sometimesPtDist, neverCarDist, neverPtDist;
	private PersonImpl person = null;

	/**
	 *
	 */
	public DailyDistanceModalSplit() {
		this.maleCarDist = 0.0;
		this.malePtDist = 0.0;
		this.femaleCarDist = 0.0;
		this.femalePtDist = 0.0;
		this.ageACarDist = 0.0;
		this.ageBCarDist = 0.0;
		this.ageCCarDist = 0.0;
		this.ageDCarDist = 0.0;
		this.ageAPtDist = 0.0;
		this.ageBPtDist = 0.0;
		this.ageCPtDist = 0.0;
		this.ageDPtDist = 0.0;
		this.withLicenseCarDist = 0.0;
		this.withoutLicenseCarDist = 0.0;
		this.withLicensePtDist = 0.0;
		this.withoutLicensePtDist = 0.0;
		this.isEmployedCarDist = 0.0;
		this.isEmployedPtDist = 0.0;
		this.notEmployedCarDist = 0.0;
		this.notEmployedPtDist = 0.0;
		this.alwaysCarDist = 0.0;
		this.alwaysPtDist = 0.0;
		this.sometimesCarDist = 0.0;
		this.sometimesPtDist = 0.0;
		this.neverCarDist = 0.0;
		this.neverPtDist = 0.0;
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
				double legDist = leg.getRoute().getDistance() / 1000.0;
				if (Integer.parseInt(this.person.getId().toString()) < 1000000000
						&& leg.getDepartureTime() < 86400) {
					if (leg.getMode().equals(TransportMode.car)) {
						if (this.person.getSex().equals("m"))
							this.maleCarDist += legDist;
						else
							this.femaleCarDist += legDist;
						if (age < 30)
							this.ageACarDist += legDist;
						else if (age >= 30 && age < 50)
							this.ageBCarDist += legDist;
						else if (age >= 50 && age < 70)
							this.ageCCarDist += legDist;
						else
							this.ageDCarDist += legDist;
						if (this.person.getLicense().equals("yes"))
							this.withLicenseCarDist += legDist;
						else
							this.withoutLicenseCarDist += legDist;
						if (carAvail.equals("always"))
							this.alwaysCarDist += legDist;
						else if (carAvail.equals("sometimes"))
							this.sometimesCarDist += legDist;
						else
							this.neverCarDist += legDist;
						if (this.person.isEmployed())
							this.isEmployedCarDist += legDist;
						else
							this.notEmployedCarDist += legDist;
					} else if (leg.getMode().equals(TransportMode.pt)) {
						if (this.person.getSex().equals("m"))
							this.malePtDist += legDist;
						else
							this.femalePtDist += legDist;
						if (age < 30)
							this.ageAPtDist += legDist;
						else if (age >= 30 && age < 50)
							this.ageBPtDist += legDist;
						else if (age >= 50 && age < 70)
							this.ageCPtDist += legDist;
						else
							this.ageDPtDist += legDist;
						if (this.person.getLicense().equals("yes"))
							this.withLicensePtDist += legDist;
						else
							this.withoutLicensePtDist += legDist;
						if (carAvail.equals("always"))
							this.alwaysPtDist += legDist;
						else if (carAvail.equals("sometimes"))
							this.sometimesPtDist += legDist;
						else
							this.neverPtDist += legDist;
						if (this.person.isEmployed())
							this.isEmployedPtDist += legDist;
						else
							this.notEmployedPtDist += legDist;
					}
				}
			}
		}
	}

	public void write(final String outputFilename) {
		SimpleWriter sw = new SimpleWriter(outputFilename + ".txt");
		sw.writeln("Modal Split -- DailyDistance");
		sw.writeln("category\tcar Distance [km]\tpt Distance [km]");
		sw.writeln("male\t" + this.maleCarDist + "\t" + this.malePtDist);
		sw.writeln("female\t" + this.femaleCarDist + "\t" + this.femalePtDist);
		sw.writeln("age<30\t" + this.ageACarDist + "\t" + this.ageAPtDist);
		sw.writeln("30<=age<50\t" + this.ageBCarDist + "\t" + this.ageBPtDist);
		sw.writeln("50<=age<70\t" + this.ageCCarDist + "\t" + this.ageCPtDist);
		sw.writeln("age>70\t" + this.ageDCarDist + "\t" + this.ageDPtDist);
		sw.writeln("with license\t" + this.withLicenseCarDist + "\t"
				+ this.withLicensePtDist);
		sw.writeln("without license\t" + this.withoutLicenseCarDist + "\t"
				+ this.withoutLicensePtDist);
		sw.writeln("always has a car\t" + this.alwaysCarDist + "\t"
				+ this.alwaysPtDist);
		sw.writeln("sometimes has a car\t" + this.sometimesCarDist + "\t"
				+ this.sometimesPtDist);
		sw.writeln("never has a car\t" + this.neverCarDist + "\t"
				+ this.neverPtDist);
		sw.writeln("employed\t" + this.sometimesCarDist + "\t"
				+ this.sometimesPtDist);
		sw.writeln("not employed\t" + this.notEmployedCarDist + "\t"
				+ this.notEmployedPtDist);
		sw.writeln("-----------------------------");
		sw.close();
		XYScatterChart chart = new XYScatterChart(
				"Modal split -- daily Distance", "pt fraction", "car fraction");
		chart.addSeries("male", new double[] { this.malePtDist * 100.0
				/ (this.maleCarDist + this.malePtDist) },
				new double[] { this.maleCarDist * 100.0
						/ (this.maleCarDist + this.malePtDist) });
		chart.addSeries("female", new double[] { this.femalePtDist * 100.0
				/ (this.femaleCarDist + this.femalePtDist) },
				new double[] { this.femaleCarDist * 100.0
						/ (this.femaleCarDist + this.femalePtDist) });
		chart.addSeries("age<30", new double[] { this.ageAPtDist * 100.0
				/ (this.ageACarDist + this.ageAPtDist) },
				new double[] { this.ageACarDist * 100.0
						/ (this.ageACarDist + this.ageAPtDist) });
		chart.addSeries("30<=age<50", new double[] { this.ageBPtDist * 100.0
				/ (this.ageBCarDist + this.ageBPtDist) },
				new double[] { this.ageBCarDist * 100.0
						/ (this.ageBCarDist + this.ageBPtDist) });
		chart.addSeries("50<=age<70", new double[] { this.ageCPtDist * 100.0
				/ (this.ageCCarDist + this.ageCPtDist) },
				new double[] { this.ageCCarDist * 100.0
						/ (this.ageCCarDist + this.ageCPtDist) });
		chart.addSeries("age>70", new double[] { this.ageDPtDist * 100.0
				/ (this.ageDCarDist + this.ageDPtDist) },
				new double[] { this.ageDCarDist * 100.0
						/ (this.ageDCarDist + this.ageDPtDist) });
		chart.addSeries("with license", new double[] { this.withLicensePtDist
				* 100.0 / (this.withLicenseCarDist + this.withLicensePtDist) },
				new double[] { this.withLicenseCarDist * 100.0
						/ (this.withLicenseCarDist + this.withLicensePtDist) });
		chart
				.addSeries(
						"without license",
						new double[] { this.withoutLicensePtDist
								* 100.0
								/ (this.withoutLicenseCarDist + this.withoutLicensePtDist) },
						new double[] { this.withoutLicenseCarDist
								* 100.0
								/ (this.withoutLicenseCarDist + this.withoutLicensePtDist) });
		chart.addSeries("always has a car", new double[] { this.alwaysPtDist
				* 100.0 / (this.alwaysCarDist + this.alwaysPtDist) },
				new double[] { this.alwaysCarDist * 100.0
						/ (this.alwaysCarDist + this.alwaysPtDist) });
		chart.addSeries("sometimes has a car",
				new double[] { this.sometimesPtDist * 100.0
						/ (this.sometimesCarDist + this.sometimesPtDist) },
				new double[] { this.sometimesCarDist * 100.0
						/ (this.sometimesCarDist + this.sometimesPtDist) });
		chart.addSeries("never has a car", new double[] { this.neverPtDist
				* 100.0 / (this.neverCarDist + this.neverPtDist) },
				new double[] { this.neverCarDist * 100.0
						/ (this.neverCarDist + this.neverPtDist) });
		chart.addSeries("employed", new double[] { this.isEmployedPtDist
				* 100.0 / (this.isEmployedCarDist + this.isEmployedPtDist) },
				new double[] { this.isEmployedCarDist * 100.0
						/ (this.isEmployedCarDist + this.isEmployedPtDist) });
		chart.addSeries("not employed", new double[] { this.notEmployedPtDist
				* 100.0 / (this.notEmployedCarDist + this.notEmployedPtDist) },
				new double[] { this.notEmployedCarDist * 100.0
						/ (this.notEmployedCarDist + this.notEmployedPtDist) });
		chart.saveAsPng(outputFilename + ".png", 1200, 900);
	}

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		Gbl.startMeasurement();

		final String netFilename = "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml";
		final String plansFilename = "../runs_SVN/run669/it.1000/1000.plans.xml.gz";
		final String outputFilename = "../runs_SVN/run669/it.1000/ModalSplitDailyDistance";

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);

		PopulationImpl population = new PopulationImpl();

		System.out.println("-->reading plansfile: " + plansFilename);
		new MatsimPopulationReader(population, network).readFile(plansFilename);

		DailyDistanceModalSplit ddms = new DailyDistanceModalSplit();
		ddms.run(population);

		ddms.write(outputFilename);

		System.out.println("--> Done!");
		Gbl.printElapsedTime();
		System.exit(0);
	}

}
