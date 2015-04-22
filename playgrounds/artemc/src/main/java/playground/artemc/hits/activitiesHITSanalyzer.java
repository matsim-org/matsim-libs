package playground.artemc.hits;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import playground.artemc.utils.DataBaseAdmin;
import playground.artemc.utils.NoConnectionException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Provides some statistics on activities in HITS database
 *
 * @author artemc
 */

public class activitiesHITSanalyzer {
	
	private static SimpleDateFormat sdf;
	
	public static void main(String args[]) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException {
		DataBaseAdmin dba = new DataBaseAdmin(new File("./data/dataBases/KrakatauHITS.properties"));
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84", "WGS84_UTM48N");
		BufferedWriter out = new BufferedWriter( new FileWriter("C:\\Work\\Workspace\\MatsimProjects\\data\\activityAnalysis\\HITSActivities_adults_temp.csv"));
		BufferedWriter outWork = new BufferedWriter( new FileWriter("C:\\Work\\Workspace\\MatsimProjects\\data\\activityAnalysis\\HITSActivities_work_hipf.csv"));
		BufferedWriter outShift = new BufferedWriter( new FileWriter("C:\\Work\\Workspace\\MatsimProjects\\data\\activityAnalysis\\HITS_shiftTimes_temp.csv"));
		
		Date startTime = new Date();
		Date endTime  = new Date();
		Date startDate  = new Date();
		Date endDate  = new Date();
		
		Date lastMainStartTime = new Date();
		Date lastMainStartDate = new Date();
		Date lastMainEndTime  = new Date();
		Date lastMainEndDate = new Date();
		
		Double startLat=0.0;
		Double startLon=0.0;
		Double endLat=0.0;
		Double endLon=0.0;
		Double firstLat=0.0;
		Double firstLon=0.0;
		Double lastPtLat=0.0;
		Double lastPtLon=0.0;
		Double tripfactor=0.0;
		
		
		String household="";
		String pax="";
		String mode="";
		String actType="";
		String transportMode="";

		int numberOfStages=0;
		int totalTrips=0;
		int numberOfPersons=0;
		int numberOfWorkers=0;
		int currentTripID =0;
		int totalActivities=0;
		int consistentActivities=0;
		int totalWork=0;
		int consistentWork=0;
		int inconsistentPtWorkActivities=0;
		
		int numberWorkActivitiesPerson = 0;
		int shiftWorkers = 0;
		int maxCount = 0;
		int twoWorkingLoc=0;
		int threeWorkingLoc=0;
		int fourWorkingLoc=0;
		String maxPaxID = null;
	
		sdf = new SimpleDateFormat("HH:mm:ss");
		
		Boolean publicTransportUse = false;
		Boolean inconsistentPtWork = false;
		
		Boolean mixedModeJourney = false;
		int mixedMode = 0;
		int publicMode=0;
		int privateMode=0;
		
		ResultSet persons = dba.executeQuery("SELECT DISTINCT pax_idx FROM hitsshort_geo WHERE " +
				"t3_starttime_24h IS NOT NULL AND start_lat!=0 AND end_lat!=0 " +
				"AND p1_age!='04-09' AND p1_age!='10-14' AND p5_econactivity!='student' AND p5_econactivity!='retired' AND h4b_u4yrs!='0-3 yrs'");
	
		while(persons.next()){
			pax = persons.getString(1);
			ResultSet agentTrips = dba.executeQuery("SELECT h1_hhid,pax_id,trip_id,stage_id,t10_mode,start_lat,start_lon, end_lat,end_lon, t3_starttime_24h, t4_endtime_24h, t6_purpose, hipf10,p6a_fixedwkpl FROM hitsshort_geo_hipf WHERE  pax_idx='"+pax+"' AND trip_id IS NOT NULL ORDER BY trip_id,stage_id");
//			ResultSet agentTrips = dba.executeQuery("SELECT h1_hhid,pax_id,trip_id,stage_id,t10_mode,start_lat,start_lon, end_lat,end_lon, t3_starttime_24h, t4_endtime_24h, t6_purpose FROM hitsshort_geo WHERE  pax_idx='080333BR3_1' AND trip_id IS NOT NULL ORDER BY trip_id,stage_id");
//			ResultSet agentTrips = dba.executeQuery("SELECT h1_hhid,pax_id,trip_id,stage_id,t10_mode,start_lat,start_lon, end_lat,end_lon, t3_starttime_24h, t4_endtime_24h, t6_purpose FROM hitsshort_geo WHERE  pax_idx='"+pax+"' AND trip_id IS NOT NULL ORDER BY t3_starttime_24h");
			numberOfPersons++;
//			System.out.println(numberOfPersons);
			int count = 1;
			currentTripID=0;
			numberWorkActivitiesPerson=0;
			publicTransportUse = false;
			while(agentTrips.next()){
				
				int tripID = agentTrips.getInt(3);
				int stageID = agentTrips.getInt(4);			
								
				startTime = agentTrips.getTime(10);
				startDate = agentTrips.getDate(10);
				
				endTime= agentTrips.getTime(11);
				endDate= agentTrips.getDate(11);
				
				mode = agentTrips.getString(5);
				tripfactor = agentTrips.getDouble(13);
				
				if(count==1){			
					startLat = agentTrips.getDouble(6);
					startLon = agentTrips.getDouble(7);
					Coord coordStart =new CoordImpl(startLon, startLat);
					Coord UTMStart = ct.transform(coordStart);
					startLon=UTMStart.getX();
					startLat=UTMStart.getY();	
				
					endLat = agentTrips.getDouble(8);
					endLon = agentTrips.getDouble(9);			  
					Coord coordEnd =new CoordImpl(endLon, endLat);
					Coord UTMEnd = ct.transform(coordEnd);
					endLon=UTMEnd.getX();
					endLat=UTMEnd.getY();
					
					lastMainStartTime=startTime;
					lastMainStartDate=startDate;
					lastMainEndTime=agentTrips.getTime(11);
					lastMainEndDate=agentTrips.getDate(11);
					
					actType=agentTrips.getString(12);
					
					currentTripID=tripID;				
				}	
				else{
					if(tripID!=currentTripID && (startTime.getTime() >= lastMainEndTime.getTime() || endTime.getTime() <= lastMainStartTime.getTime())){
						//check if it's a new trip and make sure that it's not misreported stage, which is inside the previous trip 
						
						if(inconsistentPtWork){
							outWork.write("private/other"+","+"0"+"\n");
							inconsistentPtWork=false;
							inconsistentPtWorkActivities++;
							if(mixedModeJourney) mixedMode++;
						}
						
						
						totalTrips++;
						totalActivities++;
						
						startLat = agentTrips.getDouble(6);
						startLon = agentTrips.getDouble(7);
						Coord coordStart =new CoordImpl(startLon, startLat);
						Coord UTMStart = ct.transform(coordStart);
						startLon=UTMStart.getX();
						startLat=UTMStart.getY();	
					
						double distance = Math.sqrt((startLat - endLat)*(startLat - endLat) + (startLon - endLon)*(startLon - endLon));
						if(actType.equals("work")){
							totalWork++;
						}
		
						
						if(distance<=1000.0){	
							consistentActivities++;
							double actDuration = ((double)(startTime.getTime() - lastMainEndTime.getTime()))/1000/60/60;	
							if(lastMainEndTime.getTime() > startTime.getTime()){
								actDuration = actDuration + 24.0;
								if(actType.equals("work") && actDuration>=8){
									shiftWorkers++;
									outShift.write(sdf.format(lastMainEndTime)+","+sdf.format(startTime)+","+transportMode+"\n");
								}
							}
//								System.out.println(lastMainStartTime+","+lastMainEndTime+","+startTime+","+actDuration+","+pax);																		
							if(actType.equals("work") && actDuration >= 0.5){
								numberWorkActivitiesPerson++;
								consistentWork++;
								outWork.write(pax+","+Double.toString(endLon)+","+Double.toString(endLat)+","+tripfactor+",");														
		
								if(!publicTransportUse){
									outWork.write("private/other"+","+"0"+"\n");
									privateMode++;
								}
								else{
									if(mode.equals("publBus") || mode.equals("mrt") || mode.equals("lrt")){		
											outWork.write("public"+","+"1"+"\n");
											inconsistentPtWork=false;
											publicMode++;
									}
									else{
										inconsistentPtWork=true;
									}
								}
							}
 
								
								
							out.write(actType+","+Double.toString(actDuration)+","+lastMainEndTime+","+pax+"\n");
							
							publicTransportUse=false;
//							transportMode="private/other";
						}
						else{
//							System.out.println(distance+","+startLat+","+startLon+","+endLat+","+endLon);
//							System.out.println(pax);
						}
						
						lastMainStartTime=startTime;
						lastMainStartDate=startDate;
						lastMainEndTime=agentTrips.getTime(11);
						lastMainEndDate=agentTrips.getDate(11);		
						actType=agentTrips.getString(12);
						
						
						currentTripID=tripID;
					}
					else{
						if(inconsistentPtWork){
							if(mode.equals("publBus") || mode.equals("mrt") || mode.equals("lrt")){
								double distance = Math.sqrt((startLat - lastPtLat)*(startLat - lastPtLat) + (startLon - lastPtLon)*(startLon - lastPtLon));
								if(distance<=1000.0){							
									outWork.write("public"+","+"1"+"\n");
									inconsistentPtWork=false;
									publicMode++;
								}
								else{
									mixedModeJourney=true;
								}
							}
						}
						
					}
					
					endLat = agentTrips.getDouble(8);
					endLon = agentTrips.getDouble(9);			  
					Coord coordEnd =new CoordImpl(endLon, endLat);
					Coord UTMEnd = ct.transform(coordEnd);
					endLon=UTMEnd.getX();
					endLat=UTMEnd.getY();
				}
										  		
					endTime= agentTrips.getTime(11);
					endDate= agentTrips.getDate(11);
					
					if(mode.equals("publBus") || mode.equals("mrt") || mode.equals("lrt")){
						publicTransportUse=true;
						transportMode="public";
						lastPtLat=endLat;
						lastPtLon=endLon;
					}

					count=count+1;
				  		 		
				}
			
				agentTrips.close(); 
				totalTrips++;
				
				if(inconsistentPtWork){
					outWork.write("private/other"+","+"0"+"\n");
					inconsistentPtWork=false;
					inconsistentPtWorkActivities++;
					if(mixedModeJourney) mixedMode++;
				}
				
				switch(numberWorkActivitiesPerson){
					case 1: numberOfWorkers++; break;
					case 2: twoWorkingLoc++; numberOfWorkers++; break;
					case 3: threeWorkingLoc++; numberOfWorkers++; break;
					case 4: fourWorkingLoc++;numberOfWorkers++;  break;
				}
				if(numberWorkActivitiesPerson>maxCount){
					maxCount=numberWorkActivitiesPerson;
				    maxPaxID=pax;
				}
				

		}
		out.close();
		outWork.close();
		outShift.close();
		System.out.println("Number of persons: "+numberOfPersons);
		System.out.println("Number of workers: "+numberOfWorkers);
		System.out.println("Number of Shift-workers: "+shiftWorkers);
		System.out.println("Number of workers with 2 working locations: "+twoWorkingLoc);
		System.out.println("Number of workers with 3 working locations: "+threeWorkingLoc);
		System.out.println("Number of workers with 4 or more working locations: "+fourWorkingLoc);
		System.out.println("Person with maximum number of working locations:"+maxCount+","+maxPaxID);
		System.out.println();
		System.out.println("Total trips: "+totalTrips);
		System.out.println("Total activities: "+totalActivities);
		System.out.println("Consistent activities: "+consistentActivities);
		System.out.println("Total work activities: "+totalWork);
		System.out.println("Consistent work activities in HITS: "+consistentWork);
		System.out.println("Out of consistent PT work activities in HITS, iconsistent  according to EZ-Link: "+inconsistentPtWorkActivities);
		System.out.println("Public transport to work: "+publicMode);
		System.out.println("Private transport to work: "+privateMode);
		System.out.println("Mixed Modes incl. PT both ways: "+mixedMode);
		
	}
}
