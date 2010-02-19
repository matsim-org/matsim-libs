package playground.benjamin.plans;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.charts.XYScatterChart;
import org.matsim.core.utils.io.IOUtils;

public class BkPopulationScoreDifferenceWriter {

	private Map<Id, Double> idsScoreDifferencesMap;

	//constructor
	public BkPopulationScoreDifferenceWriter(Map<Id,Double> scoreDifferences) {
		this.idsScoreDifferencesMap = scoreDifferences;
	}

	public void writeChart(String filename) {
		
		//arrays (double is demanded by chart.addSeries) with size equal to the key-value-pairs of the idsScoresMap
		double[] personIds = new double[idsScoreDifferencesMap.size()];
		double[] utilityDifferences = new double[idsScoreDifferencesMap.size()];
		
		//functions that convert the Ids and the Scores from the map to 2 double arrays
		convertPersonIdsToDoubleArray(personIds);
		convertScoreDifferencesToDoubleArray(utilityDifferences);
		
		XYScatterChart chart = new XYScatterChart("utility difference per person", "personId", "utility difference per person from selected plan");
		chart.addSeries("utility difference per person", personIds, utilityDifferences);	
		chart.saveAsPng(filename, 800, 600);
		}

	public void writeTxt(String filename) {
		StringBuffer buffer = new StringBuffer();
		
		for (Id personId : idsScoreDifferencesMap.keySet()){
			buffer.append(personId);
			buffer.append("\t");
			buffer.append(idsScoreDifferencesMap.get(personId).doubleValue());
			buffer.append("\n");
		}
		
		try {
			BufferedWriter writer = IOUtils.getBufferedWriter(filename);
			writer.write(buffer.toString());
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void convertPersonIdsToDoubleArray(double[] personIds) {
		int i = 0;
		for (Id id : idsScoreDifferencesMap.keySet()){
			personIds[i] = Double.parseDouble(id.toString());
			i++;
		}
	}
	
	private void convertScoreDifferencesToDoubleArray(double[] utilityDifference) {
		int i = 0;
		for (Double scoreDiffernece : idsScoreDifferencesMap.values()){
			utilityDifference[i] = scoreDiffernece;
			i++;
		}
	}
}
