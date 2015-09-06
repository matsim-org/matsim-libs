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

public class ptJourneysAnalysisHITS {
	
	public static void main(String args[]) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException {
		DataBaseAdmin dba = new DataBaseAdmin(new File("./data/dataBases/LocalDataBase.properties"));
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84", "WGS84_UTM48N");
//		BufferedWriter outToFile = new BufferedWriter( new FileWriter("C:\\Work\\Workspace\\MatsimProjects\\data\\ezLinkAnalysis\\HITSDistancesBetweenTrips.csv"));
//		BufferedWriter outFirstLast = new BufferedWriter( new FileWriter("C:\\Work\\Workspace\\MatsimProjects\\data\\ezLinkAnalysis\\HITSDistanceFirstLast.csv"));
		BufferedWriter outWalk = new BufferedWriter( new FileWriter("C:\\Work\\Workspace\\MatsimProjects\\data\\ezLinkAnalysis\\HITSDistanceWalkDistances.csv"));
		
		String household="";
		String pax="";
		String mode="";
		Double startLat=0.0;
		Double startLon=0.0;
		Double endLat=0.0;
		Double endLon=0.0;
		Double firstLat=0.0;
		Double firstLon=0.0;
		Double distanceTrips=0.0;
		Long distanceTripsRounded = 0L;
		Double distanceWalk=0.0;
		Long distanceWalkRounded = 0L;
		
		int bus=0;
		int walk=0;
		int rail=0;
		int compBus=0;
		int taxi=0;
		int carPsgr=0;
		int carDrv=0;
		int cycle=0;
		int unknown=0;
		int pt=0;
		int other=0;
		int agent_pt=0;
		int agent_taxi=0;
		int agent_carPsgr=0;
		int agent_carDrv=0;
		int agent_compBus=0;
		int agent_walk=0;
		int agent_cycle=0;
		int agent_unknown=0;
		int agent_other=0;
		int agent_mix=0;
		int bus_journey=0;
		int rail_journey=0;
		int busrail_journey=0;
		int nonpt_journeys=0;
		int firstorlast=0;
		int agent_taxi_1=0;
		int agent_carPsgr_1=0;
		int agent_carDrv_1=0;
		int agent_compBus_1=0;
		int agent_cycle_1=0;
		int agent_unknown_1=0;
		int longwalk=0;
		int longwalk_1=0;
		
		int numberOfStages=0;
		int totalTrips=0;
		int numberOfPersons=0;

		
//		ArrayList<Double> allDistances = new ArrayList<Double>();


		ResultSet persons = dba.executeQuery("SELECT * FROM hits_persons_using_pt");
	
		while(persons.next()){
			
			household = persons.getString(1);
			pax = persons.getString(2);
			ResultSet agentTrips = dba.executeQuery("SELECT h1_hhid,pax_id,trip_id,stage_id,t10_mode,start_lat,start_lon, end_lat,end_lon FROM hits_journeys_with_pt WHERE h1_hhid='"+household+"' AND pax_id='"+pax+"' ORDER BY trip_id,stage_id");
			int trips=1;
			numberOfPersons++;
//			System.out.println(numberOfPersons);
			
			while(agentTrips.next()){
				
				int tripID = agentTrips.getInt(3);
				int stageID = agentTrips.getInt(4);
				
				if(trips!=tripID){		
									
					//Count journeys with bus and rail
					if(bus>0 && rail==0)
						bus_journey++;
					if(rail>0 && bus==0)
						rail_journey++;
					if(bus>0 && rail>0)
						busrail_journey++;
					if(bus==0 && rail==0)
						nonpt_journeys++;
					
					if(pt>0 && other==0)
						agent_pt=agent_pt+1;
					if(pt>0 && other>0){
						agent_mix++;
						if(trips==1)firstorlast++;
					}
					if(pt==0){
						agent_other++;
						if(trips==1)firstorlast++;
					}
					if(taxi>0){
						agent_taxi++;
						if(trips==1)agent_taxi_1++;
					}
					if(carPsgr>0){
						agent_carPsgr++;
						if(trips==1)agent_carPsgr_1++;
					}
					if(carDrv>0){
						agent_carDrv++;
						if(trips==1)agent_carDrv_1++;
					}
					if(compBus>0){
						agent_compBus++;
						if(trips==1)agent_compBus_1++;
					}
					if(cycle>0){
						agent_cycle++;
						if(trips==1)agent_cycle_1++;
					}
					if(unknown>0){
						agent_unknown++;
						if(trips==1)agent_unknown_1++;
					}
					
					if(walk>0 && pt==0 && other==0){
						agent_walk = agent_walk +1;
						distanceWalk = Math.sqrt((startLat - endLat)*(startLat - endLat) + (startLon - endLon)*(startLon - endLon));
						distanceWalkRounded = Math.round(distanceWalk);
						if(distanceWalkRounded>1000){
							longwalk++;
							if(trips==1)longwalk_1++;
						}
					    outWalk.write(Long.toString(distanceWalkRounded)+"\n");	
//					    System.out.println(Long.toString(distanceWalkRounded)+","+agent_walk);
					}
					
					pt=0;
					bus=0;
					rail=0;
					other=0;
					taxi=0;
					walk=0;
					carPsgr=0;
					carDrv=0;
					compBus=0;
					cycle=0;
					unknown=0;
				
					startLat = agentTrips.getDouble(6);
					startLon = agentTrips.getDouble(7);
					Coord coordStart = new Coord(startLon, startLat);
					Coord UTMStart = ct.transform(coordStart);
					startLon=UTMStart.getX();
					startLat=UTMStart.getY();
					distanceTrips = Math.sqrt((startLat - endLat)*(startLat - endLat) + (startLon - endLon)*(startLon - endLon));
					distanceTripsRounded = Math.round(distanceTrips);
					
					if(distanceTrips>1000)
//						System.out.println(distanceTripsRounded+","+household+","+pax+","+tripID);
						agent_unknown=agent_unknown+1;
//					System.out.println(Math.round(distanceRounded));
//					outToFile.write(Long.toString(distanceRounded)+","+agentTrips.getString(1)+","+agentTrips.getString(2)+"\n");
					trips=tripID;	
					totalTrips=totalTrips+1;
//					Double oDistance = new Double(distance);
//					allDistances.add(oDistance);
				  
				}
				if(tripID==1 && stageID==1){
					firstLat = agentTrips.getDouble(6);
					firstLon = agentTrips.getDouble(7);
					Coord coordStart = new Coord(firstLon, firstLat);
					Coord UTMStart = ct.transform(coordStart);
					firstLon=UTMStart.getX();
					firstLat=UTMStart.getY();	
					startLon=firstLon;
					startLat=firstLat;
				}
			  
			  
				endLat = agentTrips.getDouble(8);
				endLon = agentTrips.getDouble(9);
				Coord coordEnd = new Coord(endLon, endLat);
				Coord UTMEnd = ct.transform(coordEnd);
				endLon=UTMEnd.getX();
				endLat=UTMEnd.getY();
				mode=agentTrips.getString(5);
//			  	System.out.println(mode);
			  		  
			  	if(mode.equals("publBus")){
			  		bus=bus+1;	
			  		pt=pt+1;
			  	}
			  	else if(mode.equals("mrt") || mode.equals("lrt")){
			    	 rail = rail +1;
			    	 pt=pt+1;
			  	}
			  	else if(mode.equals("compBus")||mode.equals("schBus")||mode.equals("shutBus")){
				     compBus = compBus +1;
				     other=other+1;
			  	}
			  	else if(mode.equals("walk")){
				     walk=walk+1;
//				     other=other+1;
			  	}
			  	else if(mode.equals("taxi")){
				    taxi=taxi+1;
				    other=other+1;
			  	}
			  	else if(mode.equals("carPsgr")||mode.equals("lhdvPsgr")||mode.equals("motoPsgr")){
				    carPsgr=carPsgr+1;
				    other=other+1;
			  	}
			  	else if(mode.equals("carDrv")||mode.equals("lhdvDrv")||mode.equals("motoDrv")){
				    carDrv=carDrv+1;
				    other=other+1;
			  	}		  
			  	else if(mode.equals("cycle")){
			  		cycle++;
			  		other=other+1;
			  	}
			  	else if(mode.equals("other")){
			  		unknown++;
			  		other=other+1;
			  	}
			  		
			
				numberOfStages=numberOfStages + 1;
			}
		    
			
			totalTrips=totalTrips+1;
			double distanceFirstLast = Math.sqrt((firstLat - endLat)*(firstLat - endLat) + (firstLon - endLon)*(firstLon - endLon));
			long distanceFirstLastRounded = Math.round(distanceFirstLast);
			if(distanceFirstLastRounded>1000)agent_unknown_1++;
			
			if(bus>0 && rail==0)
				bus_journey++;
			if(rail>0 && bus==0)
				rail_journey++;
			if(bus>0 && rail>0)
				busrail_journey++;
			if(bus==0 && rail==0)
				nonpt_journeys++;
			
			if(pt>0 && other==0)
				agent_pt=agent_pt+1;
			if(pt>0 && other>0){
				agent_mix++;
				firstorlast++;
			}
			if(pt==0){
				agent_other++;
				firstorlast++;
			}
			if(taxi>0){
				agent_taxi++;
				agent_taxi_1++;
			}
			if(carPsgr>0){
				agent_carPsgr++;
				agent_carPsgr_1++;
			}
			if(carDrv>0){
				agent_carDrv++;
				agent_carDrv_1++;
			}
			if(compBus>0){
				agent_compBus++;
				agent_compBus_1++;
			}
			if(cycle>0){
				agent_cycle++;
				agent_cycle_1++;
			}
			if(unknown>0){
				agent_unknown++;
				agent_unknown_1++;
			}
			if(walk>0 && pt==0 && other==0){
				agent_walk = agent_walk +1;
				distanceWalk = Math.sqrt((startLat - endLat)*(startLat - endLat) + (startLon - endLon)*(startLon - endLon));
				distanceWalkRounded = Math.round(distanceWalk);
				if(distanceWalkRounded>1000){
					longwalk++;
					longwalk_1++;
				}
			    outWalk.write(Long.toString(distanceWalkRounded)+"\n");	
//			    System.out.println(Long.toString(distanceWalkRounded)+","+agent_walk);
			}
			pt=0;
			bus=0;
			rail=0;
			other=0;
			taxi=0;
			walk=0;
			carPsgr=0;
			carDrv=0;
			compBus=0;	
			cycle=0;
			unknown=0;

//			outFirstLast.write(Long.toString(distanceFirstLastRounded) +"\n");
//			System.out.println(Math.round(distanceFirstLast)+","+household+","+pax);
			agentTrips.close(); 
		}
		
		System.out.println("Total stages: "+numberOfStages);
		System.out.println("PT-only journeys (incl walk): "+agent_pt);
		System.out.println("Interrupted journeys: "+agent_mix);
		System.out.println("Non-PT journeys: "+agent_other);
		System.out.println(" out of Non-PT, Journeys only by walk: "+agent_walk);
		System.out.println("   out of 'only by walk', walks>1000m: "+longwalk);
		System.out.println("Out of Non-PT and Interrupted, First or Last Journey of the Day: "+firstorlast);
		System.out.println();
		System.out.println("Journeys incl. taxi: "+agent_taxi);
		System.out.println("Journeys incl. Psgr: "+agent_carPsgr);
		System.out.println("Journeys incl. Drv: "+agent_carDrv);
		System.out.println("Journeys incl. private Bus: "+agent_compBus);
		System.out.println("Journeys incl. Cycle: "+agent_cycle);
		System.out.println("Journeys by unknown: "+agent_unknown);
		System.out.println("Journeys by walk > 1000m: "+longwalk);
		System.out.println();
		System.out.println("First or Last journeys incl. taxi: "+agent_taxi_1);
		System.out.println("First or Last journeys incl. Psgr: "+agent_carPsgr_1);
		System.out.println("First or Last journeys incl. Drv: "+agent_carDrv_1);
		System.out.println("First or Last journeys  incl. private Bus: "+agent_compBus_1);
		System.out.println("First or Last journeys  incl. Cycle: "+agent_cycle_1);
		System.out.println("First or Last journeys  by unknown: "+agent_unknown_1);
		System.out.println("First or Last journeys  by walk > 1000m: "+longwalk_1);
		System.out.println();
		System.out.println("Journeys incl. only bus pt-mode: "+bus_journey);
		System.out.println("Journeys incl. only rail pt-mode: "+rail_journey);
		System.out.println("Journeys incl. only bus&rail pt-mode: "+busrail_journey);
		System.out.println("Journeys without pt-mode: "+nonpt_journeys);
		System.out.println();
		System.out.println("===============================");
		System.out.println("Total Journeys: "+totalTrips);
		System.out.println("Number of persons: "+numberOfPersons);
		
//		System.out.println("First Trip is without PT: "+firstTripNoPt);
//		System.out.println("Last Trip is without PT: "+lastTripNoPt);

//		System.out.println(allDistances.size());
//		outToFile.close();
//		outFirstLast.close();		
		outWalk.close();
	}
	
	
	

}
