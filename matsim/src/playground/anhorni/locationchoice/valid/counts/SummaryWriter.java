package playground.anhorni.locationchoice.valid.counts;

import java.io.BufferedWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.matsim.core.utils.charts.LineChart;
import org.matsim.core.utils.io.IOUtils;

public class SummaryWriter {
	
	private final static Logger log = Logger.getLogger(SummaryWriter.class);
	
	public void write(Stations stations, String outpath) {
		this.writeRelative(stations, outpath);
		this.writeAbsoluteDifference(stations, outpath);
		this.writeAbsolute(stations, outpath);
	}
	
	public void writeRelative(Stations stations, String outpath) {	
		String header = "Hour\trel_COUNT_SIM_avg\trel_StandardDev\n";
		try {
			BufferedWriter out = IOUtils.getBufferedWriter(outpath + "COUNTS_relative_timevariant.txt");
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
				if (station.getLink1().getSimVals().size() == 0) continue;
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
			}
			
			for (int hour = 0; hour < 24; hour++) {		
				rel_StandardDev[hour] /= numberOfStations;
			}
			
			for (int hour = 0; hour < 24; hour++) {
				out.write(hour + "\t");
				out.write(formatter.format(rel_COUNT_SIM_avg[hour]) + "\t");
				out.write(formatter.format(rel_StandardDev[hour]) + "\n");	
			}
			LineChart chart = new LineChart("Validation", "hour", "%");
			chart.addSeries("re_COUNT_SIM_Avg", rel_COUNT_SIM_avg);
			chart.addSeries("reStandardDev", rel_StandardDev);
			chart.saveAsPng(outpath + "COUNTS_relative_timevariant.png", 1000, 800);
			
			out.flush();		
			out.close();	
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void writeAbsoluteDifference(Stations stations, String outpath) {	
		String header = "Hour\tabs_COUNT_SIM_avg\tabs_StandardDev\n";
		try {
			BufferedWriter out = IOUtils.getBufferedWriter(outpath + "COUNTS_absolutedifference_timevariant.txt");
			out.write(header);			
			DecimalFormat formatter = new DecimalFormat("0.0");
			
			double [] abs_COUNT_SIM_avg = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 
					0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};

			double [] abs_StandardDev = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 
					0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
						
			Iterator<CountStation> stations_it = stations.getCountStations().iterator();
			while (stations_it.hasNext()) {
				CountStation station = stations_it.next();
				
				// if station is not in region -> no sim vals
				if (station.getLink1().getSimVals().size() == 0) continue;
				
				for (int hour = 0; hour < 24; hour++) {		
					abs_COUNT_SIM_avg[hour] += 
						(station.getLink1().getAbsoluteDifference_TimeAvg(hour) + 
								station.getLink2().getAbsoluteDifference_TimeAvg(hour));
				
					abs_StandardDev[hour] +=
						(station.getLink1().getAbsoluteStandardDev_TimeAvg(hour) + 
								station.getLink2().getAbsoluteStandardDev_TimeAvg(hour));
				}
			}
			
			for (int hour = 0; hour < 24; hour++) {
				out.write(hour + "\t");
				out.write(formatter.format(abs_COUNT_SIM_avg[hour]) + "\t");
				out.write(formatter.format(abs_StandardDev[hour]) + "\n");	
			}
			LineChart chart = new LineChart("Validation", "hour", "# vehicles");
			chart.addSeries("abs_COUNT_SIM_avg", abs_COUNT_SIM_avg);
			chart.addSeries("abs_StandardDev", abs_StandardDev);
			chart.saveAsPng(outpath + "COUNTS_absolutedifference_timevariant.png", 1000, 800);
			
			out.flush();		
			out.close();	
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	public void writeAbsolute(Stations stations, String outpath) {	
		String header = "Hour\tabs_SIM_avg\tabs_COUNT_avg\tabs_StandardDevUpper\tabs_StandardDevLower\n";
		try {
			BufferedWriter out = IOUtils.getBufferedWriter(outpath + "COUNTS_absolute_timevariant.txt");
			out.write(header);			
			DecimalFormat formatter = new DecimalFormat("0.0");
			
			double [] abs_SIM_avg = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 
					0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
			
			double [] abs_COUNT_avg = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 
					0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
			
			double [] abs_StandardDev = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 
					0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};

			double [] abs_StandardDevUpper = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 
					0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
			
			double [] abs_StandardDevLower = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 
					0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
						
			Iterator<CountStation> stations_it = stations.getCountStations().iterator();
			while (stations_it.hasNext()) {
				CountStation station = stations_it.next();
				
				// if station is not in region -> no sim vals
				if (station.getLink1().getSimVals().size() == 0) continue;
				
				for (int hour = 0; hour < 24; hour++) {		
					abs_SIM_avg[hour] += 
						station.getLink1().getSimVal(hour) + station.getLink2().getSimVal(hour);
					
					abs_COUNT_avg[hour] += 
						station.getLink1().getAggregator().getAvg()[hour] + station.getLink2().getAggregator().getAvg()[hour];
				
					abs_StandardDev[hour] +=
						(station.getLink1().getAggregator().getStandarddev()[hour] + 
								station.getLink2().getAggregator().getStandarddev()[hour]);
				}
			}
			
			for (int hour = 0; hour < 24; hour++) {
				abs_StandardDevUpper[hour] = abs_COUNT_avg[hour] + abs_StandardDev[hour];
				abs_StandardDevLower[hour] = abs_COUNT_avg[hour] - abs_StandardDev[hour];
			}
			
			for (int hour = 0; hour < 24; hour++) {
				out.write(hour + "\t");
				out.write(formatter.format(abs_SIM_avg[hour]) + "\t");
				out.write(formatter.format(abs_COUNT_avg[hour]) + "\t");
				out.write(formatter.format(abs_StandardDevUpper[hour]) + "\t");
				out.write(formatter.format(abs_StandardDevLower[hour]) + "\t");
			}
			LineChart chart = new LineChart("Validation", "hour", "# vehicles");
			chart.addSeries("abs_SIM_avg", abs_SIM_avg);
			chart.addSeries("abs_COUNT_avg", abs_COUNT_avg);
			chart.addSeries("abs_StandardDevUpper", abs_StandardDevUpper);
			chart.addSeries("abs_StandardDevLower", abs_StandardDevLower);
			chart.saveAsPng(outpath + "COUNTS_absolute_timevariant.png", 1000, 800);
			
			out.flush();		
			out.close();	
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
	
