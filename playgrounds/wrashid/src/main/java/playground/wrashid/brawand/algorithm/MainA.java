package playground.wrashid.brawand.algorithm;

import java.util.LinkedList;

import org.matsim.api.core.v01.Coord;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.Matrix;

public class MainA {

	public static void main(String[] args) {
		Matrix matrix = GeneralLib.readStringMatrix("c:/tmp/homeDemand.txt", "\t");
		
		LinkedList<WeightedDemand> demand=new LinkedList<WeightedDemand>();
		LinkedList<Coord> possibleChargingLocations=new LinkedList<Coord>(); 
		int maxNumberOfChargingLocations=1;
		
		for (int i=1;i<matrix.getNumberOfRows();i++){
			WeightedDemand wd=new WeightedDemand();
			wd.demandCount=matrix.getInteger(i, 1);
			wd.coord= new Coord(matrix.getDouble(i, 2), matrix.getDouble(i, 3));
			demand.add(wd);
			possibleChargingLocations.add(wd.coord);
		}
		
		SimpleLocationOptimizer slo=new SimpleLocationOptimizer();
		slo.solveSingleChargingLocation(demand, possibleChargingLocations);
		System.out.println(slo.scoreSolution(demand, possibleChargingLocations));
		
		
	}

	
	
	
}
