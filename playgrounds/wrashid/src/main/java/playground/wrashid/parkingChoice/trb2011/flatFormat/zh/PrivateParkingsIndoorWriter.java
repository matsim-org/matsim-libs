package playground.wrashid.parkingChoice.trb2011.flatFormat.zh;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.ActivityOption;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.MatsimXmlWriter;

import playground.wrashid.lib.DebugLib;
import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.obj.StringMatrix;
import playground.wrashid.parkingChoice.infrastructure.ActInfo;
import playground.wrashid.parkingChoice.infrastructure.PrivateParking;

public class PrivateParkingsIndoorWriter extends MatsimXmlWriter {

	

	private static QuadTree<ActivityFacilityImpl> facilitiesQuadTree;
	private static LinkedList<PrivateParking> privateParkings;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String sourcePathPrivateParkingsIndoor = "ETH/static data/parking/zürich city/Private Parkplätze/PrivateParkingIndoor.txt";
		
		StringMatrix privateParkingIndoorFile = GeneralLib.readStringMatrix("c:/data/My Dropbox/" + sourcePathPrivateParkingsIndoor);

		String facilitiesPath = "K:/Projekte/herbie/output/demandCreation/facilitiesWFreight.xml.gz";
		ActivityFacilitiesImpl facilities = GeneralLib.readActivityFacilities(facilitiesPath);
		
		facilitiesQuadTree = getFacilitiesQuadTree(facilities);
		
		HashMap<Integer, String> mainUsagePurposeOfBuilding = getMainBuildingUsagePurpose();
		
		privateParkings = new LinkedList<PrivateParking>();
		
		for (int i=1;i<privateParkingIndoorFile.getNumberOfRows();i++){
			int EGID = privateParkingIndoorFile.getInteger(i, 0);
			int capacity= privateParkingIndoorFile.getInteger(i, 3);
			Coord coord=new CoordImpl(privateParkingIndoorFile.getDouble(i, 1),privateParkingIndoorFile.getDouble(i, 2));
			
			if (capacity>0){
				if (mainBuildingUsagePurposeKnown(mainUsagePurposeOfBuilding, EGID)){
					
					String mainUsagePurpose=mainUsagePurposeOfBuilding.get(EGID);
					if (mainUsagePurpose.equalsIgnoreCase("Wohnen")){
						mapPrivatParkingsToFacility(capacity, coord, "home");
					} else if (mainUsagePurpose.equalsIgnoreCase("Verkauf")){
						mapPrivatParkingsToFacility(capacity, coord, "shop");
					} else if (mainUsagePurpose.equalsIgnoreCase("Büro")){
						mapPrivatParkingsToFacilities(capacity, coord, new String[]{"work_sector2","work_sector3","work"});
					} else if (mainUsagePurpose.equalsIgnoreCase("Lager")){
						mapPrivatParkingsToFacilities(capacity, coord, new String[]{"work_sector2","work_sector3","work"});
					} else if (mainUsagePurpose.equalsIgnoreCase("Produktion")){
						mapPrivatParkingsToFacilities(capacity, coord, new String[]{"work_sector2","work_sector3","work"});
					}else {
						assignParkingCapacityToClosestFacility(coord,capacity );
					}
				
				} else {
					assignParkingCapacityToClosestFacility(coord,capacity );
				}
			}
			
			
		}
		
		PrivateParkingsIndoorWriter privateParkingsWriter=new PrivateParkingsIndoorWriter();
		privateParkingsWriter.writeFile("C:/data/My Dropbox/ETH/Projekte/TRB Aug 2011/parkings/flat/privateParkingsIndoor.xml", sourcePathPrivateParkingsIndoor);

	}

	
	
	
	public void writeFile(final String filename, String source) {
		String dtd = "./test/input/playground/wrashid/parkingChoice/infrastructure/flatParkingFormat_v1.dtd";

		try {
			openFile(filename);
			writeXmlHead();
			writeDoctype("flatParkings", dtd);

			this.writer.write("<!-- data source: "+ source +" -->\n\n");
			
			this.writer.write("<flatParkings>\n");
			
			createPrivateParkings(this.writer);
			
			this.writer.write("</flatParkings>\n");
			
			this.writer.close();
		}
		catch (IOException e) {
			Gbl.errorMsg(e);
		}
	}
	
	private void createPrivateParkings(BufferedWriter writer) throws IOException {
		for (int i=0;i<privateParkings.size();i++){
			if (privateParkings.get(i).getCapacity()<1){
				DebugLib.stopSystemAndReportInconsistency();
			}
			
			
			writer.write("\t<parking type=\"private\"");
			writer.write(" id=\"ppIndoor-" + i +"\"");
			writer.write(" x=\""+ privateParkings.get(i).getCoord().getX() +"\"");
			writer.write(" y=\""+ privateParkings.get(i).getCoord().getY() +"\"");
			writer.write(" capacity=\""+ privateParkings.get(i).getCapacity() +"\"");
			writer.write(" facilityId=\""+ privateParkings.get(i).getActInfo().getFacilityId() +"\"");
			writer.write(" actType=\""+ privateParkings.get(i).getActInfo().getActType() +"\"");
			writer.write("/>\n");
		}
		
	}
	
	
	
	
	
	
	
	
	
	
	
	private static void mapPrivatParkingsToFacilities(int capacity, Coord coord, String[] activityTypes) {
		
		ActivityFacilityImpl closestFacility=null;
		String mainActTypeOfClosestFacility=null;
		double distanceToClosestFacility=Double.MAX_VALUE;
		
		for (String actType:activityTypes){
			ActivityFacilityImpl closestFacilityWithin300MeterForActivity = getClosestFacilityWithin300MeterForActivity(coord,actType);
		
			if (closestFacilityWithin300MeterForActivity!=null){
				double currentDistance = GeneralLib.getDistance(coord, closestFacilityWithin300MeterForActivity.getCoord());
				if (distanceToClosestFacility>currentDistance){
					closestFacility=closestFacilityWithin300MeterForActivity;
					mainActTypeOfClosestFacility=actType;
					distanceToClosestFacility=currentDistance;
				}
			}
		}
		
		if (closestFacility==null){
			assignParkingCapacityToClosestFacility(coord,capacity);
		} else {
			assign75PercentOfCapacityToMainActivity(mainActTypeOfClosestFacility,closestFacility,coord,capacity);
		}
		
	}

	private static void mapPrivatParkingsToFacility(int capacity, Coord coord, String assignToActivityType) {
		ActivityFacilityImpl closestActivityFacility=getClosestFacilityWithin300MeterForActivity(coord, assignToActivityType);
		
		if (closestActivityFacility==null){
			assignParkingCapacityToClosestFacility(coord,capacity);
		} else {
			assign75PercentOfCapacityToMainActivity(assignToActivityType,closestActivityFacility,coord,capacity);
		}
	}
	
	private static void assign75PercentOfCapacityToMainActivity(String activityType, ActivityFacilityImpl closestActivityFacility,
			Coord coord, int parkingCapacity) {

		int percentageOfMainActivity=75;
		
		// parking for main activity in building
		ActInfo actInfo=new ActInfo(closestActivityFacility.getId(), activityType);
		PrivateParking privateParking=new PrivateParking(coord, actInfo);
		privateParking.setCapacity((int) Math.round((double) parkingCapacity*(double) percentageOfMainActivity/100));
		privateParkings.add(privateParking);
		
		if (privateParking.getCapacity()<1){
			DebugLib.stopSystemAndReportInconsistency();
		}
		
		// parking for secondary activities in building
		int activityCapacities[]=new int[closestActivityFacility.getActivityOptions().size()-1];
		int sumOfFacilityActivityCapacities=0;
		
		int i=0;
		for (ActivityOption activityOption: closestActivityFacility.getActivityOptions().values()){
			if (activityOption.getType().equalsIgnoreCase(activityType)){
				continue;
			}
			
			activityCapacities[i]=(int) Math.round(activityOption.getCapacity());
			sumOfFacilityActivityCapacities+=activityOption.getCapacity();
			i++;
		}
		
		i=0;
		for (ActivityOption activityOption: closestActivityFacility.getActivityOptions().values()){
			if (activityOption.getType().equalsIgnoreCase(activityType)){
				continue;
			}
			
			actInfo=new ActInfo(closestActivityFacility.getId(), activityOption.getType());
			privateParking=new PrivateParking(coord, actInfo);
			double currentParkingCapacity=(double)parkingCapacity* (double)activityCapacities[i]/ (double)sumOfFacilityActivityCapacities*(double) (100-percentageOfMainActivity)/ (double)100;
			int roundedCapacity=(int) Math.round(currentParkingCapacity);
			if (roundedCapacity>0){
				privateParking.setCapacity(roundedCapacity);
				privateParkings.add(privateParking);
			}
			
			i++;
		}
		
	}

	private static void assignParkingCapacityToClosestFacility(Coord coord,
			int parkingCapacity) {
		ActivityFacilityImpl closestFacility=facilitiesQuadTree.get(coord.getX(), coord.getY());
		int activityCapacities[]=new int[closestFacility.getActivityOptions().size()];
		int sumOfFacilityActivityCapacities=0;
		
		int i=0;
		for (ActivityOption activityOption: closestFacility.getActivityOptions().values()){
			activityCapacities[i]=(int) Math.round(activityOption.getCapacity());
			sumOfFacilityActivityCapacities+=activityOption.getCapacity();
			i++;
		}
		
		i=0;
		for (ActivityOption activityOption: closestFacility.getActivityOptions().values()){
			ActInfo actInfo=new ActInfo(closestFacility.getId(), activityOption.getType());
			PrivateParking privateParking=new PrivateParking(coord, actInfo);
			
			double currentParkingCapacity=(double)parkingCapacity* (double)activityCapacities[i]/ (double)sumOfFacilityActivityCapacities;
			int roundedCapacity=(int) Math.round(currentParkingCapacity);
			if (roundedCapacity>0){
				privateParking.setCapacity(roundedCapacity);
				privateParkings.add(privateParking);
			}
			
			i++;
		}
	}

	private static ActivityFacilityImpl getClosestFacilityWithin300MeterForActivity(Coord coord,
			 String activityType) {
	
		Collection<ActivityFacilityImpl> facilities = facilitiesQuadTree.get(coord.getX(), coord.getY(), 300);
		
		ActivityFacilityImpl bestActivityFacility=null;
		double distanceBestActivityFacility=Double.MAX_VALUE;
		
		for (ActivityFacilityImpl facility:facilities){
			if (facility.getActivityOptions().containsKey(activityType)){
				double currentDistance=GeneralLib.getDistance(facility.getCoord(), coord);
				if (distanceBestActivityFacility>currentDistance){
					distanceBestActivityFacility=currentDistance;
					bestActivityFacility=facility;
				}
			}
		}
		
		
		return bestActivityFacility;
	}

	private static QuadTree<ActivityFacilityImpl> getFacilitiesQuadTree(ActivityFacilitiesImpl facilities) {
		double minX = Double.MAX_VALUE;
		double minY = Double.MAX_VALUE;
		double maxX = Double.MIN_VALUE;
		double maxY = Double.MIN_VALUE;

		for (ActivityFacility activityFacility : facilities.getFacilities().values()) {
			if (activityFacility.getCoord().getX() < minX) {
				minX = activityFacility.getCoord().getX();
			}

			if (activityFacility.getCoord().getY() < minY) {
				minY = activityFacility.getCoord().getY();
			}

			if (activityFacility.getCoord().getX() > maxX) {
				maxX = activityFacility.getCoord().getX();
			}

			if (activityFacility.getCoord().getY() > maxY) {
				maxY = activityFacility.getCoord().getY();
			}
		}

		QuadTree<ActivityFacilityImpl> quadTree = new QuadTree<ActivityFacilityImpl>(minX - 1.0, minY - 1.0, maxX + 1.0, maxY + 1.0);
	
		for (ActivityFacility activityFacility : facilities.getFacilities().values()) {
			quadTree.put(activityFacility.getCoord().getX(), activityFacility.getCoord().getY(), (ActivityFacilityImpl) activityFacility);
		}
	
		return quadTree;
	}

	private static boolean mainBuildingUsagePurposeKnown(HashMap<Integer, String> mainUsagePurposeOfBuilding, int EGID) {
		return mainUsagePurposeOfBuilding.containsKey(EGID);
	}

	private static HashMap<Integer, String> getMainBuildingUsagePurpose() {
		StringMatrix mainUsagePurposeOfBuildingFile = GeneralLib.readStringMatrix("c:/data/My Dropbox/ETH/static data/parking/zürich city/Private Parkplätze/GebaeudeHauptnutzung.txt");

		HashMap<Integer, String> mainUsagePurposeOfBuilding=new HashMap<Integer, String>();
		
		for (int i=1;i<mainUsagePurposeOfBuildingFile.getNumberOfRows();i++){
			mainUsagePurposeOfBuilding.put(mainUsagePurposeOfBuildingFile.getInteger(i, 0) , mainUsagePurposeOfBuildingFile.getString(i, 4));
		}
		return mainUsagePurposeOfBuilding;
	}

}
