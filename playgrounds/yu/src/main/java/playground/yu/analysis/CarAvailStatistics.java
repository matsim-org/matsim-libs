/**
 *
 */
package playground.yu.analysis;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.utils.charts.BarChart;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;

import playground.yu.utils.io.SimpleWriter;

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
		this.male_al = 0;
		this.male_so = 0;
		this.male_ne = 0;
		this.female_al = 0;
		this.female_so = 0;
		this.female_ne = 0;
		this.ageA_al = 0;
		this.ageB_al = 0;
		this.ageC_al = 0;
		this.ageD_al = 0;
		this.ageA_so = 0;
		this.ageB_so = 0;
		this.ageC_so = 0;
		this.ageD_so = 0;
		this.ageA_ne = 0;
		this.ageB_ne = 0;
		this.ageC_ne = 0;
		this.ageD_ne = 0;
		this.withLicense_al = 0;
		this.withoutLicense_al = 0;
		this.withLicense_so = 0;
		this.withoutLicense_so = 0;
		this.withLicense_ne = 0;
		this.withoutLicense_ne = 0;
		this.isEmployed_al = 0;
		this.isEmployed_so = 0;
		this.isEmployed_ne = 0;
		this.notEmployed_al = 0;
		this.notEmployed_so = 0;
		this.notEmployed_ne = 0;
	}

	@Override
	public void run(final Person p) {
		PersonImpl person = (PersonImpl) p;
		String carAvail = person.getCarAvail();
		int age = person.getAge();
		String license = person.getLicense();
		boolean isEmployed = person.isEmployed();
		if (carAvail != null) {
			if (carAvail.equals("always")) {
				if (person.getSex().equals("m")) {
					this.male_al++;
				} else {
					this.female_al++;
				}
				if (age < 30) {
					this.ageA_al++;
				} else if (age >= 30 && age < 50) {
					this.ageB_al++;
				} else if (age >= 50 && age < 70) {
					this.ageC_al++;
				} else {
					this.ageD_al++;
				}
				if (license.equals("yes")) {
					this.withLicense_al++;
				} else {
					this.withoutLicense_al++;
				}
				if (isEmployed) {
					this.isEmployed_al++;
				} else {
					this.notEmployed_al++;
				}
			} else if (carAvail.equals("sometimes")) {
				if (person.getSex().equals("m")) {
					this.male_so++;
				} else {
					this.female_so++;
				}
				if (age < 30) {
					this.ageA_so++;
				} else if (age >= 30 && age < 50) {
					this.ageB_so++;
				} else if (age >= 50 && age < 70) {
					this.ageC_so++;
				} else {
					this.ageD_so++;
				}
				if (license.equals("yes")) {
					this.withLicense_so++;
				} else {
					this.withoutLicense_so++;
				}
				if (isEmployed) {
					this.isEmployed_so++;
				} else {
					this.notEmployed_so++;
				}
			} else if (carAvail.equals("never")) {
				if (person.getSex().equals("m")) {
					this.male_ne++;
				} else {
					this.female_ne++;
				}
				if (age < 30) {
					this.ageA_ne++;
				} else if (age >= 30 && age < 50) {
					this.ageB_ne++;
				} else if (age >= 50 && age < 70) {
					this.ageC_ne++;
				} else {
					this.ageD_ne++;
				}
				if (license.equals("yes")) {
					this.withLicense_ne++;
				} else {
					this.withoutLicense_ne++;
				}
				if (isEmployed) {
					this.isEmployed_ne++;
				} else {
					this.notEmployed_ne++;
				}
			}
		}
	}

	public void write(final String outputFilename) {
		SimpleWriter sw = new SimpleWriter(outputFilename + ".txt");
		sw.writeln("car_avail--always");
		sw
				.writeln("\tmale\tfemale\tage<30\t30<=age<50\t50<=age<70\tage>70\twith license\twithout license\tis employed\tnot employed");
		sw.writeln("\t" + this.male_al + "\t" + this.female_al + "\t"
				+ this.ageA_al + "\t" + this.ageB_al + "\t" + this.ageC_al
				+ "\t" + this.ageD_al + "\t" + this.withLicense_al + "\t"
				+ this.withoutLicense_al + "\t" + this.isEmployed_al + "\t"
				+ this.notEmployed_al);
		sw.writeln("-----------------------------");
		sw.writeln("car_avail--sometimes");
		sw
				.writeln("\tmale\tfemale\tage<30\t30<=age<50\t50<=age<70\tage>70\twith license\twithout license\tis employed\tnot employed");
		sw.writeln("\t" + this.male_so + "\t" + this.female_so + "\t"
				+ this.ageA_so + "\t" + this.ageB_so + "\t" + this.ageC_so
				+ "\t" + this.ageD_so + "\t" + this.withLicense_so + "\t"
				+ this.withoutLicense_so + "\t" + this.isEmployed_so + "\t"
				+ this.notEmployed_so);
		sw.writeln("-----------------------------");
		sw.writeln("car_avail--never");
		sw
				.writeln("\tmale\tfemale\tage<30\t30<=age<50\t50<=age<70\tage>70\twith license\twithout license\tis employed\tnot employed");
		sw.writeln("\t" + this.male_ne + "\t" + this.female_ne + "\t"
				+ this.ageA_ne + "\t" + this.ageB_ne + "\t" + this.ageC_ne
				+ "\t" + this.ageD_ne + "\t" + this.withLicense_ne + "\t"
				+ this.withoutLicense_ne + "\t" + this.isEmployed_ne + "\t"
				+ this.notEmployed_ne);
		sw.writeln("-----------------------------");
		sw.close();
		BarChart chart = new BarChart("Car Avail", "categories",
				"Car Availability %", new String[] { "male", "female",
						"age<30", "30<=age<50", "50<=age<70", "age>70",
						"with license", "without license", "is employed",
						"not employed" });
		chart
				.addSeries(
						"always",
						new double[] {
								this.male_al
										/ (this.male_al + this.male_ne + this.male_so)
										* 100.0,
								this.female_al
										/ (this.female_al + this.female_ne + this.female_so)
										* 100.0,
								this.ageA_al
										/ (this.ageA_al + this.ageA_ne + this.ageA_so)
										* 100.0,
								this.ageB_al
										/ (this.ageB_al + this.ageB_ne + this.ageB_so)
										* 100.0,
								this.ageC_al
										/ (this.ageC_al + this.ageC_ne + this.ageC_so)
										* 100.0,
								this.ageD_al
										/ (this.ageD_al + this.ageD_ne + this.ageD_so)
										* 100.0,
								this.withLicense_al
										/ (this.withLicense_al
												+ this.withLicense_ne + this.withLicense_so)
										* 100.0,
								this.withoutLicense_al
										/ (this.withoutLicense_al
												+ this.withoutLicense_ne + this.withoutLicense_so)
										* 100.0,
								this.isEmployed_al
										/ (this.isEmployed_al
												+ this.isEmployed_ne + this.isEmployed_so)
										* 100.0,
								this.notEmployed_al
										/ (this.notEmployed_al
												+ this.notEmployed_ne + this.notEmployed_so)
										* 100.0 });
		chart
				.addSeries(
						"sometimes",
						new double[] {
								this.male_so
										/ (this.male_al + this.male_ne + this.male_so)
										* 100.0,
								this.female_so
										/ (this.female_al + this.female_ne + this.female_so)
										* 100.0,
								this.ageA_so
										/ (this.ageA_al + this.ageA_ne + this.ageA_so)
										* 100.0,
								this.ageB_so
										/ (this.ageB_al + this.ageB_ne + this.ageB_so)
										* 100.0,
								this.ageC_so
										/ (this.ageC_al + this.ageC_ne + this.ageC_so)
										* 100.0,
								this.ageD_so
										/ (this.ageD_al + this.ageD_ne + this.ageD_so)
										* 100.0,
								this.withLicense_so
										/ (this.withLicense_al
												+ this.withLicense_ne + this.withLicense_so)
										* 100.0,
								this.withoutLicense_so
										/ (this.withoutLicense_al
												+ this.withoutLicense_ne + this.withoutLicense_so)
										* 100.0,
								this.isEmployed_so
										/ (this.isEmployed_al
												+ this.isEmployed_ne + this.isEmployed_so)
										* 100.0,
								this.notEmployed_so
										/ (this.notEmployed_al
												+ this.notEmployed_ne + this.notEmployed_so)
										* 100.0 });
		chart
				.addSeries(
						"never",
						new double[] {
								this.male_ne
										/ (this.male_al + this.male_ne + this.male_so)
										* 100.0,
								this.female_ne
										/ (this.female_al + this.female_ne + this.female_so)
										* 100.0,
								this.ageA_ne
										/ (this.ageA_al + this.ageA_ne + this.ageA_so)
										* 100.0,
								this.ageB_ne
										/ (this.ageB_al + this.ageB_ne + this.ageB_so)
										* 100.0,
								this.ageC_ne
										/ (this.ageC_al + this.ageC_ne + this.ageC_so)
										* 100.0,
								this.ageD_ne
										/ (this.ageD_al + this.ageD_ne + this.ageD_so)
										* 100.0,
								this.withLicense_ne
										/ (this.withLicense_al
												+ this.withLicense_ne + this.withLicense_so)
										* 100.0,
								this.withoutLicense_ne
										/ (this.withoutLicense_al
												+ this.withoutLicense_ne + this.withoutLicense_so)
										* 100.0,
								this.isEmployed_ne
										/ (this.isEmployed_al
												+ this.isEmployed_ne + this.isEmployed_so)
										* 100.0,
								this.notEmployed_ne
										/ (this.notEmployed_al
												+ this.notEmployed_ne + this.notEmployed_so)
										* 100.0 });
		chart.addMatsimLogo();
		chart.saveAsPng(outputFilename + ".png", 1200, 900);
	}

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		Gbl.startMeasurement();

		final String netFilename = "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml";
		final String plansFilename = "../runs_SVN/run669/it.1000/1000.plans.xml.gz";
		final String outputFilename = "../runs_SVN/run669/it.1000/CarAvail";

		ScenarioImpl scenario = new ScenarioImpl();
		new MatsimNetworkReader(scenario).readFile(netFilename);

		CarAvailStatistics cas = new CarAvailStatistics();

		System.out.println("-->reading plansfile: " + plansFilename);
		new MatsimPopulationReader(scenario).readFile(plansFilename);

		cas.run(scenario.getPopulation());

		cas.write(outputFilename);

		System.out.println("--> Done!");
		Gbl.printElapsedTime();
		System.exit(0);
	}

}
