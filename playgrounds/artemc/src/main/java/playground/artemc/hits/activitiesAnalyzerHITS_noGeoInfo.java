package playground.artemc.hits;

import org.matsim.api.core.v01.Coord;
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
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class activitiesAnalyzerHITS_noGeoInfo {
	
  	int totalActivities = 0;
  	int totalWorkActivities=0;
  	int totalHomeActivities=0;
  	int totalOtherActivities=0;
  	
	int consistentActivities=0;
  	int consistentPtActivities=0;
  	int deviantPtActivityLocations=0;
  	int shortActivities=0;
	

  	int noPtInformation=0;
  	
  	int consistentWorkActivities=0;
  	int consistentHomeActivities=0;
  	int consistentOtherActivities=0;

	int totalTrips = 0;
  	int publicTrips = 0;
	int privateTrips = 0;
	int mixedTrips = 0;
	
	int bothContainPt=0;
	int stageContainPt=0;
	int detectedTransfers=0;
	int veryLongTransfers=0;
	
	int numberOfPersons=0;
	int totalWorkers=0;
    int totalHomePersons=0;
	int detectableWorkers=0;
	int overnightWorkers=0;
	int overnightDetectableWorkers=0;
	
	int oneWorkingLoc=0;
	int twoWorkingLoc=0;
	int threeWorkingLoc=0;
	int fourWorkingLoc=0;
	
	private static SimpleDateFormat sdf;
	private ArrayList<Trip> trips = new ArrayList<Trip>();
	private AssignStopsToHITS st = new AssignStopsToHITS();
	
	
	public static void main(String args[]) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException, ParseException {
		   
		activitiesAnalyzerHITS_noGeoInfo actAnalyzer = new activitiesAnalyzerHITS_noGeoInfo();
		actAnalyzer.getTrips();
		actAnalyzer.printSummary();	
	
	}

	private void printSummary() {
		System.out.println();
		System.out.println("Total trips: "+totalTrips);
		System.out.println("Total private trips: "+publicTrips);
		System.out.println("Total public trips: "+privateTrips);
		System.out.println("Total mixed trips: "+mixedTrips);
		System.out.println("Detected transfers reported as trip purpose: "+detectedTransfers);
		System.out.println("Detected transfers longer than 0.5h reported as trip purpose: "+veryLongTransfers);
		
		System.out.println();

		System.out.println("Total activities: "+totalActivities);
		System.out.println("Consistent HITS activities: "+consistentActivities);

		System.out.println("Out of "+consistentActivities+" consistent HITS activities:");
		System.out.println("Consistent PT activities: "+consistentPtActivities);
		System.out.println("Consistent PT activities shorter than 30min:: "+shortActivities);
		System.out.println("Skipped as no PT information in HITS: "+noPtInformation);
		System.out.println("Considered PT activities:: "+(consistentPtActivities-shortActivities-noPtInformation));
		System.out.println("Deviant PT activity locations: "+deviantPtActivityLocations);
		System.out.println();
		System.out.println("------------------WORK--------------------");
		System.out.println("Total work activities: "+totalWorkActivities);
		System.out.println("Consistent PT work activities: "+consistentWorkActivities);
		
		System.out.println();
		System.out.println("------------------HOME--------------------");
		System.out.println("Total home activities: " +totalHomeActivities);
		System.out.println("Consistent PT home activities: "+consistentHomeActivities);

		System.out.println();
		System.out.println("------------------OTHER--------------------");
		System.out.println("Total other activities: " +totalOtherActivities);
		System.out.println("Consistent PT home activities: "+consistentOtherActivities);
		
		System.out.println();
		System.out.println("------------------PERSONS--------------------");
		System.out.println("Number of adult persons in HITS: "+numberOfPersons);
		System.out.println("Number of workers in HITS: "+totalWorkers);
		System.out.println("Number of home persons in HITS: "+totalHomePersons);
		System.out.println("Number of overnight-Workers: "+overnightWorkers);
		System.out.println("Number of detectable workers from PT: "+detectableWorkers);
		System.out.println("Number of detectable overnight-Workers: "+overnightDetectableWorkers);
		System.out.println("Number of workers with 1 working locations: "+oneWorkingLoc);
		System.out.println("Number of workers with 2 working locations: "+twoWorkingLoc);
		System.out.println("Number of workers with 3 working locations: "+threeWorkingLoc);
		System.out.println("Number of workers with 4 or more working locations: "+fourWorkingLoc);
	}



	private void getTrips() throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException, ParseException{
		System.out.println("Loading trips from HITS...");	
		
		DataBaseAdmin dba = new DataBaseAdmin(new File("./data/dataBases/KrakatauHITSext.properties"));
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84", "WGS84_UTM48N");
		
		BufferedWriter outActivities = new BufferedWriter( new FileWriter("C:\\Workspace\\MatsimProjects\\data\\activityAnalysis\\HITSActivities_adults_v4_500.csv"));
		BufferedWriter outOnlyWork = new BufferedWriter( new FileWriter("C:\\Workspace\\MatsimProjects\\data\\activityAnalysis\\HITSActivities_work_hipf_v4_500.csv"));
	    BufferedWriter outShiftWork = new BufferedWriter( new FileWriter("C:\\Workspace\\MatsimProjects\\data\\activityAnalysis\\HITS_shiftTimes_v4_500.csv"));
	    BufferedWriter outActivitiesPT = new BufferedWriter( new FileWriter("C:\\Workspace\\MatsimProjects\\data\\activityAnalysis\\HITSActivitiesConsistent_v4_500.csv"));
	    BufferedWriter outAllActivities = new BufferedWriter( new FileWriter("C:\\Workspace\\MatsimProjects\\data\\activityAnalysis\\HITSActivitiesAll_v4.csv"));
	    
	    //write Headers
//	    outActivities.write("WorkChoice"+"\t"+"ActivityDuration"+"\t"+"ActivityStartTime24"+"\t"+"StartTimeSin1"+"\t"+"StartTimeSin2"+"\t"+"StartTimeSin3"+"\t"+"StartTimeCos1"+"\t"+"StartTimeCos2"+"\t"+"StartTimeCos3"+"\t"
////	    		+"StartTime"+"\t"+"Lon"+"\t"+"Lat"+"\t"+"Mode"+"\t"+"PTLine"+"\t"+"AssignedStop"+"\t"+"TripFactor"+"\t"+"PersonID"+"\t"
//	    		+"AGRI"+"\t"+"BEACH"+"\t"+"BUSI1"+"\t"+"BUSI1W"+"\t"+"BUSI2"+"\t"+"BUSI2W"+"\t"+"BUSIPARKW"+"\t"+"BUSIPARK"+"\t"+"CEMETERY"+"\t"+"CIVIC"+"\t"+"COMM"+"\t"+"COMMFIRST"+"\t"+"COMMRES"
//	    		+"\t"+"EDU"+"\t"+"HEALTH"+"\t"+"HOTEL"+"\t"+"OPENSPACE"+"\t"+"PARK"+"\t"+"PORTAIRPORT"+"\t"+"RESIDENTIAL"+"\t"+"RESIDENTIALIN"+"\t"+"RESERVESITE"+"\t"+"SPECIAL"+"\t"+"SPORT"
//	    		+"\t"+"TRANSPORT"+"\t"+"UTILITY"+"\t"+"WATERBODY"+"\t"+"WHITE"+"\t"+"WORSHIP"+"\n");
	    outActivities.write("WorkChoice"+"\t"+"ActivityDuration"+"\t"+"ActivityStartTime24"+"\t"
	    		+"AGRI"+"\t"+"BUSI1"+"\t"+"BUSI1W"+"\t"+"BUSI2"+"\t"+"BUSI2W"+"\t"+"BUSIPARK"+"\t"+"CEMETERY"+"\t"+"CIVIC"+"\t"+"COMM"+"\t"+"COMMFIRST"+"\t"+"COMMRES"
	    		+"\t"+"EDU"+"\t"+"HEALTH"+"\t"+"HOTEL"+"\t"+"OPENSPACE"+"\t"+"PARK"+"\t"+"PORTAIRPORT"+"\t"+"RESERVESITE"+"\t"+"RESIDENTIAL"+"\t"+"RESIDENTIALIN"+"\t"+"SPECIAL"
	    		+"\t"+"SPORT"+"\t"+"WORSHIP"+"\t"+"Lat"+"\t"+"Lon"+"\n");    
		
		int tripID;
		int stageID;
				
		Date startTime = new Date();
		Date endTime  = new Date();
		Date startDate  = new Date();
		Date endDate  = new Date();
	
		Double startLat=0.0;
		Double startLon=0.0;
		Double endLat=0.0;
		Double endLon=0.0;
		Double tripfactor=0.0;

		String pax="";
		String mode="";
		String actType="";
		String origin="";
		
		int currentTripID =0;

	
		sdf = new SimpleDateFormat("HH:mm:ss");
		
		
		boolean publicTransportUse;
		
		ResultSet persons = dba.executeQuery("SELECT DISTINCT pax_idx FROM hitsshort_geo WHERE " +
				"t3_starttime_24h IS NOT NULL AND start_lat!=0 AND end_lat!=0 " +
				"AND p1_age!='04-09' AND p1_age!='10-14' AND p5_econactivity!='student' AND p5_econactivity!='retired' AND h4b_u4yrs!='0-3 yrs'");
	
		while(persons.next()){
			pax = persons.getString(1);
		
			//Personst with inconsistent trip nummerations
			if(pax.equals("530580DR7_2") || pax.equals("542321BR7_2") || pax.equals("670475BR1_3") || pax.equals("730806AR1_2")){
				persons.next();
				pax = persons.getString(1);
			}
			
			System.out.println("Person: "+pax);
			ResultSet agentTrips = dba.executeQuery("SELECT h1_hhid,pax_id,trip_id,stage_id,t10_mode,start_lat,start_lon, end_lat,end_lon, "+
				"t3_starttime_24h, t4_endtime_24h, t6_purpose, hipf10, p6a_fixedwkpl, t11_boardsvcstn, t12_alightstn, p13_1sttriporig_home FROM hitsshort_geo_hipf "+
				"WHERE pax_idx='"+pax+"' AND trip_id IS NOT NULL ORDER BY trip_id,stage_id");
			numberOfPersons++;
			currentTripID=0;
			publicTransportUse = false;
					
			trips.clear();
			Trip currTrip;
			Stage currStage;
			while(agentTrips.next()){
				
				tripID = agentTrips.getInt(3);
				stageID = agentTrips.getInt(4);		
	
				mode = agentTrips.getString(5);
				tripfactor = agentTrips.getDouble(13);	

				if(tripID!=currentTripID){
					
					//Add Trip
					currTrip = new Trip();
					currStage = new Stage();
					
					startLat = agentTrips.getDouble(6);
					startLon = agentTrips.getDouble(7);
					
					endLat = agentTrips.getDouble(8);
					endLon = agentTrips.getDouble(9);	
					
					startTime = agentTrips.getTime(10);
					startDate = agentTrips.getDate(10);
					endTime= agentTrips.getTime(11);
					endDate= agentTrips.getDate(11);

					Coord coordStart = new Coord(startLon, startLat);
					Coord UTMStart = ct.transform(coordStart);
					startLon=UTMStart.getX();
					startLat=UTMStart.getY();

					Coord coordEnd = new Coord(endLon, endLat);
					Coord UTMEnd = ct.transform(coordEnd);
					endLon=UTMEnd.getX();
					endLat=UTMEnd.getY();
					
					actType=agentTrips.getString(12);
					
					//Origin identification
					if(tripID==1){
						origin=agentTrips.getString("p13_1sttriporig_home");
					}
					else{
						if(trips.get(tripID-2).actType.equals("xfer") && tripID==2){
							origin=agentTrips.getString("p13_1sttriporig_home");
						}
						else if(trips.get(tripID-2).actType.equals("xfer") && tripID>2){
							origin=trips.get(tripID-3).actType;
						}
						else{
							origin=trips.get(tripID-2).actType;
						}
					}				
					
					currTrip.startTime=startTime;
					currTrip.startDate=startDate;
					currTrip.endTime=endTime;
					currTrip.endDate=endDate;
					
					currTrip.startLon=startLon;
					currTrip.startLat=startLat;
					currTrip.endLon=endLon;
					currTrip.endLat=endLat;
					currTrip.actType=actType;	
					currTrip.tripFactor=tripfactor;	
					currTrip.pax=pax;
					currTrip.origin=origin;
					
					//Information on last pt-stage of the trip (not very elegant way to program it) 
					if(mode.equals("publBus")){
						currTrip.lastPTLineInformation = agentTrips.getString("t11_boardsvcstn");
						currTrip.lastPTMode="Bus";
					}
					else if(mode.equals("mrt") || mode.equals("lrt")){
						currTrip.lastPTLineInformation = agentTrips.getString("t12_alightstn");
						currTrip.lastPTMode="RTS";
						
						currStage.boardingStation=agentTrips.getString("t11_boardsvcstn");
						currStage.alightingStation=agentTrips.getString("t12_alightstn");
						
						if(currStage.boardingStation.equals("Punggol Point"))
							currStage.boardingStation = "Punggol LRT";
						
						if(currStage.alightingStation.equals("Punggol Point"))
							currStage.alightingStation = "Punggol LRT";
						
						if(currStage.boardingStation.equals("Punggol MRT"))
							currStage.boardingStation = "Punggol";
						
						if(currStage.alightingStation.equals("Punggol MRT"))
							currStage.alightingStation = "Punggol";
						
						ResultSet stationPos = dba.executeQuery("SELECT stop_lat, stop_lon FROM trainstops WHERE stop_name='"+currStage.boardingStation+"'");
						int row=0;
						int row1=0;
						while(stationPos.next()){
							startLat = stationPos.getDouble("stop_lat");
							startLon = stationPos.getDouble("stop_lon");	
							row++;
						}
						ResultSet stationPos1 = dba.executeQuery("SELECT stop_lat, stop_lon FROM trainstops WHERE stop_name='"+currStage.alightingStation+"'");
						while(stationPos1.next()){
							endLat = stationPos1.getDouble("stop_lat");
							endLon = stationPos1.getDouble("stop_lon");			
							row1++;
						}
						
						if(row==0)
							System.out.println("ERROR: "+agentTrips.getString("t11_boardsvcstn")+" "+pax);
						if(row1==0)
							System.out.println("ERROR 1: "+agentTrips.getString("t12_alightstn")+" "+pax);

						Coord coordStartPt = new Coord(startLon, startLat);
						Coord UTMStartPt = ct.transform(coordStartPt);
						startLon=UTMStartPt.getX();
						startLat=UTMStartPt.getY();

						Coord coordEndPt = new Coord(endLon, endLat);
						Coord UTMEndPt = ct.transform(coordEndPt);
						endLon=UTMEndPt.getX();
						endLat=UTMEndPt.getY();
					
					}
					
					if(mode.equals("publBus") || mode.equals("mrt") || mode.equals("lrt")){
						currTrip.lastPTLon = endLon;
						currTrip.lastPTLat = endLat;
						currTrip.pt = true;
				 	}
					else
					{
						currTrip.other = true;
					}	
					trips.add(currTrip);				
					
					//Add stage 
					currStage.mode = mode;
					currStage.startTime=startTime;
					currStage.startDate=startDate;
					currStage.endTime=endTime;
					currStage.endDate=endDate;
					
					currStage.startLon=startLon;
					currStage.startLat=startLat;
					currStage.endLon=endLon;
					currStage.endLat=endLat;
					//System.out.println("TripID : "+tripID);
					trips.get(tripID-1).stages.add(currStage);
					
					currentTripID=tripID;
					
					
				}
				else
				{
					
					//Add stage
					currStage = new Stage();
					
					// Update trip and new stage	
					startLat = agentTrips.getDouble(6);
					startLon = agentTrips.getDouble(7);
					endLat = agentTrips.getDouble(8);
					endLon = agentTrips.getDouble(9);

					Coord coordStart = new Coord(startLon, startLat);
					Coord UTMStart = ct.transform(coordStart);
					startLon=UTMStart.getX();
					startLat=UTMStart.getY();

					Coord coordEnd = new Coord(endLon, endLat);
					Coord UTMEnd = ct.transform(coordEnd);
					endLon=UTMEnd.getX();
					endLat=UTMEnd.getY();
					
					trips.get(tripID-1).endTime=endTime;
					trips.get(tripID-1).endDate=endDate;
					trips.get(tripID-1).endLon=endLon;
					trips.get(tripID-1).endLat=endLat;
					
					
					if(mode.equals("publBus")){
						trips.get(tripID-1).lastPTLineInformation = agentTrips.getString("t11_boardsvcstn");
						trips.get(tripID-1).lastPTMode="Bus";
					}
					else if(mode.equals("mrt") || mode.equals("lrt")){
						trips.get(tripID-1).lastPTLineInformation = agentTrips.getString("t12_alightstn");
						trips.get(tripID-1).lastPTMode="RTS";
						
						currStage.boardingStation=agentTrips.getString("t11_boardsvcstn");
						currStage.alightingStation=agentTrips.getString("t12_alightstn");
						
						if(currStage.boardingStation.equals("Punggol Point"))
							currStage.boardingStation = "Punggol LRT";
						
						if(currStage.alightingStation.equals("Punggol Point"))
							currStage.alightingStation = "Punggol LRT";
						
						if(currStage.boardingStation.equals("Punggol MRT"))
							currStage.boardingStation = "Punggol";
						
						if(currStage.alightingStation.equals("Punggol MRT"))
							currStage.alightingStation = "Punggol";
						
						ResultSet stationPos = dba.executeQuery("SELECT stop_lat, stop_lon FROM trainstops WHERE stop_name='"+currStage.boardingStation+"'");
						int row=0;
						int row1=0;
						while(stationPos.next()){
							startLat = stationPos.getDouble("stop_lat");
							startLon = stationPos.getDouble("stop_lon");	
							row++;
						}
						ResultSet stationPos1 = dba.executeQuery("SELECT stop_lat, stop_lon FROM trainstops WHERE stop_name='"+currStage.alightingStation+"'");
						while(stationPos1.next()){
							endLat = stationPos1.getDouble("stop_lat");
							endLon = stationPos1.getDouble("stop_lon");			
							row1++;
						}	
						
						if(row==0)
							System.out.println("ERROR: "+agentTrips.getString("t11_boardsvcstn")+" "+pax);
						if(row1==0)
							System.out.println("ERROR 1: "+agentTrips.getString("t12_alightstn")+" "+pax);

						Coord coordStartPt = new Coord(startLon, startLat);
						Coord UTMStartPt = ct.transform(coordStartPt);
						startLon=UTMStartPt.getX();
						startLat=UTMStartPt.getY();

						Coord coordEndPt = new Coord(endLon, endLat);
						Coord UTMEndPt = ct.transform(coordEndPt);
						endLon=UTMEndPt.getX();
						endLat=UTMEndPt.getY();
					}
			
					if(mode.equals("publBus") || mode.equals("mrt") || mode.equals("lrt")){
						trips.get(tripID-1).lastPTLon = endLon;
						trips.get(tripID-1).lastPTLat = endLat;
						trips.get(tripID-1).pt = true;
				 	}
					else
					{
						trips.get(tripID-1).other = true;
					}	
					
					currStage.mode = mode;
					currStage.startTime=startTime;
					currStage.startDate=startDate;
					currStage.endTime=endTime;
					currStage.endDate=endDate;
					
					currStage.startLon=startLon;
					currStage.startLat=startLat;
					currStage.endLon=endLon;
					currStage.endLat=endLat;
					
					trips.get(tripID-1).stages.add(currStage);
				}
			} //end of while(agentTrips.next)
			
			analyzeActivities(outActivities, outOnlyWork, outActivitiesPT, outAllActivities);

		} //end of while(persons.next)		
		
		outActivities.close();
		outOnlyWork.close();
		outShiftWork.close();
		outActivitiesPT.close();
		outAllActivities.close();
	}				
	
  void analyzeActivities(BufferedWriter outActivities, BufferedWriter outOnlyWork, BufferedWriter outActivitiesPT, BufferedWriter outAllActivities) throws ParseException, IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException{
	  
	   boolean worker=false;
	   boolean homePerson = false;
	   boolean detectableWorker=false;
	   int firstPTtrip=-1;
	   int lastPTtrip=-1;
	   int i_next=0;
	   int numberWorkActivitiesPerson=0;
	   int numberHomeActivitiesPerson=0;
	   double actDuration=0;
	   DataBaseAdmin dbaWorkFacilities = new DataBaseAdmin(new File("./data/dataBases/KrakatauWorkFacilities.properties"));
	   
	   //Trip statistics
	   totalTrips=totalTrips+trips.size();
		for(int i =0; i< (trips.size());i++){
			if(trips.get(i).pt && !trips.get(i).other){
				publicTrips++;
			}
			else if (!trips.get(i).pt && trips.get(i).other){
				privateTrips++;
			}
			else{
				mixedTrips++;						
			}	
		}
  	
		//Activity Analysis		
	  	
	  	//Loop through the trips
		firstTripLoop:
		for(int i =0; i < (trips.size());i++){
			int actType=0;
			int mode=0;	
			String activityType = trips.get(i).actType;
			
			//Check if last trip or last PT trip
			if(trips.size()==1)
				break firstTripLoop;
			
			if(i==lastPTtrip)
			{	
				i_next = firstPTtrip;
			}	
			else if(i==trips.size()-1 && firstPTtrip==-1){
				i_next=0;
			}
			else{
				i_next=i+1;
			}
			
			//Detect first and last PT trip of the day
			if(trips.get(i).pt && firstPTtrip==-1){
				firstPTtrip=i;		
				for(int v = i + 1 ; v < trips.size();v++){
					if(trips.get(v).pt){
						lastPTtrip=v;
					}
				}
				if(lastPTtrip==-1)
					break firstTripLoop;
			}	
				
			if(trips.get(i_next).startTime.getTime() >=trips.get(i).endTime.getTime() || trips.get(i_next).endTime.getTime() <= trips.get(i).startTime.getTime()){
				double distance = Math.sqrt((trips.get(i_next).startLat - trips.get(i).endLat)*(trips.get(i_next).startLat - trips.get(i).endLat) + (trips.get(i_next).startLon - trips.get(i).endLon)*(trips.get(i_next).startLon - trips.get(i).endLon));							
				totalActivities++;
				//Check reporting consistency in HITS
				if(distance<=1000.0 &&  trips.get(i).endLon > 0 && trips.get(i).endLat > 0){
					consistentActivities++;				
					actDuration = ((double)(trips.get(i_next).startTime.getTime() - trips.get(i).endTime.getTime()))/1000/60/60;
					
					//Check for transfers
					if(trips.get(i).actType.equals("xfer")){
						activityType=trips.get(i_next).actType;
						detectedTransfers++;
					}

					if(trips.get(i).actType.equals("xfer") && actDuration>0.5){
						veryLongTransfers++;
					}
					
					//Check for workers over midnight
					if(trips.get(i).endTime.getTime() > trips.get(i_next).startTime.getTime()){
						actDuration = actDuration + 24.0;
						if(trips.get(i).actType.equals("work")){
							overnightWorkers++;
//							outShift.write(sdf.format(lastMainEndTime)+","+sdf.format(startTime)+","+transportMode+"\n");
						}
					}
					
					//mode
					if(trips.get(i).pt)
						mode=1;		
					
					//calculate StartTimes
					Integer actStartTime = calculateTime(trips.get(i).endTime.toString());
					int actStartTime24 = actStartTime/3600;
					
					//writeAllActivities
					outAllActivities.write(activityType+","+Integer.toString(actStartTime)+","+Double.toString(actDuration)+","+trips.get(i).pax+"\n");

					//Check if the current trip and ANY of the next trips contains pt and check for consistency
					innerLoop:
					for(int j = i+1; j <= trips.size();j++){						
						
						//Change j for the last trip 
						if(j==trips.size() && i!=lastPTtrip){
							break innerLoop;
						}							
						else if(j==trips.size()){
							j=firstPTtrip;
						}
												
						if(trips.get(i).pt && trips.get(j).pt){	
							bothContainPt++;
							//Determine distance between both pt trips
							for(int k=0; k<trips.get(j).stages.size(); k++){
								String stageMode=trips.get(j).stages.get(k).mode;
								if(stageMode.equals("publBus") || stageMode.equals("mrt") || stageMode.equals("lrt")){
									stageContainPt++;
									double distancePT = Math.sqrt((trips.get(j).stages.get(k).startLat - trips.get(i).lastPTLat)*(trips.get(j).stages.get(k).startLat - trips.get(i).lastPTLat) + (trips.get(j).stages.get(k).startLon - trips.get(i).lastPTLon)*(trips.get(j).stages.get(k).startLon - trips.get(i).lastPTLon));				
									double distance_diff = Math.abs(distance-distancePT);
									 //System.out.println(k+" "+distancePT+" "+trips.get(i).pax);						
									if(distance_diff>1000)
										deviantPtActivityLocations++;
									
									if(distancePT<=1000.0){
										consistentPtActivities++;
										actDuration = roundThreeDecimals(((double)(trips.get(j).startTime.getTime() - trips.get(i).endTime.getTime()))/1000/60/60);	
										if(trips.get(i).endTime.getTime() > trips.get(j).startTime.getTime()){
											actDuration = actDuration + 24.0;
											if(trips.get(i).actType.equals("work")  && actDuration >= 0.5)
												overnightDetectableWorkers++;
										}																			
										
										if(actDuration >=0.5){								
											
											//Correction of data inconsistencies
											boolean ptconsistency2008_2011 = checkForPTconsistency(i);
											
											if(!ptconsistency2008_2011){
												noPtInformation++;
											}
											else{
												if(activityType.equals("work")){
													actType=1;		
													consistentWorkActivities++;
													detectableWorker=true;	
												}
												else if(activityType.equals("home")){
													actType=2;		
													consistentHomeActivities++;
												}
												else{
													consistentOtherActivities++;
												}
												
												String facilityDensities[] = new String[29];
												String closestStop = null;
												if(trips.get(i).lastPTMode.equals("Bus")){
													System.out.println(trips.get(i).lastPTLineInformation);
													ArrayList<String> stopList = st.findStopIDsBus(trips.get(i).lastPTLineInformation);
													StopFinder sf = new StopFinder();
													closestStop = sf.findClosestStop(stopList, trips.get(i).endLon, trips.get(i).endLat); 
													ResultSet densities = dbaWorkFacilities.executeQuery("SELECT * FROM StopDensities500 WHERE stop_id ="+closestStop);
													while(densities.next()){
														for(int l=0;l<23;l++){
															facilityDensities[l] = densities.getString(l+2);
														}
													}
												}
												else{
													closestStop=trips.get(i).lastPTLineInformation;
													System.out.println(closestStop);
													ResultSet stopIdMRTset = dbaWorkFacilities.executeQuery("SELECT stop_id FROM StopDensities WHERE stop_name ='"+closestStop+"'");
													String closestStopID="empty";
													while(stopIdMRTset.next()){
														closestStopID = stopIdMRTset.getString(1);
													}
													ResultSet densities = dbaWorkFacilities.executeQuery("SELECT * FROM StopDensities500 WHERE stop_id ='"+closestStopID+"'");
													while(densities.next()){
														for(int l=0;l<23;l++){
															facilityDensities[l] = densities.getString(l+2);
														}
													}	
												}
												
												
												//Calculate Fourier Series for start time
												double sin1=roundThreeDecimals(Math.sin(2*Math.PI*actStartTime/86400));
												double sin2=roundThreeDecimals(Math.sin(4*Math.PI*actStartTime/86400));
												double sin3=roundThreeDecimals(Math.sin(6*Math.PI*actStartTime/86400));
												double cos1=roundThreeDecimals(Math.cos(2*Math.PI*actStartTime/86400));
												double cos2=roundThreeDecimals(Math.cos(4*Math.PI*actStartTime/86400));
												double cos3=roundThreeDecimals(Math.cos(6*Math.PI*actStartTime/86400));

																						
	//											System.out.println(workAct+","+Double.toString(actDuration)
	//													+Double.toString(sin1)+"\t"+Double.toString(sin2)+"\t"+Double.toString(sin3)+"\t"
	//													+Double.toString(cos1)+"\t"+Double.toString(cos2)+"\t"+Double.toString(cos3)+"\t"
	//													+Integer.toString(actStartTime)+","
	//													+Double.toString(trips.get(i).endLon)+","+Double.toString(trips.get(i).endLat)+","+trips.get(i).lastPTMode+","
	//													+trips.get(i).lastPTLineInformation+","+trips.get(i).tripFactor+","+trips.get(i).pax+"\t"
	//													+facilityDensities[0]+"\t"+facilityDensities[1]+"\t"+facilityDensities[2]+"\t"+facilityDensities[3]+"\t"
	//													+facilityDensities[4]+"\t"+facilityDensities[5]+"\t"+facilityDensities[6]+"\t"+facilityDensities[7]+"\t"
	//													+facilityDensities[8]+"\t"+facilityDensities[9]+"\t"+facilityDensities[10]+"\t"+facilityDensities[11]+"\t"
	//													+facilityDensities[12]+"\t"+facilityDensities[13]+"\t"+facilityDensities[14]+"\t"+facilityDensities[15]+"\t"
	//													+facilityDensities[16]+"\t"+facilityDensities[17]+"\t"+facilityDensities[18]+"\t"+facilityDensities[19]+"\t"
	//													+facilityDensities[20]+"\t"+facilityDensities[21]+"\t"+facilityDensities[22]+"\t"+facilityDensities[23]+"\t"
	//													+facilityDensities[24]+"\t"+facilityDensities[25]+"\t"+facilityDensities[26]+"\t"+facilityDensities[27]+"\t"
	//													+facilityDensities[28]+"\n");
	//											outActivities.write(workAct+"\t"+Double.toString(actDuration)+"\t"+Integer.toString(actStartTime)+"\t"+Double.toString(trips.get(i).endLon)+"\t"
	//													+Double.toString(trips.get(i).endLat)+"\t"+trips.get(i).lastPTMode+"\t"+trips.get(i).lastPTLineInformation+"\t"+trips.get(i).tripFactor+"\t"
	//													+trips.get(i).pax+"\n");
	//											outActivities.write(workAct+"\t"+Double.toString(actDuration)+"\t"
	//													+Double.toString(sin1)+"\t"+Double.toString(sin2)+"\t"+Double.toString(sin3)+"\t"
	//													+Double.toString(cos1)+"\t"+Double.toString(cos2)+"\t"+Double.toString(cos3)+"\t"
	//													+Integer.toString(actStartTime)+"\t"+Double.toString(trips.get(i).endLon)+"\t"
	//													+Double.toString(trips.get(i).endLat)+"\t"+trips.get(i).lastPTMode+"\t"+trips.get(i).lastPTLineInformation+"\t"+closestStop+"\t"
	//													+trips.get(i).tripFactor+"\t"+trips.get(i).pax+"\t"
	//													+facilityDensities[0]+"\t"+facilityDensities[1]+"\t"+facilityDensities[2]+"\t"+facilityDensities[3]+"\t"
	//													+facilityDensities[4]+"\t"+facilityDensities[5]+"\t"+facilityDensities[6]+"\t"+facilityDensities[7]+"\t"
	//													+facilityDensities[8]+"\t"+facilityDensities[9]+"\t"+facilityDensities[10]+"\t"+facilityDensities[11]+"\t"
	//													+facilityDensities[12]+"\t"+facilityDensities[13]+"\t"+facilityDensities[14]+"\t"+facilityDensities[15]+"\t"
	//													+facilityDensities[16]+"\t"+facilityDensities[17]+"\t"+facilityDensities[18]+"\t"+facilityDensities[19]+"\t"
	//													+facilityDensities[20]+"\t"+facilityDensities[21]+"\t"+facilityDensities[22]+"\t"+facilityDensities[23]+"\t"
	//													+facilityDensities[24]+"\t"+facilityDensities[25]+"\t"+facilityDensities[26]+"\t"+facilityDensities[27]+"\t"
	//													+facilityDensities[28]+"\n");
	//											outActivities.write(workAct+"\t"+Double.toString(actDuration)+"\t"+Integer.toString(actStartTime24)+"\t"
	//												+Double.toString(sin1)+"\t"+Double.toString(sin2)+"\t"+Double.toString(sin3)+"\t"
	//												+Double.toString(cos1)+"\t"+Double.toString(cos2)+"\t"+Double.toString(cos3)+"\t"	
	//												+facilityDensities[0]+"\t"+facilityDensities[1]+"\t"+facilityDensities[2]+"\t"+facilityDensities[3]+"\t"
	//												+facilityDensities[4]+"\t"+facilityDensities[5]+"\t"+facilityDensities[6]+"\t"+facilityDensities[7]+"\t"
	//												+facilityDensities[8]+"\t"+facilityDensities[9]+"\t"+facilityDensities[10]+"\t"+facilityDensities[11]+"\t"
	//												+facilityDensities[12]+"\t"+facilityDensities[13]+"\t"+facilityDensities[14]+"\t"+facilityDensities[15]+"\t"
	//												+facilityDensities[16]+"\t"+facilityDensities[17]+"\t"+facilityDensities[18]+"\t"+facilityDensities[19]+"\t"
	//												+facilityDensities[20]+"\t"+facilityDensities[21]+"\t"+facilityDensities[22]+"\t"+facilityDensities[23]+"\t"
	//												+facilityDensities[24]+"\t"+facilityDensities[25]+"\t"+facilityDensities[26]+"\t"+facilityDensities[27]+"\t"
	//												+facilityDensities[28]+"\n");
												
												outActivities.write(actType+"\t"+Double.toString(actDuration)+"\t"+Integer.toString(actStartTime24)+"\t"
												+facilityDensities[0]+"\t"+facilityDensities[1]+"\t"+facilityDensities[2]+"\t"+facilityDensities[3]+"\t"
												+facilityDensities[4]+"\t"+facilityDensities[5]+"\t"+facilityDensities[6]+"\t"+facilityDensities[7]+"\t"
												+facilityDensities[8]+"\t"+facilityDensities[9]+"\t"+facilityDensities[10]+"\t"+facilityDensities[11]+"\t"
												+facilityDensities[12]+"\t"+facilityDensities[13]+"\t"+facilityDensities[14]+"\t"+facilityDensities[15]+"\t"
												+facilityDensities[16]+"\t"+facilityDensities[17]+"\t"+facilityDensities[18]+"\t"+facilityDensities[19]+"\t"
												+facilityDensities[20]+"\t"+facilityDensities[21]+"\t"+facilityDensities[22]+"\t"+trips.get(i).endLon+"\t"+trips.get(i).endLat+"\n");
												
												//Write Activity Times Description
												outActivitiesPT.write(activityType+","+Integer.toString(actStartTime)+","+Double.toString(actDuration)+","+trips.get(i).pax+"\n");
											}
										}
										else{
											shortActivities++;
										}

									}//end of if(distancePT<=1000.0){

									//System.out.println("    "+trips.get(i).pax);
									break innerLoop;
									
								}
							}						
						}
						
						//only for the last pt trip of the day
						if(i==lastPTtrip){
							break innerLoop;
						}

					}
					
					//count workers and work activities
					
					System.out.println(activityType);
					if(activityType.equals("work")){		
						totalWorkActivities++;
						worker=true;
						numberWorkActivitiesPerson++;
						outOnlyWork.write(trips.get(i).pax+","+Double.toString(trips.get(i).endLon)+","+Double.toString(trips.get(i).endLat)+","+trips.get(i).tripFactor+","+mode+"\n");
					}else if(activityType.equals("home")){		
						totalHomeActivities++;
						homePerson=true;
						numberHomeActivitiesPerson++;
					}
					else{
						totalOtherActivities++;
					}	
				
				}
			}
			if(i==lastPTtrip || i_next==0){
				break firstTripLoop;
			}		
		} //end of for-Loop through the trips
		
		if(worker)
			totalWorkers++;
		if(detectableWorker)
			detectableWorkers++;
		if(homePerson)
			totalHomePersons++;
		
		switch(numberWorkActivitiesPerson){
		case 1: oneWorkingLoc++; break;
		case 2: twoWorkingLoc++; break;
		case 3: threeWorkingLoc++; break;
		case 4: fourWorkingLoc++; break;
	}
	dbaWorkFacilities.close();	
	//System.out.println(bothContainPt+"   "+stageContainPt);
  }
  
  private boolean checkForPTconsistency(int i) {
		if(trips.get(i).lastPTLineInformation.equals("174E"))
			trips.get(i).lastPTLineInformation="174e";
	
	if(trips.get(i).lastPTLineInformation.equals("243G") || trips.get(i).lastPTLineInformation.equals("243W"))
		trips.get(i).lastPTLineInformation="243";
	
	if(trips.get(i).lastPTLineInformation.equals("506E"))
		trips.get(i).lastPTLineInformation="506";
	
	if(trips.get(i).lastPTLineInformation.equals("95A"))
		trips.get(i).lastPTLineInformation="95";
	
	if(trips.get(i).lastPTLineInformation.equals("196E"))
		trips.get(i).lastPTLineInformation="196e";
	
	if(trips.get(i).lastPTLineInformation.equals("14E"))
		trips.get(i).lastPTLineInformation="14e";
	
	if(trips.get(i).lastPTLineInformation.equals("963e"))
		trips.get(i).lastPTLineInformation="963E";
	
	if(trips.get(i).lastPTLineInformation.equals("89E"))
		trips.get(i).lastPTLineInformation="89e";
	
	if(trips.get(i).lastPTLineInformation.equals("10E"))
		trips.get(i).lastPTLineInformation="10e";
	
	if(trips.get(i).lastPTLineInformation.equals("900A"))
		trips.get(i).lastPTLineInformation="900";
	
	if(trips.get(i).lastPTLineInformation.equals("24,45"))
		trips.get(i).lastPTLineInformation="45";
	
	  return !(trips.get(i).lastPTLineInformation.equals("999 Don't Know") || trips.get(i).lastPTLineInformation.equals("177") || trips.get(i).lastPTLineInformation.equals("50") || trips.get(i).lastPTLineInformation.equals("744") || trips.get(i).lastPTLineInformation.equals("926") || trips.get(i).lastPTLineInformation.equals("1") || trips.get(i).lastPTLineInformation.equals("234") || trips.get(i).lastPTLineInformation.equals("715") || trips.get(i).lastPTLineInformation.equals("544 Premier") || trips.get(i).lastPTLineInformation.equals("401") || trips.get(i).lastPTLineInformation.equals("CT 8") || trips.get(i).lastPTLineInformation.equals("573") || trips.get(i).lastPTLineInformation.equals("868") || trips.get(i).lastPTLineInformation.equals("296") || trips.get(i).lastPTLineInformation.equals("623") || trips.get(i).lastPTLineInformation.equals("749") || trips.get(i).lastPTLineInformation.equals("420") || trips.get(i).lastPTLineInformation.equals("967") || trips.get(i).lastPTLineInformation.equals("621") || trips.get(i).lastPTLineInformation.equals("608") || trips.get(i).lastPTLineInformation.equals("822") || trips.get(i).lastPTLineInformation.equals("CW1") || trips.get(i).lastPTLineInformation.equals("355") || trips.get(i).lastPTLineInformation.equals("522") || trips.get(i).lastPTLineInformation.equals("71") || trips.get(i).lastPTLineInformation.equals("571") || trips.get(i).lastPTLineInformation.equals("625") || trips.get(i).lastPTLineInformation.equals("968") || trips.get(i).lastPTLineInformation.equals("204") || trips.get(i).lastPTLineInformation.equals("572") || trips.get(i).lastPTLineInformation.equals("164") || trips.get(i).lastPTLineInformation.equals("206") || trips.get(i).lastPTLineInformation.equals("216") || trips.get(i).lastPTLineInformation.equals("116") || trips.get(i).lastPTLineInformation.equals("150") || trips.get(i).lastPTLineInformation.equals("864") || trips.get(i).lastPTLineInformation.equals("NR6") || trips.get(i).lastPTLineInformation.equals("CW2") || trips.get(i).lastPTLineInformation.equals("294") || trips.get(i).lastPTLineInformation.equals("986") || trips.get(i).lastPTLineInformation.equals("256") || trips.get(i).lastPTLineInformation.equals("745") || trips.get(i).lastPTLineInformation.equals("916") || trips.get(i).lastPTLineInformation.equals("824") || trips.get(i).lastPTLineInformation.equals("226") || trips.get(i).lastPTLineInformation.equals("987") || trips.get(i).lastPTLineInformation.equals("541") || trips.get(i).lastPTLineInformation.equals("253") || trips.get(i).lastPTLineInformation.equals("152"));
}

public Integer calculateTime(String time) throws ParseException
  {
  	Integer timeHours = Integer.parseInt(time.substring(0,2));
  	Integer timeMinutes = Integer.parseInt(time.substring(3,5));
  	Integer timeSeconds = Integer.parseInt(time.substring(6,8));
  	Integer timeInSeconds = timeHours*3600+timeMinutes*60+timeSeconds;

  	return timeInSeconds;		
  }
  
  double roundThreeDecimals(double d) {
	    DecimalFormat df = new DecimalFormat("#.###");
	return Double.valueOf(df.format(d));
	}
  
}


