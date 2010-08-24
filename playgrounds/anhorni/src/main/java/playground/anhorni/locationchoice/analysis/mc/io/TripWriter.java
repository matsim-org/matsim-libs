package playground.anhorni.locationchoice.analysis.mc.io;

import java.io.BufferedWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.List;

import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;

import playground.anhorni.locationchoice.analysis.mc.MZ;
import playground.anhorni.locationchoice.analysis.mc.MZTrip;
import playground.anhorni.locationchoice.analysis.mc.TripMeasureDistribution;

public class TripWriter {
	
	private String outpath;
	
	public TripWriter() {	
	}
	
	String [] modes = {"pt", "car", "bike", "walk"};
	
	public void write(MZ mz, String outpath) {
		
		this.outpath = outpath;
		
		for (int i = 0; i < 4; i++) {
			
			// CH
			this.write("shop", mz.getShopTrips().get(modes[i]).getRegionalTrips("ch"), modes[i], "ch");
			this.writeSummary("shop", mz.getShopTrips().get(modes[i]), modes[i], "ch");			
			this.write("leisure", mz.getLeisureTrips().get(modes[i]).getRegionalTrips("ch"), modes[i], "ch");
			this.writeSummary("leisure", mz.getLeisureTrips().get(modes[i]), modes[i], "ch");
			this.write("work", mz.getWorkTrips().get(modes[i]).getRegionalTrips("ch"), modes[i], "ch");
			this.writeSummary("work", mz.getWorkTrips().get(modes[i]), modes[i], "ch");
			this.write("education", mz.getEducationTrips().get(modes[i]).getRegionalTrips("ch"), modes[i], "ch");
			this.writeSummary("education", mz.getEducationTrips().get(modes[i]), modes[i], "ch");
			
			// ZH
			mz.getShopTrips().get(modes[i]).filterRegion("zh");
			mz.getLeisureTrips().get(modes[i]).filterRegion("zh");
			mz.getWorkTrips().get(modes[i]).filterRegion("zh");
			
			this.write("shop", mz.getShopTrips().get(modes[i]).getRegionalTrips("zh"), modes[i], "zh");
			this.writeSummary("shop", mz.getShopTrips().get(modes[i]), modes[i], "zh");		
			this.write("leisure", mz.getLeisureTrips().get(modes[i]).getRegionalTrips("zh"), modes[i], "zh");
			this.writeSummary("leisure", mz.getLeisureTrips().get(modes[i]), modes[i], "zh");
			this.write("work", mz.getWorkTrips().get(modes[i]).getRegionalTrips("zh"), modes[i], "zh");
			this.writeSummary("work", mz.getWorkTrips().get(modes[i]), modes[i], "zh");
			
			//ZH City
			mz.getLeisureTrips().get(modes[i]).filterRegion("cityzh");
			this.write("leisure", mz.getLeisureTrips().get(modes[i]).getRegionalTrips("cityzh"), modes[i], "cityzh");
			this.writeSummary("leisure", mz.getLeisureTrips().get(modes[i]), modes[i], "cityzh");
		}
	}
	
	private void writeSummary(String purpose, TripMeasureDistribution distr, String mode, String region) {
		String outpathExt = outpath +"/" + region + "/" + mode + "_" + purpose;
		
		try {
			BufferedWriter out = IOUtils.getBufferedWriter(outpathExt + "_Summary.txt");
			
			DecimalFormat formatterDist = new DecimalFormat("0.00");
			DecimalFormat formatterDur = new DecimalFormat("0.00");
			
			out.write("Number of trips:\t" + distr.getNumberOfTrips(region) + "\n");
			out.write("DISTANCES (km): -------------------------------------------------------------\n");			
			out.write("Max dist:\t" + formatterDist.format(distr.getMaxDist(region)) + "\n");
			out.write("Min dist:\t" + formatterDist.format(distr.getMinDist(region)) + "\n");
			out.write("Avg. dist:\t" + formatterDist.format(distr.getAvgDist(region)) + "\n");
			out.write("Median dist:\t" + formatterDist.format(distr.getMedianDist(region)) + "\n\n");
			
			out.write("DURATIONS (min): ------------------------------------------------------------\n");
			out.write("Max dur:\t" + formatterDur.format(distr.getMaxDur(region) / 60.0) + "\n");
			out.write("Min dur:\t" + formatterDur.format(distr.getMinDur(region) / 60.0) + "\n");
			out.write("Avg. dur:\t" + formatterDur.format(distr.getAvgDur(region) / 60.0) + "\n");
			out.write("Median dur:\t" + formatterDur.format(distr.getMedianDur(region) / 60.0) + "\n\n");
			
			
			distr.setFilter99(true);
			out.write("FILTER 99: =============================================================\n");
			out.write("Code 99 trips filtered\n");
			out.write("Number of trips:\t" + distr.getNumberOfTrips(region) + "\n");
			
			out.write("DISTANCES (km): -------------------------------------------------------------\n");			
			out.write("Max dist:\t" + formatterDist.format(distr.getMaxDist(region)) + "\n");
			out.write("Min dist:\t" + formatterDist.format(distr.getMinDist(region)) + "\n");
			out.write("Avg. dist:\t" + formatterDist.format(distr.getAvgDist(region)) + "\n");
			out.write("Median dist:\t" + formatterDist.format(distr.getMedianDist(region)) + "\n\n");
			
			out.write("DURATIONS (min): ------------------------------------------------------------\n");
			out.write("Max dur:\t" + formatterDur.format(distr.getMaxDur(region) / 60.0) + "\n");
			out.write("Min dur:\t" + formatterDur.format(distr.getMinDur(region) / 60.0) + "\n");
			out.write("Avg. dur:\t" + formatterDur.format(distr.getAvgDur(region) / 60.0) + "\n");
			out.write("Median dur:\t" + formatterDur.format(distr.getMedianDur(region) / 60.0) + "\n\n");			
			
			distr.setFilter99(false);
				
			out.flush();			
			out.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void write(String purpose, List<MZTrip> trips, String mode, String region) {
		
		String outpathExt = outpath +"/" + region + "/" + mode +  "_" + purpose;
		
		String header = "Person_id\tStart_X\tStart_Y\tEnd_X\tEnd_Y\tDist(km)\tDur(min)\texact_type\n";
		try {			
			BufferedWriter out = 
				IOUtils.getBufferedWriter(outpathExt + "_Trips.txt");
			out.write(header);
			
			DecimalFormat formatter = new DecimalFormat("0.00");
						
			Iterator<MZTrip> trips_it = trips.iterator();
			while (trips_it.hasNext()) {
				MZTrip trip = trips_it.next();
				
				CoordImpl coordStart = trip.getCoordStart();
				CoordImpl coordEnd = trip.getCoordEnd();
				
				double dist = coordStart.calcDistance(coordEnd)/1000.0;
				double dur = trip.getDuration();
				
				String s = trip.getPersonId().toString() + "\t";
				out.write(s + coordStart.getX() + "\t"+ coordStart.getY() + "\t" 
						+ coordEnd.getX() + "\t" + coordEnd.getY() + "\t" +
						formatter.format(dist) +  "\t" +
						formatter.format(dur) +  "\t" +
						trip.getPurposeCode() + "\n");
			}	
			out.flush();			
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
