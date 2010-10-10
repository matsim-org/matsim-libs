package playground.anhorni.locationchoice.preprocess.plans.modifications;

import java.io.BufferedWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.charts.BarChart;
import org.matsim.core.utils.io.IOUtils;

import playground.anhorni.utils.Utils;

public class DistanceBins {
	
	private int places [];
	private double interval;	
	private int totalNumberOfAvailablePlacesTemp = 0;
	private int numberOfBins = 0;
	private String mode;
	private double minDist = 999999999999999.0;
	
	private final static Logger log = Logger.getLogger(DistanceBins.class);
	
	public DistanceBins(double interval, double maxDistance, String mode) {
		this.interval = interval;	
		this.numberOfBins = (int)Math.ceil(maxDistance / interval);
		log.info(mode + " number of bins: " + this.numberOfBins);
		
		places = new int[numberOfBins];
		for (int i = 0; i < numberOfBins; i++) {
			places[i] = 0;
		}
		this.mode = mode;
	}
	
	public void addDistance(double dist) {			
		// convert km -> m
		dist *= 1000.0;
		int index = Math.min(this.numberOfBins -1, (int)Math.floor(dist / interval));
		this.places[index]++;
		totalNumberOfAvailablePlacesTemp++;
		
		if (dist < minDist) {
			this.minDist = dist;
		}
	}
	
	/* -------------------------------------------------------------------------------------------------------
																	max dist
	0km		1km		2km		3km		4km		5km								20km 
	____________________________________________...
	|		|		|		|		|		|								|
	|	20	|	15		11		8		6					4						available places
	|_______|_______|_______|_______|_______|____...						|
		0		1		2		3		4					5
	
	totalNumberOfAvailablePlacesTemp = 20 + 15 + 11 + 8 + 6 + 4
	numberOfBins = 6
	
	------------------------------------------------------------------------------------------------------- */
	public double getRandomDistance(boolean planContainsLeisurePriorToWork) {
		
		// Prop = x take first free bin
		double first = MatsimRandom.getRandom().nextDouble();
		
		int r = 0;
		if ((!planContainsLeisurePriorToWork && first > 0.25)  && totalNumberOfAvailablePlacesTemp > 1) {
			// MatsimRandom.getRandom().nextInt: 
			// return  a pseudorandom, uniformly distributed int
		    // value between 0 (inclusive) and n (exclusive).				
			r = MatsimRandom.getRandom().nextInt(totalNumberOfAvailablePlacesTemp);
		}
		int index = this.getIndex(r);
		
		// if no place left
		index = this.moveAsLongAsNoPlaceLeft(index);
		
		int rInBin = this.getDistWithinBin(index);	
		this.places[index]--;
		totalNumberOfAvailablePlacesTemp--;
		
		// rounding error due to coarse network
		// only necessary for zh scenario
		//double correctionFactor = 0.91;
		double correctionFactor = 1.0;
		return correctionFactor * Math.max(10.0, index * interval + rInBin);
	}
	
	private int moveAsLongAsNoPlaceLeft(int index) {
		while (this.places[index] <= 0 && index < this.numberOfBins - 1) {			
			index++;	
		}
		return index;
	}
	
	private int getDistWithinBin(int index) {
		int rInBin = (int)Math.round(
				MatsimRandom.getRandom().nextInt((int)(interval - this.minDist)) + this.minDist);
		return rInBin;
	}
	
	private int getIndex(int r) {
		int i = 0;
		int sumBin = this.places[0];
		while (sumBin < r && i < this.numberOfBins - 1) {						
			i++;
			sumBin += this.places[i];
		}
		return i;
	}
	
	public void finish(int numberOfActs2Assign) {
		double scaleFactor = (1.0 *numberOfActs2Assign) / (1.0 * totalNumberOfAvailablePlacesTemp);
		
		log.info("Number of leisure acts to assign: " + numberOfActs2Assign);
		log.info(mode + " scale factor: " + scaleFactor);
		
		this.totalNumberOfAvailablePlacesTemp = 0;
		for (int i = 0; i < this.numberOfBins; i++) {
			this.places[i] = (int)Math.ceil((places[i] * scaleFactor));	
			this.totalNumberOfAvailablePlacesTemp += this.places[i];
		}
		this.plotDistribution("DistanceBins.places_before_sampling", "./output/plans/");
	}
	
	public void plotDistribution(String filename, String path) {	
		
		log.info(filename + ": Total number of available places: " + this.totalNumberOfAvailablePlacesTemp);
		String [] categories  = new String[this.numberOfBins];
		for (int i = 0; i < this.numberOfBins; i++) {
			categories[i] = Integer.toString(i);
		}	
		BarChart chartDistancesLeisureSize = new BarChart(mode + " trip distances", "distance bin [km]", "seize", categories);
		chartDistancesLeisureSize.addSeries("Seize per bin", Utils.convert2double(this.places));
		chartDistancesLeisureSize.saveAsPng(path + filename +  "_" + mode + ".png", 1600, 800);
		
		try {			
			BufferedWriter out = IOUtils.getBufferedWriter(filename + ".txt");
			out.write("Distance bin [" + (this.interval / 1000.0) + "km]" + "\tSize\n");
			for (int j = 0; j < places.length;  j++) {
				out.write(j + "\t" + places[j] + "\n");
			}					
			out.flush();			
			out.close();			
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
}
