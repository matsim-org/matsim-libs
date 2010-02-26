package playground.benjamin.charts.types;

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

import playground.benjamin.Row;



public class BkDeltaUtilsChartGeneral {

	private XYSeriesCollection dataset;
	private SortedMap<Id, Row> populationInformation;
	
	//constructor for row approach
	public BkDeltaUtilsChartGeneral(SortedMap<Id, Row> populationInformation) {
		this.populationInformation = populationInformation;
		this.dataset = createNeededDataset();
	}
	
//============================================================================================================	
	
	private XYSeriesCollection createNeededDataset() {
		//instancing the dataset 
		XYSeriesCollection ds = new XYSeriesCollection();
		
		ds.addSeries(this.createSeries("Delta utils per person", Id2Scores(populationInformation)));
		return ds;
	}
	
	private XYSeries createSeries(final String title, SortedMap<Id, Double> result) {
		XYSeries series = new XYSeries(title, false, true);
		for (Id id : result.keySet()) {
			series.add(Double.parseDouble(id.toString()), result.get(id));
		}
		return series;
	}
	
	/**
	 * Dependent on what to plot this method has to be adapted
	 * @param populationInformation (Map from Id to Row (all desired information))
	 * @return Map from Id to the chosen information (e.g. scoreDiff)
	 * 
	 * 		the return could also be sth like Map<income, scoreDiff>
	 */
	private SortedMap<Id, Double> Id2Scores(SortedMap<Id, Row> populationInformation) {
		SortedMap<Id, Double> result = new TreeMap<Id, Double>(new ComparatorImplementation());
		
		for (Id id : populationInformation.keySet()){
			Row row = populationInformation.get(id);
			Double scoreDiff = row.getScoreDiff();
			result.put(id, scoreDiff);
		}
		return result ;
	}	
	
//============================================================================================================		
	
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
