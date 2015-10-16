package playground.wrashid.brawand.algorithm;

import java.util.LinkedList;

import org.matsim.api.core.v01.Coord;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.core.utils.collections.QuadTree;


public class SimpleLocationOptimizer {

	public LinkedList<Coord> solve(LinkedList<WeightedDemand> demand, LinkedList<Coord> possibleChargingLocations, int maxNumberOfChargingLocations){
		
		
		return possibleChargingLocations;
	}
	
	public LinkedList<Coord> solveSingleChargingLocation(LinkedList<WeightedDemand> demand, LinkedList<Coord> possibleChargingLocations){
		LinkedList<Coord> selectedLocations=new LinkedList<Coord>();
		
		Coord bestLocation=null;
		double bestScore=Double.MAX_VALUE;
		for (int i=0;i<possibleChargingLocations.size();i++){
			selectedLocations.clear();
			selectedLocations.add(possibleChargingLocations.get(i));
			
			double score=scoreSolution(demand, selectedLocations);
			if (score<bestScore){
				score=bestScore;
				bestLocation=possibleChargingLocations.getFirst();
			}
		}
		
		
		System.out.println("bestScore: " + bestScore);
		selectedLocations.clear();
		selectedLocations.add(bestLocation);
		
		return possibleChargingLocations;
	}
	
	public double scoreSolution(LinkedList<WeightedDemand> demand, LinkedList<Coord> selectedLocations){
		double minx = Double.POSITIVE_INFINITY;
		double miny = Double.POSITIVE_INFINITY;
		double maxx = Double.NEGATIVE_INFINITY;
		double maxy = Double.NEGATIVE_INFINITY;
		for (Coord supply : selectedLocations) {
			if (supply.getX() < minx) {
				minx = supply.getX();
			}
			if (supply.getY() < miny) {
				miny = supply.getY();
			}
			if (supply.getX() > maxx) {
				maxx = supply.getX();
			}
			if (supply.getY() > maxy) {
				maxy = supply.getY();
			}
		}
		minx -= 1.0;
		miny -= 1.0;
		maxx += 1.0;
		maxy += 1.0;

		QuadTree<Coord> quadTree= new QuadTree<Coord>(minx, miny, maxx, maxy);
		
		for (Coord supply : selectedLocations) {
			quadTree.put(supply.getX(), supply.getY(), supply);
		}
		
		double score=0;
		
		for (WeightedDemand wd : demand) {
			Coord supply = quadTree.getClosest(wd.coord.getX(), wd.coord.getY());
			score+= GeneralLib.getDistance(supply, wd.coord)*wd.demandCount;
		}
		
		return score;
	}
	
	
	
	
}
