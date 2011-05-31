package sandbox;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import kid.KiDDataReader;
import kid.ScheduledVehicles;
import kid.filter.BusinessSectorFilter;

import org.apache.commons.math.stat.Frequency;

import sandbox.Classifier.FrequencyClass;

public class StatRunner {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException {
		ScheduledVehicles vehicles = new ScheduledVehicles();
		KiDDataReader dataReader = new KiDDataReader(vehicles);
		String directory = "/Volumes/projekte/2000-Watt-City/Daten/KiD/";
		dataReader.setVehicleFile(directory + "KiD_2002_Fahrzeug-Datei.txt");
		dataReader.setTransportChainFile(directory + "KiD_2002_Fahrtenketten-Datei.txt");
		dataReader.setTransportLegFile(directory + "KiD_2002_(Einzel)Fahrten-Datei.txt");
		dataReader.setVehicleFilter(new BusinessSectorFilter("D"));
		dataReader.run();
		VehicleStats stats = new VehicleStats(vehicles);
		System.out.println("correlation=" + stats.computeVehicleCorrelation());
		System.out.println("corr(employees,pkw)=" + stats.computeCorrelation(stats.getEmployees(), stats.getPkw()));
		System.out.println("corr(employees,lkw)=" + stats.computeCorrelation(stats.getEmployees(), stats.getLkw()));
		
		List<FrequencyClass> classes = new ArrayList<FrequencyClass>();
		classes.add(new FrequencyClass(1, 0.0, 20.0));
		classes.add(new FrequencyClass(2, 20.0, 50.0));
		classes.add(new FrequencyClass(3, 50.0, 250.0));
		classes.add(new FrequencyClass(4, 250.0, 1000.0));
		classes.add(new FrequencyClass(5, 1000.0, null));
//		Classifier classifier = new Classifier(stats.getEmployees());
//		Frequency frequency = classifier.makeFrequency(classes);
		Plotter plotter = new Plotter();
//		plotter.plotCounts("output/employees_counts.png","output/employees_counts_legend.txt", "employees", "emplClasses", "frequency", frequency);
//		plotter.plotCumPct("output/employees_cumPct.png","output/employees_cumPct_legend.txt", "employees", "emplClasses", "frequency", frequency);
		
		Frequency conditionalFrequency = Classifier.makeConditionalFrequency(new FrequencyClass(1, 20.0, 50.0), stats.getEmployees(), stats.getLkw());
		plotter.plotCounts("output/smallCompanies_lkw_counts.png","output/smallCompanies_lkw_counts_legend.txt", "lkws-tit", "lkws", "nOfLkws", conditionalFrequency);
		plotter.plotCumPct("output/smallCompanies_lkw_cumPct.png","output/smallCompanies_lkw_cumPct_legend.txt", "lkws_tit", "lkws", "percent", conditionalFrequency);
		
//		stats.plot("output/lkw.png", "lkw", classifier.classify(10, 10));
//		Classifier pkw_classifier = new Classifier(stats.getPkw());
//		stats.plot("output/pkw.png", "pkw", pkw_classifier.classify(10, 10));
		
		
		
	}

}
