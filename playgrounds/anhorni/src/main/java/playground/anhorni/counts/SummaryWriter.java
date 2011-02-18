package playground.anhorni.counts;

import java.io.BufferedWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.matsim.core.utils.charts.LineChart;
import org.matsim.core.utils.io.IOUtils;

public class SummaryWriter {
	
	private final static Logger log = Logger.getLogger(SummaryWriter.class);
	private boolean writeForSpecificArea = false;
	
	public void write(Stations stations, String outpath, boolean writeForSpecificArea) {
		this.writeForSpecificArea = writeForSpecificArea;
		
		this.writeRelative(stations, outpath);
		this.writeAbsoluteDifference(stations, outpath);
		this.writeAbsolute(stations, outpath);
		this.writeStats(stations, outpath);
		this.writeStdDevBoxPlotsAbsolute(stations, outpath);
		this.writeVolumesBoxPlotsAbsolute(stations, outpath);
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
		
			int counter = 0;
			int nextMsg = 1;
			int numberOfStations = 0;
			Iterator<CountStation> stations_it = stations.getCountStations().iterator();
			while (stations_it.hasNext()) {
				CountStation station = stations_it.next();
				counter++;
				
				if (counter % nextMsg == 0) {
					nextMsg *= 2;
					log.info(" 			station # " + counter);
				}
				
				// if station is not in region -> no sim vals
				if ((station.getLink1().getSimVals().size() == 0 || station.getLink2().getSimVals().size() == 0) && writeForSpecificArea) continue;
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
				if ((station.getLink1().getSimVals().size() == 0 || station.getLink2().getSimVals().size() == 0) && writeForSpecificArea) continue;
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
				if ((station.getLink1().getSimVals().size() == 0 || station.getLink2().getSimVals().size() == 0) && writeForSpecificArea) continue;
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
	
	public void writeStdDevBoxPlotsAbsolute(Stations stations, String outpath) {	
		String header = "Station\tLink\tHour 0\tHour 1 ...\n";
		try {
			BufferedWriter out = IOUtils.getBufferedWriter(outpath + "stdDevsAbsolute.txt");
			BufferedWriter outScaled = IOUtils.getBufferedWriter(outpath + "stdDevsScaled.txt");
			out.write(header);			
			outScaled.write(header);
			DecimalFormat formatter = new DecimalFormat("0.0");
			
			StdDevBoxPlot boxPlotAbsolute = new StdDevBoxPlot("Standard Deviations Absolute", "Hour", "Standard Deviations [veh]");
			StdDevBoxPlot boxPlotScaled = new StdDevBoxPlot("Standard Deviations Relative","Hour", "Standard Deviations [%]");
											
			int numberOfStations = 0;
			Iterator<CountStation> stations_it = stations.getCountStations().iterator();
			while (stations_it.hasNext()) {
				CountStation station = stations_it.next();
				
				// if station is not in region -> no sim vals
				if ((station.getLink1().getSimVals().size() == 0 || station.getLink2().getSimVals().size() == 0) && writeForSpecificArea) continue;
				numberOfStations++;
				
				out.write(station.getId() + "\t" + "Link 1\t" ); outScaled.write(station.getId() + "\t" + "Link 1\t" );
				for (int hour = 0; hour < 24; hour++) {	
					double v = station.getLink1().getAggregator().getStandarddev()[hour];
					out.write(formatter.format(v) + "\t");
					boxPlotAbsolute.addHourlyData(hour, v);
					
					double relV = 100 * v / station.getLink1().getAggregator().getAvg()[hour];
					outScaled.write(formatter.format(relV) + "\t");
					boxPlotScaled.addHourlyData(hour, relV);
				}
				out.write("\n"); outScaled.write("\n");
				out.write(station.getId() + "\t" + "Link 2\t" ); outScaled.write(station.getId() + "\t" + "Link 2\t" );
				for (int hour = 0; hour < 24; hour++) {	
					double v = station.getLink2().getAggregator().getStandarddev()[hour];
					out.write(formatter.format(v) + "\t");
					boxPlotAbsolute.addHourlyData(hour, v);
					
					double relV = 100 * v / station.getLink2().getAggregator().getAvg()[hour];
					outScaled.write(formatter.format(relV) + "\t");
					boxPlotScaled.addHourlyData(hour, relV);
				}
				out.write("\n"); outScaled.write("\n");
			}
			outScaled.flush();
			out.flush();
			outScaled.close();
			out.close();
			
			boxPlotAbsolute.saveAsPng(outpath + "stdDevsAbsolute.png", 1000, 800);
			log.info("Boxplot written to: " + outpath + "stdDevsAbsolute.png"); 
			
			boxPlotScaled.saveAsPng(outpath + "stdDevsScaled.png", 1000, 800);
			log.info("Boxplot written to: " + outpath + "stdDevsScaled.png"); 
		} catch (IOException e) {
			e.printStackTrace();
		}
	}	
	
	public void writeVolumesBoxPlotsAbsolute(Stations stations, String outpath) {	
		String header = "Station\tLink\tHour 0\tHour 1 ...\n";
		try {
			BufferedWriter out = IOUtils.getBufferedWriter(outpath + "volumesAbsolute.txt");
			out.write(header);			
			DecimalFormat formatter = new DecimalFormat("0.0");
			
			StdDevBoxPlot boxPlotAbsolute = new StdDevBoxPlot("Volumes Absolute", "hour", "Volumes Absolute [veh]");
											
			int numberOfStations = 0;
			Iterator<CountStation> stations_it = stations.getCountStations().iterator();
			while (stations_it.hasNext()) {
				CountStation station = stations_it.next();
				
				// if station is not in region -> no sim vals
				if ((station.getLink1().getSimVals().size() == 0 || station.getLink2().getSimVals().size() == 0) && writeForSpecificArea) continue;
				numberOfStations++;
				
				out.write(station.getId() + "\t" + "Link 1\t" );
				for (int hour = 0; hour < 24; hour++) {	
					double v = station.getLink1().getAggregator().getAvg()[hour];
					out.write(formatter.format(v) + "\t");
					boxPlotAbsolute.addHourlyData(hour, v);
				}
				out.write("\n");
				out.write(station.getId() + "\t" + "Link 2\t" );
				for (int hour = 0; hour < 24; hour++) {	
					double v = station.getLink2().getAggregator().getAvg()[hour];
					out.write(formatter.format(v) + "\t");
					boxPlotAbsolute.addHourlyData(hour, v);
				}
				out.write("\n");
			}
			out.flush();
			out.close();
			
			boxPlotAbsolute.saveAsPng(outpath + "volumesAbsolute.png", 1000, 800);
			log.info("Boxplot written to: " + outpath + "volumesAbsolute.png"); 
		
		} catch (IOException e) {
			e.printStackTrace();
		}
	}	

	public void writeStats(Stations stations, String outpath) {	
		try {
			BufferedWriter out = IOUtils.getBufferedWriter(outpath + "Stats.txt");					
			Iterator<CountStation> stations_it = stations.getCountStations().iterator();
			out.write("Station: " + "\t" + "number of entries for hour i (i.e. days)" +"\n");
			while (stations_it.hasNext()) {
				CountStation station = stations_it.next();
								
				for (int hour = 0; hour < 24; hour++) {	
					out.write(station.getId() + "\t" + " hour " + hour + "\t" + station.getLink1().getAggregator().getSize(hour) +"\n");
				}
				out.write("---\n");
			}			
			out.flush();		
			out.close();	
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
	
