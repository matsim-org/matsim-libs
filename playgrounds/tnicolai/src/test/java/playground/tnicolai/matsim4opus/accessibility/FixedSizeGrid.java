package playground.tnicolai.matsim4opus.accessibility;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;

import playground.tnicolai.matsim4opus.constants.InternalConstants;
import playground.tnicolai.matsim4opus.utils.helperObjects.AggregateObject2NearestNode;
import playground.tnicolai.matsim4opus.utils.helperObjects.SquareLayer;

public class FixedSizeGrid {
	
	private static int res = 10;
	private static double xmin, xmax, ymin, ymax;

	public static void main(String[] args) {

		NetworkImpl network = SpatialGridFillOrder.createNetwork();
		AggregateObject2NearestNode[] dummyJobClusterArray = SpatialGridFillOrder.createWorkplaces(network);
		Map<Id, Double> resultMap = SpatialGridFillOrder.travelTimeAccessibility(network, dummyJobClusterArray);
		
		Bin[] binArray = createBins( network, resultMap );
		
		ArrayList<SquareLayer[][]> fixedGridList = new ArrayList<SquareLayer[][]>();
		
		for(int i = 0; i < binArray.length; i++){
			
			Bin bins = binArray[i];
			
			SquareLayer[][] fixedGrid = new SquareLayer[bins.xBins][bins.yBins];
			double binResolution = bins.resolution;

			// create grid
			fixedGridList.add( createGrid(network, resultMap, bins, fixedGrid, binResolution) );
			// end grid
		}		
		
		dumpGrids(fixedGridList);
		
		System.out.println("done!");
		
	}

	private static SquareLayer[][] createGrid(final NetworkImpl network, final Map<Id, Double> resultMap,
								   final Bin bins, final SquareLayer[][] fixedGrid, final double binResolution) {
		
		double xCoord;
		double yCoord;
		
		for(int xBin = 0; xBin < bins.xBins; xBin++){
			
			xCoord = xmin + (xBin * binResolution);
			
			for(int yBin = 0; yBin < bins.yBins; yBin++){
				
				yCoord = ymin + (yBin * binResolution);
				
				Coord coord = new CoordImpl(xCoord, yCoord);
				// get nearest node on network
				Node node = network.getNearestNode( coord );
				
				SquareLayer sq = new SquareLayer();
				sq.setSquareCentroidV2(node.getId(), resultMap.get(node.getId()), coord);
				
				fixedGrid[xBin][yBin] = sq;
			}
		}
		return fixedGrid;
	}
	
	private static Bin[] createBins(final NetworkImpl network, final Map<Id, Double> resultMap){
		
		// The bounding box of all the given nodes as double[] = {minX, minY, maxX, maxY}
		double networkBoundingBox[] = NetworkUtils.getBoundingBox(network.getNodes().values());
		xmin = networkBoundingBox[0];
		ymin = networkBoundingBox[1];
		xmax = networkBoundingBox[2];
		ymax = networkBoundingBox[3];
		
		Bin[] binArray = new Bin[3];

		Bin bin = new Bin();
		bin.xBins = getInitialNumOfBins( (xmax - xmin), res);
		bin.yBins = getInitialNumOfBins( (ymax - ymin), res);
		bin.resolution = res;
		
		double newXLength = (bin.xBins - 1) * res;
		double newYLength = (bin.yBins - 1) * res;
			
		binArray[0] = bin;
		
		for(int i = 1; i < binArray.length; i++){
			
			int prevXBins = binArray[0].xBins;
			int prevYBins = binArray[0].yBins;
			
			bin = new Bin();
			bin.xBins = getNumOfBins(prevXBins, i);
			bin.yBins = getNumOfBins(prevYBins, i);
			
			double xRes = determineResolution(newXLength, bin.xBins);
			double yRes = determineResolution(newYLength, bin.yBins);
			
			assert(xRes == yRes);
			if(xRes != yRes)
				System.err.println("Buggy resolution");
			bin.resolution = xRes;
			
			binArray[i] = bin;
		}
		return binArray;
	}
	
	private static int getInitialNumOfBins(double length, double resolution){
		int bins = (int)Math.ceil( length / resolution ) + 1;
		return bins;
	}
	
	private static int getNumOfBins(int binSize, int shrinkFactor){
		int bins = (int)Math.ceil( ((binSize - 1) / (2. * shrinkFactor)) + 1 );
		return bins;
	}
	
	private static double determineResolution(double length, int binSize){
		return (length / (binSize - 1));
	}
	
	private static void dumpGrids(ArrayList<SquareLayer[][]> fixedGridList){
		
		Iterator<SquareLayer[][]> gridIterator = fixedGridList.iterator();
		int counter = 0;
		while(gridIterator.hasNext()){
		
			SquareLayer[][] sl = gridIterator.next();
			
			String fileName = "/Users/thomas/Desktop/test/";
			BufferedWriter layer1 = IOUtils.getBufferedWriter(fileName + "_Centroid" + counter++ + InternalConstants.FILE_TYPE_TXT);
			
			int numXBins = sl.length;
			int numYBins = sl[0].length;
			
			try{
				
				for(int xBin = 0; xBin < numXBins; xBin++){
					// write x coordinates
					double xCoord = xmin + (xBin * res);
					
					layer1.write("\t");
					layer1.write(String.valueOf(xCoord));
				}
				layer1.newLine();				
				
				for(int yBin = numYBins - 1; yBin >= 0; yBin--){
					// write y coordinates 
					double yCoord = ymax - (yBin * res);
					
					layer1.write(String.valueOf(yCoord));
					
					for(int xBin = 0; xBin < numXBins; xBin++){
						layer1.write("\t");
						Double centroidValue = sl[xBin][yBin].getCentroidAccessibility();
						if(centroidValue != null)
							layer1.write( String.valueOf(centroidValue));
						else
							layer1.write("NA");
					}	
					layer1.newLine();
				}
				layer1.flush();
				layer1.close();
			}
			catch(Exception e){
				e.printStackTrace();
			}	
		}
	}
	
	public static class Bin{
		public int xBins = 0;
		public int yBins = 0;
		public double resolution = 0.;
		
	}
}
