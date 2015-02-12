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


public class extractWorkActivities1week_old {
	
	
	public static void main(String args[]) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException, ParseException {
		DataBaseAdmin dba = new DataBaseAdmin(new File("./data/dataBases/Ezlink_1week_DataBase.properties"));
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84", "WGS84_UTM48N");
//		BufferedWriter out = new BufferedWriter( new FileWriter("C:\\Work\\Workspace\\MatsimProjects\\data\\ezLinkAnalysis\\HomeLocations1.csv"));
		BufferedWriter outWorkLocations = new BufferedWriter( new FileWriter("C:\\Work\\Workspace\\MatsimProjects\\data\\ezLinkAnalysis\\WorkLocations_1week_full.csv"));
//		BufferedWriter outDistances = new BufferedWriter( new FileWriter("C:\\Work\\Workspace\\MatsimProjects\\data\\ezLinkAnalysis\\WorkAreaSize100000_2.csv"));
		BufferedWriter outWorkdays = new BufferedWriter( new FileWriter("C:\\Work\\Workspace\\MatsimProjects\\data\\ezLinkAnalysis\\WorkdaysNumber_1week_full.csv"));
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		endTimeCalculator c = new endTimeCalculator();
		
		Long CARD_ID=0L;
		String jStartTime = "";
		String jEndTime = "";
		String passenger=""; 
		Double startLat=0.0;
		Double startLon=0.0;
		Double endLat=0.0;
		Double endLon=0.0;
		Double firstLat=0.0;
		Double firstLon=0.0;
		Integer twoworks=0;
		Integer consistent=0;
		Integer longact=0;
		Integer adult=0;
		Integer agent=0;
		int tenthousand=10000;
		
		String[] dayTables={"v1_trips11042011","v1_trips12042011","v1_trips13042011","v1_trips14042011","v1_trips15042011"};
		Boolean worker;
		long timerStart = System.currentTimeMillis();
		long elapsedTime = 0L;
		
		ArrayList<Double> workLocationsLat = new ArrayList<Double>();
		ArrayList<Double> workLocationsLon = new ArrayList<Double>();
			
		ResultSet cardIDs = dba.executeQuery("SELECT * FROM CARD_IDs WHERE PassengerType='Adult' OR PassengerType='Senior Citizen'");
		while(cardIDs.next()){
		  	workLocationsLat.clear();
		  	workLocationsLon.clear();
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
				ResultSet agentTrips = dba.executeQuery("SELECT CARD_ID,Ride_Start_Time,Ride_Time, start_lat,start_lon, end_lat,end_lon,PassengerType FROM "+dayTable+" WHERE CARD_ID="+Long.toString(CARD_ID)+" ORDER BY Ride_Start_Date, Ride_Start_Time");	
				int count=0;
				int workActivity=0;				
				while(agentTrips.next()){
					if(count!=0){
						startLat = agentTrips.getDouble(4);
						startLon = agentTrips.getDouble(5);
						Coord coordStart =new CoordImpl(startLon, startLat);
						Coord UTMStart = ct.transform(coordStart);
						startLon=UTMStart.getX();
						startLat=UTMStart.getY();
						double distance = Math.sqrt((startLat - endLat)*(startLat - endLat) + (startLon - endLon)*(startLon - endLon));
						long distanceRounded = Math.round(distance);
				  
						jStartTime = agentTrips.getString(2);	  
						Date startTime = sdf.parse(jStartTime);
						Date endTime = sdf.parse(jEndTime);
						long difference = startTime.getTime() - endTime.getTime();
				  
	//					passenger = agentTrips.getString(8);
		
						if(difference >= 25200000 && difference <= 57600000){
							longact=longact+1;
//							outDistances.write(Double.toString(distance)+"\n");
//						  	outDistances.flush();
							if (distanceRounded<=1000){							
								workActivity=workActivity+1;								
								consistent=consistent+1;							
								boolean sameLocation = false;
								
								//Check if there is already same work location in the array list for this worker
								if(!workLocationsLat.isEmpty()){
									for(int i=0;i<workLocationsLat.size();i++){
										double distanceWorkLocation = Math.sqrt((workLocationsLat.get(i) - endLat)*(workLocationsLat.get(i) - endLat) + (workLocationsLon.get(i) - endLon)*(workLocationsLon.get(i) - endLon));
										if(distanceWorkLocation<=1000.0){
											sameLocation=true;
											break;
										}
									}
								}
								
								//If work location is new, add it to the work-locations list
								if(sameLocation==false)									
								{
									workLocationsLat.add(endLat);
									workLocationsLon.add(endLon);
									outWorkLocations.write(Long.toString(CARD_ID)+","+Double.toString(endLat)+","+Double.toString(endLon)+"\n");  
									outWorkLocations.flush();
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
			  
					endLat = agentTrips.getDouble(6);
					endLon = agentTrips.getDouble(7);			  
					Coord coordEnd =new CoordImpl(endLon, endLat);
					Coord UTMEnd = ct.transform(coordEnd);
					endLon=UTMEnd.getX();
					endLat=UTMEnd.getY();
			  
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
			outWorkdays.write(Long.toString(CARD_ID)+","+Integer.toString(numberOfWorkDays)+","+Integer.toString(numberOfWorkLocations)+","+Mo+","+Tu+","+We+","+Th+","+Fr+"\n");
		  	outWorkdays.flush();
		}
//		out.close();
		outWorkLocations.close();
		outWorkdays.close();
	
		  System.out.println("Total activities 7.5-16h: "+longact);
		  System.out.println("Consistent: "+consistent);
		  System.out.println("More then 1 work: "+twoworks);
		  System.out.println("Total adult workers: "+adult);
	}

	
}
