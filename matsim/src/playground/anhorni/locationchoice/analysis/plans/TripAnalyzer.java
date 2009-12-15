package playground.anhorni.locationchoice.analysis.plans;

import java.io.BufferedWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Vector;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.io.IOUtils;

import playground.anhorni.locationchoice.preprocess.helper.Utils;

public class TripAnalyzer {
	
	private DecimalFormat formatter = new DecimalFormat("0.0");

	List<Trip> trips = new Vector<Trip>();
	
	public void addTrip(Trip trip) {
		trips.add(trip);
	}
	
	public void analyze(String outfile) {
		
		String purposes[] = {"h", "w", "e", "s", "l", "all"};
		
		try {
			final BufferedWriter out = IOUtils.getBufferedWriter(outfile);

			out.write("Average leg distance [m] \n");
			out.write("==================================== \n");
			for (int i = 0; i < purposes.length; i++) {
				for (TransportMode mode: TransportMode.values()) {
					out.write(purposes[i] + "\t" + mode + "\t" +
							formatter.format(this.getAverageDistanceByPurposeAndMode(purposes[i], mode)[0])+ "\t" +
							formatter.format(this.getAverageDistanceByPurposeAndMode(purposes[i], mode)[1])+ "\n");
				}
				out.write("-----------------\n");
			}
			out.write("\n\n" + "Average leg duration [min] \n");
			out.write("==================================== \n");
			for (int i = 0; i < purposes.length; i++) {
				for (TransportMode mode: TransportMode.values()) {
					out.write(purposes[i] + "\t" + mode + "\t" +
							formatter.format(this.getAverageDurationByPurposeAndMode(purposes[i], mode)[0]) + "\t" +
							formatter.format(this.getAverageDurationByPurposeAndMode(purposes[i], mode)[1]) + "\n");
				}
				out.write("-----------------\n");
			}
			out.flush();
			out.close();
		}
		catch (final IOException e) {
			Gbl.errorMsg(e);
		}	
	}
		
	private double[] getAverageDistanceByPurposeAndMode(String purpose, TransportMode mode) {	
		double totalDistance = 0;
		double numberOfLegsWeighted = 0;
		double r[] = new double[2];
		
		for (Trip trip : this.trips) {
			if (trip.getPurpose().startsWith(purpose) || purpose.equals("all")) {
				if (trip.getMode().equals(mode) || mode.equals(TransportMode.undefined)) {
					totalDistance += trip.getDistance();
					numberOfLegsWeighted += trip.getWeight();
				}
			}
		}
		r[0] = totalDistance / numberOfLegsWeighted;
		r[1] = numberOfLegsWeighted;
		return r;
	}
	
	private double[] getAverageDurationByPurposeAndMode(String purpose, TransportMode mode) {	
		double totalDuration = 0;
		double numberOfLegsWeighted = 0;
		double r[] = new double[2];
		
		for (Trip trip : this.trips) {
			if (trip.getPurpose().startsWith(purpose) || purpose.equals("all")) {
				if (trip.getMode().equals(mode) || mode.equals(TransportMode.undefined)) {
					totalDuration += trip.getDuration();
					numberOfLegsWeighted += trip.getWeight();
				}
			}
		}
		r[0] = totalDuration / 60 / numberOfLegsWeighted;
		r[1] = numberOfLegsWeighted;
		return r;
	}
	
	private double getMedianDistanceByPurposeAndMode(String purpose, TransportMode mode) {	
		List<Double> distances = new Vector<Double>();
		
		for (Trip trip : this.trips) {
			if (trip.getPurpose().startsWith(purpose) || purpose.equals("all")) {
				if (trip.getMode().equals(mode) || mode.equals(TransportMode.undefined)) {
					distances.add(trip.getDistance());
				}
			}
		}
		return Utils.median(distances);	
	}
	
	private double getMedianDurationByPurposeAndMode(String purpose, TransportMode mode) {	
		List<Double> durations = new Vector<Double>();
		
		for (Trip trip : this.trips) {
			if (trip.getPurpose().startsWith(purpose) || purpose.equals("all")) {
				if (trip.getMode().equals(mode) || mode.equals(TransportMode.undefined)) {
					durations.add(trip.getDuration());
				}
			}
		}
		return Utils.median(durations);	
	}
}
