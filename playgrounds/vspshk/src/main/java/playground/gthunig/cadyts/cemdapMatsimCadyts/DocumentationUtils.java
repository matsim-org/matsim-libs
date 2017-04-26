package playground.gthunig.cadyts.cemdapMatsimCadyts;

import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Gabriel Thunig
 */
public class DocumentationUtils {

	public static final Logger log = Logger.getLogger(DocumentationUtils.class);

	private static final String SEPERATOR = "\t-\t";

	public static void main(String[] args) {
		String rootRunDirectory = "../../../runs-svn/berlin_scenario_2016/be_109";
		List<String> tripAnalyzerExtendedAnalysisDirectories = new ArrayList<>();
		tripAnalyzerExtendedAnalysisDirectories.add("analysis_300");
		tripAnalyzerExtendedAnalysisDirectories.add("analysis_300_ber_dist");

		String rootRunDirectory2 = "../../../runs-svn/berlin_scenario_2016/be_109a";
		List<String> tripAnalyzerExtendedAnalysisDirectories2 = new ArrayList<>();
		tripAnalyzerExtendedAnalysisDirectories2.add("analysis_300");
		tripAnalyzerExtendedAnalysisDirectories2.add("analysis_300_ber_dist");
		try {
			List<String> output = searchDocumentationValues(rootRunDirectory, tripAnalyzerExtendedAnalysisDirectories);
			List<String> output2 = searchDocumentationValues(rootRunDirectory2, tripAnalyzerExtendedAnalysisDirectories2);

			writeOutput(output2);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@SafeVarargs
	private static void writeOutput(List<String>... outputLists) {
		List<String> output = new ArrayList<>();

		boolean instantiated = false;
		for (List<String> outputList : outputLists) {
			if (!instantiated) {
				for (String outputLine : outputList) {
					output.add(outputLine);
				}
				instantiated = true;
			} else {
				for (int i = 0; i < outputList.size(); i++) {
					output.set(i, output.get(i) + SEPERATOR + outputList.get(i));
				}
			}
		}

		output.forEach(System.out::println);
	}

	private static void compare(List<String> output1, List<String> output2) {

	}

	private static List<String> searchDocumentationValues(String rootRunDirectory, List<String> tripAnalyzerExtendedAnalysisDirectories) throws IOException {
		rootRunDirectory += "/";
		List<String> result = new ArrayList<>();
		log.info("Start searching for documentation values in \"" + rootRunDirectory + "\"");

		String calibrationStats = "calibration-stats.txt";
		File calibrationStatsFile = new File(rootRunDirectory + calibrationStats);
		log.info("Searching for \"" + calibrationStatsFile.getAbsolutePath() + "\"");
		if(calibrationStatsFile.exists() && !calibrationStatsFile.isDirectory()) {
			List<String> lines = Files.readAllLines(calibrationStatsFile.toPath());
			String line = lines.get(lines.size()-1);
			String[] splitLine = line.split("	");
			String totalLL = splitLine[3];
			result.add("logLike after final it: " + formatInput(totalLL));
			result.add("logLike/measure: auto");
		} else {
			result.add("Did not found \"" + calibrationStats + "\" at \"" + calibrationStatsFile.getAbsolutePath()+ "\"");
		}

		String selectedPlans = "analysis/selectedPlans.txt";
		File selectedPlansFile = new File(rootRunDirectory + selectedPlans);
		if(selectedPlansFile.exists() && !selectedPlansFile.isDirectory()) {
			List<String> lines = Files.readAllLines(selectedPlansFile.toPath());
			String line = lines.get(lines.size()-1);
			String[] splitLine = line.split("	");
			String stayHomePlans = splitLine[1];
			result.add("Plan Selection SH: " + formatInput(stayHomePlans));
			String otherPlans = splitLine[2];
			result.add("Plan Selection Other: " + formatInput(otherPlans));
		} else {
			result.add("-");
			result.add("-");
		}

		searchTripAnalyzerExtendedDocumentationValues(result, rootRunDirectory, tripAnalyzerExtendedAnalysisDirectories);

		String runName = extractRunName(rootRunDirectory);
		String scoreStats = runName + ".scorestats.txt";
		File scoreStatsFile = new File(rootRunDirectory + scoreStats);
		if(scoreStatsFile.exists() && !scoreStatsFile.isDirectory()) {
			List<String> lines = Files.readAllLines(scoreStatsFile.toPath());
			String line = lines.get(lines.size()-1);
			String[] splitLine = line.split("	");
			String avgExec = splitLine[1];
			result.add("Avg Exec Score: " + formatInput(avgExec));
		}

		return result;
	}

	private static String extractRunName(String rootRunDirectory) {
		String[] splitRootRunDirectory = rootRunDirectory.split("/");
		String runName = splitRootRunDirectory[splitRootRunDirectory.length - 1];
		log.info("Expecting the run name to be \"" + runName + "\"");
		return runName;
	}

	private static void searchTripAnalyzerExtendedDocumentationValues(List<String> result,
																	  String rootRunDirectory,
																	  List<String> tripAnalyzerExtendedAnalysisDirectories) throws IOException {

		int i = 0;
		for (String currentSubDirectory : tripAnalyzerExtendedAnalysisDirectories) {

			String otherInformation = "otherInformation.txt";
			File otherInformationFile = new File(rootRunDirectory + currentSubDirectory + "/" + otherInformation);
			if(i==0 && otherInformationFile.exists() && !otherInformationFile.isDirectory()) {
				List<String> lines = Files.readAllLines(otherInformationFile.toPath());
				String line = lines.get(1);
				String[] splitLine = line.split("	");
				String incompleteTrips = splitLine[splitLine.length-1];
				result.add("Incomplete Trips: " + formatInput(incompleteTrips));
				result.add("Non-removed Agents: auto");
				String line2 = lines.get(0);
				String[] splitLine2 = line2.split("	");
				String completeTrips = splitLine2[splitLine2.length-1];
				result.add("Column BJ: " + formatInput(completeTrips));
				result.add("Complete Trips (m): auto");
				result.add("Trips per person: auto");
			} else {
				List<String> lines = Files.readAllLines(otherInformationFile.toPath());
				String line2 = lines.get(0);
				String[] splitLine2 = line2.split("	");
				String completeTrips = splitLine2[splitLine2.length-1];
				result.add("Column BM: " + formatInput(completeTrips));
			}

			result.add("Analysis " + i);

			String beeline = "beeline.txt";
			File beelineFile = new File(rootRunDirectory + currentSubDirectory + "/" + beeline);
			if(beelineFile.exists() && !beelineFile.isDirectory()) {
				List<String> lines = Files.readAllLines(beelineFile.toPath());
				String line = lines.get(lines.size()-2);
				String[] splitLine = line.split(" ");
				String avgBeelineRatio = splitLine[splitLine.length-1];
				result.add(formatInput(avgBeelineRatio));
			}

			String tripDistanceBeeline = "tripDistanceBeeline.txt";
			File tripDistanceBeelineFile = new File(rootRunDirectory + currentSubDirectory + "/" + tripDistanceBeeline);
			if(tripDistanceBeelineFile.exists() && !tripDistanceBeelineFile.isDirectory()) {
				List<String> lines = Files.readAllLines(tripDistanceBeelineFile.toPath());
				String line = lines.get(lines.size()-2);
				String[] splitLine = line.split(" ");
				String avgDistanceBeeline = splitLine[splitLine.length-1];
				result.add(formatInput(avgDistanceBeeline));
			}

			String tripDuration = "tripDuration.txt";
			File tripDurationFile = new File(rootRunDirectory + currentSubDirectory + "/" + tripDuration);
			if(tripDurationFile.exists() && !tripDurationFile.isDirectory()) {
				List<String> lines = Files.readAllLines(tripDurationFile.toPath());
				String line = lines.get(lines.size()-2);
				String[] splitLine = line.split(" ");
				String avgDuration = splitLine[splitLine.length-1];
				result.add(formatInput(avgDuration));
			}
			result.add("/Analysis " + i++);
		}

	}

	private static String formatInput(String input) {
		return input.replace('.', ',');
	}
}
