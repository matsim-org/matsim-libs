package playground.artemc.scenarioTools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.NetworkReaderMatsimV1;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.io.StreamingPopulationWriter;
import org.matsim.core.population.io.StreamingUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.FacilitiesReaderMatsimV1;

import playground.artemc.utils.SortEntriesByValueDesc;

public class Assignment {

//	private static Double avgHouseholdSize = 2.0;
	private static Double scenarioScaleFactor = 1.0;
	private static Integer populationSize;
	private static Double noCarPercentage = 0.1;
	
	public static void main(String[] args) throws IOException {

		String networkPath = args[0];
		String facilitiesPath = args[1];
		String populationPath = args[2];
	//	String incomeFilePath = args[3];
		
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new FacilitiesReaderMatsimV1(scenario).readFile(facilitiesPath);
		
		Population population = (Population) scenario.getPopulation();
		PopulationFactory pf = population.getFactory();
		StreamingUtils.setIsStreaming(population, true);
		StreamingPopulationWriter popWriter = new StreamingPopulationWriter(population, scenario.getNetwork());
		popWriter.startStreaming(populationPath);
		
		new NetworkReaderMatsimV1(scenario.getNetwork()).readFile(networkPath);
		Network network = (Network) scenario.getNetwork();
		
		NodeDistances nodeDistances = new NodeDistances(networkPath);
		
		Map<Id<ActivityFacility>, ? extends ActivityFacility> facilities = scenario.getActivityFacilities().getFacilities();
		HashMap<Integer,Double> bedsInZone = new HashMap<Integer, Double>();
		HashMap<Integer,Integer> workplacesInZone = new HashMap<Integer, Integer>();
		HashMap<Integer,ArrayList<Double>> workPlaces = new HashMap<Integer, ArrayList<Double>>();
		HashMap<Id, Double> remainingHomeCapacity = new HashMap<Id, Double>();
		HashMap<Integer, ArrayList<Id>> zoneHomeFacilities = new HashMap<Integer, ArrayList<Id>>();	
		HashMap<Integer, ArrayList<Id>> zoneWorkFacilities = new HashMap<Integer, ArrayList<Id>>();	
		HashMap<Integer, Double> occupancy = new HashMap<Integer, Double>();
		
		Random generator = new Random();	
		
		//ArrayList<String[]> facilityIncome = CSVReader.readCSV(incomeFilePath);	
		//HashMap<Id, Integer> facilityIncomeMap = new HashMap<Id, Integer>(); 

//		for(String[] entry:facilityIncome){
//			facilityIncomeMap.put(Id.create(entry[0]), Integer.valueOf(entry[1]));
//		}
		
		for(Integer i=1;i<25;i++){
			System.out.println("Creating bedcount for zone: "+i);
			bedsInZone.put(i, 0.0);
			workplacesInZone.put(i, 0);
			occupancy.put(i, 0.0);
			zoneHomeFacilities.put(i, new ArrayList<Id>());
			zoneWorkFacilities.put(i, new ArrayList<Id>());
		}

		/*New OD-Matrix*/
		Integer[][] newODmatrix = new Integer[24][24];;
		for(Integer row=0;row<24;row++){
			for(Integer column=0;column<24;column++){
				newODmatrix[row][column] =0;
			}
		}
		
		
		/* Importing ODMatrix*/
		Integer[][] ODmatrix = new Integer[24][24];;
		try {
			ODmatrix = readODMatrix("C:/Work/Roadpricing Scenarios/SiouxFalls/OD-Table_export.csv");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Integer totalTripsOD = 0;
		Integer row = 0;
		Integer[] originSum = new Integer[24];
		
		for(Integer[] zoneTrips:ODmatrix){
			row++;
			originSum[row-1]=0;
			for(Integer trips:zoneTrips){
				originSum[row-1] = originSum[row-1]+trips;
				totalTripsOD = totalTripsOD + trips;
			}			
		}
		
		/*Adding all facilities*/
		for(Id facilityId:facilities.keySet()){
			Integer zone = Integer.valueOf(facilityId.toString().split("_")[1]);	
			if(facilities.get(facilityId).getActivityOptions().containsKey("home"))
			{
				Double capacity = facilities.get(facilityId).getActivityOptions().get("home").getCapacity();
				Double newCapacity = bedsInZone.get(zone)+capacity;
				
				if(newCapacity>(originSum[zone-1]*0.98))
					newCapacity = originSum[zone-1]*0.98;
				
				bedsInZone.put(zone,newCapacity);		
				remainingHomeCapacity.put(facilityId, capacity*1.5);
				zoneHomeFacilities.get(zone).add(facilityId);	
			}
			
			if(facilities.get(facilityId).getActivityOptions().containsKey("work"))
			{
				zoneWorkFacilities.get(zone).add(facilityId);	
			}
		}	
	
		
		/*Scale scenario*/
		Double totalBeds = 0.0;
		for(Integer zone:bedsInZone.keySet()){
			bedsInZone.put(zone,bedsInZone.get(zone)*scenarioScaleFactor);

			//System.out.println("Zone: "+zone+" Beds: "+bedsInZone.get(zone));
			totalBeds = totalBeds + bedsInZone.get(zone).intValue();
		}
		
		populationSize = totalBeds.intValue();
		//populationSize = 20;
		System.out.println("Population size: "+populationSize);
		
		
		/*Generate scaled number of work places for each zone*/
		Double scaleFactor = (totalBeds / ((double) totalTripsOD-totalBeds)); 
		System.out.println("Total beds: "+totalBeds+" Total OD Trips :"+totalTripsOD+" Scalefactor: "+scaleFactor);
		row = 1;
		Integer totalWorkplaces = 0;
		for(Integer originTrips:originSum){
			workPlaces.put(row,new ArrayList<Double>());
			Integer zoneWorkPlaces = (int) Math.ceil((originTrips-bedsInZone.get(row))*scaleFactor);
			workplacesInZone.put(row, zoneWorkPlaces);
			totalWorkplaces = totalWorkplaces + zoneWorkPlaces;
			if(zoneWorkPlaces<0)
				zoneWorkPlaces=0;	
			System.out.println("Zone "+row+" Beds: "+bedsInZone.get(row)+" OD-Trips: "+originTrips+" OD-Trips scaled: "+originTrips*scaleFactor+" Workplaces: "+zoneWorkPlaces);
			for(Integer i=0;i<zoneWorkPlaces;i++){
				workPlaces.get(row).add(generator.nextDouble());
			}
			//System.out.println("Zone: "+row+" Benefits: "+workPlaces.get(row).size());
			row++;
		}
		System.out.println("Total workplaces: "+totalWorkplaces);
			
//		/*Create map of still available work locations*/
//		HashMap<Integer,ArrayList<Double>> workPlacesFree = new HashMap<Integer, ArrayList<Double>>();
//		for(Integer key:workPlaces.keySet()){
//			workPlacesFree.put(key, new ArrayList<Double>());
//			for(Double value:workPlaces.get(key)){
//				workPlacesFree.get(key).add(value);	
//			}
//		}	
		
		/*Assign random home zone*/
		for(Integer i=0;i<populationSize;i++){	
			Person person = pf.createPerson(Id.create(i, Person.class));
			Plan plan = pf.createPlan();
					
			System.out.println("Agent: "+i+" from "+populationSize);
			
			//Pick a zone for home location
			Integer zoneCounter = 1;
			do{
				zoneCounter = 1;
				Integer randomBedNumber = generator.nextInt(totalBeds.intValue()+1);
				Double cumBeds =  bedsInZone.get(1);
				while(cumBeds < randomBedNumber){	
					zoneCounter++;
					cumBeds = cumBeds +  bedsInZone.get(zoneCounter);
				}
			}while(occupancy.get(zoneCounter).intValue()==bedsInZone.get(zoneCounter).intValue());
			
			/*Find a zone for work location*/
			
			/*Get sorted list of distances to other zone*/
			SortEntriesByValueDesc sortEntriesByValueDesc = new SortEntriesByValueDesc();
			List<Entry<Integer,Double>> sortedWorkZoneOptions = sortEntriesByValueDesc.entriesSortedByValues(nodeDistances.getDistanceMapForNode(zoneCounter.toString()));
			sortedWorkZoneOptions.remove(0);
			
			
			/*Create new set of random attractions*/
			for(Integer zone:workplacesInZone.keySet()){
				workPlaces.get(zone).clear();
				for(Integer w=0;w<workplacesInZone.get(zone);w++){
					workPlaces.get(zone).add(generator.nextDouble());
				}
			}
			
			/*Create sorted list of home zone work attractions for each zone*/
			HashMap<Integer,ArrayList<Double>> homeZoneWorkAttractions = new HashMap<Integer, ArrayList<Double>>();
			for(Integer z=1;z<25;z++){
				homeZoneWorkAttractions.put(z, new ArrayList<Double>());
				for(Double wp:workPlaces.get(z)){
					homeZoneWorkAttractions.get(z).add(wp);
				}	
				Collections.sort(homeZoneWorkAttractions.get(z));
				Collections.reverse(homeZoneWorkAttractions.get(z));
			}
			
			Boolean workZoneFound = false;			
			Integer workZone = 0;
			Boolean homeScoreAttractivityTooHigh = false;
			Double bestHomeZoneWorkFacilityAttraction = 0.0;
			do{		
				/*Find maximal WorkPlaceAttractivity in zone of residence*/
				if(homeScoreAttractivityTooHigh){
					homeZoneWorkAttractions.get(zoneCounter).remove(0);
				}
				
				if(!homeZoneWorkAttractions.get(zoneCounter).isEmpty()){
					bestHomeZoneWorkFacilityAttraction = homeZoneWorkAttractions.get(zoneCounter).get(0);
				}
				else{
					System.out.println("NO WORKPLACE FOUND: New, smaller random home zone work attractivity generated");
					bestHomeZoneWorkFacilityAttraction = generator.nextDouble()*bestHomeZoneWorkFacilityAttraction;
					homeZoneWorkAttractions.get(zoneCounter).add(bestHomeZoneWorkFacilityAttraction);
				}
				
				/*Find closest Zone with higher WorkPlaceAttractivity*/
				for(Entry<Integer, Double> workOption:sortedWorkZoneOptions){
					//System.out.println("Searching in Zone: "+workOption.getKey()+" with distance "+workOption.getValue());
					for(Double att:workPlaces.get(workOption.getKey())){
						if(att.doubleValue()>bestHomeZoneWorkFacilityAttraction.doubleValue()){
							workZone = workOption.getKey();
							workZoneFound = true;				
							break;
						}
					}
					if(workZoneFound == true)
						break;
				}
				homeScoreAttractivityTooHigh=true; 
			}while(!workZoneFound);
			
			System.out.println("   Work zone for "+zoneCounter+" found: "+workZone);			
			
			newODmatrix[zoneCounter-1][workZone-1]++;
			newODmatrix[workZone-1][zoneCounter-1]++;
			
			//Pick a home facility
			Id homeFacilityId;
			boolean freeSpace = false;
			System.out.println("   Looking for home facility...");
			do{
				
				Integer randomFacilityInZone = generator.nextInt((zoneHomeFacilities.get(zoneCounter).size()));
				homeFacilityId = zoneHomeFacilities.get(zoneCounter).get(randomFacilityInZone);
				
				/*
				Integer randomBedNumberZone = generator.nextInt((int) (bedsInZone.get(zoneCounter)+1));
				Integer facilityCounter = 0;
				homeFacilityId = zoneHomeFacilities.get(zoneCounter).get(facilityCounter);
				Double cumZoneBeds =   facilities.get(homeFacilityId).getActivityOptions().get("home").getCapacity();
				//System.out.println("Random number: "+randomBedNumberZone+" Beds counter:"+cumZoneBeds.intValue());
				while(cumZoneBeds.intValue() < randomBedNumberZone ){		
					facilityCounter++;
				//	System.out.println(facilityCounter+","+randomBedNumberZone+","+facilityId.toString()+","+cumZoneBeds);
				//	System.out.println("Number of facilites in the zone "+zoneCounter+": "+zoneFacilities.get(zoneCounter).size());
					homeFacilityId = zoneHomeFacilities.get(zoneCounter).get(facilityCounter);
					cumZoneBeds = cumZoneBeds + facilities.get(homeFacilityId).getActivityOptions().get("home").getCapacity();
				}
				*/
				
				if(remainingHomeCapacity.get(homeFacilityId)>=1.0){
					freeSpace = true;
					remainingHomeCapacity.put(homeFacilityId, remainingHomeCapacity.get(homeFacilityId)-1); 
				}
			
			}while(!freeSpace);
			occupancy.put(zoneCounter, occupancy.get(zoneCounter)+1);
			
			//Pick a work facility
			boolean workAtSameLinkFromHome=true;
			
			Id workFacilityId = null ;
			while(workAtSameLinkFromHome){
				Integer randomWorkFacility = generator.nextInt((zoneWorkFacilities.get(workZone).size()));
				workFacilityId = zoneWorkFacilities.get(workZone).get(randomWorkFacility);
				Link workLink = NetworkUtils.getNearestLink(network, facilities.get(workFacilityId).getCoord());
				Link homeLink = NetworkUtils.getNearestLink(network, facilities.get(homeFacilityId).getCoord());
				if(workLink.getId().equals(homeLink.getId())){
					System.out.println("   Work and Home on the same link! Looking for new facility...");
				}
				else{
					workAtSameLinkFromHome=false;
				}
				workAtSameLinkFromHome=false;
				System.out.println("   Home: "+homeFacilityId.toString()+","+homeLink.getId()+"   Work: "+workFacilityId.toString()+","+workLink.getId());
			}		
			
			
			//Add person attributes
			//person.setCarAvail("always");
			
			double carAvailToss = generator.nextDouble();
			if(carAvailToss<noCarPercentage){
				PersonUtils.setCarAvail(person, "never");
			}
			else{
				PersonUtils.setCarAvail(person, "always");
			}
			
			PersonUtils.setEmployed(person, true);
		//	person.getCustomAttributes().put("household_income", facilityIncomeMap.get(homeFacilityId));
			
			//Add home location to the plan
			Activity actHome = (Activity) pf.createActivityFromCoord("home", facilities.get(homeFacilityId).getCoord());
			Activity actWork = (Activity) pf.createActivityFromCoord("work", facilities.get(workFacilityId).getCoord());
			Leg leg = (Leg) pf.createLeg("car");
			actHome.setFacilityId(homeFacilityId);
			actHome.setEndTime(3600.00*8.5);
			plan.addActivity(actHome);
			plan.addLeg(leg);
			actWork.setFacilityId(workFacilityId);
			actWork.setStartTime(3600.00*9);
			actWork.setEndTime(3600.00*18);
			plan.addActivity(actWork);
			plan.addLeg(leg);
			
			Activity actHome2 = (Activity) pf.createActivityFromCoord("home", facilities.get(homeFacilityId).getCoord());
			plan.addActivity(actHome2);		
			
			person.addPlan(plan);
			
			popWriter.writePerson(person);
		
		}
		popWriter.closeStreaming();
		
		BufferedWriter writerODmatrix = new BufferedWriter( new FileWriter("../roadpricingSingapore/scenarios/siouxFalls/newODmatrix.csv"));
		
		for(Integer row1=0;row1<24;row1++){
			for(Integer column1=0;column1<24;column1++){
				writerODmatrix.write(newODmatrix[row1][column1]+",");
			}
			writerODmatrix.write("\n");
		}
		writerODmatrix.close();
		
//		Integer[][] diffODmatrix = new Integer[24][24];;
//		for(Integer rowDiff=0;rowDiff<24;rowDiff++){
//			for(Integer columnDiff=0;columnDiff<24;columnDiff++){
//				diffODmatrix[rowDiff][columnDiff] = (int) ((newODmatrix[rowDiff][columnDiff] - ODmatrix[rowDiff][columnDiff]*scaleFactor) / (ODmatrix[rowDiff][columnDiff]*scaleFactor));
//			}
//		}
		
		
		for(Integer zone:bedsInZone.keySet()){
			System.out.println("Zone: "+zone+" Beds: "+bedsInZone.get(zone)+" Occupied: "+occupancy.get(zone)+" Workplaces: "+workplacesInZone.get(zone));
		}
	}
	
	public static Integer[][] readODMatrix(String filepath) throws IOException{

		BufferedReader reader = new BufferedReader(new FileReader(filepath));
		Integer row = -1;
		Integer[][] matrix = new Integer[24][24];
		try {
			while (true) {
				row++;
				String line = reader.readLine();
				if (line == null) break;
				String[] fields = line.split(",");
				Integer column=-1;
				for(String trips:fields){
				column++;
				matrix[row][column] = Integer.valueOf(trips);
				}
			}
		} finally {
				reader.close();
		}
		return matrix;
	}
}
