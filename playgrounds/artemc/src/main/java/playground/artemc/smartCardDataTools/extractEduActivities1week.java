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

public class extractEduActivities1week {
	
	
	public static void main(String args[]) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException, ParseException {
		DataBaseAdmin dba = new DataBaseAdmin(new File("./data/dataBases/Ezlink_1week_DataBase.properties"));
		DataBaseAdmin artemc = new DataBaseAdmin(new File("./data/dataBases/artemcKrakatau.properties"));
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84", "WGS84_UTM48N");
		BufferedWriter outEduLocations = new BufferedWriter( new FileWriter("C:\\Work\\Workspace\\MatsimProjects\\data\\ezLinkAnalysis\\EduLocations_1week_temp.csv"));
		outEduLocations.close();
		
		BufferedWriter outEduDays = new BufferedWriter( new FileWriter("C:\\Work\\Workspace\\MatsimProjects\\data\\ezLinkAnalysis\\EduDaysNumber_1week_temp.csv"));
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		endTimeCalculator c = new endTimeCalculator();
		extractEduActivities1week eduActivities = new extractEduActivities1week();
		
		Long CARD_ID=0L;
		String jStartTime = "";
		String jEndTime = "";
		Double startLat=0.0;
		Double startLon=0.0;
		Double endLat=0.0;
		Double endLon=0.0;
		Double firstLat=0.0;
		Double firstLon=0.0;
		Double schoolLat=0.0;
		Double schoolLon=0.0;
		
		Integer twoedus=0;
		Integer consistent=0;
		Integer longact=0;
		Integer numStudents=0;
		Integer numSchoolStudents=0;
		Integer agent=0;
		int building_id=0;
		int tenthousand=10000;
		
		
		String[] dayTables={"v1_trips11042011","v1_trips12042011","v1_trips13042011","v1_trips14042011","v1_trips15042011"};
		Boolean student;
		Boolean twoEduLocations;
		Boolean schoolStudent;
		long timerStart = System.currentTimeMillis();
		long elapsedTime = 0L;
		
		ArrayList<Double> eduLocationsLat = new ArrayList<Double>();
		ArrayList<Double> eduLocationsLon = new ArrayList<Double>();
		ArrayList<Integer> eduLocationVisits = new ArrayList<Integer>();
		ArrayList<Integer> numberOfSchoolsInArea = new ArrayList<Integer>();

		ArrayList<Double> schoolDistance1st = new ArrayList<Double>();
		ArrayList<Double> schoolDistance2nd = new ArrayList<Double>();
		ArrayList<Double> schoolDistance3rd = new ArrayList<Double>();
		
		ArrayList<Integer> areaSchool_ids = new ArrayList<Integer>();
		ArrayList<Double> areaSchool_distances = new ArrayList<Double>();
		ArrayList<Integer> assigned_schools = new ArrayList<Integer>();
		
		ResultSet schools = artemc.executeQuery("SELECT id_building_directory, longitude, latitude FROM public_schools");
		ResultSet cardIDs = dba.executeQuery("SELECT * FROM CARD_IDs WHERE PassengerType='Child/Student'");
		while(cardIDs.next()){
		  	eduLocationsLat.clear();
		  	eduLocationsLon.clear();
		  	eduLocationVisits.clear();
		  	numberOfSchoolsInArea.clear();
		  	schoolDistance1st.clear();
		  	schoolDistance2nd.clear();
		  	assigned_schools.clear();
			CARD_ID = cardIDs.getLong(1);
			agent++;
			int numberOfSchoolDays=0;
			int numberOfEduLocations=0;
			student=false;
			schoolStudent = false;
			twoEduLocations=false;
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
				int eduActivity=0;		
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
				  
//						passenger = agentTrips.getString(8);
//						System.out.println(startTime.getTime()+","+endTime.getTime()+","+difference);
						if(difference>=7200000 && endTime.getTime() >= 21600000 && endTime.getTime() <= 43200000 && startTime.getTime() <= 64800000 && startTime.getTime() >= 36000000){
							longact=longact+1;
							if (distanceRounded<=1000){							
								eduActivity=eduActivity+1;								
								consistent=consistent+1;							
								boolean sameLocation = false;
								
								//Check if there is already same edu location in the array list for this student
								if(!eduLocationsLat.isEmpty()){
									for(int i=0;i<eduLocationsLat.size();i++){
										double distanceWorkLocation = Math.sqrt((eduLocationsLat.get(i) - endLat)*(eduLocationsLat.get(i) - endLat) + (eduLocationsLon.get(i) - endLon)*(eduLocationsLon.get(i) - endLon));
										if(distanceWorkLocation<=500.0){
											sameLocation=true;
											eduLocationVisits.set(i,eduLocationVisits.get(i)+1);
											break;
										}
									}
								}
								
								//If edu location is new, add it to the edu-locations list
								if(sameLocation==false)									
								{
									eduLocationsLat.add(endLat);
									eduLocationsLon.add(endLon);
									eduLocationVisits.add(1);
									int numberOfSchoolsCounter =0;
									double distance1st=100000000.0;
									double distance2nd=100000000.0;
									double distance3rd=100000000.0;
									areaSchool_distances.clear();
									areaSchool_ids.clear();
									schools.beforeFirst();
									//Find schools in the area
									while(schools.next()){
										building_id = schools.getInt(1);
										schoolLon = schools.getDouble(2);
										schoolLat = schools.getDouble(3);
										Coord coordSchool = new CoordImpl(schoolLon, schoolLat);
										Coord UTMSchool = ct.transform(coordSchool);
										schoolLon=UTMSchool.getX();
										schoolLat=UTMSchool.getY();
										double distanceToSchool = Math.sqrt((schoolLat - endLat)*(schoolLat - endLat) + (schoolLon - endLon)*(schoolLon - endLon));										
										if(distanceToSchool<distance1st){
											distance3rd=distance2nd;
											distance2nd=distance1st;
											distance1st=distanceToSchool;
										}
										else if(distanceToSchool<distance2nd){
												distance3rd=distance2nd;
												distance2nd=distanceToSchool;
											}
										else if(distanceToSchool<distance3rd){
													distance3rd=distanceToSchool;
										}										
										
										if(distanceToSchool<=500.0){
											schoolStudent=true;
											numberOfSchoolsCounter++;
											areaSchool_ids.add(building_id);
											areaSchool_distances.add(distanceToSchool/1000);
										}							
										
									}
									numberOfSchoolsInArea.add(numberOfSchoolsCounter);		
									schoolDistance1st.add(distance1st);
									schoolDistance2nd.add(distance2nd);
									
									//Assign a school according to exp-probability fucntion dependent on distance
									assigned_schools.add(assignSchool(areaSchool_ids,areaSchool_distances));														
									numberOfEduLocations++;
								}
									
							  	if(eduActivity==2)
									twoedus=twoedus+1;
							  		twoEduLocations=true;
								if(eduActivity==1){
									numberOfSchoolDays++;
									student=true;
								}
							}			  						
//					  	System.out.println(twoedus+","+Long.toString(CARD_ID));
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
					
				
				if(dayTable.equals("v1_trips11042011")) Mo=eduActivity;
				if(dayTable.equals("v1_trips12042011")) Tu=eduActivity;
				if(dayTable.equals("v1_trips13042011")) We=eduActivity;
				if(dayTable.equals("v1_trips14042011")) Th=eduActivity;
				if(dayTable.equals("v1_trips15042011")) Fr=eduActivity;

			}
			
			//Select the school location out of a detected set
			if(!eduLocationVisits.isEmpty()){
				//Find location with most visits
				int maxVisits = eduLocationVisits.get(0);
				int maxVisitsPosition = 0;
				int numberOfSchools=0;
				for(int i=1;i<eduLocationVisits.size();i++){
					if(eduLocationVisits.get(i)>maxVisits){
						maxVisits = eduLocationVisits.get(i);
						maxVisitsPosition=i;
						numberOfSchools=numberOfSchoolsInArea.get(i);
					}
					if(eduLocationVisits.get(i)==maxVisits && numberOfSchoolsInArea.get(i)>numberOfSchools){
						maxVisitsPosition=i;						
					}
				}
				//Call method for writing to File
				eduActivities.appendToLocationsFile(Long.toString(CARD_ID),Double.toString(eduLocationsLat.get(maxVisitsPosition)),Double.toString(eduLocationsLon.get(maxVisitsPosition)),Integer.toString(maxVisits),Integer.toString(numberOfSchoolsInArea.get(maxVisitsPosition)),Double.toString(schoolDistance1st.get(maxVisitsPosition)),Double.toString(schoolDistance2nd.get(maxVisitsPosition)),Integer.toString(assigned_schools.get(maxVisitsPosition)));
			}
			
			if(student) numStudents=numStudents+1;
			if(schoolStudent) numSchoolStudents=numSchoolStudents+1;
			outEduDays.write(Long.toString(CARD_ID)+","+Integer.toString(numberOfSchoolDays)+","+Integer.toString(numberOfEduLocations)+","+Mo+","+Tu+","+We+","+Th+","+Fr+"\n");
		  	outEduDays.flush();

		}
		outEduDays.close();
	
		  System.out.println("Total edu activities (Start=6am-12am, End=10am-6pm, Duration>= 2 hours): "+longact);
		  System.out.println("Consistent: "+consistent);
		  System.out.println("Total students: "+numStudents);
		  System.out.println("School students: "+numSchoolStudents);
		  System.out.println();
		  System.out.println("More then 1 edu: "+twoedus);
	}
	

	private static int assignSchool(ArrayList<Integer> areaSchool_ids,ArrayList<Double> areaSchool_distances) {
		
		double sumEvalues = 0;
		int assignedSchool=0;
		ArrayList<Double> efunctions = new ArrayList<Double>();
		for(int i=0;i<areaSchool_distances.size();i++){		
			double efunctionvalue = 1*Math.exp(-1*areaSchool_distances.get(i));
			sumEvalues = sumEvalues + efunctionvalue;
			efunctions.add(efunctionvalue);
		}
//		System.out.println("   "+sumEvalues);
		for(int i=0;i<efunctions.size();i++){	
			double probability = efunctions.get(i)/sumEvalues;
			efunctions.set(i, probability);						
//			System.out.println(probability+","+areaSchool_ids.get(i));
		}
		double randNumber = Math.random();
//		System.out.print(randNumber+",");
		double probabilitySum=0;
		for(int i=0;i<efunctions.size();i++){
			probabilitySum = probabilitySum + efunctions.get(i);
			if(randNumber<probabilitySum){
				assignedSchool = areaSchool_ids.get(i); 
				break;
			}
		}
		return assignedSchool;
	}


	public void appendToLocationsFile (String card_id, String lat, String lon, String visits, String numberOfschools, String distance1st, String distance2nd, String school) {
	     BufferedWriter bw = null;
	     try {
	         bw = new BufferedWriter(new FileWriter("C:\\Work\\Workspace\\MatsimProjects\\data\\ezLinkAnalysis\\EduLocations_1week_temp.csv", true));
	         bw.write(card_id+","+lat+","+lon+","+visits+","+numberOfschools+","+distance1st+","+distance2nd+","+school+"\n");
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
