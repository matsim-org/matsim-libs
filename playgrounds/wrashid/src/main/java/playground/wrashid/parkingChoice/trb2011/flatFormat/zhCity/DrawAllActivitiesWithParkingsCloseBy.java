package playground.wrashid.parkingChoice.trb2011.flatFormat.zhCity;

import java.util.LinkedList;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.core.utils.collections.QuadTree;

import playground.wrashid.lib.tools.kml.BasicPointVisualizer;
import playground.wrashid.lib.tools.kml.Color;
import playground.wrashid.parkingChoice.infrastructure.api.PParking;
import playground.wrashid.parkingChoice.trb2011.ParkingHerbieControler;

public class DrawAllActivitiesWithParkingsCloseBy {

	
	public static void main(String[] args) {
		
		String inputPlansFile="K:/Projekte/herbie/output/demandCreation/plans.xml.gz";
		String inputNetworkFile="K:/Projekte/matsim/data/switzerland/networks/ivtch-multimodal/zh/network.multimodal-wu.xml.gz";
		String inputFacilities="K:/Projekte/herbie/output/demandCreation/facilitiesWFreight.xml.gz";
		
		String outputKmlFile="C:/data/My Dropbox/ETH/Projekte/TRB Aug 2011/parkings/kmls/activitiesWithNoParkingsCloseBy.kml";
		
		Scenario scenario= GeneralLib.readScenario(inputPlansFile, inputNetworkFile, inputFacilities);
		
		QuadTree<PParking> parkingsQuadTree = getParkingsQuadTreeZHCity();
		
		Population population = scenario.getPopulation();
		
		BasicPointVisualizer basicPointVisualizer=new BasicPointVisualizer();
		
		int personCount=0;
		for (Person person:population.getPersons().values()){
			for (PlanElement pe:person.getSelectedPlan().getPlanElements()){
				
				if (pe instanceof Activity){
					Activity activity=(Activity) pe;
					Coord actCoord = activity.getCoord();
					PParking parking = parkingsQuadTree.getClosest(actCoord.getX(), actCoord.getY());
					
					if (GeneralLib.getDistance(actCoord, parking.getCoord())>300){
						basicPointVisualizer.addPointCoordinate(actCoord, activity.getType() ,Color.GREEN);
					}
					
					
				}
			}
			System.out.println(personCount++);
		}
		
		System.out.println("writing kml file...");
		basicPointVisualizer.write(outputKmlFile);
		
	}
	
	
	public static QuadTree<PParking> getParkingsQuadTreeZHCity() {
		LinkedList<PParking> parkingCollection = ParkingHerbieControler.getParkingCollectionZHCity();
		
		double minX = Double.MAX_VALUE;
		double minY = Double.MAX_VALUE;
		double maxX = Double.MIN_VALUE;
		double maxY = Double.MIN_VALUE;
		
		for (PParking parking : parkingCollection) {
			if (parking.getCoord().getX() < minX) {
				minX = parking.getCoord().getX();
			}

			if (parking.getCoord().getY() < minY) {
				minY = parking.getCoord().getY();
			}

			if (parking.getCoord().getX() > maxX) {
				maxX = parking.getCoord().getX();
			}

			if (parking.getCoord().getY() > maxY) {
				maxY = parking.getCoord().getY();
			}
		}

		QuadTree<PParking> quadTree = new QuadTree<PParking>(minX - 1.0, minY - 1.0, maxX + 1.0, maxY + 1.0);
	
		for (PParking parking : parkingCollection) {
			quadTree.put(parking.getCoord().getX(), parking.getCoord().getY(), parking);
		}
	
		return quadTree;
	}
	
}
