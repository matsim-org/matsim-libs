package playground.gthunig.SrvTripSampler;

import org.matsim.contrib.util.CSVReaders;
import playground.gthunig.utils.CSVWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author GabrielT on 07.11.2016.
 */
public class SrvTripSampler {

	public static void main(String[] args) {

		String directory = "C:/Users/GabrielT/Desktop/survey/";
		String personsFile = "P2008_Berlin2.dat";
		String tripsFile = "W2008_Berlin_Weekday.dat";

		List<String[]> personsContent = CSVReaders.readFile(directory + personsFile, ';');
		personsContent.remove(0);
		List<String> hnr2pid = new ArrayList<>();

		for (String[] line : personsContent) {
			hnr2pid.add(line[2] + "." + line[3]);
		}

		List<String[]> tripsContent = CSVReaders.readFile(directory + tripsFile, ';');

		for (int i = 1; i < tripsContent.size(); i++) {
			String[] line = tripsContent.get(i);
			if (!hnr2pid.contains(line[2] + "." + line[3])) {
				tripsContent.remove(i);
				i--;
			}
		}

		CSVWriter writer = new CSVWriter(directory + "W2008_Berlin_Weekday_Sample.dat", ";");
		for (String[] line : tripsContent) {
			writer.writeLine(line);
		}
		writer.close();

	}

}
