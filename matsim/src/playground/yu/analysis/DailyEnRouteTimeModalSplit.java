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
	private Person person;

	/**
	 * 
	 */
	public DailyEnRouteTimeModalSplit() {
		maleCarTime = 0.0;
		malePtTime = 0.0;
		femaleCarTime = 0.0;
		femalePtTime = 0.0;
		ageACarTime = 0.0;
		ageBCarTime = 0.0;
		ageCCarTime = 0.0;
		ageDCarTime = 0.0;
		ageAPtTime = 0.0;
		ageBPtTime = 0.0;
		ageCPtTime = 0.0;
		ageDPtTime = 0.0;
		withLicenseCarTime = 0.0;
		withoutLicenseCarTime = 0.0;
		withLicensePtTime = 0.0;
		withoutLicensePtTime = 0.0;
		isEmployedCarTime = 0.0;
		isEmployedPtTime = 0.0;
		notEmployedCarTime = 0.0;
		notEmployedPtTime = 0.0;
		alwaysCarTime = 0.0;
		alwaysPtTime = 0.0;
		sometimesCarTime = 0.0;
		sometimesPtTime = 0.0;
		neverCarTime = 0.0;
		neverPtTime = 0.0;
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
			double legTime = leg.getRoute().getTravelTime() / 60.0;
			if (Integer.parseInt(person.getId().toString()) < 1000000000
					&& leg.getDepartureTime() < 86400)
				if (leg.getMode().equals(Mode.car)) {
					if (person.getSex().equals("m"))
						maleCarTime += legTime;
					else
						femaleCarTime += legTime;
					if (age < 30)
						ageACarTime += legTime;
					else if (age >= 30 && age < 50)
						ageBCarTime += legTime;
					else if (age >= 50 && age < 70)
						ageCCarTime += legTime;
					else
						ageDCarTime += legTime;
					if (person.getLicense().equals("yes"))
						withLicenseCarTime += legTime;
					else
						withoutLicenseCarTime += legTime;
					if (carAvail.equals("always"))
						alwaysCarTime += legTime;
					else if (carAvail.equals("sometimes"))
						sometimesCarTime += legTime;
					else
						neverCarTime += legTime;
					if (person.isEmployed())
						isEmployedCarTime += legTime;
					else
						notEmployedCarTime += legTime;
				} else if (leg.getMode().equals(Mode.pt)) {
					if (person.getSex().equals("m"))
						malePtTime += legTime;
					else
						femalePtTime += legTime;
					if (age < 30)
						ageAPtTime += legTime;
					else if (age >= 30 && age < 50)
						ageBPtTime += legTime;
					else if (age >= 50 && age < 70)
						ageCPtTime += legTime;
					else
						ageDPtTime += legTime;
					if (person.getLicense().equals("yes"))
						withLicensePtTime += legTime;
					else
						withoutLicensePtTime += legTime;
					if (carAvail.equals("always"))
						alwaysPtTime += legTime;
					else if (carAvail.equals("sometimes"))
						sometimesPtTime += legTime;
					else
						neverPtTime += legTime;
					if (person.isEmployed())
						isEmployedPtTime += legTime;
					else
						notEmployedPtTime += legTime;
				}

		}
	}

	public void write(String outputFilename) {
		SimpleWriter sw = new SimpleWriter(outputFilename + ".txt");
		sw.writeln("Modal Split -- DailyTime");
		sw.writeln("category\tcar Time [km]\tpt Time [km]");
		sw.writeln("male\t" + maleCarTime + "\t" + malePtTime);
		sw.writeln("female\t" + femaleCarTime + "\t" + femalePtTime);
		sw.writeln("age<30\t" + ageACarTime + "\t" + ageAPtTime);
		sw.writeln("30<=age<50\t" + ageBCarTime + "\t" + ageBPtTime);
		sw.writeln("50<=age<70\t" + ageCCarTime + "\t" + ageCPtTime);
		sw.writeln("age>70\t" + ageDCarTime + "\t" + ageDPtTime);
		sw.writeln("with license\t" + withLicenseCarTime + "\t"
				+ withLicensePtTime);
		sw.writeln("without license\t" + withoutLicenseCarTime + "\t"
				+ withoutLicensePtTime);
		sw.writeln("always has a car\t" + alwaysCarTime + "\t" + alwaysPtTime);
		sw.writeln("sometimes has a car\t" + sometimesCarTime + "\t"
				+ sometimesPtTime);
		sw.writeln("never has a car\t" + neverCarTime + "\t" + neverPtTime);
		sw.writeln("employed\t" + sometimesCarTime + "\t" + sometimesPtTime);
		sw.writeln("not employed\t" + notEmployedCarTime + "\t"
				+ notEmployedPtTime);
		sw.writeln("-----------------------------");
		try {
			sw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		XYScatterChart chart = new XYScatterChart("Modal split -- daily Time",
				"pt fraction", "car fraction");
		chart.addSeries("male", new double[] { malePtTime * 100.0
				/ (maleCarTime + malePtTime) }, new double[] { maleCarTime
				* 100.0 / (maleCarTime + malePtTime) });
		chart.addSeries("female", new double[] { femalePtTime * 100.0
				/ (femaleCarTime + femalePtTime) },
				new double[] { femaleCarTime * 100.0
						/ (femaleCarTime + femalePtTime) });
		chart.addSeries("age<30", new double[] { ageAPtTime * 100.0
				/ (ageACarTime + ageAPtTime) }, new double[] { ageACarTime
				* 100.0 / (ageACarTime + ageAPtTime) });
		chart.addSeries("30<=age<50", new double[] { ageBPtTime * 100.0
				/ (ageBCarTime + ageBPtTime) }, new double[] { ageBCarTime
				* 100.0 / (ageBCarTime + ageBPtTime) });
		chart.addSeries("50<=age<70", new double[] { ageCPtTime * 100.0
				/ (ageCCarTime + ageCPtTime) }, new double[] { ageCCarTime
				* 100.0 / (ageCCarTime + ageCPtTime) });
		chart.addSeries("age>70", new double[] { ageDPtTime * 100.0
				/ (ageDCarTime + ageDPtTime) }, new double[] { ageDCarTime
				* 100.0 / (ageDCarTime + ageDPtTime) });
		chart.addSeries("with license", new double[] { withLicensePtTime
				* 100.0 / (withLicenseCarTime + withLicensePtTime) },
				new double[] { withLicenseCarTime * 100.0
						/ (withLicenseCarTime + withLicensePtTime) });
		chart.addSeries("without license", new double[] { withoutLicensePtTime
				* 100.0 / (withoutLicenseCarTime + withoutLicensePtTime) },
				new double[] { withoutLicenseCarTime * 100.0
						/ (withoutLicenseCarTime + withoutLicensePtTime) });
		chart.addSeries("always has a car", new double[] { alwaysPtTime * 100.0
				/ (alwaysCarTime + alwaysPtTime) },
				new double[] { alwaysCarTime * 100.0
						/ (alwaysCarTime + alwaysPtTime) });
		chart.addSeries("sometimes has a car", new double[] { sometimesPtTime
				* 100.0 / (sometimesCarTime + sometimesPtTime) },
				new double[] { sometimesCarTime * 100.0
						/ (sometimesCarTime + sometimesPtTime) });
		chart.addSeries("never has a car", new double[] { neverPtTime * 100.0
				/ (neverCarTime + neverPtTime) }, new double[] { neverCarTime
				* 100.0 / (neverCarTime + neverPtTime) });
		chart.addSeries("employed", new double[] { isEmployedPtTime * 100.0
				/ (isEmployedCarTime + isEmployedPtTime) },
				new double[] { isEmployedCarTime * 100.0
						/ (isEmployedCarTime + isEmployedPtTime) });
		chart.addSeries("not employed", new double[] { notEmployedPtTime
				* 100.0 / (notEmployedCarTime + notEmployedPtTime) },
				new double[] { notEmployedCarTime * 100.0
						/ (notEmployedCarTime + notEmployedPtTime) });
		chart.saveAsPng(outputFilename + ".png", 1200, 900);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Gbl.startMeasurement();

		final String netFilename = "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml";
		final String plansFilename = "../runs_SVN/run669/it.1000/1000.plans.xml.gz";
		final String outputFilename = "../runs_SVN/run669/it.1000/ModalSplitDailyTime";

		Gbl.createConfig(null);

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);

		Population population = new Population();

		DailyEnRouteTimeModalSplit ddms = new DailyEnRouteTimeModalSplit();
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
