package playground.artemc.plansTools;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.artemc.utils.DataBaseAdmin;
import playground.artemc.utils.NoConnectionException;


public class PlansFromEzLink_JourneysToPersons_Routed extends MatsimXmlWriter{

	static String outputPlanFilePath; 


	public static void main(String[] args) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException, ParseException {
		DataBaseAdmin dba = new DataBaseAdmin(new File("./data/dataBases/Ezlink_1week_DataBase_local.properties"));

		outputPlanFilePath = "./data/ezLinkPlans/ezlinkPlans_25pctRouted_fixedTime.xml";
		
		String startTime="";
		Float rideTime=0f;
		String endTime="";
		String newTime="";
		String headway="";
		Integer arrivalOffset=0;
		Double startLat=0.0;
		Double startLon=0.0;
		Double endLat=0.0;
		Double endLon=0.0;
		String srvc_Number = "";
		String boardingStop = "";
		String alightingStop = "";
		String mode="";
		String endTimeLastLeg="";
		Double previousEndLat=0.0;
		Double previousEndLon=0.0;
		String previousStopLink ="";
		String journeyID="";
		Integer journeysCount=0;
		Boolean journeyContainsUnroutedStage = false;
		Integer routedStages = 0;
		Integer unroutedStages = 0;
		Integer unroutedJourneys = 0;
		Integer tripNumber =0;
		Integer unknownAlighting=0;
		Integer tripsOmitted=0;
		Integer journeysOmitted=0;

		Boolean newPerson;

		String boardingStopLink = "";
		String alightingStopLink = "";
		String rideDuration = "";

		ResultSet mrtOD = null;
		Map<Id<TransitRoute>, Integer>  routesMap = new HashMap<Id<TransitRoute>,Integer>();
		Map<Id<TransitLine>,TransitLine> lines;
		Set<Id<TransitRoute>> routes;

		ArrayList<String> emptyResultSetIDs = new ArrayList<String>();
		TransitStopFacility accessFacility; 
		TransitStopFacility egressFacility;
		TransitLine line;
		TransitRoute route;

		Random generator = new Random();

		ScheduleHeadwayFinder scheduleHeadwayFinder = new ScheduleHeadwayFinder();

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

		new NetworkReaderMatsimV1(scenario).parse("C:/Work/MATSim/inputMATSimSingapore2.2/network/network100.xml.gz");
		scenario.getConfig().transit().setUseTransit(true);
		new TransitScheduleReader(scenario).readFile("C:/Work/MATSim/inputMATSimSingapore2.2/transit/transitSchedule.xml.gz");		

		PlansFromEzLink_JourneysToPersons_Routed plansFileFromEzLink = new PlansFromEzLink_JourneysToPersons_Routed();
		plansFileFromEzLink.writeHeader();

		//MATSim object for coordinate transformation
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84", "WGS84_UTM48N"); 

		//Read cardIDs for one day
		//25% sample
		ResultSet agents = dba.executeQuery("SELECT newJourney_ID FROM v1_journeys12042011_25min ORDER BY RAND() LIMIT 996295");
		//ResultSet agents = dba.executeQuery("SELECT newJourney_ID FROM v1_journeys12042011_25min ORDER BY RAND() LIMIT 10");
		//ResultSet agents = dba.executeQuery("SELECT newJourney_ID FROM `v1_journeys12042011_25min` WHERE `newJourney_ID`='102460886630_1'"); 
		System.out.println("Reading of all Journey IDs done!");

		//Loop through obtained jounreyIDs
		int k=0,i=0;
		while(agents.next()){
			i=i+1;

			journeyID  = agents.getString(1);

			newPerson=true;
			if(i==1000){
				k=k+1000;
				System.out.println("Agents: "+k);
				i=0;
			}

			ResultSet trips;
			boolean newAgent = true;
			do{
				//Read all trips of one journey_ID
				//System.out.println("Load all trips for the journey...");
				trips = dba.executeQuery("SELECT CARD_ID,Ride_Start_Time, Ride_Time, start_lat,start_lon, end_lat,end_lon, Srvc_Number, BOARDING_STOP_STN, ALIGHTING_STOP_STN, TRAVEL_MODE FROM v1_trips12042011_25min WHERE newJourney_ID='"+journeyID+"' ORDER BY newTripID");
				//ResultSet rs = dba.executeQuery("SELECT CARD_ID,Ride_Start_Time, Ride_Time, start_lat,start_lon, end_lat,end_lon FROM v2_trips12042011 WHERE CARD_ID="+Long.toString(CARD_ID)+" ORDER BY Ride_Start_Time");
				//System.out.println("Trips loaded!");
				journeyContainsUnroutedStage = false;
				if(trips.next()){
					if(trips.getString(10).equals("") && !trips.next()){
						newAgent=false;
						trips.close();
						journeysOmitted++;
						i=i+1;
						agents.next();
						if(!agents.isAfterLast()){
							journeyID  = agents.getString(1);
						}
						else{
							break;
						}
					}
					else{
						newAgent=true;
					}
				}else{
					emptyResultSetIDs.add(journeyID);
					newAgent=false;
					trips.close();
					journeysOmitted++;
					i=i+1;
					agents.next();
					if(!agents.isAfterLast()){
						journeyID  = agents.getString(1);
					}
					else{
						break;
					}
				}

			}while(!newAgent);

			if(agents.isAfterLast()){
				break;
			}
			
			trips.beforeFirst();
			plansFileFromEzLink.writeNewPerson(journeyID);

			tripNumber= 0;
			while(trips.next()){
				tripNumber++;

				//				if(!journeyID.equals(rs.getString("JOURNEY_ID"))){
				//					if(journeysCount!=0){
				//						plansFileFromEzLink.writeClosePerson(endTime, Double.toString(endLat),Double.toString(endLon),alightingStopLink);
				//						newPerson=true;
				//						plansFileFromEzLink.writeNewPerson(journeyID+"_"+journeysCount);
				//					}
				//					journeysCount++;
				//				}								
				//				plansFileFromEzLink.writeNewPerson(journeyID+"_"+journeysCount);

				startTime = trips.getString(2);
				rideTime = trips.getFloat(3);
				startLat = trips.getDouble(4);
				startLon = trips.getDouble(5);
				endLat = trips.getDouble(6);
				endLon = trips.getDouble(7);
				if(trips.getString(8)!=null)
					srvc_Number = trips.getString(8).trim();
				boardingStop = trips.getString(9).trim();
				alightingStop = trips.getString(10).trim();
				mode = trips.getString(11).trim();

				//Check tap out is available and if not, use next boarding station as alighting station
				if(alightingStop.equals("")){
					unknownAlighting++;
					if(trips.next()){
						endLat = trips.getDouble(4);
						endLon = trips.getDouble(5);
						alightingStop = trips.getString(9).trim();
						trips.previous();
					}
				}

				//In case no tap out available and there is no next trip, omit the trip. Otherwise, get data and add to plan. 
				if(alightingStop.equals("") && !trips.next()){
					tripsOmitted++;
				}
				else{
					rideDuration = plansFileFromEzLink.transformMinutesToTime(rideTime);
					endTime = plansFileFromEzLink.calculateEndTime(startTime,rideDuration);

					//If endtime after midnight, add 24 hours 
					if(endTime.substring(0,2).equals("00") && !startTime.substring(0,2).equals("00") ){
						newTime="24"+endTime.substring(2,8);
						endTime=newTime;
					}

					//Transform coordinates to UTM48N
					Coord coordStart = new Coord(startLon, startLat);
					Coord coordEnd = new Coord(endLon, endLat);
					Coord UTMStart = ct.transform(coordStart);
					Coord UTMEnd = ct.transform(coordEnd);
					startLon=UTMStart.getX();
					startLat=UTMStart.getY();
					endLon=UTMEnd.getX();
					endLat=UTMEnd.getY();	


					if(mode.equals("RTS")){
						double timeOfDay = 39600.00;
						do{
							mrtOD = dba.executeQuery("SELECT originMATSim, destinationMATSim, line1_MATSim FROM MRT_OD_extended WHERE origin='"+boardingStop+"' AND destination='"+alightingStop+"' AND timeOfDay="+timeOfDay);	

							timeOfDay=timeOfDay+7200.00;
							if(timeOfDay>104400) break;
						}while(!mrtOD.next());

						//System.out.println("SELECT originMATSim, destinationMATSim, line1_MATSim FROM MRT_OD_extended WHERE origin='"+boardingStop+"' AND destination='"+alightingStop+"' AND `number of transfers`=0 AND timeOfDay="+(timeOfDay-7200.00));
						//mrtOD.next();
						boardingStop = mrtOD.getString(1);
						alightingStop = mrtOD.getString(2);
						srvc_Number = mrtOD.getString(3);
						mrtOD.close();
						//					if(mrtOD.next()){
						//						System.out.println("Double Entry in MRT-OD Table, shouldn't happen! Please check table consistency!");
						//						System.out.println("SELECT originMATSim, destinationMATSim, line1_MATSim FROM MRT_OD_extended_9am WHERE origin='"+boardingStop+"' AND destination='"+alightingStop+"'");
						//					}


					}

					//	List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();

					//Check for TransitLine
					line = null;
					route = null;
					accessFacility = null;
					egressFacility = null;

					lines = ((ScenarioImpl) scenario).getTransitSchedule().getTransitLines();
					//System.out.println(journeyID+","+srvc_Number+","+boardingStop+","+alightingStop);
					for(TransitLine currentLine:lines.values()){
						if(currentLine.getId().toString().equals(srvc_Number)){
							line = currentLine;		
							break;
						}
					}


					if(line!=null){
						//Check for stops
						routes = ((ScenarioImpl) scenario).getTransitSchedule().getTransitLines().get(line.getId()).getRoutes().keySet();
						routesMap.clear();
						for(Id<TransitRoute> routeID:routes){	
							routesMap.put(routeID, ((ScenarioImpl) scenario).getTransitSchedule().getTransitLines().get(line.getId()).getRoutes().get(routeID).getDepartures().size());
							//System.out.println("RouteID: "+routeID.toString()+"  Dep.:"+((ScenarioImpl) scenario).getTransitSchedule().getTransitLines().get(line.getId()).getRoutes().get(routeID).getDepartures().size());
						}

						//System.out.println("Looking for a route containing boarding and alighting stops...");
						//Check if route contains boarding and alighting stops
						for(Entry<Id<TransitRoute>, Integer> routeKey:entriesSortedByValues(routesMap)){
							Id<TransitRoute> routeID = routeKey.getKey();
							//System.out.println("   RouteID: "+routeID.toString()+"  Dep.:"+((ScenarioImpl) scenario).getTransitSchedule().getTransitLines().get(line.getId()).getRoutes().get(routeID).getDepartures().size());
							List<TransitRouteStop> currentRouteStops = ((ScenarioImpl) scenario).getTransitSchedule().getTransitLines().get(line.getId()).getRoutes().get(routeID).getStops();						
							for(TransitRouteStop currentStop:currentRouteStops){	
								//System.out.println(currentStop.getStopFacility().getId().toString()+"|"+BoardingStop+","+AlightingStop);
								if(currentStop.getStopFacility().getId().toString().equals(boardingStop)){
									accessFacility=currentStop.getStopFacility();
								}	
								if(currentStop.getStopFacility().getId().toString().equals(alightingStop) && accessFacility!=null ){
									egressFacility=currentStop.getStopFacility();

								}			
							}
							if(accessFacility!=null && egressFacility!=null){
								route = ((ScenarioImpl) scenario).getTransitSchedule().getTransitLines().get(line.getId()).getRoutes().get(routeID);
								//System.out.println("Route found!");
								break;			
							}
							else{
								accessFacility=null; 
								egressFacility=null;
							}

						}

						//Error output if boarding or alighting station are not found
						if(accessFacility==null){
							//System.out.println("AccessFacility NOT FOUND for Boarding Stop: "+boardingStop+", Line:"+srvc_Number);
						}
						if(egressFacility==null){
							//System.out.println("EgressFacility NOT FOUND for Boarding Stop: "+alightingStop+", Line:"+srvc_Number);
						}
						if(route==null){
							//System.out.println("Route containing both stops was not found!");
						}

					}else{
						//System.out.println("Line NOT FOUND:"+srvc_Number);
					}

					ExperimentalTransitRoute routeString;
					if(accessFacility==null || line==null || route == null || egressFacility == null)
					{
						routeString = null;
						boardingStopLink = "";
						alightingStopLink = "";
						unroutedStages++;
						journeyContainsUnroutedStage=true;

						arrivalOffset = generator.nextInt(1500)+1; 
						
						/*Subtract randomised offset from the start time in order to model the arrival time at the stop/station*/
						//startTime = timeTools.secondsTotimeString(timeTools.timeStringToSeconds(startTime) - (double) arrivalOffset);

					}else
					{
						routeString = new ExperimentalTransitRoute(accessFacility, line, route, egressFacility);
						boardingStopLink = accessFacility.getLinkId().toString();
						alightingStopLink = egressFacility.getLinkId().toString();
						startLon = accessFacility.getCoord().getX();
						startLat = accessFacility.getCoord().getY();
						endLon =  egressFacility.getCoord().getX();
						endLat =  egressFacility.getCoord().getY();

						routedStages++;		

						//Generate the arrival time at the stop
						headway = scheduleHeadwayFinder.findHeadway(startTime, line, route, accessFacility, ((ScenarioImpl) scenario).getTransitSchedule());
						//If headway couldn't be determined, use fixed interval
						if(TimeTools.timeStringToSeconds(headway).intValue()>0){
							arrivalOffset = generator.nextInt((int) Math.round(TimeTools.timeStringToSeconds(headway)*2.5))+1; 
							if(arrivalOffset>1500)
								arrivalOffset = 1500;
						}
						else{
							arrivalOffset = generator.nextInt(1500)+1; 	
						}

						//System.out.println(startTime+","+timeTools.secondsTotimeString((double) arrivalOffset)+","+headway);
						
						/*Subtract randomised offset from the start time in order to model the arrival time at the stop/station*/
						//startTime = timeTools.secondsTotimeString(timeTools.timeStringToSeconds(startTime) - (double) arrivalOffset);

					}

					//Write the trip
					plansFileFromEzLink.writeTrip(journeyID,startTime,rideDuration,endTimeLastLeg,Double.toString(startLat),Double.toString(startLon),Double.toString(endLat),Double.toString(endLon),newPerson,Double.toString(previousEndLat),Double.toString(previousEndLon), routeString, boardingStopLink, previousStopLink);
					newPerson=false;
					endTimeLastLeg=endTime;
					previousEndLat=endLat;
					previousEndLon=endLon;	
					previousStopLink = alightingStopLink;
				}

			}
			plansFileFromEzLink.writeClosePerson(endTime, Double.toString(endLat),Double.toString(endLon),alightingStopLink);
			journeysCount++;
			if(journeyContainsUnroutedStage){
				unroutedJourneys++;
			}
			trips.close();
		}
		dba.close();
		plansFileFromEzLink.writeEnd();
		System.out.println("Done!");
		System.out.println("Persons/Journeys: " + plansFileFromEzLink.getPersons()+" (="+journeysCount+")");
		System.out.println("Unrouted journeys: " + unroutedJourneys);	  
		System.out.println();
		System.out.println("Routed stages: " + routedStages);	  
		System.out.println("Unrouted stages: " + unroutedStages);
		System.out.println();	
		System.out.println("Unknwon alighting: " + unknownAlighting);	  
		System.out.println("Trips omitted: " + tripsOmitted);	
		System.out.println("Journeys omitted: " + journeysOmitted);	
		System.out.println("--------------------------------");	 
		System.out.println("Total Stages: " + plansFileFromEzLink.getTrips());
		System.out.println();
		System.out.println("JourneyIDs which returned empty result set:");
		Iterator<String> iterator = emptyResultSetIDs.iterator();
		while(iterator.hasNext()){
			System.out.print(iterator.next()+" ");
		}
	}

	private int persons=0;
	private int trips=0;
	private SimpleDateFormat sdf;

	public int getPersons() {
		return persons;
	}

	public int getTrips() {
		return trips;
	}


	public void writeNewPerson(String PlanID) throws IOException{
		//	this.appendFile("./data/ezLinkDataSimulation/plans1000.xml");

		List<Tuple<String, String>> listEmpty = new ArrayList<Tuple<String,String>>();
		List<Tuple<String, String>> listPerson = new ArrayList<Tuple<String,String>>();
		listPerson.add(new Tuple<String, String>("id", PlanID));
		this.writer.write(NL);
		this.writeStartTag("person", listPerson);
		this.writeStartTag("plan", listEmpty);
		persons++;
	}

	public void writeClosePerson(String EndTime, String EndLat,String EndLon, String prevStopLink) throws IOException  {

		List<Tuple<String, String>> listAct = new ArrayList<Tuple<String,String>>();
		List<Tuple<String, String>> listLegWalk = new ArrayList<Tuple<String,String>>();
		List<Tuple<String, String>> listPTInteraction = new ArrayList<Tuple<String,String>>();


		//		String FinalTime = "23:59:59";
		//		if(EndTime.substring(0,2).equals("24")){
		//			FinalTime=EndTime;
		//		}


		listPTInteraction.add(new Tuple<String, String>("type", "pt interaction"));
		if(prevStopLink!="")
			listPTInteraction.add(new Tuple<String, String>("link", prevStopLink));
		listPTInteraction.add(new Tuple<String, String>("x", EndLon));
		listPTInteraction.add(new Tuple<String, String>("y", EndLat));
		listPTInteraction.add(new Tuple<String, String>("dur", "00:00:00"));
		this.writeStartTag("act", listPTInteraction, true);	

		listLegWalk.add(new Tuple<String, String>("mode", "transit_walk"));
		listLegWalk.add(new Tuple<String, String>("trav_time", "00:00:00"));
		this.writeStartTag("leg", listLegWalk, false);	
		this.writeStartTag("route", null);
		this.writeEndTag("route");
		this.writeEndTag("leg");

		listAct.add(new Tuple<String, String>("type", "dummy"));
		if(prevStopLink!="")
			listAct.add(new Tuple<String, String>("link", prevStopLink));	
		listAct.add(new Tuple<String, String>("x", EndLon));
		listAct.add(new Tuple<String, String>("y", EndLat));
		listAct.add(new Tuple<String, String>("start_time", EndTime));

		this.writeStartTag("act", listAct, true);
		this.writeEndTag("plan");
		this.writeEndTag("person");
		//	this.close();
	}

	public void writeTrip(String card_id,String startTime,String duration,String endTimeLastLeg,String startLat,String startLon, String endLat,String endLon,Boolean newPerson, String previousEndLat, String previousEndLon, ExperimentalTransitRoute route, String stopLink, String prevStopLink) throws IOException, ParseException {

		//		sdf = new SimpleDateFormat("HH:mm:ss");
		//		Date time = sdf.parse(startTime);
		//		Date twentyMin = sdf.parse("00:20:00");
		//		startTime = sdf.format(time.getTime() - twentyMin.getTime() - 27000000);	

		List<Tuple<String, String>> listActStart = new ArrayList<Tuple<String,String>>();	
		List<Tuple<String, String>> listActEnd = new ArrayList<Tuple<String,String>>();	
		List<Tuple<String, String>> listLeg = new ArrayList<Tuple<String,String>>();
		List<Tuple<String, String>> listLegWalk = new ArrayList<Tuple<String,String>>();
		List<Tuple<String, String>> listPTInteraction = new ArrayList<Tuple<String,String>>();

		if (newPerson==true){
			listActStart.add(new Tuple<String, String>("type", "dummy"));
			if(stopLink!="")
				listActStart.add(new Tuple<String, String>("link", stopLink));
			listActStart.add(new Tuple<String, String>("x", startLon));
			listActStart.add(new Tuple<String, String>("y", startLat));		
			listActStart.add(new Tuple<String, String>("end_time", startTime));
			this.writeStartTag("act", listActStart, true);				

			listLegWalk.add(new Tuple<String, String>("mode", "transit_walk"));
			listLegWalk.add(new Tuple<String, String>("trav_time", "00:00:00"));
			this.writeStartTag("leg", listLegWalk, false);	
			this.writeStartTag("route", null);
			this.writeEndTag("route");
			this.writeEndTag("leg");

			listPTInteraction.add(new Tuple<String, String>("type", "pt interaction"));
			if(stopLink!="")
				listPTInteraction.add(new Tuple<String, String>("link", stopLink));
			listPTInteraction.add(new Tuple<String, String>("x", startLon));
			listPTInteraction.add(new Tuple<String, String>("y", startLat));
			listPTInteraction.add(new Tuple<String, String>("dur", "00:00:00"));
			this.writeStartTag("act", listPTInteraction, true);						
		}
		else{		
			listPTInteraction.add(new Tuple<String, String>("type", "pt interaction"));
			if(prevStopLink!="")
				listPTInteraction.add(new Tuple<String, String>("link", prevStopLink));
			listPTInteraction.add(new Tuple<String, String>("x", previousEndLon));
			listPTInteraction.add(new Tuple<String, String>("y", previousEndLat));
			listPTInteraction.add(new Tuple<String, String>("dur", "00:00:00"));
			this.writeStartTag("act", listPTInteraction, true);	

			listLegWalk.add(new Tuple<String, String>("mode", "transit_walk"));
			listLegWalk.add(new Tuple<String, String>("trav_time", "00:00:00"));
			this.writeStartTag("leg", listLegWalk, false);	
			this.writeStartTag("route", null);
			this.writeEndTag("route");
			this.writeEndTag("leg");

			listPTInteraction.clear();

			listPTInteraction.add(new Tuple<String, String>("type", "pt interaction"));
			if(stopLink!="")
				listPTInteraction.add(new Tuple<String, String>("link", stopLink));
			listPTInteraction.add(new Tuple<String, String>("x", startLon));
			listPTInteraction.add(new Tuple<String, String>("y", startLat));
			listPTInteraction.add(new Tuple<String, String>("dur", "00:00:00"));
			this.writeStartTag("act", listPTInteraction, true);	
		}	

		listLeg.add(new Tuple<String, String>("mode", "pt"));
		listLeg.add(new Tuple<String, String>("trav_time", duration));
		this.writeStartTag("leg", listLeg, false);	
		if(route!=null){
			listLeg.clear();
			//listLeg.add(new Tuple<String, String>("type", "experimentalPt1"));
			this.writeStartTag("route", null);
			this.writeContent(route.getRouteDescription(),true);
			this.writeEndTag("route");
		}
		this.writeEndTag("leg");
		trips++;
	}		

	public void writeHeader() throws IOException {
		this.useCompression(true);
		List<Tuple<String, String>> listEmpty = new ArrayList<Tuple<String,String>>();
		this.openFile(outputPlanFilePath);
		this.writeXmlHead();
		this.writeDoctype("plans", "http://www.matsim.org/files/dtd/plans_v4.dtd");
		this.writeStartTag("plans", listEmpty);
		//	this.close();		
	}	

	public void writeEnd() throws IOException{
		//	 	this.appendFile("./data/ezLinkDataSimulation/plansComplete1.xml");
		this.writeEndTag("plans");
		this.close();
	}

	public String transformMinutesToTime(float minutes) throws ParseException{

		Integer durationInSeconds = Math.round(minutes * 60);
		Integer durationHours = durationInSeconds / 3600;
		Integer remainder = durationInSeconds % 3600;
		Integer durationMinutes = remainder / 60;
		Integer durationSeconds = remainder % 60;
		String durationFull = durationHours+":"+durationMinutes+":"+durationSeconds;

		sdf = new SimpleDateFormat("HH:mm:ss");
		Date durationFull_df = sdf.parse(durationFull);
		durationFull = sdf.format(durationFull_df);

		return durationFull; 
	}

	public String calculateEndTime (String startTime, String duration) throws ParseException{

		Integer startHours = Integer.parseInt(startTime.substring(0,2));
		Integer startMinutes = Integer.parseInt(startTime.substring(3,5));
		Integer startSeconds = Integer.parseInt(startTime.substring(6,8));
		Integer startInSeconds = startHours*3600+startMinutes*60+startSeconds;

		Integer durationHours = Integer.parseInt(duration.substring(0,2));
		Integer durationMinutes = Integer.parseInt(duration.substring(3,5));
		Integer durationSeconds = Integer.parseInt(duration.substring(6,8));
		Integer durationInSeconds = durationHours*3600+durationMinutes*60+durationSeconds;

		Integer endTimeInSeconds = startInSeconds+durationInSeconds;
		Integer endTimeHours = endTimeInSeconds / 3600;
		Integer remainder = endTimeInSeconds % 3600;
		Integer endTimeMinutes = remainder / 60;
		Integer endTimeSeconds = remainder % 60;
		String endTime = endTimeHours+":"+endTimeMinutes+":"+endTimeSeconds;

		sdf = new SimpleDateFormat("HH:mm:ss");
		Date endTime_df= sdf.parse(endTime);
		endTime=sdf.format(endTime_df);

		return endTime;
	}

	static <K,V extends Comparable<? super V>> List<Entry<K, V>> entriesSortedByValues(Map<K,V> map) {

		List<Entry<K,V>> sortedEntries = new ArrayList<Entry<K,V>>(map.entrySet());

		Collections.sort(sortedEntries, 
				new Comparator<Entry<K,V>>() {

			@Override
			public int compare(Entry<K,V> e1, Entry<K,V> e2) {
				return e2.getValue().compareTo(e1.getValue());
			}
		}
				);

		return sortedEntries;
	}

}
