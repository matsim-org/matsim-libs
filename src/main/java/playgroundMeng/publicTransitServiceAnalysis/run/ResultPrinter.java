package playgroundMeng.publicTransitServiceAnalysis.run;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import playgroundMeng.publicTransitServiceAnalysis.basicDataBank.Trip;
import playgroundMeng.publicTransitServiceAnalysis.gridAnalysis.GridCreator;
import playgroundMeng.publicTransitServiceAnalysis.infoCollector.EventsReader;

public class ResultPrinter {
	
	private static PtAccessabilityConfig ptAccessabilityConfig = PtAccessabilityConfig.getInstance();
	
	static void printTripInfo() throws Exception {
		File file = new File(ptAccessabilityConfig.getOutputDirectory() + "tripInfo.csv");
		FileWriter fileWriter = new FileWriter(file);
		BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

		bufferedWriter.write(
				"OriginZoneId,DestinationZoneId,OriginZoneLatitude,OriginZoneLongtitude,DestinationZoneLatitude,DestinationZoneLongtitude,OriginLatitude,OriginLongtitude,DestinationLatitude,DestinationLongtitude,departureTime,carTravelTime,ptTravelTime,Ratio,ptTravelTimeWW,ratioWW,oldMode");
		for (Trip trip : EventsReader.getInstance().getTrips()) {
			if (trip.isFoundDestinationZone() || trip.isFoundOriginZone()) {
				bufferedWriter.newLine();
				bufferedWriter.write(
						trip.getOriginZoneId() + "," 
								+ trip.getDestinationZoneId() + "," 
								+ trip.getOriginZoneCoord()[0] + ","
								+ trip.getOriginZoneCoord()[1] + ","
								+ trip.getDestinationZoneCoord()[0] + ","
								+ trip.getDestinationZoneCoord()[1] + ","
								+ trip.getOriginCoordinate()[0] + "," 
								+ trip.getOriginCoordinate()[1] + "," 
								+ trip.getDestinationCoordinate()[0] + ","
								+ trip.getDestinationCoordinate()[1] + "," 
								+ trip.getActivityEndImp().getTime() + ","
								+ trip.getCarTravelInfo().getTravelTime() + "," 
								+ trip.getPtTraveInfo().getTravelTime()
								+ "," + trip.getRatio() + "," + trip.getPtTraveInfo().getTraveLTimeWithOutWaitingTime()
								+ "," + trip.getRatioWithOutWaitingTime() + "," + trip.getMode());
			}
		}
		bufferedWriter.close();
	}

	static void print3DGrafikFile() throws IOException {
		File file1 = new File(ptAccessabilityConfig.getOutputDirectory() + "3dGrafikOrigin.csv");
		File file2 = new File(ptAccessabilityConfig.getOutputDirectory() + "3dGrafikDestination.csv");
		FileWriter fileWriter1 = new FileWriter(file1);
		BufferedWriter bufferedWriter1 = new BufferedWriter(fileWriter1);
		FileWriter fileWriter2 = new FileWriter(file2);
		BufferedWriter bufferedWriter2 = new BufferedWriter(fileWriter2);

		bufferedWriter1.write("District,Latitude,longitude,time,ratio,ratioWW,score,kpi,numOfTrips,numofNoPtTrips");
		bufferedWriter2.write("District,Latitude,longitude,time,ratio,ratioWW,score,kpi,numOfTrips,numofNoPtTrips");
		for (int x = 0; x < 24 * 3600; x += ptAccessabilityConfig.getAnalysisTimeSlice()) {
			int h = (int) (x / 3600);
			int m = (int) ((x - h * 3600) / 60);
			int s = (int) (x - h * 3600 - m * 60);
			String time = "," + timeConvert(h) + ":" + timeConvert(m) + ":" + timeConvert(s);

			for (String string : GridCreator.getInstacne().getNum2Grid().keySet()) {
				bufferedWriter1.newLine();
				bufferedWriter2.newLine();
				bufferedWriter1.write(string + ","
						+ GridCreator.getInstacne().getNum2Grid().get(string).getCoordinate()[0] + ","
						+ GridCreator.getInstacne().getNum2Grid().get(string).getCoordinate()[1] + time + ","
						+ GridCreator.getInstacne().getNum2Grid().get(string).getTime2RatioOfOrigin().get(x) + ","
						+ GridCreator.getInstacne().getNum2Grid().get(string).getTime2RatioWWOfOrigin().get(x) + ","
						+ GridCreator.getInstacne().getNum2Grid().get(string).getTime2Score().get(x) + ","
						+ GridCreator.getInstacne().getNum2Grid().get(string).getTime2OriginKpi().get(x) + ","
						+ GridCreator.getInstacne().getNum2Grid().get(string).getTime2NumTripsOfOrigin().get(x) + ","
						+ GridCreator.getInstacne().getNum2Grid().get(string).getTime2NumNoPtTripsOfOrigin().get(x));

				bufferedWriter2.write(string + ","
						+ GridCreator.getInstacne().getNum2Grid().get(string).getCoordinate()[0] + ","
						+ GridCreator.getInstacne().getNum2Grid().get(string).getCoordinate()[1] + time + ","
						+ GridCreator.getInstacne().getNum2Grid().get(string).getTime2RatioOfDestination().get(x) + ","
						+ GridCreator.getInstacne().getNum2Grid().get(string).getTime2RatioWWOfDestination().get(x)
						+ "," + GridCreator.getInstacne().getNum2Grid().get(string).getTime2Score().get(x) + ","
						+ GridCreator.getInstacne().getNum2Grid().get(string).getTime2DestinationKpi().get(x) + ","
						+ GridCreator.getInstacne().getNum2Grid().get(string).getTime2NumTripsOfDestination().get(x)
						+ "," + GridCreator.getInstacne().getNum2Grid().get(string).getTime2NumNoPtTripsOfDestination()
								.get(x));

			}
		}
		bufferedWriter1.close();
		bufferedWriter2.close();
	}

	private static String timeConvert(int a) {
		if (a < 10) {
			return "0" + a;
		} else {
			return String.valueOf(a);
		}
	}
}
