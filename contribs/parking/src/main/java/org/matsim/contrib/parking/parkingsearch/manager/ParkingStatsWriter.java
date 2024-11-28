package org.matsim.contrib.parking.parkingsearch.manager;

import com.google.inject.Inject;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

//the functionality of this class is mainly copied from old ParkingListener and could be improved
public class ParkingStatsWriter {
	@Inject
	OutputDirectoryHierarchy output;

	public void writeStatsByTimesteps(List<String> produceBeneStatistics, int iteration) {
		BufferedWriter bw = IOUtils.getBufferedWriter(output.getIterationFilename(iteration, "parkingStatsPerTimeSteps.csv"));
		try {

			String header = "time;rejectedParkingRequest;foundParking;unpark";
			bw.write(header);
			bw.newLine();
			for (String s : produceBeneStatistics) {
				bw.write(s);
				bw.newLine();
			}
			bw.flush();
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	/**
	 * @param produceStatistics
	 */
	public void writeStatsByFacility(List<String> produceStatistics, int iteration) {
		BufferedWriter bw = IOUtils.getBufferedWriter(output.getIterationFilename(iteration, "parkingStats.csv"));
		try {

			String header = "linkId;X;Y;parkingFacility;capacity;EndOccupation;reservationsRequests;numberOfParkedVehicles;rejectedParkingRequest;" +
				"numberOfWaitingActivities;numberOfStaysFromGetOffUntilGetIn;numberOfParkingBeforeGetIn";
			bw.write(header);
			bw.newLine();
			for (String s : produceStatistics) {
				bw.write(s);
				bw.newLine();
			}
			bw.flush();
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void writePlainStats(List<String> produceStatistics, int iteration) {
		BufferedWriter bw = IOUtils.getBufferedWriter(output.getIterationFilename(iteration, "parkingStats.txt"));
		try {
			for (String s : produceStatistics) {
				bw.write(s);
				bw.newLine();
			}
			bw.flush();
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
