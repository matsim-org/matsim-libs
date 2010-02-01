package playground.mmoyo.analysis.counts.chen;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.charts.BarChart;

public class CountsComparingGraph {	
	String [] hours = new String[24];
	int itNum = 0;   									//iteration number where the data will be taken from
	Map <Id, CountChart> chartsMap = new TreeMap <Id, CountChart>();
	
	public CountsComparingGraph (String directory){
		File dir = new File(directory);    				 //System directory that contains all routing algorithms results with iterations  
		if (!dir.exists()) {  
			//TODO: stop here 
		}
		
		for (byte h = 1 ; h<=hours.length ; h++){
			hours[h-1] = Byte.toString(h);
		}
		
		for (int i =0; i< dir.list().length ; i++ ){     //go through the 3 routing cost calculations
				String algorithm=  dir.list()[i]; 				

				//get alighting values
				String alightCountCompFilePath = directory + "/" + algorithm + "/ITERS/it." + itNum + "/" +  itNum + ".simCountCompareAlighting.txt";
				File alightCountCompFile = new File(alightCountCompFilePath);
				if (alightCountCompFile.exists()){
					System.out.println ("Counts file :" + alightCountCompFilePath + " algorithm:" + algorithm);
					CountsReader alightCountsValues = new CountsReader (alightCountCompFilePath); 
					for (Id alightStopId : alightCountsValues.getStopsIds()){
						Id chartId = new IdImpl(dir.getName() + " " + alightStopId  + " " + "alighting" );
						if (!chartsMap.containsKey(chartId)){
							CountChart countChart = new CountChart(chartId, alightCountsValues.getRoutValues(alightStopId,false));
							chartsMap.put(chartId, countChart);
							System.out.println("creating chart: " + chartId );
						}
						chartsMap.get(chartId).seriesMap.put(algorithm, alightCountsValues.getRoutValues(alightStopId,true));
					}
				}
				
				//get boarding values       //TODO create get method
				String boardCountCompFilePath = directory + "/" + algorithm + "/ITERS/it." + itNum + "/" +  itNum + ".simCountCompareBoarding.txt";        
				File boardCountCompFile = new File(boardCountCompFilePath);
				if (boardCountCompFile.exists()){
					System.out.println ("Counts file :" + boardCountCompFilePath + " algorithm:" + algorithm);
					CountsReader boardCountsValues = new CountsReader (boardCountCompFilePath); 
					for (Id boardStopId : boardCountsValues.getStopsIds()){
						Id chartId = new IdImpl(dir.getName() + " " + boardStopId  + " " + "boarding" );
						if (!chartsMap.containsKey(chartId)){
							CountChart countChart = new CountChart(chartId, boardCountsValues.getRoutValues(boardStopId,false));
							chartsMap.put(chartId, countChart);
							System.out.println("creating chart: " + chartId );
						}
						chartsMap.get(chartId).seriesMap.put(algorithm, boardCountsValues.getRoutValues(boardStopId,true));
					}
				}
				
		}

		for ( CountChart  cChart : chartsMap.values()){
			createGraphic (cChart);
		}
		
	}
	
	private void createGraphic (CountChart cChart){
		BarChart chart = new BarChart(cChart.getId().toString(), "hour", "passager counts", hours );
		chart.addSeries("Counts", cChart.getCounts());

		for( Map.Entry <String,double[]> entry: cChart.seriesMap.entrySet() ){
			chart.addSeries(entry.getKey(),  entry.getValue()); //key: serieName, value:serieValues
		}
		chart.addMatsimLogo();
		chart.saveAsPng("../playgrounds/mmoyo/output/chart_" + cChart.getId() + ".png", 800, 600);	
	}

	
	private class CountChart{
		Id id;
		double [] counts; 
		Map <String, double []> seriesMap = new TreeMap <String, double []>();
		
		private CountChart (Id idChart, double[] counts ){
			this.id = idChart;
			this.counts = counts;
		}

		private Id getId() {
			return id;
		}
		
		private double[] getCounts() {
			return counts;
		}
	}
	
	
	public static void main(String[] args) {
		String path = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/lines344_M44/counts/chen/KMZ_counts_scalefactor50";
		new CountsComparingGraph (path);
	}

}
