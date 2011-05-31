package sandbox;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.math.stat.Frequency;
import org.apache.log4j.Logger;
import org.matsim.core.utils.charts.BarChart;
import org.matsim.core.utils.io.IOUtils;

public class Plotter {
	
	static class LegendEntry {
		private String classNumber;
		
		private String value;

		public LegendEntry(String classNumber, String value) {
			super();
			this.classNumber = classNumber;
			this.value = value;
		}

		public String getClassNumber() {
			return classNumber;
		}

		public String getValue() {
			return value;
		}
		
		
	}
	
	private static Logger log = Logger.getLogger(Plotter.class);
	
	private List<LegendEntry> legend = new ArrayList<LegendEntry>();
	
	public void plotCounts(String filename, String legendName, String title, String x, String y, Frequency frequency) throws FileNotFoundException, IOException{
		legend.clear();
		BarChart barChart = new BarChart(title, x, y);
		barChart.addSeries(title, getCountedValues(frequency));
		barChart.saveAsPng(filename, 800, 600);
		writeLegend(legendName);
	}
	
	public void plotCumPct(String filename, String legendName, String title, String x, String y, Frequency frequency) throws FileNotFoundException, IOException{
		legend.clear();
		BarChart barChart = new BarChart(title, x, y);
		barChart.addSeries(title, getCumPct(frequency));
		barChart.saveAsPng(filename, 800, 600);
		writeLegend(legendName);
	}
	
	private void writeLegend(String legendName) throws FileNotFoundException, IOException {
		BufferedWriter writer = IOUtils.getBufferedWriter(legendName);
		for(LegendEntry entry : legend){
			writer.write(entry.getClassNumber() + "\t" + entry.getValue() + "\n");
		}
		writer.close();
	}

	private double[] getCumPct(Frequency frequency) {
		double[] values = new double[frequency.getUniqueCount()];
		Iterator<Comparable<?>> iter = frequency.valuesIterator();
		Integer uniqueValueCounter = 0;
		while(iter.hasNext()){
			Comparable<?> value = iter.next();
			legend.add(new LegendEntry(uniqueValueCounter.toString(), value.toString()));
			values[uniqueValueCounter] = frequency.getCumPct(value);
			uniqueValueCounter++;
		}
		return values;
	}

	private double[] getCountedValues(Frequency frequency) {
		double[] values = new double[frequency.getUniqueCount()];
		Iterator<Comparable<?>> iter = frequency.valuesIterator();
		Integer uniqueValueCounter = 0;
		while(iter.hasNext()){
			Comparable<?> value = iter.next();
			legend.add(new LegendEntry(uniqueValueCounter.toString(), value.toString()));
			values[uniqueValueCounter] = frequency.getCount(value);
			uniqueValueCounter++;
		}
		return values;
	}

}
