/**
 * 
 */
package playground.yu.analysis;

import java.io.IOException;

import org.matsim.basic.v01.BasicPlanImpl.LegIterator;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.basic.v01.BasicLeg.Mode;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Leg;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.utils.charts.XYScatterChart;

import playground.yu.utils.SimpleWriter;

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
	private Person person;

	/**
	 * 
	 */
	public DailyDistanceModalSplit() {
		maleCarDist = 0.0;
		malePtDist = 0.0;
		femaleCarDist = 0.0;
		femalePtDist = 0.0;
		ageACarDist = 0.0;
		ageBCarDist = 0.0;
		ageCCarDist = 0.0;
		ageDCarDist = 0.0;
		ageAPtDist = 0.0;
		ageBPtDist = 0.0;
		ageCPtDist = 0.0;
		ageDPtDist = 0.0;
		withLicenseCarDist = 0.0;
		withoutLicenseCarDist = 0.0;
		withLicensePtDist = 0.0;
		withoutLicensePtDist = 0.0;
		isEmployedCarDist = 0.0;
		isEmployedPtDist = 0.0;
		notEmployedCarDist = 0.0;
		notEmployedPtDist = 0.0;
		alwaysCarDist = 0.0;
		alwaysPtDist = 0.0;
		sometimesCarDist = 0.0;
		sometimesPtDist = 0.0;
		neverCarDist = 0.0;
		neverPtDist = 0.0;
	}

	@Override
	public void run(Person person) {
		this.person = person;
		run(person.getSelectedPlan());
	}

	public void run(Plan plan) {
		int age = person.getAge();
		String carAvail = person.getCarAvail();
		for (LegIterator li = plan.getIteratorLeg(); li.hasNext();) {
			Leg leg = (Leg) li.next();
			double legDist = leg.getRoute().getDist() / 1000.0;
			if (Integer.parseInt(person.getId().toString()) < 1000000000)
				if (leg.getMode().equals(Mode.car)) {
					if (person.getSex().equals("m"))
						maleCarDist += legDist;
					else
						femaleCarDist += legDist;
					if (age < 30)
						ageACarDist += legDist;
					else if (age >= 30 && age < 50)
						ageBCarDist += legDist;
					else if (age >= 50 && age < 70)
						ageCCarDist += legDist;
					else
						ageDCarDist += legDist;
					if (person.getLicense().equals("yes"))
						withLicenseCarDist += legDist;
					else
						withoutLicenseCarDist += legDist;
					if (carAvail.equals("always"))
						alwaysCarDist += legDist;
					else if (carAvail.equals("sometimes"))
						sometimesCarDist += legDist;
					else
						neverCarDist += legDist;
					if (person.isEmployed())
						isEmployedCarDist += legDist;
					else
						notEmployedCarDist += legDist;
				} else if (leg.getMode().equals(Mode.pt)) {
					if (person.getSex().equals("m"))
						malePtDist += legDist;
					else
						femalePtDist += legDist;
					if (age < 30)
						ageAPtDist += legDist;
					else if (age >= 30 && age < 50)
						ageBPtDist += legDist;
					else if (age >= 50 && age < 70)
						ageCPtDist += legDist;
					else
						ageDPtDist += legDist;
					if (person.getLicense().equals("yes"))
						withLicensePtDist += legDist;
					else
						withoutLicensePtDist += legDist;
					if (carAvail.equals("always"))
						alwaysPtDist += legDist;
					else if (carAvail.equals("sometimes"))
						sometimesPtDist += legDist;
					else
						neverPtDist += legDist;
					if (person.isEmployed())
						isEmployedPtDist += legDist;
					else
						notEmployedPtDist += legDist;
				}

		}
	}

	public void write(String outputFilename) {
		SimpleWriter sw = new SimpleWriter(outputFilename + ".txt");
		sw.writeln("Modal Split -- DailyDistance");
		sw.writeln("category\tcar Distance [km]\tpt Distance [km]");
		sw.writeln("male\t" + maleCarDist + "\t" + malePtDist);
		sw.writeln("female\t" + femaleCarDist + "\t" + femalePtDist);
		sw.writeln("age<30\t" + ageACarDist + "\t" + ageAPtDist);
		sw.writeln("30<=age<50\t" + ageBCarDist + "\t" + ageBPtDist);
		sw.writeln("50<=age<70\t" + ageCCarDist + "\t" + ageCPtDist);
		sw.writeln("age>70\t" + ageDCarDist + "\t" + ageDPtDist);
		sw.writeln("with license\t" + withLicenseCarDist + "\t"
				+ withLicensePtDist);
		sw.writeln("without license\t" + withoutLicenseCarDist + "\t"
				+ withoutLicensePtDist);
		sw.writeln("always has a car\t" + alwaysCarDist + "\t" + alwaysPtDist);
		sw.writeln("sometimes has a car\t" + sometimesCarDist + "\t"
				+ sometimesPtDist);
		sw.writeln("never has a car\t" + neverCarDist + "\t" + neverPtDist);
		sw.writeln("employed\t" + sometimesCarDist + "\t" + sometimesPtDist);
		sw.writeln("not employed\t" + notEmployedCarDist + "\t"
				+ notEmployedPtDist);
		sw.writeln("-----------------------------");
		try {
			sw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		XYScatterChart chart = new XYScatterChart(
				"Modal split -- daily Distance", "pt fraction", "car fraction");
		chart.addSeries("male", new double[] { malePtDist * 100.0
				/ (maleCarDist + malePtDist) }, new double[] { maleCarDist
				* 100.0 / (maleCarDist + malePtDist) });
		chart.addSeries("female", new double[] { femalePtDist * 100.0
				/ (femaleCarDist + femalePtDist) },
				new double[] { femaleCarDist * 100.0
						/ (femaleCarDist + femalePtDist) });
		chart.addSeries("age<30", new double[] { ageAPtDist * 100.0
				/ (ageACarDist + ageAPtDist) }, new double[] { ageACarDist
				* 100.0 / (ageACarDist + ageAPtDist) });
		chart.addSeries("30<=age<50", new double[] { ageBPtDist * 100.0
				/ (ageBCarDist + ageBPtDist) }, new double[] { ageBCarDist
				* 100.0 / (ageBCarDist + ageBPtDist) });
		chart.addSeries("50<=age<70", new double[] { ageCPtDist * 100.0
				/ (ageCCarDist + ageCPtDist) }, new double[] { ageCCarDist
				* 100.0 / (ageCCarDist + ageCPtDist) });
		chart.addSeries("age>70", new double[] { ageDPtDist * 100.0
				/ (ageDCarDist + ageDPtDist) }, new double[] { ageDCarDist
				* 100.0 / (ageDCarDist + ageDPtDist) });
		chart.addSeries("with license", new double[] { withLicensePtDist
				* 100.0 / (withLicenseCarDist + withLicensePtDist) },
				new double[] { withLicenseCarDist * 100.0
						/ (withLicenseCarDist + withLicensePtDist) });
		chart.addSeries("without license", new double[] { withoutLicensePtDist
				* 100.0 / (withoutLicenseCarDist + withoutLicensePtDist) },
				new double[] { withoutLicenseCarDist * 100.0
						/ (withoutLicenseCarDist + withoutLicensePtDist) });
		chart.addSeries("always has a car", new double[] { alwaysPtDist * 100.0
				/ (alwaysCarDist + alwaysPtDist) },
				new double[] { alwaysCarDist * 100.0
						/ (alwaysCarDist + alwaysPtDist) });
		chart.addSeries("sometimes has a car", new double[] { sometimesPtDist
				* 100.0 / (sometimesCarDist + sometimesPtDist) },
				new double[] { sometimesCarDist * 100.0
						/ (sometimesCarDist + sometimesPtDist) });
		chart.addSeries("never has a car", new double[] { neverPtDist * 100.0
				/ (neverCarDist + neverPtDist) }, new double[] { neverCarDist
				* 100.0 / (neverCarDist + neverPtDist) });
		chart.addSeries("employed", new double[] { isEmployedPtDist * 100.0
				/ (isEmployedCarDist + isEmployedPtDist) },
				new double[] { isEmployedCarDist * 100.0
						/ (isEmployedCarDist + isEmployedPtDist) });
		chart.addSeries("not employed", new double[] { notEmployedPtDist
				* 100.0 / (notEmployedCarDist + notEmployedPtDist) },
				new double[] { notEmployedCarDist * 100.0
						/ (notEmployedCarDist + notEmployedPtDist) });
		chart.saveAsPng(outputFilename + ".png", 1200, 900);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Gbl.startMeasurement();

		final String netFilename = "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml";
		final String plansFilename = "../runs_SVN/run669/it.1000/1000.plans.xml.gz";
		final String outputFilename = "../runs_SVN/run669/it.1000/ModalSplitDailyDistance";

		Gbl.createConfig(null);

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);

		Population population = new Population();

		DailyDistanceModalSplit ddms = new DailyDistanceModalSplit();
		population.addAlgorithm(ddms);

		System.out.println("-->reading plansfile: " + plansFilename);
		new MatsimPopulationReader(population, network).readFile(plansFilename);

		population.runAlgorithms();

		ddms.write(outputFilename);

		System.out.println("--> Done!");
		Gbl.printElapsedTime();
		System.exit(0);
	}

}
