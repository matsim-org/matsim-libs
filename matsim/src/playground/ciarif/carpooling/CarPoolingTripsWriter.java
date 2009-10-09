package playground.ciarif.carpooling;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import playground.ciarif.retailers.RetailersLocationListener;

public class CarPoolingTripsWriter { //TODO generalize this for all trips type, generalize trips class too
	
	private final static Logger log = Logger.getLogger(RetailersLocationListener.class);
	
	private FileWriter fw = null;
	private BufferedWriter out = null;
	private WorkTrips worktrips;
	
	public CarPoolingTripsWriter(String filename) {
		
		super();
		
		//String outfile = "/scr/baug/ciarif/output/zurich_10pc/" + filename;
		String outfile = filename;
		
		try {
			fw = new FileWriter(outfile);
			System.out.println(outfile);
			out = new BufferedWriter(fw);
			out.write("PersonId\tTripId\tTripType\tHomeCoord\tWorkCoord\tDepartureTime\tTravelTime\tTravelDistance\n");
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		System.out.println("    done.");
	}
	
	public final void close() {
		try {
			out.flush();
			out.close();
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	public void write(WorkTrips worktrips) {
		try {
			this.worktrips = worktrips;
			
			for (WorkTrip wt : this.worktrips.getWorkTrips()) {
					out.write(wt.getPersonId().toString()+"\t");
					out.write(wt.getTripId().toString() +"\t");
					if (wt.getHomeWork()) {
						out.write("0" + "\t");
					}
					else {
						out.write("1" + "\t");
					}
					out.write(wt.getHomeCoord()+ "\t");
					out.write(wt.getWorkCoord()+"\t");
					out.write(wt.getDepartureTime()+"\t");
					out.write(wt.getTravelTime()+"\t");
					out.write(wt.getTravelDistance()+"\n");
			}
			out.flush();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
}
