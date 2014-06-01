package playground.vbmh.einzel_klassen_tests;

import playground.vbmh.vmParking.ParkHistoryWriter;

public class Park_hist_writer_test {
	public static void main(String[] args){
		ParkHistoryWriter phwriter = new ParkHistoryWriter();
		phwriter.start("output/test_outputs/test_parkhistory.xml");
		phwriter.addParkingOccupied(null, null, null);
		phwriter.addParkingOccupied(null, null, null);
		phwriter.end();
		
		

	}

}
