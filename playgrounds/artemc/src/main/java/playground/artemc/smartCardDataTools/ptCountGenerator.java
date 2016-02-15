package playground.artemc.smartCardDataTools;
import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.artemc.utils.DataBaseAdmin;
import playground.artemc.utils.NoConnectionException;



/**
 * Class for generation of PT-Counts XML-file for MATSim out of one-day EZ-Link card record
 *
 * @author achakirov
 */

public class ptCountGenerator extends MatsimXmlWriter{
	
	
	public static void main(String[] args) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException {
		DataBaseAdmin dbKrakatau = new DataBaseAdmin(new File("./data/dataBases/artemcKrakatau.properties"));
		Config config = ConfigUtils.createConfig();
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config);
		config.transit().setUseTransit(true);
		new TransitScheduleReader(scenario).readFile("H:/FCL/Operations/Data/MATSimXMLCurrentData/transitScheduleWV.xml");
		TransitSchedule transitSchedule = scenario.getTransitSchedule();
		transitSchedule.getFacilities();
		
		//Get all RTS stop_ids and stop names from train_stops table
		ResultSet rtsStops = dbKrakatau.executeQuery("SELECT DISTINCT stop_id, stop_name FROM train_stops INNER JOIN trips12042011 ON train_stops.stop_name=trips12042011.BOARDING_STOP_STN");
		
		
		//Provide paths, where generated count files will be saved  		
		String boardingCountFile = "./data/ptCountsFromEZLink/MRTboarding_Count_test.xml";
		String alightingCountFile = "./data/ptCountsFromEZLink/MRTalighting_Count_test.xml";
		
		ptCountGenerator boardingCountGenerator = new ptCountGenerator();
		ptCountGenerator alightngCountGenerator = new ptCountGenerator();
		boardingCountGenerator.writeHeaderBoarding(boardingCountFile);
		alightngCountGenerator.writeHeaderAlighting(alightingCountFile);
		
		int numberOfStations=0;
		int[] boardCount = new int[24];
		int[] alightCount = new int[24];
		
		while(rtsStops.next()){
			String stop_id_ezlink = rtsStops.getString(1);
			String[] stop_id_ezlink_split = stop_id_ezlink.split("/");
			int numberOfSubstations = stop_id_ezlink_split.length;
			
			
			//Split joined station codes (Interchanges)
			for(String s:stop_id_ezlink_split){
				numberOfStations++;
				Id<TransitStopFacility> stopId = Id.create(s,TransitStopFacility.class);
				if(transitSchedule.getFacilities().get(stopId)!=null){
					String stationName = transitSchedule.getFacilities().get(stopId).getName();
					String stationLocation = transitSchedule.getFacilities().get(stopId).getLinkId().toString();
					if(stationName.equals("HarbourFront")){
						stationName="Harbour Front";
					}
					String stationNameEzLink = "STN "+stationName;		
					
					//Get total Boarding count for each station (only display purpose, not used inside count files
					ResultSet rs = dbKrakatau.executeQuery("SELECT COUNT(*) FROM trips12042011 WHERE BOARDING_STOP_STN='"+stationNameEzLink+"'");
					while(rs.next()){
						int count = rs.getInt(1);
						System.out.println(count+" "+stationNameEzLink);

					}
					
					//Get boarding and alighting counts for each station 
					System.out.println("Station Number "+numberOfStations+" from 118: "+s);
					System.out.println(" BoardingCount");
					boardCount = getBoardCount(stationNameEzLink,dbKrakatau,numberOfSubstations);	
					System.out.println(" AlightingCount");
					alightCount = getAlightCount(stationNameEzLink,dbKrakatau,numberOfSubstations);

					
					//Write counts for each station
					boardingCountGenerator.writeStation(s,stationLocation,boardCount,boardingCountFile);
					alightngCountGenerator.writeStation(s,stationLocation,alightCount,alightingCountFile);

				}
			}
	
		}
		boardingCountGenerator.writeEnd(boardingCountFile);
		alightngCountGenerator.writeEnd(alightingCountFile);
	}

	public static int[] getBoardCount(String station, DataBaseAdmin dbKrakatau, int substations) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException{
		int startTime=0;
		int endTime=0;
		int[] count = new int[24] ;
	 
		while(startTime<24){
			endTime=startTime+1;
			ResultSet rs = dbKrakatau.executeQuery("SELECT COUNT(*) FROM trips12042011 WHERE BOARDING_STOP_STN='"+station+"' AND Ride_Start_Time>='"+startTime+":00:00' AND Ride_Start_Time<'"+endTime+":00:00'");
			rs.next();
			count[startTime]=rs.getInt(1)/substations;	
			System.out.println(" "+startTime+" "+endTime+" "+count[startTime]);	
			startTime++;	
		}
		return count;
	}
	
	public static int[] getAlightCount(String station, DataBaseAdmin dbKrakatau, int substations) throws SQLException, NoConnectionException{
		int startTime=0;
		int endTime=0;
		String startTimeString="00";
		String endTimeString="00";
		int[] count = new int[24] ;
		while(startTime<24){
			endTime=startTime+1;
			
			if(startTime<10){
				startTimeString="0"+startTime;
			}
			else{
				startTimeString=Integer.toString(startTime);
			}
			if(endTime<10){
				endTimeString="0"+endTime;
			}
			else{
				endTimeString=Integer.toString(endTime);
			}				
			ResultSet rs = dbKrakatau.executeQuery("SELECT COUNT(*) FROM trips12042011 WHERE ALIGHTING_STOP_STN='"+station+"' AND (ADDTIME(Ride_Start_Time, CONCAT('0 ',Ride_Time DIV 60,':',TRUNCATE((Ride_Time-60*(Ride_Time DIV 60)),0),':',TRUNCATE((Ride_Time-TRUNCATE((Ride_Time-60*(Ride_Time DIV 60)),0))*60,0))))>='"+startTimeString+":00:00' AND (ADDTIME(Ride_Start_Time, CONCAT('0 ',Ride_Time DIV 60,':',TRUNCATE((Ride_Time-60*(Ride_Time DIV 60)),0),':',TRUNCATE((Ride_Time-TRUNCATE((Ride_Time-60*(Ride_Time DIV 60)),0))*60,0))))<'"+endTimeString+":00:00'");
			rs.next();
			count[startTime]=rs.getInt(1)/substations;	
			System.out.println(startTime+" "+endTime+" "+count[startTime]);	
			startTime++;	
		}
		return count;
	}
	
	
	
	
	 public void writeHeaderBoarding(String path) throws IOException {
		 	this.useCompression(false);
		 	this.openFile(path);
			this.writeXmlHead();
			this.writeDoctype("counts", "http://www.matsim.org/files/dtd/plans_v4.dtd");
			List<Tuple<String, String>> head = new ArrayList<Tuple<String,String>>();
			head.add(new Tuple<String, String>("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance"));
			head.add(new Tuple<String, String>("xsi:noNamespaceSchemaLocation", "http://matsim.org/files/dtd/counts_v1.xsd"));
			head.add(new Tuple<String, String>("name", "FCL_Singapore"));
			head.add(new Tuple<String, String>("desc", "EZ Link MRT Boarding counts, 12.04.2012 "));
			head.add(new Tuple<String, String>("year", "2012"));
			head.add(new Tuple<String, String>("layer", "0"));
			this.writeStartTag("counts", head);
//			this.close();
	 }	
	 
	 public void writeHeaderAlighting(String path) throws IOException {
		 	this.useCompression(false);
		 	this.openFile(path);
			this.writeXmlHead();
			this.writeDoctype("counts", "http://www.matsim.org/files/dtd/plans_v4.dtd");
			List<Tuple<String, String>> head = new ArrayList<Tuple<String,String>>();
			head.add(new Tuple<String, String>("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance"));
			head.add(new Tuple<String, String>("xsi:noNamespaceSchemaLocation", "http://matsim.org/files/dtd/counts_v1.xsd"));
			head.add(new Tuple<String, String>("name", "FCL_Singapore"));
			head.add(new Tuple<String, String>("desc", "EZ Link MRT Alighting counts, 12.04.2012 "));
			head.add(new Tuple<String, String>("year", "2012"));
			head.add(new Tuple<String, String>("layer", "0"));
			this.writeStartTag("counts", head);	
//			this.close();
	 }
	 
	 public void writeStation(String station, String stationLink, int[] count, String path) throws IOException{
//	 	 this.openFile(path);
		 List<Tuple<String, String>> location = new ArrayList<Tuple<String,String>>();
		 location.add(new Tuple<String, String>("loc_id", stationLink));
		 location.add(new Tuple<String, String>("cs_id", station));
 
		 this.writeStartTag("count", location, true);
		
		 int hour = 0;
		 for(int val:count){
			 hour=hour+1;
			 List<Tuple<String, String>> counts = new ArrayList<Tuple<String,String>>();
			 counts.add(new Tuple<String, String>("h", Integer.toString(hour)));
			 counts.add(new Tuple<String, String>("val", Integer.toString(val)));
			 this.writeStartTag("volume", counts, true);	
		 } 
	 	
		 this.writeEndTag("count");
//		 this.close();
	 }
	 
	 
	 
	 public void writeEnd(String path) throws IOException{
//		    this.openFile(path);
		 	this.writeEndTag("counts");
		 	this.close();
	 }

}
