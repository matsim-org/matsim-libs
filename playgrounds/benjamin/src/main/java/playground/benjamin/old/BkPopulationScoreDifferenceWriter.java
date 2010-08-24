package playground.benjamin.old;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.charts.XYScatterChart;
import org.matsim.core.utils.io.IOUtils;

public class BkPopulationScoreDifferenceWriter {

// this is my version of writing charts and tables, for dominiks version see below	
	
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

	
//// with DgChartWritert, this would be the following - not adjusted!!!:
//	
//	private static final Logger log = Logger.getLogger(DgChartWriter.class);
//
//	
//	public static void writeChart(String filename, JFreeChart jchart){
//		writeChartDataToFile(filename, jchart);
//		writeToPng(filename, jchart);
//	}
//	
//	public static void writeToPng(String filename, JFreeChart jchart) {
//		filename += ".png";
//		try {
//			ChartUtilities.saveChartAsPNG(new File(filename), jchart, 1200, 800, null, true, 9);
//			log.info("Chart written to : " +filename);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
//
//
//	public static void writeChartDataToFile(String filename, JFreeChart chart) {
//		filename += ".txt";
//		try {
//			BufferedWriter writer = IOUtils.getBufferedWriter(filename);
//			try{ /*read "try" as if (plot instanceof XYPlot)*/
//				XYPlot xy = chart.getXYPlot();
//				String yAxisLabel = xy.getRangeAxis().getLabel();
//				
//				String xAxisLabel = "";
//				if (xy.getDomainAxis() != null){
//					xAxisLabel = xy.getDomainAxis().getLabel();
//				}
//				String header = xAxisLabel + "\t " + yAxisLabel;
//				writer.write(header);
//				writer.newLine();
//				for (int i = 0; i < xy.getDatasetCount(); i++){
//					XYDataset xyds = xy.getDataset(i);
//					for (int seriesIndex = 0; seriesIndex < xyds.getSeriesCount(); seriesIndex ++) {
//						writer.newLine();
//						writer.write("Series" + xyds.getSeriesKey(seriesIndex).toString());
//						writer.newLine();
//						int items = xyds.getItemCount(seriesIndex);
//						for (int itemsIndex = 0; itemsIndex < items; itemsIndex++){
//							Number xValue = xyds.getX(seriesIndex, itemsIndex);
//							Number yValue = xyds.getY(seriesIndex, itemsIndex);
//							writer.write(xValue.toString());
//							writer.write("\t");
//							writer.write(yValue.toString());
//							writer.newLine();
//						}
//					}
//				}
//				log.info("Table written to : " +filename);
//				
//			} catch(ClassCastException e){ //else instanceof CategoryPlot
//				log.info("caught class cast exception, trying to write CategoryPlot");
//				CategoryPlot cp = chart.getCategoryPlot();
//				String header = "CategoryRowKey \t CategoryColumnKey \t CategoryRowIndex \t CategoryColumnIndex \t Value";
//				writer.write(header);
//				writer.newLine();
//				for (int i = 0; i < cp.getDatasetCount(); i++) {
//					CategoryDataset cpds = cp.getDataset(i);
//					for (int rowIndex = 0; rowIndex < cpds.getRowCount(); rowIndex++){
//						for (int columnIndex = 0; columnIndex < cpds.getColumnCount(); columnIndex ++) {
//							Number value = cpds.getValue(rowIndex, columnIndex);
//							writer.write(cpds.getRowKey(rowIndex).toString());
//							writer.write("\t");
//							writer.write(cpds.getColumnKey(columnIndex).toString());
//							writer.write("\t");
//							writer.write(Integer.toString(rowIndex));
//							writer.write("\t");
//							writer.write(Integer.toString(columnIndex));
//							writer.write("\t");
//							writer.write(value.toString());
//							writer.newLine();
//						}
//					}
//				}
//				
//				
//			}
//		  writer.close();
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		} 
//			
//		
//	}
//	
//}



//with dominiks chart template, this would be the following - not adjusted!!!:	

//private XYSeriesCollection dataset;
//
//private void createDataSet() {
//  
//  double xvalue = 0;
//  double yvalue = 0;
//  
//  this.dataset = new XYSeriesCollection();
//  // 1. boolean = autosort, 2. boolean = allow multiple x-values
//  XYSeries series = new XYSeries("Series name", false, true);
//  this.dataset.addSeries(series);
//  series.add(xvalue, yvalue);
//}
//
//
//public JFreeChart createChart() {
//  XYPlot plot = new XYPlot();
//  plot.setDataset(0, this.getDataset());
//  JFreeChart chart = new JFreeChart("", plot);
//  chart.setBackgroundPaint(ChartColor.WHITE);
//  chart.setTextAntiAlias(true);
////  chart.removeLegend();
//  return chart;
//}
//
//public XYSeriesCollection getDataset() {
//  if (this.dataset == null) {
//    createDataSet();
//  }
//  return dataset;
//}
//}
