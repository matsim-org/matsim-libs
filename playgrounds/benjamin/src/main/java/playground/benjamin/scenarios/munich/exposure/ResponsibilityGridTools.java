package playground.benjamin.scenarios.munich.exposure;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;


public class ResponsibilityGridTools {
	
	private Double dist0factor = 0.216;
	private Double dist1factor = 0.132;
	private Double dist2factor = 0.029;
	private Double dist3factor = 0.002;
	
	Double timeBinSize;
	int noOfTimeBins;
	Map<Double, Map<Id<Link>, Double>> timebin2link2factor;
 
	private Map<Id<Link>, Integer> links2xCells;
	private Map<Id<Link>, Integer> links2yCells;
	private int noOfXCells;
	private int noOfYCells;
	
	public ResponsibilityGridTools(Double timeBinSize, int noOfTimeBins,
			Map<Id<Link>, Integer> links2xCells, Map<Id<Link>, Integer> links2yCells, int noOfXCells, int noOfYCells) {
		this.init(timeBinSize, noOfTimeBins, links2xCells, links2yCells, noOfXCells, noOfYCells);		
	}
	
	public Double getFactorForLink(Id<Link> linkId, double time) {
		Double currentTimeBin = getTimeBin(time);
		
		if(timebin2link2factor!=null){
			if(timebin2link2factor.get(currentTimeBin)!=null){
				if(timebin2link2factor.get(currentTimeBin).get(linkId)!=null){
					return timebin2link2factor.get(currentTimeBin).get(linkId);
				}
			}
		}
		return 0.0;
	}
 
	public void resetAndcaluculateRelativeDurationFactors(SortedMap<Double, Double[][]> duration) {
		
		timebin2link2factor = new HashMap<Double, Map<Id<Link>, Double>>();
		
		// each time bin - generate new map
		for(Double timeBin : duration.keySet()){timebin2link2factor.put(timeBin, new HashMap<Id<Link>, Double>());
		// calculate total durations for each time bin
			Double sumOfCurrentTimeBin = 0.0;
			Double [][] currentDurations = duration.get(timeBin);
			for(int x=0; x< currentDurations.length; x++){
				for(int y=0; y<currentDurations[x].length;y++){
					sumOfCurrentTimeBin += currentDurations[x][y];
				}
			}
			// calculate average for each time bin
			Double averageOfCurrentTimeBin = sumOfCurrentTimeBin/currentDurations.length/currentDurations[0].length;
			// calculate factor for each link for current time bin
			for(Id<Link> linkId: links2xCells.keySet()){
				if (links2yCells.containsKey(linkId)) { // only if in research are
					
					Double relativeFactorForCurrentLink = getRelativeFactorForCurrentLink(
							averageOfCurrentTimeBin, currentDurations, linkId);
					timebin2link2factor.get(timeBin).put(linkId,relativeFactorForCurrentLink); 
				}
			}
		}		
	}

	private Double getRelativeFactorForCurrentLink(Double averageOfCurrentTimeBin, Double[][] currentDurations, Id<Link> linkId) {
		
		Double relevantDuration = new Double(0.0);
		if(links2xCells.get(linkId)!=null && links2yCells.get(linkId)!=null){
		Cell cellOfLink = new Cell(links2xCells.get(linkId), links2yCells.get(linkId));		
		for(int distance =0; distance <= 3; distance++){
			List<Cell> distancedCells = cellOfLink .getCellsWithExactDistance(noOfXCells, noOfYCells, distance);
			for(Cell dc: distancedCells){
				
				try {
						if (currentDurations[dc.getX()][dc.getY()]!=null) {
							Double valueOfdc = currentDurations[dc.getX()][dc.getY()];
							//System.out.println("distanced cell: " + dc.toString() + "with duration amount " + valueOfdc);
							switch (distance) {
							case 0:
								relevantDuration += dist0factor * valueOfdc; 
								break;
							case 1:
								relevantDuration += dist1factor * valueOfdc;
								break;
							case 2:
								relevantDuration += dist2factor * valueOfdc;
								break;
							case 3:
								relevantDuration += dist3factor * valueOfdc;
								break;
							}
						}
					
				} catch (ArrayIndexOutOfBoundsException e) {
					// nothing to do not in research area
				}
			}
		}
		//following print statments are commented. amit, Oct'15
//		if(!linkId.toString().contains("_p")){
//			if(relevantDuration>1.){
//			System.out.println("average of time bin is " + averageOfCurrentTimeBin);
//			System.out.println("calculating relative factor for link " + linkId.toString() + " in cell " + cellOfLink.toString());
//			System.out.println("relevant duration for this link " + relevantDuration 
//					+ " resulting factor " + (relevantDuration/averageOfCurrentTimeBin));
//			}
//		}
		return relevantDuration / averageOfCurrentTimeBin;	
		}
		return 0.0;
	}


	private Double getTimeBin(double time) {
		Double timeBin = Math.ceil(time/timeBinSize)*timeBinSize;
		if(timeBin<=1)timeBin=timeBinSize;
		return timeBin;
	}


	public void init(Double timeBinSize, int noOfTimeBins,
			Map<Id<Link>, Integer> links2xCells, Map<Id<Link>, Integer> links2yCells, int noOfXCells, int noOfYCells) {
		this.timeBinSize = timeBinSize;
		this.noOfTimeBins = noOfTimeBins;
		this.timebin2link2factor = new HashMap<Double, Map<Id<Link>,Double>>();
		for(int i=1; i<noOfTimeBins+1; i++){
			timebin2link2factor.put((i*this.timeBinSize), new HashMap<Id<Link>, Double>());
		}
		this.links2xCells = links2xCells;
		this.links2yCells = links2yCells;
		this.noOfXCells=noOfXCells;
		this.noOfYCells=noOfYCells;
		
	}

	public int getNoOfTimeBins() {
		return this.noOfTimeBins;
	}

	public Double getTimeBinSize() {
		return this.timeBinSize;
	}

	public int getNoOfXCells() {
		return this.noOfXCells;
	}

	public int getNoOfYCells() {
		return this.noOfYCells;
	}
}
