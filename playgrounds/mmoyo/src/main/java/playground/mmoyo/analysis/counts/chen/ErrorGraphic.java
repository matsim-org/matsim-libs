package playground.mmoyo.analysis.counts.chen;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.charts.LineChart;

/**searches error data text files in a directory and creates comparing graphs */
public class ErrorGraphic {
	int itNum = 0;     // the number of the iteration to be read;
	String [] hours = new String[24];
	
	public ErrorGraphic (String dirPath){
	
		for (byte h = 1 ; h<=hours.length ; h++){
			hours[h-1] = Byte.toString(h);
		}
		
		ErrorData errorData =null;
		ErrorChart boardRelErrorChart = new ErrorChart (new IdImpl("Boarding Mean Relative Error"));
		ErrorChart boardAbsBiasChart = new ErrorChart (new IdImpl("Boarding Absolute Bias"));
		ErrorChart alightRelErrorChart = new ErrorChart (new IdImpl("Alighting Mean Relative Error"));
		ErrorChart alightaAbsBiasChart = new ErrorChart (new IdImpl("Alighting Absolute Bias"));

		File dir = new File(dirPath);    				 //System directory that contains all routing algorithms results with iterations  
		if (!dir.exists()) {
			//TODO: show warning 
		}
		
		for (int i =0; i< dir.list().length ; i++ ){     //go through the 3 routing cost calculations
			String algorithm=  dir.list()[i];
			
			//get board error data
			String boardFileName = "biasErrorGraphDataBoard.txt";
			String boardErrorFilePath = dirPath + "/" + algorithm + "/ITERS/it." + itNum + "/" +  boardFileName;
			File boardErrorFile = new File(boardErrorFilePath);
			if (boardErrorFile.exists()){
				System.out.println ("reading file: " + boardFileName + " algorithm:" + algorithm);
				try {errorData = new ErrorReader().readFile(boardErrorFilePath, algorithm);} catch (IOException e) { e.printStackTrace(); 	}
				boardRelErrorChart.addSerie(algorithm, errorData.getMeanRelError());
				boardAbsBiasChart.addSerie(algorithm, errorData.getMeanAbsBias());
			}

			//get alighting error data
			String alightFileName = "biasErrorGraphDataAlight.txt";
			String alightErrorFilePath = dirPath + "/" + algorithm + "/ITERS/it." + itNum + "/" +  alightFileName;
			File alightErrorFile = new File(alightErrorFilePath);
			if (alightErrorFile.exists()){
				System.out.println ("reading file: " + alightFileName + " algorithm:" + algorithm);
				try {errorData = new ErrorReader().readFile(alightErrorFilePath, algorithm);} catch (IOException e) { e.printStackTrace(); 	}
				alightRelErrorChart.addSerie(algorithm, errorData.getMeanRelError());
				alightaAbsBiasChart.addSerie(algorithm, errorData.getMeanAbsBias());
				
			}
		}
		
		System.out.println ("creating graphs...");
		createGraphic (boardRelErrorChart); 
		createGraphic (boardAbsBiasChart); 
		createGraphic (alightRelErrorChart);
		createGraphic (alightaAbsBiasChart); 
		System.out.println ("done...");
	}

	private void createGraphic (ErrorChart errorChart){
		LineChart chart = new LineChart(errorChart.getId().toString(), "hour", "error", hours );
		
		for( Map.Entry <String,double[]> entry: errorChart.getSeriesMap().entrySet()){
			chart.addSeries(entry.getKey(),  entry.getValue()); //key: serieName, value:serieValues
		}
		chart.addMatsimLogo();
		chart.saveAsPng("../playgrounds/mmoyo/output/chart_" + errorChart.getId() + ".png", 800, 600);	
	}
	
	public class ErrorChart{
		Id id;
		Map <String, double []> errorSeriesMap = new TreeMap <String, double []>();
		
		private ErrorChart (Id idChart){
			this.id = idChart;
		}

		private Id getId() {
			return id;
		}
		
		public void addSerie (String algorithm, double[] errorData){
			this.errorSeriesMap.put(algorithm, errorData);
		}

		private Map <String, double []> getSeriesMap() {
			return this.errorSeriesMap;
		}
	}
	
	public static void main(String[] args) {
		String dirPath = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/lines344_M44/counts/chen/KMZ_counts_scalefactor50";
		new ErrorGraphic (dirPath);
	}
}
