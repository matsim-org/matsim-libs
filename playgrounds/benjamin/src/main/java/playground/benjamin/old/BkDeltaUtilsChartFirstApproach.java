package playground.benjamin.old;

import java.util.Comparator;
import java.util.SortedMap;
import java.util.TreeMap;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.matsim.api.core.v01.Id;



public class BkDeltaUtilsChartFirstApproach {

	private XYSeriesCollection dataset;
	private SortedMap<Id, Double> scores1;
	private SortedMap<Id, Double> scores2;

	//constructor
	public BkDeltaUtilsChartFirstApproach(SortedMap<Id, Double> scores1,	SortedMap<Id, Double> scores2) {
		this.scores1 = scores1;
		this.scores2 = scores2;
		//call for dataset creation
		this.dataset = createDeltaScoreDataset();
	}
		
//============================================================================================================	
	
	public XYSeriesCollection createDeltaScoreDataset(){
		//instancing the dataset 
		XYSeriesCollection ds = new XYSeriesCollection();
		
		//call to add xy-series to the dataset (several series can be included) with an argument from type XYSeries
		             //call to create a xy-series from the sorted map
		ds.addSeries(this.createSeries("Delta utils per person", calculateScoreDifferences(scores1, scores2)));
		return ds;	
	}
	
	//create xy-series with id and score difference from map
	private XYSeries createSeries(final String title, SortedMap<Id, Double> result) {
		XYSeries series = new XYSeries(title, false, true);
		for (Id id : result.keySet()) {
			series.add(Double.parseDouble(id.toString()), result.get(id));
		}
		return series;
	}

	//this calculates the score differences
	private SortedMap<Id, Double> calculateScoreDifferences(SortedMap<Id, Double> scores1, SortedMap<Id, Double> scores2) {
		SortedMap<Id, Double> result = new TreeMap<Id, Double>(new ComparatorImplementation());
		
		for (Id id : scores1.keySet()){
			//value = map.get(key) !!!
			Double score1 = scores1.get(id);
			Double score2 = scores2.get(id);
			Double scoreDifference = score2 - score1;
			result.put(id, scoreDifference);
		}		
		return result;
	}
	
	public JFreeChart createChart() {
		XYPlot plot = new XYPlot(this.dataset, new NumberAxis("PersonId"), new NumberAxis("Delta utils"), null);
		XYItemRenderer renderer = new XYLineAndShapeRenderer(false, true);
		plot.setRenderer(renderer);
		
		JFreeChart jchart = new JFreeChart("", plot);
		return jchart;
	}

	//comparator to compare Ids not as Strings but as Integers (see above)
	private final class ComparatorImplementation implements Comparator<Id> {
		@Override
		public int compare(Id id1, Id id2) {
			Integer i1 = Integer.parseInt(id1.toString());
			Integer i2 = Integer.parseInt(id2.toString()); 
			return i1.compareTo(i2);
		}
	}
	
}
