package playground.artemc.smartCardDataTools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import playground.artemc.utils.DataBaseAdmin;
import playground.artemc.utils.NoConnectionException;


public class extractWorkTimes1week {

	Long CARD_ID=0L;
	String jStartTime = "";
	String jEndTime = "";
	String passenger=""; 
	String startStation=""; 
	String endStation=""; 
	Double startLat=0.0;
	Double startLon=0.0;
	Double endLat=0.0;
	Double endLon=0.0;
	Double startLatGCS=0.0;
	Double startLonGCS=0.0;
	Double endLatGCS=0.0;
	Double endLonGCS=0.0;
	Double firstLat=0.0;
	Double firstLon=0.0;
	int twoworks=0;
	int consistent=0;
	int longact=0;
	int adult=0;
	int agent=0;
	int tenthousand=10000;


	String[] dayTables={"v1_trips11042011","v1_trips12042011","v1_trips13042011","v1_trips14042011","v1_trips15042011"};
	Boolean worker;
	long timerStart = System.currentTimeMillis();
	long elapsedTime = 0L;
	
	ArrayList<Double> workLocationsLat = new ArrayList<Double>();
	ArrayList<Double> workLocationsLon = new ArrayList<Double>();
	ArrayList<Double> workLocationsArrivalLat = new ArrayList<Double>();
	ArrayList<Double> workLocationsArrivalLon = new ArrayList<Double>();
	ArrayList<Double> workLocationsReturnLat = new ArrayList<Double>();
	ArrayList<Double> workLocationsReturnLon = new ArrayList<Double>();
	ArrayList<String> earliestTime = new ArrayList<String>();
	ArrayList<String> latestTime = new ArrayList<String>();
	ArrayList<String> workStation = new ArrayList<String>();
	ArrayList<String> arrivingStations = new ArrayList<String>();
	ArrayList<String> returningStations = new ArrayList<String>();

	
	public static void main(String args[]) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException, ParseException {
		new extractWorkTimes1week().go();	
	}


	private void go() throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException, ParseException {
		DataBaseAdmin dba = new DataBaseAdmin(new File("./data/dataBases/Ezlink_1week_DataBase.properties"));
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84", "WGS84_UTM48N");
//		BufferedWriter out = new BufferedWriter( new FileWriter("C:\\Work\\Workspace\\MatsimProjects\\data\\ezLinkAnalysis\\HomeLocations1.csv"));
//		BufferedWriter outDistances = new BufferedWriter( new FileWriter("C:\\Work\\Workspace\\MatsimProjects\\data\\ezLinkAnalysis\\WorkAreaSize100000_2.csv"));
		BufferedWriter outWorkLocations = new BufferedWriter( new FileWriter("C:\\Work\\Workspace\\MatsimProjects\\data\\ezLinkAnalysis\\WorkTimes_1week_part2.csv"));
		outWorkLocations.close();
		BufferedWriter outWorkdays = new BufferedWriter( new FileWriter("C:\\Work\\Workspace\\MatsimProjects\\data\\ezLinkAnalysis\\WorkDays_1week_part2.csv"));
		outWorkdays.close();
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		endTimeCalculator c = new endTimeCalculator();	
		
		
		ResultSet cardIDs = dba.executeQuery("SELECT * FROM CARD_IDs2 WHERE PassengerType='Adult' OR PassengerType='Senior Citizen'");
		while(cardIDs.next()){
		  	workLocationsLat.clear();
		  	workLocationsLon.clear();
		  	workLocationsArrivalLat.clear();
		  	workLocationsArrivalLon.clear();
		  	workLocationsReturnLat.clear();
		  	workLocationsReturnLon.clear();
		  	workStation.clear();
		  	arrivingStations.clear();
		  	returningStations.clear();
		  	earliestTime.clear();
		  	latestTime.clear();
			CARD_ID = cardIDs.getLong(1);
			agent++;
			int numberOfWorkDays=0;
			int numberOfWorkLocations=0;
			worker=false;
			if(agent==tenthousand){
				elapsedTime = System.currentTimeMillis() - timerStart;
				System.out.println("Agent:"+agent+" Time: "+elapsedTime+"\n");
				tenthousand=tenthousand + 10000;
				timerStart = System.currentTimeMillis();
			}
			int Mo=0,Tu=0,We=0,Th=0,Fr=0;
			for(String dayTable : dayTables){
//				ResultSet agentTrips = dba.executeQuery("SELECT CARD_ID,Journey_Start_Time,Journey_End_Time, start_lat,start_lon, end_lat,end_lon,PassengerType FROM trips_ver1 WHERE CARD_ID="+Long.toString(CARD_ID)+" AND PassengerType='Adult' ORDER BY Journey_Start_Date, Journey_Start_Time");
				ResultSet agentTrips = dba.executeQuery("SELECT CARD_ID,Ride_Start_Time,Ride_Time, start_lat,start_lon, end_lat,end_lon,PassengerType,BOARDING_STOP_STN,ALIGHTING_STOP_STN FROM "+dayTable+" WHERE CARD_ID="+Long.toString(CARD_ID)+" ORDER BY Ride_Start_Date, Ride_Start_Time");	
				int count=0;
				int workActivity=0;				
				while(agentTrips.next()){
					if(count!=0){
						startLatGCS = agentTrips.getDouble(4);
						startLonGCS = agentTrips.getDouble(5);
						startStation=agentTrips.getString(9);
						
						Coord coordStart =new CoordImpl(startLonGCS, startLatGCS);
						Coord UTMStart = ct.transform(coordStart);
						startLon=UTMStart.getX();
						startLat=UTMStart.getY();
						double distance = Math.sqrt((startLat - endLat)*(startLat - endLat) + (startLon - endLon)*(startLon - endLon));
						long distanceRounded = Math.round(distance);
				  
						jStartTime = agentTrips.getString(2);	  
						Date startTime = sdf.parse(jStartTime);
						Date endTime = sdf.parse(jEndTime);
						long difference = startTime.getTime() - endTime.getTime();
//						System.out.println(jStartTime+","+startTime.getTime()+"   "+jEndTime+","+endTime.getTime()+" = "+difference);
//						passenger = agentTrips.getString(8);
		
						if(difference >= 25200000 && difference <= 57600000){
							longact=longact+1;
//							outDistances.write(Double.toString(distance)+"\n");
//						  	outDistances.flush();
							if (distanceRounded<=1000){							
								workActivity=workActivity+1;								
								consistent=consistent+1;							
								boolean sameLocation = false;
								
								//Check if there is already same work location in the array list for this worker
								if(!workLocationsArrivalLat.isEmpty()){
									for(int i=0;i<workLocationsArrivalLat.size();i++){
										double distanceWorkLocation = Math.sqrt((workLocationsLat.get(i) - endLat)*(workLocationsLat.get(i) - endLat) + (workLocationsLon.get(i) - endLon)*(workLocationsLon.get(i) - endLon));
										double distanceWorkLocationReturn = Math.sqrt((workLocationsReturnLat.get(i) - startLat)*(workLocationsReturnLat.get(i) - startLat) + (workLocationsReturnLon.get(i) - startLon)*(workLocationsReturnLon.get(i) - startLon));
										if(distanceWorkLocation<=1000.0){
											sameLocation=true;
//											if(sdf.parse(earliestTime.get(i)).getTime() > endTime.getTime())
//												earliestTime.set(i, jEndTime);
//												System.out.println(dayTable+" "+agentTrips.getString(1)+" "+Long.toString(endTimes.get(i).getTime())+" "+Long.toString(endTime.getTime()));
//											if(sdf.parse(latestTime.get(i)).getTime() < startTime.getTime())
//												latestTime.set(i,jStartTime);
											break;
										}
										if(distanceWorkLocation>1000.0 && distanceWorkLocationReturn<=1000.0){
											sameLocation=true;
											workLocationsLat.set(i,workLocationsReturnLat.get(i));
											workLocationsLon.set(i,workLocationsReturnLon.get(i));
											workStation.set(i,returningStations.get(i));
											break;
										}
										
									}
								}
								
								//If work location is new, add it to the work-locations list
								if(sameLocation==false)									
								{
									workLocationsLat.add(endLat);
									workLocationsLon.add(endLon);
									workStation.add(endStation);
									
									workLocationsArrivalLat.add(endLat);
									workLocationsArrivalLon.add(endLon);
									arrivingStations.add(endStation);
									
									workLocationsReturnLat.add(startLat);
									workLocationsReturnLon.add(startLon);
									returningStations.add(startStation);
									
									earliestTime.add(jEndTime);
									latestTime.add(jStartTime);
									numberOfWorkLocations++;
								}
									
							  	if(workActivity==2)
									twoworks=twoworks+1;
								if(workActivity==1){
									numberOfWorkDays++;
									worker=true;
								}
//								outWork.write(Long.toString(CARD_ID)+","+Double.toString(startLat)+","+Double.toString(startLon)+"\n");  
//								outWork.flush();
							}
					  						
//					  	System.out.println(twoworks+","+Long.toString(CARD_ID));
//					 	System.out.println(Long.toString(CARD_ID)+","+Double.toString(firstLat)+","+Double.toString(firstLon)+","+Double.toString(startLat)+","+Double.toString(startLon)+"\n");		
					 
						}
				  
					}
					else{
						firstLat = agentTrips.getDouble(4);
						firstLon = agentTrips.getDouble(5);
						Coord coordStart =new CoordImpl(firstLon, firstLat);
						Coord UTMStart = ct.transform(coordStart);
						firstLon=UTMStart.getX();
						firstLat=UTMStart.getY();				  
					}
			  	  
					jEndTime = c.calculateEndTime(agentTrips.getString(2),agentTrips.getDouble(3));
//					System.out.println(agentTrips.getString(2)+","+agentTrips.getLong(3)+", "+jEndTime+"\n");
			  
					endLatGCS = agentTrips.getDouble(6);
					endLonGCS = agentTrips.getDouble(7);			  
					Coord coordEnd =new CoordImpl(endLonGCS, endLatGCS);
					Coord UTMEnd = ct.transform(coordEnd);
					endLon=UTMEnd.getX();
					endLat=UTMEnd.getY();
					endStation=agentTrips.getString(10);
					count=count+1;		  		 
				}
							
				agentTrips.close(); 
				
				if(dayTable.equals("v1_trips11042011")) Mo=workActivity;
				if(dayTable.equals("v1_trips12042011")) Tu=workActivity;
				if(dayTable.equals("v1_trips13042011")) We=workActivity;
				if(dayTable.equals("v1_trips14042011")) Th=workActivity;
				if(dayTable.equals("v1_trips15042011")) Fr=workActivity;

			}
			if(worker) adult=adult+1;
			appendToWorkdaysFile(Long.toString(CARD_ID),Integer.toString(numberOfWorkDays),Integer.toString(numberOfWorkLocations),Integer.toString(Mo),Integer.toString(Tu),Integer.toString(We),Integer.toString(Th),Integer.toString(Fr));
			
			for(int i=0;i<workLocationsLat.size();i++){
				appendToLocationsFile(Long.toString(CARD_ID),arrivingStations.get(i),Double.toString(workLocationsArrivalLat.get(i)),Double.toString(workLocationsArrivalLon.get(i)),earliestTime.get(i),returningStations.get(i),Double.toString(workLocationsReturnLat.get(i)),Double.toString(workLocationsReturnLon.get(i)),latestTime.get(i),workStation.get(i));
			}
		}
//		out.close();
	
		  System.out.println("Total activities 7.5-16h: "+longact);
		  System.out.println("Consistent: "+consistent);
		  System.out.println("More then 1 work per day: "+twoworks);
		  System.out.println("Total adult workers: "+adult);
		
	}

	public void appendToLocationsFile (String card_id, String arrivingStation, String actStartLat, String actStartLon, String actStartTime, String returningStation, String actEndLat, String actEndLon, String actEndTime, String workStation) {
	     BufferedWriter bw = null;
	     try {
	         bw = new BufferedWriter(new FileWriter("C:\\Work\\Workspace\\MatsimProjects\\data\\ezLinkAnalysis\\WorkTimes_1week_part2.csv", true));
	         bw.write(card_id+","+arrivingStation+","+actStartLat+","+actStartLon+","+actStartTime+","+returningStation+","+actEndLat+","+actEndLon+","+actEndTime+","+workStation+"\n");
	         bw.flush();
	     } catch (IOException ioe) {
	    	 ioe.printStackTrace();
	     } finally {                      
	    	 if (bw != null) try { 
	    		 bw.close();
	    		 } catch (IOException ioe2) {
	    		 }
	     } 
	} 
	
	public void appendToWorkdaysFile (String card_id, String numberOfWorkDays, String numberOfWorkLocations, String Mo, String Tu, String We, String Th, String Fr) {
	     BufferedWriter bw = null;
	     try {
	         bw = new BufferedWriter(new FileWriter("C:\\Work\\Workspace\\MatsimProjects\\data\\ezLinkAnalysis\\WorkDays_1week_part2.csv", true));
	         bw.write(card_id+","+numberOfWorkDays+","+numberOfWorkLocations+","+Mo+","+Tu+","+We+","+Th+","+Fr+"\n");
	         bw.flush();
	     } catch (IOException ioe) {
	    	 ioe.printStackTrace();
	     } finally {                      
	    	 if (bw != null) try { 
	    		 bw.close();
	    		 } catch (IOException ioe2) {
	    		 }
	     } 
	}  
}
