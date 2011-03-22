package GTFSConverter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class GtfsConverter {

	private String filepath = "";
	
	public static void main(String[] args) {
		GtfsConverter gtfs = new GtfsConverter("../../matsim/input");
		gtfs.convertStops();
	}
	
	public GtfsConverter(String filepath){
		this.filepath = filepath;
	}
	
	public void convertStops(){
		String filename = filepath + "/stops.txt";
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(filename)));
			List<String> header = new ArrayList<String>(Arrays.asList(br.readLine().split(",")));
			int stopIdIndex = header.indexOf("stop_id");
			int stopNameIndex = header.indexOf("stop_name");
			int stopLatitudeIndex = header.indexOf("stop_lat");
			int stopLongitudeIndex = header.indexOf("stop_lon");
			
			TransitScheduleFactory tf = new TransitScheduleFactoryImpl();
			TransitSchedule ts = tf.createTransitSchedule();
			String row = br.readLine();
			do{
				String[] entries = row.split(",");
				TransitStopFacility t = tf.createTransitStopFacility(new IdImpl(entries[stopIdIndex]), new CoordImpl(entries[stopLongitudeIndex], entries[stopLatitudeIndex]), false);
				t.setName(entries[stopNameIndex]);
				ts.addStopFacility(t);
				row = br.readLine();
			}while(row != null);
			TransitScheduleWriter tsw = new TransitScheduleWriter(ts);
			tsw.writeFile("./transitSchedule.xml");
		} catch (FileNotFoundException e) {
			System.out.println(filename + " not found!");
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}

}
