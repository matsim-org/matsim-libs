/**
 * 
 */
package playground.yu.analysis;

import java.io.IOException;

import org.matsim.gbl.Gbl;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.Population;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.utils.charts.BarChart;

import playground.yu.utils.SimpleWriter;

/**
 * @author yu
 * 
 */
public class CarAvailStatistics extends AbstractPersonAlgorithm {
	private double male_al, male_so, male_ne, female_al, female_so, female_ne;
	private double ageA_al, ageB_al, ageC_al, ageD_al, ageA_so, ageB_so,
			ageC_so, ageD_so, ageA_ne, ageB_ne, ageC_ne, ageD_ne;
	private double withLicense_al, withoutLicense_al, withLicense_so,
			withoutLicense_so, withLicense_ne, withoutLicense_ne;
	private double isEmployed_al, isEmployed_so, isEmployed_ne, notEmployed_al,
			notEmployed_so, notEmployed_ne;

	public CarAvailStatistics() {
		male_al = 0;
		male_so = 0;
		male_ne = 0;
		female_al = 0;
		female_so = 0;
		female_ne = 0;
		ageA_al = 0;
		ageB_al = 0;
		ageC_al = 0;
		ageD_al = 0;
		ageA_so = 0;
		ageB_so = 0;
		ageC_so = 0;
		ageD_so = 0;
		ageA_ne = 0;
		ageB_ne = 0;
		ageC_ne = 0;
		ageD_ne = 0;
		withLicense_al = 0;
		withoutLicense_al = 0;
		withLicense_so = 0;
		withoutLicense_so = 0;
		withLicense_ne = 0;
		withoutLicense_ne = 0;
		isEmployed_al = 0;
		isEmployed_so = 0;
		isEmployed_ne = 0;
		notEmployed_al = 0;
		notEmployed_so = 0;
		notEmployed_ne = 0;
	}

	@Override
	public void run(Person person) {
		String carAvail = person.getCarAvail();
		int age = person.getAge();
		String license = person.getLicense();
		boolean isEmployed = person.isEmployed();
		if (carAvail != null) {
			if (carAvail.equals("always")) {
				if (person.getSex().equals("m")) {
					male_al++;
				} else {
					female_al++;
				}
				if (age < 30) {
					ageA_al++;
				} else if (age >= 30 && age < 50) {
					ageB_al++;
				} else if (age >= 50 && age < 70) {
					ageC_al++;
				} else {
					ageD_al++;
				}
				if (license.equals("yes")) {
					withLicense_al++;
				} else {
					withoutLicense_al++;
				}
				if (isEmployed) {
					isEmployed_al++;
				} else {
					notEmployed_al++;
				}
			} else if (carAvail.equals("sometimes")) {
				if (person.getSex().equals("m")) {
					male_so++;
				} else {
					female_so++;
				}
				if (age < 30) {
					ageA_so++;
				} else if (age >= 30 && age < 50) {
					ageB_so++;
				} else if (age >= 50 && age < 70) {
					ageC_so++;
				} else {
					ageD_so++;
				}
				if (license.equals("yes")) {
					withLicense_so++;
				} else {
					withoutLicense_so++;
				}
				if (isEmployed) {
					isEmployed_so++;
				} else {
					notEmployed_so++;
				}
			} else if (carAvail.equals("never")) {
				if (person.getSex().equals("m")) {
					male_ne++;
				} else {
					female_ne++;
				}
				if (age < 30) {
					ageA_ne++;
				} else if (age >= 30 && age < 50) {
					ageB_ne++;
				} else if (age >= 50 && age < 70) {
					ageC_ne++;
				} else {
					ageD_ne++;
				}
				if (license.equals("yes")) {
					withLicense_ne++;
				} else {
					withoutLicense_ne++;
				}
				if (isEmployed) {
					isEmployed_ne++;
				} else {
					notEmployed_ne++;
				}
			}
		}
	}

	public void write(String outputFilename) {
		SimpleWriter sw = new SimpleWriter(outputFilename + ".txt");
		sw.writeln("car_avail--always");
		sw
				.writeln("\tmale\tfemale\tage<30\t30<=age<50\t50<=age<70\tage>70\twith license\twithout license\tis employed\tnot employed");
		sw.writeln("\t" + male_al + "\t" + female_al + "\t" + ageA_al + "\t"
				+ ageB_al + "\t" + ageC_al + "\t" + ageD_al + "\t"
				+ withLicense_al + "\t" + withoutLicense_al + "\t"
				+ isEmployed_al + "\t" + notEmployed_al);
		sw.writeln("-----------------------------");
		sw.writeln("car_avail--sometimes");
		sw
				.writeln("\tmale\tfemale\tage<30\t30<=age<50\t50<=age<70\tage>70\twith license\twithout license\tis employed\tnot employed");
		sw.writeln("\t" + male_so + "\t" + female_so + "\t" + ageA_so + "\t"
				+ ageB_so + "\t" + ageC_so + "\t" + ageD_so + "\t"
				+ withLicense_so + "\t" + withoutLicense_so + "\t"
				+ isEmployed_so + "\t" + notEmployed_so);
		sw.writeln("-----------------------------");
		sw.writeln("car_avail--never");
		sw
				.writeln("\tmale\tfemale\tage<30\t30<=age<50\t50<=age<70\tage>70\twith license\twithout license\tis employed\tnot employed");
		sw.writeln("\t" + male_ne + "\t" + female_ne + "\t" + ageA_ne + "\t"
				+ ageB_ne + "\t" + ageC_ne + "\t" + ageD_ne + "\t"
				+ withLicense_ne + "\t" + withoutLicense_ne + "\t"
				+ isEmployed_ne + "\t" + notEmployed_ne);
		sw.writeln("-----------------------------");
		// TODO fractions
		try {
			sw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		BarChart chart = new BarChart("Car Avail", "categories",
				"Car Availability %", new String[] { "male", "female",
						"age<30", "30<=age<50", "50<=age<70", "age>70",
						"with license", "without license", "is employed",
						"not employed" });
		chart
				.addSeries(
						"always",
						new double[] {
								male_al / (male_al + male_ne + male_so) * 100.0,
								female_al / (female_al + female_ne + female_so)
										* 100.0,
								ageA_al / (ageA_al + ageA_ne + ageA_so) * 100.0,
								ageB_al / (ageB_al + ageB_ne + ageB_so) * 100.0,
								ageC_al / (ageC_al + ageC_ne + ageC_so) * 100.0,
								ageD_al / (ageD_al + ageD_ne + ageD_so) * 100.0,
								withLicense_al
										/ (withLicense_al + withLicense_ne + withLicense_so)
										* 100.0,
								withoutLicense_al
										/ (withoutLicense_al
												+ withoutLicense_ne + withoutLicense_so)
										* 100.0,
								isEmployed_al
										/ (isEmployed_al + isEmployed_ne + isEmployed_so)
										* 100.0,
								notEmployed_al
										/ (notEmployed_al + notEmployed_ne + notEmployed_so)
										* 100.0 });
		chart
				.addSeries(
						"sometimes",
						new double[] {
								male_so / (male_al + male_ne + male_so) * 100.0,
								female_so / (female_al + female_ne + female_so)
										* 100.0,
								ageA_so / (ageA_al + ageA_ne + ageA_so) * 100.0,
								ageB_so / (ageB_al + ageB_ne + ageB_so) * 100.0,
								ageC_so / (ageC_al + ageC_ne + ageC_so) * 100.0,
								ageD_so / (ageD_al + ageD_ne + ageD_so) * 100.0,
								withLicense_so
										/ (withLicense_al + withLicense_ne + withLicense_so)
										* 100.0,
								withoutLicense_so
										/ (withoutLicense_al
												+ withoutLicense_ne + withoutLicense_so)
										* 100.0,
								isEmployed_so
										/ (isEmployed_al + isEmployed_ne + isEmployed_so)
										* 100.0,
								notEmployed_so
										/ (notEmployed_al + notEmployed_ne + notEmployed_so)
										* 100.0 });
		chart
				.addSeries(
						"never",
						new double[] {
								male_ne / (male_al + male_ne + male_so) * 100.0,
								female_ne / (female_al + female_ne + female_so)
										* 100.0,
								ageA_ne / (ageA_al + ageA_ne + ageA_so) * 100.0,
								ageB_ne / (ageB_al + ageB_ne + ageB_so) * 100.0,
								ageC_ne / (ageC_al + ageC_ne + ageC_so) * 100.0,
								ageD_ne / (ageD_al + ageD_ne + ageD_so) * 100.0,
								withLicense_ne
										/ (withLicense_al + withLicense_ne + withLicense_so)
										* 100.0,
								withoutLicense_ne
										/ (withoutLicense_al
												+ withoutLicense_ne + withoutLicense_so)
										* 100.0,
								isEmployed_ne
										/ (isEmployed_al + isEmployed_ne + isEmployed_so)
										* 100.0,
								notEmployed_ne
										/ (notEmployed_al + notEmployed_ne + notEmployed_so)
										* 100.0 });
		chart.addMatsimLogo();
		chart.saveAsPng(outputFilename + ".png", 1200, 900);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Gbl.startMeasurement();

		final String netFilename = "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml";
		final String plansFilename = "../runs_SVN/run669/it.1000/1000.plans.xml.gz";
		final String outputFilename = "../runs_SVN/run669/it.1000/CarAvail";

		Gbl.createConfig(null);

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);

		Population population = new Population();

		CarAvailStatistics cas = new CarAvailStatistics();
		population.addAlgorithm(cas);

		System.out.println("-->reading plansfile: " + plansFilename);
		new MatsimPopulationReader(population, network).readFile(plansFilename);

		population.runAlgorithms();

		cas.write(outputFilename);

		System.out.println("--> Done!");
		Gbl.printElapsedTime();
		System.exit(0);
	}

}
