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
import java.util.Date;

public class activitiesHITS_beta1 {
	
		
	
	public static void main(String args[]) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException {
		DataBaseAdmin dba = new DataBaseAdmin(new File("./data/dataBases/KrakatauHITS.properties"));
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84", "WGS84_UTM48N");
		BufferedWriter out = new BufferedWriter( new FileWriter("C:\\Work\\Workspace\\MatsimProjects\\data\\activityAnalysis\\HITSActivitiesWork.csv"));
//		BufferedWriter outFirstLast = new BufferedWriter( new FileWriter("C:\\Work\\Workspace\\MatsimProjects\\data\\ezLinkAnalysis\\HITSDistanceFirstLast.csv"));
//		BufferedWriter outWalk = new BufferedWriter( new FileWriter("C:\\Work\\Workspace\\MatsimProjects\\data\\HITS\\HITSactivities.csv"));
		
		Date startTime = new Date();
		Date endTime  = new Date();
		Date startDate  = new Date();
		Date endDate  = new Date();
		Date firstTime  = new Date();
		Date firstDate  = new Date();
		
		Double startLat=0.0;
		Double startLon=0.0;
		Double endLat=0.0;
		Double endLon=0.0;
		Double firstLat=0.0;
		Double firstLon=0.0;
		
		
		String household="";
		String pax="";
		String mode="";
		String actType="";

		int numberOfStages=0;
		int totalTrips=0;
		int numberOfPersons=0;
		int numberOfWorkers=0;
		
		int numberWorkActivitiesPerson = 0;
		int shiftWorkers = 0;
		int maxCount = 0;
		int twoWorkingLoc=0;
		int threeWorkingLoc=0;
		int fourWorkingLoc=0;
		String maxPaxID = null;

		ResultSet persons = dba.executeQuery("SELECT DISTINCT pax_idx FROM hitsshort_geo WHERE t3_starttime_24h IS NOT NULL OR start_lat!=0 OR end_lat!=0");
	
		while(persons.next()){
			pax = persons.getString(1);
			ResultSet agentTrips = dba.executeQuery("SELECT h1_hhid,pax_id,trip_id,stage_id,t10_mode,start_lat,start_lon, end_lat,end_lon, t3_starttime_24h, t4_endtime_24h, t6_purpose FROM hitsshort_geo WHERE  pax_idx='"+pax+"' AND trip_id IS NOT NULL ORDER BY trip_id,stage_id");
//			ResultSet agentTrips = dba.executeQuery("SELECT h1_hhid,pax_id,trip_id,stage_id,t10_mode,start_lat,start_lon, end_lat,end_lon, t3_starttime_24h, t4_endtime_24h, t6_purpose FROM hitsshort_geo WHERE  pax_idx='"+pax+"' AND trip_id IS NOT NULL ORDER BY t3_starttime_24h");
			numberOfPersons++;
//			System.out.println(numberOfPersons);
			int count = 1;
			numberWorkActivitiesPerson=0;
			while(agentTrips.next()){
				
				int tripID = agentTrips.getInt(3);
				int stageID = agentTrips.getInt(4);
				Date previousStartTime = startTime;
				
				
//				if(count==1){
//					
//					firstTime = agentTrips.getTime(10);
//					firstDate = agentTrips.getDate(10);
//					
//					firstLat = agentTrips.getDouble(6);
//					firstLon = agentTrips.getDouble(7);
//					Coord coordStart =new CoordImpl(firstLon, firstLat);
//					Coord UTMStart = ct.transform(coordStart);
//					firstLon=UTMStart.getX();
//					firstLat=UTMStart.getY();				  
//				}	
				
				startTime = agentTrips.getTime(10);
				startDate = agentTrips.getDate(10);
			 
				startLat = agentTrips.getDouble(6);
				startLon = agentTrips.getDouble(7);
				Coord coordStart =new CoordImpl(startLon, startLat);
				Coord UTMStart = ct.transform(coordStart);
				startLon=UTMStart.getX();
				startLat=UTMStart.getY();			

								
				if(startTime.getTime()!=previousStartTime.getTime() && count!=1 && actType.equals("work")){
					System.out.println(startTime+","+previousStartTime);
					double actDuration = ((double)(startTime.getTime() - endTime.getTime()))/1000/60/60;
					double actDateDifference = ((double)(startDate.getTime() - endDate.getTime()))/1000/60/60;
					actDuration=actDuration+actDateDifference;												
					System.out.println(Double.toString(actDuration));
					System.out.println(count);	
					System.out.println(startTime+","+endTime+","+actDateDifference+","+pax+","+actType+"\n");		
	
					out.write(Double.toString(actDuration)+"\n");			
					
					  numberWorkActivitiesPerson++;
					if(actDuration<0){
					  actDuration = actDuration + 24;
					  shiftWorkers++;
					  }
					if(numberWorkActivitiesPerson==2)
					   twoWorkingLoc++;
					if(numberWorkActivitiesPerson==3)
					  threeWorkingLoc++;
					if(numberWorkActivitiesPerson==4)
					  fourWorkingLoc++;
					if(numberWorkActivitiesPerson>maxCount){
						maxCount=numberWorkActivitiesPerson;
					    maxPaxID=pax;
					}
					    
					if(numberWorkActivitiesPerson==1){
						numberOfWorkers++;}
					   
//				 	if(difference >= 28800000 && difference <= 43200000){
//						System.out.println(CARD_ID);
//						aggregationErrors.write(Long.toString(CARD_ID)+"\n");
//					}
					
				}

				  
					actType=agentTrips.getString(12);
					
					endTime= agentTrips.getTime(11);
					endDate= agentTrips.getDate(11);
				  
					endLat = agentTrips.getDouble(8);
					endLon = agentTrips.getDouble(9);			  
					Coord coordEnd =new CoordImpl(endLon, endLat);
					Coord UTMEnd = ct.transform(coordEnd);
					endLon=UTMEnd.getX();
					endLat=UTMEnd.getY();
				  	  
					count=count+1;
				  		 
				
				}
				agentTrips.close(); 

		}
		out.close();
		System.out.println("Number of persons: "+numberOfPersons);
		System.out.println("Number of workers: "+numberOfWorkers);
		System.out.println(numberWorkActivitiesPerson+","+shiftWorkers+",");
		System.out.println(twoWorkingLoc+","+threeWorkingLoc+","+fourWorkingLoc+":::"+maxCount+","+maxPaxID);

	}
	
	

}
