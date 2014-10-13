package playground.wrashid.parkingSearch.ppSim.ttmatrix;

import java.util.ArrayList;
import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.Matrix;

public class  TTMatrix {
	
	protected int timeBinSizeInSeconds;
	protected int simulatedTimePeriod;
	protected HashMap<Id<Link>, double[]> linkTravelTimes;
	protected Network network;
	
	public double getTravelTime(double time, Id<Link> linkId) {
		double travelTime;

		int timeBinIndex = (int) (Math.round(GeneralLib.projectTimeWithin24Hours(time)) / timeBinSizeInSeconds);
		Link link = network.getLinks().get(linkId);

		if (!linkTravelTimes.containsKey(linkId)) {
			double minTravelTime = link.getLength() / link.getFreespeed();
			travelTime = minTravelTime;
		} else {
			travelTime = linkTravelTimes.get(linkId)[timeBinIndex];
		}

		return travelTime;
	}
	
	protected int getNumberOfBins(){
		return simulatedTimePeriod/timeBinSizeInSeconds + 1;
	}
	
	public void writeTTMatrixToFile(String outputFile){
		Matrix sm=new Matrix();
		
		ArrayList<String> row=new ArrayList<String>();
		row.add("firstColumn=linkId");
		row.add("simulatedTimePeriod=" +simulatedTimePeriod);
		row.add("timeBinSizeInSeconds=" + timeBinSizeInSeconds);
		sm.addRow(row);
		
		for (Id<Link> linkId:linkTravelTimes.keySet()){
			row=new ArrayList<String>();
			double[] ds = linkTravelTimes.get(linkId);
			row.add(linkId.toString());
			for (int i=0;i<ds.length;i++){
				row.add(Double.toString(ds[i]));
			}	
			sm.addRow(row);
		}
		
		sm.writeMatrix(outputFile);
	}

}
