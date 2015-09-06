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
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import playground.artemc.utils.DataBaseAdmin;
import playground.artemc.utils.NoConnectionException;

public class extractWorkActivities1week_DCM_v2 {

	Long CARD_ID=0L;
	String jStartTime = "";
	String jEndTime = "";
	String passenger=""; 
	String endStation="";
	String lastTravelMode="";
	Double startLat=0.0;
	Double startLon=0.0;
	Double endLat=0.0;
	Double endLon=0.0;
	Double firstLat=0.0;
	Double firstLon=0.0;
	int multipleWorks=0;
	int consistent=0;
	int nightConsistent=0;
	int longact=0;
	int adultWorkers=0;
	int agent=0;
	int tenthousand=10000;
	
//	String[] dayTables={"v1_trips11042011","v1_trips12042011","v1_trips13042011","v1_trips14042011","v1_trips15042011"};
	String[] dayTables={"v1_trips12042011"};
	Boolean worker;
	long timerStart = System.currentTimeMillis();
	long elapsedTime = 0L;
	
	ArrayList<Double> workLocationsLat = new ArrayList<Double>();
	ArrayList<Double> workLocationsLon = new ArrayList<Double>();

	
	//DCM variables	
	double b_ActivityDuration9;	
	double b_ActivityDuration9Plus;
	double b_ActivityDurationHome_AlPHA;
	double b_ActivityDurationHome_BETA;
	double b_ActivityDurationHome_GAMMA;
	int ActivityDuration9_dummy = 0;
 	int ActivityDuration9Plus_dummy = 0;
	double b_Home_BUSI1_BP;
	double b_Home_COMM;
	double b_Home_HOTEL;
	double b_Home_RESIDENTIAL;
	double b_StartTime_10_16H_Work_dummy;
	double b_StartTime_12_15H_Home_dummy;
	double b_StartTime_16_1H_Home_dummy;
	double b_StartTime_17_4H_Work_dummy;
	double b_StartTime_2_11H_Home_dummy;
	double b_StartTime_5_8H_Work_dummy;
	double b_StartTime_9H_Work_dummy;
	double b_Work_BP;
	double b_Work_BUSI2;
	double b_Work_COMMRES;
	double b_Work_RESIDENTIAL;
	double constant;
	
	double business1 = 0.0;
	double business2 = 0.0;
	double businessPark = 0.0;
	double residential = 0.0;
	double comm=0.0;
	double hotel=0.0;
	double sumAllDensities=0.0;
	
	double utility_work = 0.0;
	double utility_home = 0.0;
	double utility_other = 0.0;
	
	double probabilityWork = 0.0;
	double probabilityHome = 0.0;
	double random = 0.0;
	

	ArrayList<Double> workLocationsLat_DCM = new ArrayList<Double>();
	ArrayList<Double> workLocationsLon_DCM = new ArrayList<Double>();
	
	int consistent_DCM = 0;
	int workActivities_DCM = 0;
	int homeActivities_DCM = 0;
	int otherActivities_DCM = 0;
	int multipleWorks_DCM = 0;
	int numberOfWorkDays=0;
	int numberOfWorkLocations=0;
	int numberOfWorkDays_DCM=0;
	int numberOfWorkLocations_DCM=0;

	boolean worker_DCM;
	int adultWorkers_DCM=0;
	
	int	startTime_5_8H = 0;
 	int startTime_9H = 0;
 	int startTime_10_16H = 0;
 	int startTime_17_4H = 0;
	
 	int startTime_2_11H = 0;
 	int startTime_12_15H = 0;
 	int startTime_16_1H = 0;
	
	boolean detectedWork;
	boolean detectedWork_DCM;
	int identicalDetection=0;
	int differentDetection=0;
	
	double avgNumberOfWorkDays=0.0;
	double avgNumberOfWorkDays_DCM=0.0;
	
	int count=0;
	int workActivity=0;	
	int workActivitiesPerDay_DCM=0;
	int overnight = 0;
	String activityType_DCM = "";
	

	
	
	public static void main(String args[]) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException, ParseException {
		new extractWorkActivities1week_DCM_v2().go();	
	}


	private void go() throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException, ParseException {
		DataBaseAdmin dba = new DataBaseAdmin(new File("./data/dataBases/Ezlink_1week_DataBase_local.properties"));
		DataBaseAdmin dbaWorkFacilities = new DataBaseAdmin(new File("./data/dataBases/KrakatauWorkFacilities_local.properties"));
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84", "WGS84_UTM48N");
//		BufferedWriter out = new BufferedWriter( new FileWriter("C:\\Work\\Workspace\\MatsimProjects\\data\\ezLinkAnalysis\\HomeLocations1.csv"));
//		BufferedWriter outWorkActivities = new BufferedWriter( new FileWriter("C:\\Work\\Workspace\\MatsimProjects\\data\\ezLinkAnalysis\\EZ-Link_Activities_Work.csv"));
//		outWorkActivities.close();
		BufferedWriter outActivities = new BufferedWriter( new FileWriter("C:\\Workspace\\MatsimProjects\\data\\ezLinkAnalysis\\DCM\\Activities_DCM_Tuesday1.csv"));
		outActivities.close();
		BufferedWriter outWorkLocations = new BufferedWriter( new FileWriter("C:\\Workspace\\MatsimProjects\\data\\ezLinkAnalysis\\DCM\\WorkActivities_1week_Tuesday1.csv"));
		outWorkLocations.close();
		BufferedWriter outWorkdays = new BufferedWriter( new FileWriter("C:\\Workspace\\MatsimProjects\\data\\ezLinkAnalysis\\DCM\\WorkdaysNumber_1week_Tuesday1.csv"));
		outWorkdays.close();
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		endTimeCalculator c = new endTimeCalculator();	
		
		ArrayList<Long> cardIDsArray = new ArrayList<Long>();
//		ResultSet cardIDs = dba.executeQuery("SELECT * FROM card_ids WHERE PassengerType='Adult' OR PassengerType='Senior Citizen' ORDER BY rand() LIMIT 300");
		ResultSet cardIDs = dba.executeQuery("SELECT * FROM card_ids12042011 WHERE PassengerType='Adult' OR PassengerType='Senior Citizen' ORDER BY `CARD_ID` LIMIT 1000000");
		while(cardIDs.next()){
		  	workLocationsLat.clear();
		  	workLocationsLon.clear();
		  	workLocationsLat_DCM.clear();
		  	workLocationsLon_DCM.clear();

			CARD_ID = cardIDs.getLong(1);
			agent++;
			numberOfWorkDays=0;
			numberOfWorkLocations=0;
			numberOfWorkDays_DCM=0;
			numberOfWorkLocations_DCM=0;
			worker=false;
			worker_DCM=false;
			if(agent==tenthousand){
				elapsedTime = System.currentTimeMillis() - timerStart;
				System.out.println("Agent:"+agent+" Time: "+elapsedTime+"\n");
				tenthousand=tenthousand + 10000;
				timerStart = System.currentTimeMillis();
			}
			int Mo=0,Tu=0,We=0,Th=0,Fr=0;
			for(String dayTable : dayTables){
//				ResultSet agentTrips = dba.executeQuery("SELECT CARD_ID,Journey_Start_Time,Journey_End_Time, start_lat,start_lon, end_lat,end_lon,PassengerType FROM trips_ver1 WHERE CARD_ID="+Long.toString(CARD_ID)+" AND PassengerType='Adult' ORDER BY Journey_Start_Date, Journey_Start_Time");
				ResultSet agentTrips = dba.executeQuery("SELECT CARD_ID,Ride_Start_Time,Ride_Time, start_lat,start_lon, end_lat,end_lon,PassengerType,TRAVEL_MODE, ALIGHTING_STOP_STN FROM "+dayTable+" WHERE CARD_ID="+Long.toString(CARD_ID)+" ORDER BY Ride_Start_Date, Ride_Start_Time");	
				count=0;
				workActivity=0;	
				workActivitiesPerDay_DCM=0;
				overnight = 0;
				String activityType_DCM = "";
				while(agentTrips.next()){
					detectedWork=false;
					detectedWork_DCM=false;
					if(count==0){
						firstLat = agentTrips.getDouble(4);
						firstLon = agentTrips.getDouble(5);
						Coord coordStart = new Coord(firstLon, firstLat);
						Coord UTMStart = ct.transform(coordStart);
						firstLon=UTMStart.getX();
						firstLat=UTMStart.getY();
						
						agentTrips.last();
						jEndTime = c.calculateEndTime(agentTrips.getString(2),agentTrips.getDouble(3));
	//					System.out.println(agentTrips.getString(2)+","+agentTrips.getLong(3)+", "+jEndTime+"\n");
						endLat = agentTrips.getDouble(6);
						endLon = agentTrips.getDouble(7);
						Coord coordEnd = new Coord(endLon, endLat);
						Coord UTMEnd = ct.transform(coordEnd);
						endLon=UTMEnd.getX();
						endLat=UTMEnd.getY();
						endStation=agentTrips.getString("ALIGHTING_STOP_STN");
						lastTravelMode=agentTrips.getString("TRAVEL_MODE");
						agentTrips.first();
					}
					
					startLat = agentTrips.getDouble(4);
					startLon = agentTrips.getDouble(5);
					Coord coordStart = new Coord(startLon, startLat);
					Coord UTMStart = ct.transform(coordStart);
					startLon=UTMStart.getX();
					startLat=UTMStart.getY();
					double distance = Math.sqrt((startLat - endLat)*(startLat - endLat) + (startLon - endLon)*(startLon - endLon));
					long distanceRounded = Math.round(distance);
				  
					jStartTime = agentTrips.getString(2);	  
					Date startTime = sdf.parse(jStartTime);
					Date endTime = sdf.parse(jEndTime);
					long actDuration = startTime.getTime() - endTime.getTime();
					if(actDuration < 0)
						actDuration = actDuration + 24*60*60*1000;
					double actDurationH = ((double)actDuration/(1000*60*60));
	//				passenger = agentTrips.getString(8);
		
					//Rule based
					if (distanceRounded<=1000 && actDuration>=3600000 && count!=0){
						consistent=consistent+1;
						if(actDuration >= 21600000 && actDuration <= 57600000){						
							workActivity=workActivity+1;	
							detectedWork = true;
							longact=longact+1;							
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
								appendToWorkActivityFile(Long.toString(CARD_ID),Double.toString(actDurationH),Double.toString(startTime.getTime()/(1000*60*60)),Double.toString(endLat),Double.toString(endLon));
								numberOfWorkLocations++;
							}
									
//						  	if(workActivity==2)
//								multipleWorks++;
							if(workActivity==1){
								numberOfWorkDays++;
								worker=true;
							}
//							outWork.write(Long.toString(CARD_ID)+","+Double.toString(startLat)+","+Double.toString(startLon)+"\n");  
//							outWork.flush();
						}
					  						
//					  	System.out.println(twoworks+","+Long.toString(CARD_ID));
//					 	System.out.println(Long.toString(CARD_ID)+","+Double.toString(firstLat)+","+Double.toString(firstLon)+","+Double.toString(startLat)+","+Double.toString(startLon)+"\n");		
					 
					}
							
						//Discrete Choice model
					if (distanceRounded<=1000 && actDuration>=3600000){	
						consistent_DCM=consistent_DCM+1;	
						if(count==0){
							nightConsistent = nightConsistent + 1;
							overnight = 1;
						}
						startTime_5_8H = 0;
					 	startTime_9H = 0;
					 	startTime_10_16H = 0;
					 	startTime_17_4H = 0;
						
					 	startTime_2_11H = 0;
					 	startTime_12_15H = 0;
					 	startTime_16_1H = 0;
					 	
						ActivityDuration9_dummy = 0;
						ActivityDuration9Plus_dummy = 0;
				 	
					 	//DCM Parameters
					 	b_ActivityDuration9 =	0.805338;
					 	b_ActivityDuration9Plus =	-0.269127;
					 	b_ActivityDurationHome_AlPHA	=7.18203;
					 	b_ActivityDurationHome_BETA	=-0.439731;
					 	b_ActivityDurationHome_GAMMA	=3.31529;
					 	b_Home_COMM	=-6.42943;
					 	b_Home_HOTEL	=-28.6499;
					 	b_Home_RESIDENTIAL	=2.13803;
					 	b_StartTime_10_16H_Work_dummy	=-1.56829;
					 	b_StartTime_12_15H_Home_dummy	=-1.28766;
					 	b_StartTime_16_1H_Home_dummy=	0;
					 	b_StartTime_17_4H_Work_dummy=	-3.40832;
					 	b_StartTime_2_11H_Home_dummy=	-3.1303;
					 	b_StartTime_5_8H_Work_dummy=	0;
					 	b_StartTime_9H_Work_dummy=	-0.715151;
					 	b_Work_BP	=58.2552;
					 	b_Work_BUSI2	=2.27299;
					 	b_Work_RESIDENTIAL	=-1.27675;
					 	constant =	2.57561;
						
						int actStartTime = calculateTime(jEndTime)/3600;
	
							
						//Work Start Time Dummys
						if(actStartTime >= 5 && actStartTime < 9){
							startTime_5_8H =1;
						}
						else if(actStartTime == 9){
						 	startTime_9H = 1;
						}
						else if (actStartTime >= 10 && actStartTime < 17){
						 	startTime_10_16H = 1;
						}
					 	else
					 	{
						 	startTime_17_4H = 1;
						}
						
						//Home Start Times Dummys
						if(actStartTime >= 2 && actStartTime < 12){
							startTime_2_11H = 1;
						}
						else if(actStartTime >= 12 && actStartTime < 16){
						 	startTime_12_15H = 1;
						}
						else{
							startTime_16_1H = 1;
						}
						
						if(actDurationH < 9){
							ActivityDuration9_dummy = 1;
						}
						else{
							ActivityDuration9Plus_dummy = 1;
						}

						
						
						if(lastTravelMode.equals("RTS")){
							ResultSet stopID = dba.executeQuery("SELECT stop_id FROM train_stops WHERE stop_name='"+endStation+"'"); 
							while(stopID.next()){
								endStation = stopID.getString(1);
							}
							stopID.close();
						}
						
		
						ResultSet densities = dbaWorkFacilities.executeQuery("SELECT * FROM StopDensitiesKernel1000 WHERE stop_id ='"+endStation+"'");
						while(densities.next()){									
							businessPark = densities.getDouble("BUSIPARK");
							business2 = densities.getDouble("BUSI2");
							residential = densities.getDouble("RESIDENTIAL");
							comm = densities.getDouble("COMM");
							hotel = densities.getDouble("HOTEL");
						}
						densities.close();

						businessPark = businessPark / 1000000;
						business2 = business2 / 1000000;
						residential = residential / 1000000;
						comm = comm / 1000000;
						hotel = hotel / 1000000;
						

								
						//Define utilities
						 utility_work =  b_ActivityDuration9 *  actDurationH * ActivityDuration9_dummy
								+ ( b_ActivityDuration9Plus * (actDurationH - 9) + 9 * b_ActivityDuration9 ) * ActivityDuration9Plus_dummy 
								+ b_StartTime_5_8H_Work_dummy * startTime_5_8H + b_StartTime_9H_Work_dummy * startTime_9H + b_StartTime_10_16H_Work_dummy * startTime_10_16H + b_StartTime_17_4H_Work_dummy * startTime_17_4H 
								+ b_Work_BP * businessPark  + b_Work_BUSI2 * business2  + b_Work_RESIDENTIAL * residential;
						 utility_home = b_ActivityDurationHome_AlPHA * ( 1 / ( 1 + Math.exp( b_ActivityDurationHome_BETA * actDurationH + b_ActivityDurationHome_GAMMA ) ) ) 
								+ b_StartTime_2_11H_Home_dummy * startTime_2_11H + b_StartTime_12_15H_Home_dummy * startTime_12_15H + b_StartTime_16_1H_Home_dummy * startTime_16_1H
								+ b_Home_COMM * comm  + b_Home_HOTEL * hotel + b_Home_RESIDENTIAL * residential;
						 utility_other = constant;
							
//						System.out.println();
//						System.out.println("Activity Start time: "+actStartTime);
//						System.out.println("Duration: "+actDurationH);
//						System.out.println("--------Utilities-------");
//						System.out.println("Duration Work < 10: "+(  b_ActivityDuration10 *  actDurationH + b_ActivityDurationConst10_Work)* ActivityDuration10_dummy);
//						System.out.println("Duration Work >= 10: "+( b_ActivityDuration10Plus * actDurationH + b_ActivityDurationConst10Plus_Work) * ActivityDuration10Plus_dummy );
//						System.out.println("Duration Home: "+b_ActivityDurationHome_AlPHA * ( 1 / ( 1 + Math.exp( b_ActivityDurationHome_BETA * actDurationH + b_ActivityDurationHome_GAMMA ))));
//						System.out.println("Start Work 5-8: "+ b_StartTime_5_8H_Work_dummy * startTime_5_8H);
//						System.out.println("Start Work 9: "+ b_StartTime_9H_Work_dummy * startTime_9H);
//						System.out.println("Start Work 10-16: "+ b_StartTime_10_16H_Work_dummy * startTime_10_16H);
//						System.out.println("Start Work 17-4: "+ b_StartTime_17_4H_Work_dummy * startTime_17_4H);
//						System.out.println("Start Home 2-11:  "+(b_StartTime_2_11H_Home_dummy * startTime_2_11H));
//						System.out.println("Start Home 12-15:  "+(b_StartTime_12_15H_Home_dummy * startTime_12_15H));
//						System.out.println("Start Home 16-1:  "+(b_StartTime_16_1H_Home_dummy * startTime_16_1H));
////						System.out.println("Business2: "+(b_Busi2 * business2));
////						System.out.println("BusinessOther: "+(b_OtherBusiRatio * businessOther));
//	//					System.out.println("Residential: "+(b_ResidentialMix * residential));
////						System.out.println("SumAll: "+sumAllDensities);
////						System.out.println("UtilitySpecial: "+(b_UTILITY_SPEC * utility_spec));
//						System.out.println("Uw="+utility_work+"     Uh="+utility_home+"	U0="+utility_other);
							
						probabilityWork = Math.exp(utility_work)/(Math.exp(utility_other)+Math.exp(utility_work)+Math.exp(utility_home));
						probabilityHome = Math.exp(utility_home)/(Math.exp(utility_other)+Math.exp(utility_work)+Math.exp(utility_home));
						random = Math.random();
							
//						System.out.println("Pw="+probabilityWork+"     Ph="+probabilityHome+"   Po="+(1-(probabilityHome+probabilityWork))+"	r="+random);
						if(random<=probabilityWork){
							activityType_DCM = "Work";
//							System.out.println("WORK");
							workActivities_DCM++;
							detectedWork_DCM=true;								
							workActivitiesPerDay_DCM++;	
							worker_DCM=true;
								
						  	if(workActivitiesPerDay_DCM==1)
								numberOfWorkDays_DCM++;
								
								
							boolean sameLocation = false;
								
							//Check if there is already same work location in the array list for this worker
							if(!workLocationsLat_DCM.isEmpty()){
								for(int i=0;i<workLocationsLat_DCM.size();i++){
									double distanceWorkLocation = Math.sqrt((workLocationsLat_DCM.get(i) - endLat)*(workLocationsLat_DCM.get(i) - endLat) + (workLocationsLon_DCM.get(i) - endLon)*(workLocationsLon_DCM.get(i) - endLon));
									if(distanceWorkLocation<=1000.0){
										sameLocation=true;
										break;
									}
								}
							}	
								
							//If work location is new, add it to the work-locations list		
							if(sameLocation==false)									
							{
								workLocationsLat_DCM.add(endLat);
								workLocationsLon_DCM.add(endLon);
//								appendToLocationsFile_DCM(Long.toString(CARD_ID),Double.toString(endLat),Double.toString(endLon));
								numberOfWorkLocations_DCM++;
							}
						}
						else if(random > probabilityWork && random <= (probabilityWork + probabilityHome)){
//							System.out.println("HOME");
							homeActivities_DCM++;
							activityType_DCM = "Home";
						}
						else{
//							System.out.println("OTHER");
							otherActivities_DCM=otherActivities_DCM+1;
							activityType_DCM = "Other";
						}
					}						
									  		
//					outWork.write(Long.toString(CARD_ID)+","+Double.toString(startLat)+","+Double.toString(startLon)+"\n");  
//					outWork.flush();
				  				
					appendToActivityDCMFile(activityType_DCM,Long.toString(CARD_ID),Double.toString(actDurationH),Double.toString(endTime.getTime()/(1000*60*60)),Double.toString(endLat),Double.toString(endLon), Integer.toString(overnight));
			  	  
					jEndTime = c.calculateEndTime(agentTrips.getString(2),agentTrips.getDouble(3));		  
					endLat = agentTrips.getDouble(6);
					endLon = agentTrips.getDouble(7);
					Coord coordEnd = new Coord(endLon, endLat);
					Coord UTMEnd = ct.transform(coordEnd);
					endLon=UTMEnd.getX();
					endLat=UTMEnd.getY();
					endStation=agentTrips.getString("ALIGHTING_STOP_STN");
					lastTravelMode=agentTrips.getString("TRAVEL_MODE");
					count=count+1;		
					
					if(detectedWork==detectedWork_DCM){
						identicalDetection++;
					}
					else{
						differentDetection++;			
//						System.out.println("--------------- MISSMATCH !!! --------------");
					}
					
				}
					
				agentTrips.close(); 

				
				if(dayTable.equals("v1_trips11042011")) Mo=workActivity;
				if(dayTable.equals("v1_trips12042011")) Tu=workActivity;
				if(dayTable.equals("v1_trips13042011")) We=workActivity;
				if(dayTable.equals("v1_trips14042011")) Th=workActivity;
				if(dayTable.equals("v1_trips15042011")) Fr=workActivity;

			}
			
		  	if(workLocationsLat_DCM.size()>1){
				multipleWorks_DCM++;
			}
		  	
		  	if(workLocationsLat.size()>1){
				multipleWorks++;
			}

	
			if(worker) 
			{
				adultWorkers=adultWorkers+1;
				avgNumberOfWorkDays = (avgNumberOfWorkDays*(adultWorkers-1)+numberOfWorkDays)/adultWorkers;
			}
			if(worker_DCM){
				adultWorkers_DCM=adultWorkers_DCM+1;
				avgNumberOfWorkDays_DCM = (avgNumberOfWorkDays_DCM*(adultWorkers_DCM-1)+numberOfWorkDays_DCM)/adultWorkers_DCM;
			}
			
			
					
			appendToWorkdaysFile(Long.toString(CARD_ID),Integer.toString(numberOfWorkDays),Integer.toString(numberOfWorkLocations),Integer.toString(Mo),Integer.toString(Tu),Integer.toString(We),Integer.toString(Th),Integer.toString(Fr));
		}
		dba.close();
//		out.close();
	
		  System.out.println("Agents: "+agent);
		  System.out.println();
		  System.out.println("--------- Rule based approach--------");
		  System.out.println("Consistent activities: "+consistent);		
		  System.out.println("Work activities (7-16h): "+longact);
		  System.out.println("Other activities: "+(consistent-longact));
		  System.out.println("Multiple work locations: "+multipleWorks);
		  System.out.println("Total adult workers: "+adultWorkers);
		  System.out.println(avgNumberOfWorkDays);
		  
		  System.out.println();
		  System.out.println("--------- DCM Model--------");
		  System.out.println("Consistent activities: "+consistent_DCM);
		  System.out.println("Consistent overnight activities: "+nightConsistent);	
		  System.out.println("Work activities: "+workActivities_DCM);
		  System.out.println("Home activities: "+homeActivities_DCM);
		  System.out.println("Other activities: "+otherActivities_DCM);
		  System.out.println("Multiple work locations: "+multipleWorks_DCM);
		  System.out.println("Total adult workers: "+adultWorkers_DCM);
		  System.out.println(avgNumberOfWorkDays_DCM);
		
		  System.out.println();
		  System.out.println("---------Model comparison--------");
		  System.out.println("Match: "+identicalDetection);
		  System.out.println("Mismatch: "+differentDetection);
	}

	
	public void appendToActivityDCMFile (String type, String card_id, String duration, String startTime, String lat, String lon, String overnight) {
	     BufferedWriter bw = null;
	     try {
	         bw = new BufferedWriter(new FileWriter("C:\\Workspace\\MatsimProjects\\data\\ezLinkAnalysis\\DCM\\Activities_DCM_Tuesday2.csv", true));
	         bw.write(type+","+card_id+","+duration+","+startTime+","+lat+","+lon+","+overnight+"\n");
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
	
	public void appendToWorkActivityFile (String card_id, String duration, String startTime, String lat, String lon) {
	     BufferedWriter bw = null;
	     try {
	         bw = new BufferedWriter(new FileWriter("C:\\Workspace\\MatsimProjects\\data\\ezLinkAnalysis\\DCM\\WorkActivities_1week_Tuesday2.csv", true));
	         bw.write(card_id+","+duration+","+startTime+","+lat+","+lon+"\n");
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
	         bw = new BufferedWriter(new FileWriter("C:\\Workspace\\MatsimProjects\\data\\ezLinkAnalysis\\DCM\\WorkdaysNumber_1week_Tuesday2.csv", true));
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
	
	
	
	  public Integer calculateTime(String time) throws ParseException
	  {
	  	Integer timeHours = Integer.parseInt(time.substring(0,2));
	  	Integer timeMinutes = Integer.parseInt(time.substring(3,5));
	  	Integer timeSeconds = Integer.parseInt(time.substring(6,8));
	  	Integer timeInSeconds = timeHours*3600+timeMinutes*60+timeSeconds;

	  	return timeInSeconds;		
	  }
}
