package playground.tnicolai.matsim4opus.gis;

import java.util.ArrayList;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jfree.util.Log;
import org.matsim.api.core.v01.Id;

import playground.tnicolai.matsim4opus.utils.helperObjects.NetworkBoundary;
import playground.tnicolai.matsim4opus.utils.helperObjects.SquareLayer;

public class FixedSizeGrid {
	
	private static final Logger logger = Logger.getLogger(FixedSizeGrid.class);
	
	private final int maxXBins;
	private final int maxYBins;
	
	private final double gridLengthX;
	private final double gridLengthY;
	
	private final double resolutionMeter;
	
	private final Map<Id, Double> resultMap;
	
	private ArrayList<SquareLayer[][]> fixedGridList;
	private ArrayList<Bin> binList;
	
	public FixedSizeGrid(final double resolutionMeter, final NetworkBoundary networkBoundary, final Map<Id, Double> resultMap, int shrinkStep){
		
		this.resolutionMeter = resolutionMeter;
		this.resultMap = resultMap;
		
		this.maxXBins = (int)Math.ceil( networkBoundary.getXLength() / resolutionMeter ) + 1;
		this.maxYBins = (int)Math.ceil( networkBoundary.getYLength() / resolutionMeter ) + 1;
		
		// set grid length 
		this.gridLengthX = (maxXBins - 1) * resolutionMeter;
		this.gridLengthY = (maxYBins - 1) * resolutionMeter;
		
		this.binList = new ArrayList<Bin>();
		this.fixedGridList = new ArrayList<SquareLayer[][]>();
		
		
	}
	
	private void initBinSizes(int shrinkStep){
		
		assert(shrinkStep >= 0);
		
		Log.info("Determining raster for " + shrinkStep + " FixedGrids ...");
		
		for(int i = 0; i <= shrinkStep; i++){
			
			Bin bin = new Bin();
			
			if(i == 0){
				bin.xBins = this.maxXBins;
				bin.xBins = this.maxYBins;
				bin.resolution = this.resolutionMeter;
			}
			else{
				bin.xBins = (int)Math.ceil( this.maxXBins / (2. * i) );
				bin.yBins = (int)Math.ceil( this.maxYBins / (2. * i) );
				double xResolution = this.gridLengthX / bin.xBins;
				double yResolution = this.gridLengthY / bin.yBins;
				assert (xResolution == yResolution);
				bin.resolution = xResolution;
			}
			Log.info("Will create a FixedGrid with x="+ bin.xBins + " y="+bin.yBins + " with a square length of " + bin.resolution + " meter.");
		}
	}
	
	private class Bin{
		
		public int xBins = 0;
		public int yBins = 0;
		public double resolution = 0.;
	}

}
