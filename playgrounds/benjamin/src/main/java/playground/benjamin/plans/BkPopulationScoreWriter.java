package playground.benjamin.plans;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.charts.XYScatterChart;
import org.matsim.core.utils.io.IOUtils;

public class BkPopulationScoreWriter {

	private Map<Id, Double> idsScoresMap;

	//constructor
	public BkPopulationScoreWriter(Map<Id, Double> idsScoresMap) {
		this.idsScoresMap = idsScoresMap;
	}

	public void writeChart(String filename) {
		
		//arrays (double is demanded by chart.addSeries) with size equal to the key-value-pairs of the idsScoresMap
		double[] personIds = new double[idsScoresMap.size()];
		double[] utilities = new double[idsScoresMap.size()];
		
		//functions that convert the Ids and the Scores from the map to 2 double arrays
		convertPersonIdsToDouble(personIds);
		convertScoresToDouble(utilities);
		
		XYScatterChart chart = new XYScatterChart("utility per person", "personId", "utility from selected plan");
		chart.addSeries("utility per person", personIds, utilities);	
		chart.saveAsPng(filename, 800, 600);
		}

	public void writeTxt(String filename) {
		StringBuffer buffer = new StringBuffer();
		
		for (Id personId : idsScoresMap.keySet()){
			buffer.append(personId);
			buffer.append("\t");
			buffer.append(idsScoresMap.get(personId).doubleValue());
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
	
	private void convertScoresToDouble(double[] utilities) {
		int i = 0;
		for (Double score : idsScoresMap.values()){
			utilities[i] = score;
			i++;
		}
	}

	private void convertPersonIdsToDouble(double[] personIds) {
		int i = 0;
		for (Id id : idsScoresMap.keySet()){
			personIds[i] = Double.parseDouble(id.toString());
			i++;
		}
	}
}
