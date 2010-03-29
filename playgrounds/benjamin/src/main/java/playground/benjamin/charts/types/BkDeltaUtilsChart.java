package playground.benjamin.charts.types;

import java.util.Comparator;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.matsim.api.core.v01.Id;

import playground.benjamin.analysis.Row;



public class BkDeltaUtilsChart {

	private XYSeriesCollection dataset;
	private SortedMap<Id, Row> populationInformation;
	
	//constructor for row approach
	public BkDeltaUtilsChart(SortedMap<Id, Row> populationInformation) {
		this.populationInformation = populationInformation;
		this.dataset = createNeededDataset();
	}
	
//============================================================================================================	
	
	private XYSeriesCollection createNeededDataset() {
		//instancing the dataset 
		XYSeriesCollection ds = new XYSeriesCollection();
		
		ds.addSeries(this.createSeries("delta utils over personal income", personalIncome2Scores(populationInformation)));
		return ds;
	}
	
	private XYSeries createSeries(final String title, SortedMap<Double, Double> result) {
		XYSeries series = new XYSeries(title, false, true);
		for (Entry<Double,Double> entry : result.entrySet()) {
			series.add(entry.getKey(), entry.getValue());
		}
		return series;
	}
	
	/**
	 * Dependent on what to plot this method has to be adapted
	 * @param populationInformation (Map from Id to Row (all desired information))
	 * @return Map from income to the chosen information (e.g. scoreDiff)
	 * 
	 */
	private SortedMap<Double, Double> personalIncome2Scores(SortedMap<Id, Row> populationInformation) {
		SortedMap<Double, Double> result = new TreeMap<Double, Double>();
		
		//iterating over a map and getting the desired values out of Row (personal income and score difference)
		for (Entry<Id, Row> entry: populationInformation.entrySet()){
			Row row = entry.getValue();
			Double personalIncome = row.getPersonalIncome();
			Double scoreDiff = row.getScoreDiff();
			result.put(personalIncome, scoreDiff);
		}
		return result;
	}	
	
//============================================================================================================		
	
	public JFreeChart createChart() {
		XYPlot plot = new XYPlot(this.dataset, new NumberAxis("individual income"), new NumberAxis("delta utils"), null);
		XYItemRenderer renderer = new XYLineAndShapeRenderer(false, true);
		plot.setRenderer(renderer);
		
		JFreeChart jchart = new JFreeChart("", plot);
		return jchart;
	}

//	//comparator to compare Ids not as Strings but as Integers (see above)
//	private final class ComparatorImplementation implements Comparator<Id> {
//		@Override
//		public int compare(Id id1, Id id2) {
//			Integer i1 = Integer.parseInt(id1.toString());
//			Integer i2 = Integer.parseInt(id2.toString()); 
//			return i1.compareTo(i2);
//		}
//	}
	
}
