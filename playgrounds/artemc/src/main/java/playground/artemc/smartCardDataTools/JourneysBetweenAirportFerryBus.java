package playground.artemc.smartCardDataTools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import playground.artemc.utils.DataBaseAdmin;
import playground.artemc.utils.NoConnectionException;


public class JourneysBetweenAirportFerryBus {

	/**
	 * @param args
	 * @throws NoConnectionException 
	 * @throws java.sql.SQLException
	 * @throws java.io.IOException
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	public static void main(String[] args) throws SQLException, NoConnectionException, IOException, InstantiationException, IllegalAccessException, ClassNotFoundException {

		//String[] dayTables={"v1_trips11042011","v1_trips12042011","v1_trips13042011","v1_trips14042011","v1_trips15042011","trips16042011","trips17042011"};
		String[] dayTables={"trips16042011","trips17042011"};
		//String[] dayTables={ "v1_trips12042011"};


		ArrayList<String> airportStations = new ArrayList<String>();
		ArrayList<String> harbourFrontStations = new ArrayList<String>();
		ArrayList<String> ferryTerminalStations = new ArrayList<String>();
		ArrayList<String> queenStreetStations = new ArrayList<String>();

		airportStations.add("95109");
		airportStations.add("95029");
		airportStations.add("95129");
		airportStations.add("STN Changi Airport");

		harbourFrontStations.add("14119");
		harbourFrontStations.add("14141");
		harbourFrontStations.add("14129");
		harbourFrontStations.add("14461");
		harbourFrontStations.add("14009");
		harbourFrontStations.add("STN Harbour Front");

		ferryTerminalStations.add("96219");

		queenStreetStations.add("01109");
		queenStreetStations.add("07589");
		queenStreetStations.add("01121");
		queenStreetStations.add("01129");
		queenStreetStations.add("07589");
		queenStreetStations.add("07529");

		String journeyID = "";
		String boarding_stop_stn = ""; 
		String alighting_stop_stn = ""; 
		String mode ="";

		Integer changiHarbourFrontRTS = 0;
		Integer changiHarbourFrontBus = 0;
		Integer changiHarbourFrontMixed = 0;

		Integer harbourFrontChangiRTS = 0;
		Integer harbourFrontChangiBus = 0;
		Integer harbourFrontChangiMixed = 0;

		Integer changiFerryRTS = 0;
		Integer changiFerryBus = 0;
		Integer changiFerryMixed = 0;

		Integer ferryChangiRTS = 0;
		Integer ferryChangiBus = 0;
		Integer ferryChangiMixed = 0;

		Integer changiQueenStRTS = 0;
		Integer changiQueenStBus = 0;
		Integer changiQueenStMixed = 0;

		Integer queenStChangiRTS = 0;
		Integer queenStChangiBus = 0;
		Integer queenStChangiMixed = 0;

		int journeyCount =0;
		int count = 0;

		DataBaseAdmin dba = new DataBaseAdmin(new File("Ezlink_1week_DataBase_local.properties"));
		//DataBaseAdmin dba = new DataBaseAdmin(new File("./data/dataBases/Ezlink_1week_DataBase_local.properties"));
		ResultSet trips;
		ResultSet journeys;
		for(String dayTable : dayTables){

			System.out.println("Day: "+dayTable);
			
			changiHarbourFrontRTS = 0;
			changiHarbourFrontBus = 0;
			changiHarbourFrontMixed = 0;

			harbourFrontChangiRTS = 0;
			harbourFrontChangiBus = 0;
			harbourFrontChangiMixed = 0;

			changiFerryRTS = 0;
			changiFerryBus = 0;
			changiFerryMixed = 0;

			ferryChangiRTS = 0;
			ferryChangiBus = 0;
			ferryChangiMixed = 0;

			changiQueenStRTS = 0;
			changiQueenStBus = 0;
			changiQueenStMixed = 0;

			queenStChangiRTS = 0;
			queenStChangiBus = 0;
			queenStChangiMixed = 0;
			
			journeyCount = 0;
			count = 0;


			BufferedWriter singaporeTransfers= new BufferedWriter( new FileWriter("singaporeTransfers_"+dayTable+".csv"));
			journeys = dba.executeQuery("SELECT DISTINCT JOURNEY_ID FROM "+dayTable);
			//ResultSet journeys = dba.executeQuery("SELECT JOURNEY_ID FROM v1_trips12042011");
			while(journeys.next()){
				journeyCount++;
				if(journeyCount==100000){
					journeyCount = 0;
					count = count + 100000;
					System.out.println(count);
				}

				mode = "";
				journeyID = journeys.getString(1);
				trips = dba.executeQuery("SELECT JOURNEY_ID,CARD_ID,PassengerType,TRAVEL_MODE,BOARDING_STOP_STN,ALIGHTING_STOP_STN,Ride_Start_Date,Ride_Start_Time FROM "+dayTable+" WHERE JOURNEY_ID="+journeyID+" ORDER BY Ride_Start_Date, Ride_Start_Time");
				while(trips.next()){
					if(mode.equals("")){
						mode = trips.getString("TRAVEL_MODE");
					}
					else{
						if(!mode.equals(trips.getString("TRAVEL_MODE"))){
							mode = "RTS/Bus";
						}
					}
				}

				if(trips.first())
					boarding_stop_stn = trips.getString("BOARDING_STOP_STN");
				if(trips.last())
					alighting_stop_stn = trips.getString("ALIGHTING_STOP_STN");

				//	System.out.println(journeyID+","+boarding_stop_stn+","+alighting_stop_stn+","+mode);


				/*Changi <--> Harbour Front*/
				if(airportStations.contains(boarding_stop_stn) && harbourFrontStations.contains(alighting_stop_stn)){
					if(mode.equals("RTS"))
						changiHarbourFrontRTS++;
					if(mode.equals("Bus"))
						changiHarbourFrontBus++;
					if(mode.equals("RTS/Bus"))
						changiHarbourFrontMixed++;
				}

				/*Harbour Front <--> Changi*/
				if(harbourFrontStations.contains(boarding_stop_stn) && airportStations.contains(alighting_stop_stn)){
					if(mode.equals("RTS"))
						harbourFrontChangiRTS++;
					if(mode.equals("Bus"))
						harbourFrontChangiBus++;
					if(mode.equals("RTS/Bus"))
						harbourFrontChangiMixed++;
				}

				/*Changi <--> Tanah Merah*/
				if(airportStations.contains(boarding_stop_stn) && ferryTerminalStations.contains(alighting_stop_stn)){
					if(mode.equals("RTS"))
						changiFerryRTS++;
					if(mode.equals("Bus"))
						changiFerryBus++;
					if(mode.equals("RTS/Bus"))
						changiFerryMixed++;
				}

				/*Tanah Merah <--> Changi*/
				if(ferryTerminalStations.contains(boarding_stop_stn) && airportStations.contains(alighting_stop_stn)){
					if(mode.equals("RTS"))
						ferryChangiRTS++;
					if(mode.equals("Bus"))
						ferryChangiBus++;
					if(mode.equals("RTS/Bus"))
						ferryChangiMixed++;
				}

				/*Changi <--> Queen St*/
				if(airportStations.contains(boarding_stop_stn) &&  queenStreetStations.contains(alighting_stop_stn)){
					if(mode.equals("RTS"))
						changiQueenStRTS++;
					if(mode.equals("Bus"))
						changiQueenStBus++;
					if(mode.equals("RTS/Bus"))
						changiQueenStMixed++;
				}

				/*Queen St <--> Changi*/
				if(queenStreetStations.contains(boarding_stop_stn) && airportStations.contains(alighting_stop_stn)){
					if(mode.equals("RTS"))
						queenStChangiRTS++;
					if(mode.equals("Bus"))
						queenStChangiBus++;
					if(mode.equals("RTS/Bus"))
						queenStChangiMixed++;
				}


				trips.close();

			}			

			singaporeTransfers.write("Changi --> Harbour Front"+"\n");
			singaporeTransfers.write("RTS,"+changiHarbourFrontRTS+"\n");
			singaporeTransfers.write("Bus,"+changiHarbourFrontBus+"\n");
			singaporeTransfers.write("Mixed,"+changiHarbourFrontMixed+"\n"+"\n");

			singaporeTransfers.write("Harbour Front --> Changi"+"\n");
			singaporeTransfers.write("RTS,"+harbourFrontChangiRTS+"\n");
			singaporeTransfers.write("Bus,"+harbourFrontChangiBus+"\n");
			singaporeTransfers.write("Mixed,"+harbourFrontChangiMixed+"\n"+"\n");

			singaporeTransfers.write("Changi --> Tanah Merah"+"\n");
			singaporeTransfers.write("RTS,"+changiFerryRTS+"\n");
			singaporeTransfers.write("Bus,"+changiFerryBus+"\n");
			singaporeTransfers.write("Mixed,"+changiFerryMixed+"\n"+"\n");

			singaporeTransfers.write("Tanah Merah --> Changi"+"\n");
			singaporeTransfers.write("RTS,"+ferryChangiRTS+"\n");
			singaporeTransfers.write("Bus,"+ferryChangiBus+"\n");
			singaporeTransfers.write("Mixed,"+ferryChangiMixed+"\n"+"\n");

			singaporeTransfers.write("Changi --> Queen St"+"\n");
			singaporeTransfers.write("RTS,"+changiQueenStRTS+"\n");
			singaporeTransfers.write("Bus,"+changiQueenStBus+"\n");
			singaporeTransfers.write("Mixed,"+changiQueenStMixed+"\n"+"\n");

			singaporeTransfers.write("Queen St --> Changi"+"\n");
			singaporeTransfers.write("RTS,"+queenStChangiRTS+"\n");
			singaporeTransfers.write("Bus,"+queenStChangiBus+"\n");
			singaporeTransfers.write("Mixed,"+queenStChangiMixed+"\n"+"\n");

			singaporeTransfers.close();
			journeys.close();
		}

		System.out.println("Changi <--> Harbour Front");
		System.out.println(changiHarbourFrontRTS);
		System.out.println(changiHarbourFrontBus);
		System.out.println(changiHarbourFrontMixed);
		System.out.println();
		System.out.println(harbourFrontChangiRTS);
		System.out.println(harbourFrontChangiBus);
		System.out.println(harbourFrontChangiMixed);
		System.out.println();
		System.out.println();


		System.out.println("Changi <--> Tanah Merah");
		System.out.println(changiFerryRTS);
		System.out.println(changiFerryBus);
		System.out.println(changiFerryMixed);
		System.out.println();
		System.out.println(ferryChangiRTS);
		System.out.println(ferryChangiBus);
		System.out.println(ferryChangiMixed);
		System.out.println();
		System.out.println();

		System.out.println("Changi <--> Queen St");
		System.out.println(changiQueenStRTS);
		System.out.println(changiQueenStBus);
		System.out.println(changiQueenStMixed);
		System.out.println();
		System.out.println(queenStChangiRTS);
		System.out.println(queenStChangiBus);
		System.out.println(queenStChangiMixed);
		System.out.println();
		System.out.println();

	}

}
