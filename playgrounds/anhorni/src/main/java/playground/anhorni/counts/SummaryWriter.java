package playground.anhorni.counts;

import java.io.BufferedWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Iterator;
//import org.apache.log4j.Logger;
import org.matsim.core.utils.charts.LineChart;
import org.matsim.core.utils.io.IOUtils;

public class SummaryWriter {
	
	//private final static Logger log = Logger.getLogger(SummaryWriter.class);
	
	public void write(Stations stations, String outpath) {
		this.writeRelative(stations, outpath);
		this.writeAbsoluteDifference(stations, outpath);
		this.writeAbsolute(stations, outpath);
		this.writeStats(stations, outpath);
	}
	
	public void writeRelative(Stations stations, String outpath) {	
		String header = "Hour\tAVG(|(SIM_i - COUNT_i)/COUNT_i|)\tAVG(StandardDev_i/COUNT_i)\n";
		try {
			BufferedWriter out = IOUtils.getBufferedWriter(outpath + "AVG_relative.txt");
			out.write(header);			
			DecimalFormat formatter = new DecimalFormat("0.0");
			
			double [] rel_COUNT_SIM_avg = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 
					0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};

			double [] rel_StandardDev = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 
					0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
						
			int numberOfStations = 0;
			Iterator<CountStation> stations_it = stations.getCountStations().iterator();
			while (stations_it.hasNext()) {
				CountStation station = stations_it.next();
				
				// if station is not in region -> no sim vals
				if (station.getLink1().getSimVals().size() == 0 || station.getLink2().getSimVals().size() == 0) continue;
				numberOfStations++;
				
				for (int hour = 0; hour < 24; hour++) {		
					rel_COUNT_SIM_avg[hour] += 
						100.0 * (station.getLink1().getRelativeError_TimeAvg(hour) + 
								station.getLink2().getRelativeError_TimeAvg(hour)) / 2.0;
				
					rel_StandardDev[hour] +=
						100.0 * (station.getLink1().getRelativeStandardDeviation(hour) + 
								station.getLink2().getRelativeStandardDeviation(hour)) / 2.0;
				}
			}
			
			for (int hour = 0; hour < 24; hour++) {		
				rel_COUNT_SIM_avg[hour] /= numberOfStations;
				rel_StandardDev[hour] /= numberOfStations;
			}
						
			for (int hour = 0; hour < 24; hour++) {
				out.write(hour + "\t");
				out.write(formatter.format(rel_COUNT_SIM_avg[hour]) + "\t");
				out.write(formatter.format(rel_StandardDev[hour]) + "\n");	
			}
			LineChart chart = new LineChart("Relative Measures", "hour", "%");
			chart.addSeries("AVG(|(SIM_i - COUNT_i)/COUNT_i|)", rel_COUNT_SIM_avg);
			chart.addSeries("AVG(StandardDev_i/COUNT_i)", rel_StandardDev);
			chart.saveAsPng(outpath + "AVG_relative.png", 1000, 800);
			
			out.flush();		
			out.close();	
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void writeAbsoluteDifference(Stations stations, String outpath) {	
		String header = "Hour\tAVG(|SIM_i - COUNT_i|)\tAVG(|StandardDev_i|\n";
		try {
			BufferedWriter out = IOUtils.getBufferedWriter(outpath + "AVG_difference.txt");
			out.write(header);			
			DecimalFormat formatter = new DecimalFormat("0.0");
			
			double [] count_sim = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 
					0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};

			double [] standarddev = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 
					0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
			
			int numberOfStations = 0;
			Iterator<CountStation> stations_it = stations.getCountStations().iterator();
			while (stations_it.hasNext()) {
				CountStation station = stations_it.next();
				
				// if station is not in region -> no sim vals
				if (station.getLink1().getSimVals().size() == 0 || station.getLink2().getSimVals().size() == 0) continue;
				numberOfStations++;
				
				for (int hour = 0; hour < 24; hour++) {		
					count_sim[hour] += 
						(station.getLink1().getAbsoluteDifference_TimeAvg(hour) + 
								station.getLink2().getAbsoluteDifference_TimeAvg(hour));
				
					standarddev[hour] +=
						(station.getLink1().getAbsoluteStandardDev_TimeAvg(hour) + 
								station.getLink2().getAbsoluteStandardDev_TimeAvg(hour));
				}
			}
			
			for (int hour = 0; hour < 24; hour++) {		
				count_sim[hour] /= numberOfStations;
				standarddev[hour] /= numberOfStations;
			}
			
			for (int hour = 0; hour < 24; hour++) {
				out.write(hour + "\t");
				out.write(formatter.format(count_sim[hour]) + "\t");
				out.write(formatter.format(standarddev[hour]) + "\n");	
			}
			LineChart chart = new LineChart("Difference values", "hour", "# vehicles");
			chart.addSeries("AVG(|SIM_i- COUNT_i|)", count_sim);
			chart.addSeries("AVG(|StandardDev_i|)", standarddev);
			chart.saveAsPng(outpath + "AVG_difference.png", 1000, 800);
			
			out.flush();		
			out.close();	
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void writeAbsolute(Stations stations, String outpath) {	
		String header = "Hour\tAVG(SIM)\tAVG(COUNT)\tAVG(StandardDevUpper)\tAVG(StandardDevLower)\t(|AVG(SIM_i) - AVG(COUNT_i)|) / AVG(COUNT_i)\n";
		try {
			BufferedWriter out = IOUtils.getBufferedWriter(outpath + "AVG_values.txt");
			out.write(header);			
			DecimalFormat formatter = new DecimalFormat("0.0");
			
			double [] sim = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 
					0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
			
			double [] count = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 
					0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
			
			double [] standarddev = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 
					0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};

			double [] standarddevUpper = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 
					0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
			
			double [] standarddevLower = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 
					0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
			
			double [] rel_Error = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 
					0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
			
			int numberOfStations = 0;
			Iterator<CountStation> stations_it = stations.getCountStations().iterator();
			while (stations_it.hasNext()) {
				CountStation station = stations_it.next();
				
				// if station is not in region -> no sim vals
				if (station.getLink1().getSimVals().size() == 0 || station.getLink2().getSimVals().size() == 0) continue;
				numberOfStations++;
				
				for (int hour = 0; hour < 24; hour++) {		
					sim[hour] += 
						station.getLink1().getSimVal(hour) + station.getLink2().getSimVal(hour);
					
					count[hour] += 
						station.getLink1().getAggregator().getAvg()[hour] + station.getLink2().getAggregator().getAvg()[hour];
				
					standarddev[hour] +=
						(station.getLink1().getAggregator().getStandarddev()[hour] + 
								station.getLink2().getAggregator().getStandarddev()[hour]);
				}
			}
			for (int hour = 0; hour < 24; hour++) {		
				sim[hour] /= numberOfStations;
				count[hour] /= numberOfStations;
				standarddev[hour] /= numberOfStations;
			}
			
			for (int hour = 0; hour < 24; hour++) {
				standarddevUpper[hour] = count[hour] + standarddev[hour];
				standarddevLower[hour] = count[hour] - standarddev[hour];
			}
			
			for (int hour = 0; hour < 24; hour++) {		
				rel_Error[hour] = 100* Math.abs(sim[hour] - count[hour] ) / count[hour];
			}
			
			double [] standarddevRel = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 
					0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
			
			for (int hour = 0; hour < 24; hour++) {		
				standarddevRel[hour] = 100* Math.abs(standarddev[hour] - count[hour] ) / count[hour];
			}
			
			
			for (int hour = 0; hour < 24; hour++) {
				out.write(hour + "\t");
				out.write(formatter.format(sim[hour]) + "\t");
				out.write(formatter.format(count[hour]) + "\t");
				out.write(formatter.format(standarddevUpper[hour]) + "\t");
				out.write(formatter.format(standarddevLower[hour]) + "\t");
				out.write(formatter.format(rel_Error[hour]) + "\n");
			}
			LineChart chart = new LineChart("Overview", "hour", "# vehicles");
			chart.addSeries("AVG(SIM)", sim);
			chart.addSeries("AVG(COUNT)", count);
			chart.addSeries("AVG(StandardDevUpper)", standarddevUpper);
			chart.addSeries("AVG(StandardDevLower)", standarddevLower);
			chart.saveAsPng(outpath + "AVG_values.png", 1000, 800);
			
			LineChart chart2 = new LineChart("Validation", "hour", "%");
			chart2.addSeries("100*(|AVG(SIM_i) - AVG(COUNT_i)|) / AVG(COUNT_i)", rel_Error);
			chart2.addSeries("100*(|AVG(StandardDev_i) - AVG(COUNT_i)|) / AVG(COUNT_i)", standarddevRel);
			chart2.saveAsPng(outpath + "AVG_rel_Error.png", 1000, 800);
			
			out.flush();		
			out.close();	
		} catch (IOException e) {
			e.printStackTrace();
		}
	}	

	public void writeStats(Stations stations, String outpath) {	
		try {
			BufferedWriter out = IOUtils.getBufferedWriter(outpath + "Stats.txt");					
			Iterator<CountStation> stations_it = stations.getCountStations().iterator();
			while (stations_it.hasNext()) {
				CountStation station = stations_it.next();
								
				//for (int hour = 0; hour < 24; hour++) {	
					out.write("Station: " + station.getId() + ":\t" + station.getLink1().getAggregator().getSize(0) +"\n");
				//}
			}			
			out.flush();		
			out.close();	
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
	
