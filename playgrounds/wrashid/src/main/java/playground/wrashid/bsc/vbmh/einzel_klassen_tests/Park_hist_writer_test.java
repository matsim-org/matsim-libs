package playground.wrashid.bsc.vbmh.einzel_klassen_tests;

import playground.wrashid.bsc.vbmh.vm_parking.Park_History_Writer;

public class Park_hist_writer_test {
	public static void main(String[] args){
		Park_History_Writer phwriter = new Park_History_Writer();
		phwriter.start("output/test_outputs/test_parkhistory.xml");
		phwriter.add_parking_occupied(null, null, null);
		phwriter.add_parking_occupied(null, null, null);
		phwriter.end();
		
		

	}

}
