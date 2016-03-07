package playground.dhosse.gap.scenario.mid;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;

import playground.dhosse.gap.Global;
import playground.dhosse.gap.scenario.mid.MiDTravelChain.MiDTravelStage;
import playground.dhosse.utils.EgapHashGenerator;

public class MiDCSVReader {

	private final int idxPersonId = 0;
	private final int idxMode = 2;
	private final int idxPurpose = 3;
	private final int idxDistance = 4;
	private final int idxDuration = 5;
	private final int idxStart = 6;
	private final int idxEnd = 7;
	private final int idxSex = 8;
	private final int idxAge = 9;
	private final int idxCarAvailability = 10;
	private final int idxLicense = 11;
	private final int idxEmployment = 13;
	
	public Map<String, MiDSurveyPerson> persons;
	
	public MiDCSVReader(){
		this.persons = new HashMap<>();
	}
	
	public void readV2(String file, MiDPersonGroupTemplates templates){
		
		BufferedReader reader = IOUtils.getBufferedReader(file);
		
		String pId = null; //current person id
		String previousPId = null; //person id read in previous line
		
		String acts = null;
		String legs = null;
		String times = null;
		String distance = null;
		
		String pHash = null;
		
		try{
			
			String line = reader.readLine();
			
			while((line = reader.readLine()) != null){
				
				String[] parts = line.split("\t");
				
				pId = parts[idxPersonId];
				
				if(previousPId != null){
					
					if(!pId.equals(previousPId)){
						
						if(!times.contains("NULL")){
							
							MiDTravelChain tChain = new MiDTravelChain(previousPId, legs.split("_"), acts.split("_"), times.split("_"), distance.split("_"),"0");
							templates.addTravelChainToPattern(pHash, acts, tChain);
							
						}
						
						acts = null;
						legs = null;
						times = null;
						pHash = null;
						distance = null;
						
					}
					
				}

				if(pHash == null){

					String age = parts[idxAge];
					int sex = parts[idxSex].equals("male") ? 0 : 1;
					String employed = parts[idxEmployment];
					String license = parts[idxLicense];
					String carAvail = parts[idxCarAvailability].equals("never") ? "false" : "true";
					if(license.equals("false")){
						carAvail = "false";
					}
					
					pHash = EgapHashGenerator.generatePersonHash(Integer.parseInt(age), sex, Boolean.parseBoolean(carAvail), Boolean.parseBoolean(license), Boolean.parseBoolean(employed));
					
				}
				
				String nextAct = parts[idxPurpose];
				String mode = parts[idxMode];
				String departure = parts[idxStart];
				String arrival = parts[idxEnd];
				String d = parts[idxDistance];
				
				if(acts == null){
					
					if(nextAct.equals(Global.ActType.home.name())){
						
						acts = Global.ActType.other.name();
						
					} else{
						
						acts = Global.ActType.home.name();
						
					}
					
				}
				acts += "_" + nextAct;
				
				if(legs == null){
					
					legs = mode;
					
				} else {
					
					legs += "_" + mode;
					
				}
				
				if(times == null){
					
					times = departure + "-" + arrival;
					
				} else{
					
					times += "_" + departure + "-" + arrival;
					
				}
				
				if(distance == null){
					
					distance = d;
					
				} else{
					
					distance += "_" + d;
					
				}
				
				previousPId = pId;
				
			}
			
			reader.close();
			
			BufferedWriter writer = IOUtils.getBufferedWriter("./home/dhosse/travelchains.csv");
			
			for(Entry<String, Map<String, List<MiDTravelChain>>> entry : templates.personGroupId2TravelPattern2TravelChains.entrySet()){
				
				for(Entry<String, List<MiDTravelChain>> pattern : entry.getValue().entrySet()){
					
					for(MiDTravelChain chain : pattern.getValue()){
						
						for(MiDTravelStage stage : chain.getStages()){
							
							String purpose = identify(stage.getPreviousActType(), stage.getNextActType());
							
							writer.write(stage.getDepartureTime() + "," + purpose + "\n");
							
						}
						
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

		if(prev.equals(Global.ActType.home.name())){
			
			if(next.equals(Global.ActType.education.name())){
				return "WB";
			} else if(next.equals(Global.ActType.leisure.name())){
				return "WF";
			} else if(next.equals(Global.ActType.shop.name())){
				return "WE";
			} else if(next.equals(Global.ActType.work.name())){
				return "WA";
			} else {
				return "WS";
			}
			
		} else if(prev.equals(Global.ActType.education.name())){
			
			if(next.equals(Global.ActType.home.name())){
				return "BW";
			} else if(next.equals(Global.ActType.leisure.name())){
				return "BF";
			} else if(next.equals(Global.ActType.shop.name())){
				return "BE";
			} else if(next.equals(Global.ActType.work.name())){
				return "BA";
			} else {
				return "BS";
			}
			
		} else if(prev.equals(Global.ActType.leisure.name())){
			
			if(next.equals(Global.ActType.education.name())){
				return "FB";
			} else if(next.equals(Global.ActType.home.name())){
				return "FW";
			} else if(next.equals(Global.ActType.shop.name())){
				return "FE";
			} else if(next.equals(Global.ActType.work.name())){
				return "FA";
			} else {
				return "FS";
			}
			
		} else if(prev.equals(Global.ActType.shop.name())){
			
			if(next.equals(Global.ActType.education.name())){
				return "EB";
			} else if(next.equals(Global.ActType.leisure.name())){
				return "EF";
			} else if(next.equals(Global.ActType.home.name())){
				return "EW";
			} else if(next.equals(Global.ActType.work.name())){
				return "EA";
			} else {
				return "ES";
			}
			
		} else if(prev.equals(Global.ActType.work.name())){
			
			if(next.equals(Global.ActType.education.name())){
				return "AB";
			} else if(next.equals(Global.ActType.leisure.name())){
				return "AF";
			} else if(next.equals(Global.ActType.shop.name())){
				return "AE";
			} else if(next.equals(Global.ActType.home.name())){
				return "AW";
			} else {
				return "AS";
			}
			
		} else {
			
			if(next.equals(Global.ActType.education.name())){
				return "SB";
			} else if(next.equals(Global.ActType.leisure.name())){
				return "SF";
			} else if(next.equals(Global.ActType.shop.name())){
				return "SE";
			} else if(next.equals(Global.ActType.work.name())){
				return "SA";
			} else {
				return "SW";
			}
			
		}
		
	}
	
	public Map<String, MiDSurveyPerson> getPersons(){
		return this.persons;
	}
	
	
	public static class MiDData{
		
		String personId;
		int sex;
		int age;
		boolean carAvail;
		boolean hasLicense;
		boolean isEmployed;
		
		private LinkedList<MiDWaypoint> waypoints;
		
		public MiDData(String id, String sex, String mode, String age, String carAvail, String license, String employed){
			
			this.personId = id;
			this.sex = sex.equals("male") ? 0 : 1;
			this.age = !age.equals("NULL") ? Integer.parseInt(age) : Integer.MIN_VALUE;
			
			if(carAvail.equals("always") || carAvail.equals("sometimes") || mode.equals(TransportMode.car)){
				
				this.carAvail = true;
				
			} else{
				
				this.carAvail = false;
				
			}
			
			this.hasLicense = Boolean.parseBoolean(license);
			this.isEmployed = Boolean.parseBoolean(employed);
			
			this.waypoints = new LinkedList<>();
			
		}
		
		public LinkedList<MiDWaypoint> getWayPoints(){
			
			return this.waypoints;
		}
		
		public String generateWaypointHash(){
			
			StringBuffer sb = new StringBuffer();
			for(MiDWaypoint p : this.waypoints){
				sb.append(p.purpose + "-");
			}
			
			return sb.toString();
			
		}
		
		void addWaypoint(MiDWaypoint p){
			
			this.waypoints.addLast(p);
			
		}
		
	}
	
	public static class MiDWaypoint{
		
		String mode;
		String purpose;
		double distance;
		double duration;
		double startTime;
		double endTime;
		
		MiDWaypoint(String mode, String purpose, String distance, String duration, String startTime, String endTime) {
			
			this.mode = mode;
			this.purpose = purpose;
			this.distance = !distance.equals("NULL") ? Double.parseDouble(distance.replace(",", ".")) * 1000 : Double.NEGATIVE_INFINITY;
			this.duration = !duration.equals("NULL") ? Double.parseDouble(duration) * 60 : Double.NEGATIVE_INFINITY;
			this.startTime = !startTime.equals("NULL") ? Time.parseTime(startTime) : Double.NEGATIVE_INFINITY;
			this.endTime = !endTime.equals("NULL") ? Time.parseTime(endTime) : Double.NEGATIVE_INFINITY;
			
			
		}

		public String getMode() {
			return mode;
		}

		public String getPurpose() {
			return purpose;
		}

		public double getDistance() {
			return distance;
		}

		public double getDuration() {
			return duration;
		}

		public double getStartTime() {
			return startTime;
		}

		public double getEndTime() {
			return endTime;
		}
		
		
	}
	
}
