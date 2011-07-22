package playground.wrashid.parkingChoice.trb2011.flatFormat.zhCity;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.ActivityOption;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.MatsimXmlWriter;

import playground.wrashid.lib.DebugLib;
import playground.wrashid.lib.obj.DoubleValueHashMap;
import playground.wrashid.lib.obj.TwoHashMapsConcatenated;
import playground.wrashid.lib.obj.TwoKeyHashMapsWithDouble;
import playground.wrashid.lib.tools.facility.FacilityLib;
import playground.wrashid.parkingChoice.infrastructure.ActInfo;
import playground.wrashid.parkingChoice.infrastructure.PrivateParking;
import playground.wrashid.parkingChoice.infrastructure.api.Parking;
import playground.wrashid.parkingChoice.trb2011.ParkingHerbieControler;

/**
 * - Assign the private parkings to facility located within 200 meter according to capacities.
 * 1.) cluster parkings
 * 2.) assign each cluster parking to the parkings within 200meter
 * 3.) cluster all parkings attached to a single facility and activity pair to one single parking
 * (to reduce the number of parkings).
 * (save capacities as double, so that they can be better scaled)
 * @author wrashid
 *
 */
public class PrivateParkingsWriter_v1 extends MatsimXmlWriter {

	private static QuadTree<ActivityFacilityImpl> facilitiesQuadTree;
	private static LinkedList<PrivateParking> reducedPrivateParkings;

	public static void main(String[] args) {
		facilitiesQuadTree = PrivateParkingsIndoorWriter_v0.getFacilitiesQuadTree();
		
		LinkedList<Parking> privateParkings = getPrivateParkings();
		//checkParking(privateParkings,"p1");
		int clusterSideLengthInMeters=50;
		HashMap<Coord,Double>  clusteredParkingCapacities =clusterParkings(clusterSideLengthInMeters, privateParkings);
		
//		double totalCap=0;
//		for (Double capacity:clusteredParkingCapacities.values()){
//			totalCap+=capacity;
//		}
//		System.out.println("p1.1:" + totalCap);
		
		double maxFacilityDistanceInMeters=200;
		LinkedList<PrivateParking> assignedPrivateParkings=assignPrivateParkingsToFacilities(maxFacilityDistanceInMeters,clusteredParkingCapacities);
		//checkParking(assignedPrivateParkings,"p2");
		reducedPrivateParkings=reducePrivateParkings(assignedPrivateParkings);
		//checkParking(reducedPrivateParkings,"p3");
		PrivateParkingsWriter_v1 privateParkingsWriter=new PrivateParkingsWriter_v1();
		privateParkingsWriter.writeFile("C:/data/My Dropbox/ETH/static data/parking/zürich city/flat/privateParkings_v1.xml", "PrivateParkingWriter_v1");

	}
	
	private static void checkParking(LinkedList privateParkings, String point){
		double totalCapacity=0;
		for (Object obj:privateParkings){
			Parking parking=(Parking) obj;
			totalCapacity+=parking.getCapacity();
		}
		
		System.out.println("totalCapacity:" + totalCapacity + " - point: " + point);
	}
	
	
	private static LinkedList<PrivateParking> reducePrivateParkings(LinkedList<PrivateParking> assignedPrivateParkings) {	
		LinkedList<PrivateParking> reducedPrivatParkingSet=new LinkedList<PrivateParking>();
		
		TwoHashMapsConcatenated<String, String, LinkedList<PrivateParking>> reclustering=new TwoHashMapsConcatenated<String, String, LinkedList<PrivateParking>>();
		for (PrivateParking privateParking:assignedPrivateParkings){
			String facilityIdString = privateParking.getActInfo().getFacilityId().toString();
			String actType = privateParking.getActInfo().getActType();
			if (reclustering.get(facilityIdString, actType)==null){
				reclustering.put(facilityIdString, actType, new LinkedList<PrivateParking>());
			}
			LinkedList<PrivateParking> linkedList = reclustering.get(facilityIdString, actType);
			linkedList.add(privateParking);
		}
		
		for (String facilityIdString:reclustering.getKeySet1()){
			for (String actType:reclustering.getKeySet2(facilityIdString)){
				LinkedList<PrivateParking> linkedList = reclustering.get(facilityIdString, actType);
				double averageX=0;
				double averageY=0;
				double totalCapacity=0;
				for (PrivateParking privateParking:linkedList){
					averageX+=privateParking.getCoord().getX();
					averageY+=privateParking.getCoord().getY();
					totalCapacity+=privateParking.getCapacity();
				}
				averageX/=linkedList.size();
				averageY/=linkedList.size();
				
				PrivateParking privateParking=new PrivateParking(new CoordImpl(averageX, averageY), linkedList.get(0).getActInfo());
				privateParking.setCapacity(totalCapacity);
				reducedPrivatParkingSet.add(privateParking);
			}
		}
		
		
		return reducedPrivatParkingSet;
	}

	private static LinkedList<PrivateParking> assignPrivateParkingsToFacilities(double maxFacilityDistanceInMeters,
			HashMap<Coord, Double> clusteredParkingCapacities) {
		
		LinkedList<PrivateParking> privateParkings=new LinkedList<PrivateParking>();
		
		for (Coord coord:clusteredParkingCapacities.keySet()){
			Collection<ActivityFacilityImpl> actFacilities = new LinkedList<ActivityFacilityImpl>();
			actFacilities.addAll(facilitiesQuadTree.get(coord.getX(), coord.getY(), maxFacilityDistanceInMeters));
			
			if (actFacilities.size()==0){
				actFacilities.add(facilitiesQuadTree.get(coord.getX(), coord.getY()));
			}
			
			double totalCapacityOfFacilitiesAttachedToCluster=0;
			for (ActivityFacilityImpl actFacility:actFacilities){
				totalCapacityOfFacilitiesAttachedToCluster+=FacilityLib.getTotalCapacityOfFacility(actFacility);
			}
			
			for (ActivityFacilityImpl actFacility:actFacilities){
				privateParkings.addAll(assignParkingCapacityToFacility(coord,clusteredParkingCapacities.get(coord)* FacilityLib.getTotalCapacityOfFacility(actFacility)/totalCapacityOfFacilitiesAttachedToCluster,actFacility));
			}
		}
		
		return privateParkings;
	}

	private static HashMap<Coord,Double> clusterParkings(int clusterSideLengthInMeters, LinkedList<Parking> privateParkings) {
		DoubleValueHashMap<String> parkingCapacities=new DoubleValueHashMap<String>();
		
		for (Parking parking:privateParkings){
			String id= Math.round(parking.getCoord().getX()/clusterSideLengthInMeters)*clusterSideLengthInMeters + "," + Math.round(parking.getCoord().getY()/clusterSideLengthInMeters*clusterSideLengthInMeters);
			parkingCapacities.incrementBy(id, parking.getCapacity());
		}		
		
		HashMap<Coord,Double> clusteredCapacities=new HashMap<Coord, Double>();
		for (String id:parkingCapacities.keySet()){
			String[] idParts=id.split(",");
			Coord coord=new CoordImpl(idParts[0],idParts[1]);
			clusteredCapacities.put(coord, parkingCapacities.get(id));
		}
		
		return clusteredCapacities;
	}

	public static LinkedList<Parking> getPrivateParkings(){
		LinkedList<Parking> parkingCollection=new LinkedList<Parking>();
		String baseFolder="C:/data/My Dropbox/ETH/static data/parking/zürich city/flat/";
		ParkingHerbieControler.readParkings(1.0, baseFolder + "privateParkingsIndoor_v0.xml", parkingCollection);
		ParkingHerbieControler.readParkings(1.0, baseFolder + "privateParkingsOutdoor_v0.xml", parkingCollection);
		
		return parkingCollection;
	}
	
	
	public static LinkedList<PrivateParking> assignParkingCapacityToFacility(Coord parkingCoordinate, double parkingCapacity, ActivityFacilityImpl facility){
		LinkedList<PrivateParking> privateParkings=new LinkedList<PrivateParking>();
		double activityCapacities[]=new double[facility.getActivityOptions().size()];
		double sumOfFacilityActivityCapacities=0;
		
		int i=0;
		for (ActivityOption activityOption: facility.getActivityOptions().values()){
			activityCapacities[i]=activityOption.getCapacity();
			sumOfFacilityActivityCapacities+=activityOption.getCapacity();
			i++;
		}
		
		i=0;
		for (ActivityOption activityOption: facility.getActivityOptions().values()){
			ActInfo actInfo=new ActInfo(facility.getId(), activityOption.getType());
			PrivateParking privateParking=new PrivateParking(parkingCoordinate, actInfo);
			
				double currentParkingCapacity=parkingCapacity* activityCapacities[i]/ sumOfFacilityActivityCapacities;
				privateParking.setCapacity(currentParkingCapacity);
				privateParkings.add(privateParking);
			
			i++;
		}
		
		return privateParkings;
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
		for (int i=0;i<reducedPrivateParkings.size();i++){
			if (reducedPrivateParkings.get(i).getCapacity()<=0){
				DebugLib.stopSystemAndReportInconsistency();
			}
			
			writer.write("\t<parking type=\"private\"");
			writer.write(" id=\"privateParkings-" + i +"\"");
			writer.write(" x=\""+ reducedPrivateParkings.get(i).getCoord().getX() +"\"");
			writer.write(" y=\""+ reducedPrivateParkings.get(i).getCoord().getY() +"\"");
			writer.write(" capacity=\""+ reducedPrivateParkings.get(i).getCapacity() +"\"");
			writer.write(" facilityId=\""+ reducedPrivateParkings.get(i).getActInfo().getFacilityId() +"\"");
			writer.write(" actType=\""+ reducedPrivateParkings.get(i).getActInfo().getActType() +"\"");
			writer.write("/>\n");
		}
		
	}
	
}
