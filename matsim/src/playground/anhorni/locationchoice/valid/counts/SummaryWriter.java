package playground.anhorni.locationchoice.valid.counts;

import java.io.BufferedWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.List;
import org.matsim.core.utils.io.IOUtils;

public class SummaryWriter {
	
	public void write(List<CountStation> stations, String outpath, CountsCompareReader reader) {	
		String header = "Hour\tAvg_Sim_Vol\tAvgStationAvg_Count\tAvgStationMedian_Count\tAvgStationStandardDev_Count\n";
		try {
			BufferedWriter out = IOUtils.getBufferedWriter(outpath + "counts.txt");
			out.write(header);			
			DecimalFormat formatter = new DecimalFormat("0.0");
			
			double [] avgStationAvg = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 
					0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
			double [] avgStationMedian = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 
					0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
			double [] avgStationStandardDev = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 
					0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
						
			Iterator<CountStation> stations_it = stations.iterator();
			while (stations_it.hasNext()) {
				CountStation station = stations_it.next();
				
				double [][] avg = station.getAggregator().getAvg();
				double [][] standarddev = station.getAggregator().getStandarddev();
				double [][] median = station.getAggregator().getMedian();

				for (int hour = 0; hour < 24; hour++) {
					avgStationAvg[hour] += (avg[0][hour] + avg[1][hour])/(2.0 * stations.size());
					avgStationMedian[hour] += (median[0][hour] + median[1][hour])/(2.0 * stations.size());
					avgStationStandardDev[hour] += (standarddev[0][hour] + standarddev[1][hour])/(2.0 * stations.size());
				}
			}
			for (int hour = 0; hour < 24; hour++) {
				out.write(hour + "\t");
				out.write(formatter.format(reader.getAvgs()[hour]) + "\t");
				out.write(formatter.format(avgStationAvg[hour]) + "\t");
				out.write(formatter.format(avgStationMedian[hour]) + "\t");
				out.write(formatter.format(avgStationStandardDev[hour]) + "\n");
				
			}
			
			out.flush();		
			out.close();	
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
