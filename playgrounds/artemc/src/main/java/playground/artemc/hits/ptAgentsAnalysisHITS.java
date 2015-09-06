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

public class ptAgentsAnalysisHITS {
	
	public static void main(String args[]) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException {
		DataBaseAdmin dba = new DataBaseAdmin(new File("./data/dataBases/LocalDataBase.properties"));
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84", "WGS84_UTM48N");
		BufferedWriter outToFile = new BufferedWriter( new FileWriter("C:\\Work\\Workspace\\MatsimProjects\\data\\ezLinkAnalysis\\HITSDistancesBetweenTrips.csv"));
//		BufferedWriter outModes = new BufferedWriter( new FileWriter("C:\\Work\\Workspace\\MatsimProjects\\data\\ezLinkAnalysis\\HITSModes.csv"));
		
		String household="";
		String pax="";
		String mode="";
		Double startLat=0.0;
		Double startLon=0.0;
		Double endLat=0.0;
		Double endLon=0.0;
		Double firstLat=0.0;
		Double firstLon=0.0;
		int bus=0;
		int walk=0;
		int rail=0;
		int compBus=0;
		int taxi=0;
		int carPsgr=0;
		int carDrv=0;
		int pt=0;
		int other=0;
		int agent_pt=0;
		int agent_bus=0;
		int agent_rail=0;
		int agent_taxi=0;
		int agent_carPsgr=0;
		int agent_carDrv=0;
		int agent_compBus=0;
		int agent_walk=0;
		int agent_unknown=0;
		int agent_other=0;
		int bus_journey=0;
		int rail_journey=0;
		int busrail_journey=0;
		int nonpt_journeys=0;
		
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
			System.out.println(numberOfPersons);
			
			while(agentTrips.next()){
				
				
				int tripID = agentTrips.getInt(3);
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
					
					bus=0;
					rail=0;
				
					startLat = agentTrips.getDouble(6);
					startLon = agentTrips.getDouble(7);
					Coord coordStart = new Coord(startLon, startLat);
					Coord UTMStart = ct.transform(coordStart);
					startLon=UTMStart.getX();
					startLat=UTMStart.getY();
					double distanceTrips = Math.sqrt((startLat - endLat)*(startLat - endLat) + (startLon - endLon)*(startLon - endLon));
//					long distanceTripsRounded = Math.round(distanceTrips);
					if(distanceTrips>500)
//						System.out.println(distanceTripsRounded+","+household+","+pax+","+tripID);
						agent_unknown=agent_unknown+1;
//					System.out.println(Math.round(distanceRounded));
//					outToFile.write(Long.toString(distanceRounded)+","+agentTrips.getString(1)+","+agentTrips.getString(2)+"\n");
					
					trips=tripID;	
					totalTrips=totalTrips+1;
//					Double oDistance = new Double(distance);
//					allDistances.add(oDistance);
				  
				  
				}
				else{
					firstLat = agentTrips.getDouble(3);
					firstLon = agentTrips.getDouble(4);
					Coord coordStart = new Coord(firstLon, firstLat);
					Coord UTMStart = ct.transform(coordStart);
					firstLon=UTMStart.getX();
					firstLat=UTMStart.getY();				  
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
			  	else if(mode.equals("compBus")){
				     compBus = compBus +1;
				     other=other+1;
			  	}
			  	else if(mode.equals("walk")){
				     walk=walk+1;
//			     other=other+1;
			  	}
			  	else if(mode.equals("taxi")){
				    taxi=taxi+1;
				    other=other+1;
			  	}
			  	else if(mode.equals("carPsgr")){
				    carPsgr=carPsgr+1;
				    other=other+1;
			  	}
			  	else if(mode.equals("carDrv")){
				    carDrv=carDrv+1;
				    other=other+1;
			  	}		  	
			
				numberOfStages=numberOfStages + 1;
			}
		    
			totalTrips=totalTrips+1;
			
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
			if(pt>0 && other>0)
				agent_other=agent_other+1;
			if(taxi>0)
				agent_taxi=agent_taxi+1;
			if(carPsgr>0)
				agent_carPsgr=agent_carPsgr +1;
			if(carDrv>0)
				agent_carDrv=agent_carDrv +1;
			if(compBus>0)
				agent_compBus=agent_compBus +1;
			if(walk>0)
				agent_walk = agent_walk +1;
			
			pt=0;
			bus=0;
			rail=0;
			other=0;
			taxi=0;
			walk=0;
			carPsgr=0;
			carDrv=0;
			compBus=0;
					
		    
//			double distanceFirstLast = Math.sqrt((firstLat - endLat)*(firstLat - endLat) + (firstLon - endLon)*(firstLon - endLon));
//			long distanceFirstLastRounded = Math.round(distanceFirstLast);
//			outFirstLast.write(Long.toString(distanceFirstLastRounded) +"\n");
//			System.out.println(Math.round(distanceFirstLast));
			agentTrips.close(); 
		}
//		System.out.println("Bus: "+bus);
//		System.out.println("MRT/LRT: "+mrtlrt);
//		System.out.println("CompBus: "+compBus);
//		System.out.println("Walk: "+walk);
//		System.out.println("PT: "+pt);
//		System.out.println("Other: "+other);
		
//		System.out.println("Taxi: "+taxi);
//		System.out.println("=============");
		
		System.out.println("Total stages: "+numberOfStages);
		System.out.println("PT-only journey chains (incl walk): "+agent_pt);
		System.out.println("Interrupted journey chains: "+agent_other);
		
		
		System.out.println("Chains interrupted by taxi: "+agent_taxi);
		System.out.println("Chains interrupted by carPsgr: "+agent_carPsgr);
		System.out.println("Chains interrupted by carDrv: "+agent_carDrv);
		System.out.println("Chains interrupted by compBus: "+agent_compBus);
		System.out.println("Chains interrupted by unknown: "+agent_unknown);
		
		System.out.println();
		System.out.println("Chains including by walk: "+agent_walk);
//		System.out.println("Trips with pt&others: "+ptothers_trips);
//		System.out.println("Trips with pt&walk&others: "+allmode_trips);
//		System.out.println("Trips without pt: "+nopt_trips);
		System.out.println("===============================");
		System.out.println("Total Trips: "+totalTrips);
		System.out.println("Number of persons: "+numberOfPersons);
//		System.out.println("First Trip is without PT: "+firstTripNoPt);
//		System.out.println("Last Trip is without PT: "+lastTripNoPt);
		System.out.println();
		System.out.println("Journeys incl. only bus pt-mode: "+bus_journey);
		System.out.println("Journeys incl. only rail pt-mode: "+rail_journey);
		System.out.println("Journeys incl. only bus&rail pt-mode: "+busrail_journey);
		System.out.println("Journeys without pt-mode: "+nonpt_journeys);
		
//		System.out.println(allDistances.size());
		outToFile.close();
//		outFirstLast.close();		
		
	}
	
	
	

}
