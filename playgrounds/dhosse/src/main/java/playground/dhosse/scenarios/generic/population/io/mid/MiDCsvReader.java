package playground.dhosse.scenarios.generic.population.io.mid;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;

import playground.dhosse.scenarios.generic.population.HashGenerator;
import playground.dhosse.scenarios.generic.population.io.mid.MiDTravelChain.MiDTravelStage;
import playground.dhosse.scenarios.generic.utils.ActivityTypes;

public class MiDCsvReader {

	public Map<String, MiDPerson> persons;
	
	public MiDCsvReader(){
		this.persons = new HashMap<>();
	}
	
	public void readV2(String file, playground.dhosse.scenarios.generic.population.io.mid.MiDPersonGroupTemplates templates){
		
		BufferedReader reader = IOUtils.getBufferedReader(file);
		
		String hId = null; //household id
		String pId = null; //current person id
		String wId = null; //trip id
		String weight = null;
		
		String previousHId = null;
		String previousPId = null; //person id read in previous line
		String previousWid = null; //previous way id
		String previousAct = null;
		String previousWeight = null;
		String type = null;
		
		boolean roundBasedTrip = false;
		String types = null;
		
		String acts = null;
		String legs = null;
		String times = null;
		String distance = null;
		
		String pHash = null;
		
		try{
			
			String line = reader.readLine();
			
			while((line = reader.readLine()) != null){
				
				String[] parts = line.split(",");
				
				hId = parts[MiDIndices.householdId];
				pId = parts[MiDIndices.personId];
				wId = parts[MiDIndices.tripId];
				roundBasedTrip = parts[MiDIndices.endPoint].equals("5") ? true : false;
				weight = parts[MiDIndices.weightFactorTrip];
				type = parts[MiDIndices.endPoint];
				
				if(wId.length() > 2) continue;
				
				int dayOfWeek = Integer.parseInt(parts[MiDIndices.dayOfWeek]);
				
				if(previousPId != null){
					
					if(!pId.equals(previousPId) || Integer.parseInt(previousWid) > Integer.parseInt(wId)) {
						
						MiDTravelChain tChain = new MiDTravelChain(previousHId.concat(previousPId), legs.split("_"), acts.split("_"), times.split("_"), distance.split("_"), previousWeight, types);
						templates.addTravelChainToPattern(pHash, acts, tChain);
							
						acts = null;
						legs = null;
						times = null;
						pHash = null;
						distance = null;
						types = null;
						
					}
					
				}
				
				if(dayOfWeek >= 6){
					previousPId = null;
					continue;
				}
				
				if(roundBasedTrip){
					
					previousPId = null;
					continue;
				}
				
				if(pHash == null){

					String age = parts[MiDIndices.age];
					int sex = parts[MiDIndices.sex].equals("1") ? 0 : 1;
					String employed = Integer.parseInt(parts[MiDIndices.employed]) <= 3 ? "true" : "false";
					String license = parts[MiDIndices.hasLicense].equals("1") ? "true" : "false";
					String carAvail = Integer.parseInt(parts[MiDIndices.carAvail]) > 0 ? "true" : "false";
					
					pHash = HashGenerator.generatePersonHash(Integer.parseInt(age), sex, Boolean.parseBoolean(carAvail), Boolean.parseBoolean(license), Boolean.parseBoolean(employed));
					
				}
				
				String lastPreviousAct = null;
				if(acts != null){
					if(acts.split("_").length > 2){
						lastPreviousAct = acts.split("_")[acts.split("_").length-2];
					}
				}
				
				String nextAct = interpretPurpose(parts[MiDIndices.purpose], lastPreviousAct);
				String mode = interpretMainMode(parts[MiDIndices.mainMode]);
				String departureTime = parts[MiDIndices.departureTime];
				String arrivalTime = parts[MiDIndices.arrivalTime];
				String d = parts[MiDIndices.distance];
				
				if(acts == null){
					
					String start = parts[MiDIndices.startingPoint];
					
					if(start.equals("1")){
						
						acts = ActivityTypes.HOME;
						previousAct = ActivityTypes.HOME;
						
					} else if(start.equals("2")){
						
						acts = ActivityTypes.WORK;
						previousAct = ActivityTypes.WORK;
						
					} else{
						
						acts = ActivityTypes.OTHER;
						previousAct = ActivityTypes.OTHER;
						
					}
					
				}
				
				if(types == null){
					
					types = type;
					
				} else {
					types += "_" + type;
				}
				
				if(roundBasedTrip){
					
					acts += "_" + ActivityTypes.DUMMY + "_" + previousAct;
					nextAct = previousAct;
					
				} else{
					
					acts += "_" + nextAct;
					
				}
				
				if(legs == null){
					
					if(roundBasedTrip){
						
						legs = mode + "_" + mode;
						
					} else{
						
						legs = mode;
						
					}
					
				} else {
					
					if(roundBasedTrip){
						
						legs += "_" + mode + "_" + mode;
						
					} else{
						
						legs += "_" + mode;
						
					}
					
				}
				
				if(times == null){
					
					if(roundBasedTrip){
						
						times = departureTime + "-" + (Double.parseDouble(arrivalTime) - (Double.parseDouble(arrivalTime) - Double.parseDouble(departureTime))/2 - 1) + "_" + 
								(Double.parseDouble(arrivalTime) - (Double.parseDouble(arrivalTime) - Double.parseDouble(departureTime))/2) + "-" + arrivalTime;
						
					} else {
						
						times = departureTime + "-" + arrivalTime;
					
					}
					
				} else{
					
					if(roundBasedTrip){
						
						times += "_" + departureTime + "-" + (Double.parseDouble(arrivalTime) - (Double.parseDouble(arrivalTime) - Double.parseDouble(departureTime))/2 - 1) + "_" + 
								(Double.parseDouble(arrivalTime) - (Double.parseDouble(arrivalTime) - Double.parseDouble(departureTime))/2) + "-" + arrivalTime;
						
					} else {
						
						times += "_" + departureTime + "-" + arrivalTime;
						
					}
					
				}
				
				if(distance == null){
					
					if(roundBasedTrip){
						
						distance = Double.parseDouble(d)/2 + "_" + Double.parseDouble(d)/2;
						
					} else{
						
						distance = d;
						
					}
					
				} else{
					
					if(roundBasedTrip){
						
						distance += "_" + Double.parseDouble(d)/2 + "_" + Double.parseDouble(d)/2;
						
					} else{
						
						distance += "_" + d;
						
					}
					
				}
				
				previousHId = hId;
				previousPId = pId;
				previousWid = wId;
				previousAct = nextAct;
				previousWeight = weight;
				
			}
			
			reader.close();
			
			BufferedWriter writer = IOUtils.getBufferedWriter("/home/dhosse/travelchains_c.csv");
			
			for(Entry<String, Map<String, List<MiDTravelChain>>> entry : templates.personGroupId2TravelPattern2TravelChains.entrySet()){
				
				for(Entry<String, List<MiDTravelChain>> pattern : entry.getValue().entrySet()){
					
					for(MiDTravelChain chain : pattern.getValue()){
						
						StringBuffer sb = new StringBuffer();
						
						for(MiDTravelStage stage : chain.getStages()){
							
							String purpose = stage.getPreviousActType() + "==" + Time.writeTime(stage.getDepartureTime()) + " " + stage.getLegMode() + " " + Time.writeTime(stage.getArrivalTime()) + "==>" + stage.getNextActType();
//							String purpose = identify(stage.getPreviousActType(), stage.getNextActType());
							sb.append(purpose + ",");
							
						}
						
						writer.write(sb.toString() + "\n");
						
					}
					
				}
				
			}
			
			writer.flush();
			writer.close();
			
				
		} catch (IOException e) {
		
			e.printStackTrace();
		
		}
		
	}
	
	private static String identify(String prev, String next){

		if(prev.equals(ActivityTypes.HOME)){
			
			if(next.equals(ActivityTypes.EDUCATION)){
				return "WB";
			} else if(next.equals(ActivityTypes.KINDERGARTEN)){
				return "WK";
			} else if(next.equals(ActivityTypes.LEISURE)){
				return "WF";
			} else if(next.equals(ActivityTypes.SHOPPING)){
				return "WE";
			} else if(next.equals(ActivityTypes.WORK)){
				return "WA";
			} else {
				return "WS";
			}
			
		} else if(prev.equals(ActivityTypes.EDUCATION)){
			
			if(next.equals(ActivityTypes.HOME)){
				return "BW";
			} else if(next.equals(ActivityTypes.KINDERGARTEN)){
				return "BK";
			} else if(next.equals(ActivityTypes.LEISURE)){
				return "BF";
			} else if(next.equals(ActivityTypes.SHOPPING)){
				return "BE";
			} else if(next.equals(ActivityTypes.WORK)){
				return "BA";
			} else {
				return "BS";
			}
			
		} else if(prev.equals(ActivityTypes.KINDERGARTEN)){
			
			if(next.equals(ActivityTypes.HOME)){
				return "KW";
			} else if(next.equals(ActivityTypes.EDUCATION)){
				return "KB";
			} else if(next.equals(ActivityTypes.LEISURE)){
				return "KF";
			} else if(next.equals(ActivityTypes.SHOPPING)){
				return "KE";
			} else if(next.equals(ActivityTypes.WORK)){
				return "KA";
			} else {
				return "KS";
			}
			
		} else if(prev.equals(ActivityTypes.LEISURE)){
			
			if(next.equals(ActivityTypes.HOME)){
				return "FW";
			} else if(next.equals(ActivityTypes.KINDERGARTEN)){
				return "FK";
			} else if(next.equals(ActivityTypes.EDUCATION)){
				return "FB";
			} else if(next.equals(ActivityTypes.SHOPPING)){
				return "FE";
			} else if(next.equals(ActivityTypes.WORK)){
				return "FA";
			} else {
				return "FS";
			}
			
		} else if(prev.equals(ActivityTypes.SHOPPING)){
			
			if(next.equals(ActivityTypes.HOME)){
				return "EW";
			} else if(next.equals(ActivityTypes.KINDERGARTEN)){
				return "EK";
			} else if(next.equals(ActivityTypes.LEISURE)){
				return "EF";
			} else if(next.equals(ActivityTypes.EDUCATION)){
				return "EB";
			} else if(next.equals(ActivityTypes.WORK)){
				return "EA";
			} else {
				return "ES";
			}
			
		} else if(prev.equals(ActivityTypes.WORK)){
			
			if(next.equals(ActivityTypes.HOME)){
				return "AW";
			} else if(next.equals(ActivityTypes.KINDERGARTEN)){
				return "AK";
			} else if(next.equals(ActivityTypes.LEISURE)){
				return "AF";
			} else if(next.equals(ActivityTypes.SHOPPING)){
				return "AE";
			} else if(next.equals(ActivityTypes.EDUCATION)){
				return "AB";
			} else {
				return "AS";
			}
			
		} else{
			
			if(next.equals(ActivityTypes.HOME)){
				return "SW";
			} else if(next.equals(ActivityTypes.KINDERGARTEN)){
				return "SK";
			} else if(next.equals(ActivityTypes.EDUCATION)){
				return "SB";
			} else if(next.equals(ActivityTypes.LEISURE)){
				return "SF";
			} else if(next.equals(ActivityTypes.SHOPPING)){
				return "SE";
			} else if(next.equals(ActivityTypes.WORK)){
				return "SA";
			} else {
				return "SS";
			}
			
		}
		
	}
	
	public Map<String, MiDPerson> getPersons(){
		return this.persons;
	}
	
	
	private static String interpretPurpose(String p, String previousActType){
		
		if(p.equals("1")){
			return ActivityTypes.WORK;
		} else if(p.equals("2")){
			return ActivityTypes.OTHER; //business
		} else if(p.equals("3")){
			return ActivityTypes.EDUCATION;
		} else if(p.equals("4")){
			return ActivityTypes.SHOPPING;
		} else if(p.equals("5")){
			return ActivityTypes.OTHER; //private
		} else if(p.equals("6")){
			return ActivityTypes.OTHER; //pickdrop
		} else if(p.equals("7")){
			return ActivityTypes.LEISURE;
		} else if(p.equals("8")){
			return ActivityTypes.HOME;
		} else if(p.equals("9") && previousActType != null){
			return previousActType;
		} else if(p.equals("10")){
			return ActivityTypes.OTHER;
		} else if(p.equals("11")){
			return ActivityTypes.OTHER; //accompany
		} else if(p.equals("12")){
			return ActivityTypes.EDUCATION; //schule / vorschule
		} else if(p.equals("13")){
			return ActivityTypes.KINDERGARTEN;
		} else{
			return ActivityTypes.OTHER;
		}
		
	}
	
	private static String interpretMainMode(String mm){
		
		if(mm.equals("1")){
			return TransportMode.walk;
		} else if(mm.equals("2")){
			return TransportMode.bike;
		} else if(mm.equals("3")){
			return TransportMode.ride;
		} else if(mm.equals("4")){
			return TransportMode.car;
		} else{
			return TransportMode.pt;
		}
		
	}
	
}
