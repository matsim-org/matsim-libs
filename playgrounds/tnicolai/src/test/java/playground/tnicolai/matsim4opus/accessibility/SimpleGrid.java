package playground.tnicolai.matsim4opus.accessibility;

import java.io.BufferedWriter;
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

public class SimpleGrid {
	
	private static int res = 10;
	private static double xmin, xmax, ymin, ymax;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		
		NetworkImpl network = SpatialGridFillOrder.createNetwork();
		AggregateObject2NearestNode[] dummyJobClusterArray = SpatialGridFillOrder.createWorkplaces(network);
		Map<Id, Double> resultMap = SpatialGridFillOrder.travelTimeAccessibility(network, dummyJobClusterArray);
		
		SquareLayer[][] mySimpleGrid = network2SimpleGrid( network, resultMap );
		
		dumpResults( mySimpleGrid );
		
		System.out.println("done!");
		
	}

	private static SquareLayer[][] network2SimpleGrid( final NetworkImpl network, final Map<Id, Double> resultMap ){
		
		// The bounding box of all the given nodes as double[] = {minX, minY, maxX, maxY}
		double networkBoundingBox[] = NetworkUtils.getBoundingBox(network.getNodes().values());
		xmin = networkBoundingBox[0];
		xmax = networkBoundingBox[1];
		ymin = networkBoundingBox[2];
		ymax = networkBoundingBox[3];
		
		double xlength = xmax - xmin;
		double ylength = ymax - ymin;
		
		// Math.ceil(x) rundet auf
		// Math.floor(x) rundet ab
		int numXBins = (int)Math.ceil( xlength / res ) +1;
		int numYBins = (int)Math.ceil( ylength / res ) +1;
	    
		SquareLayer[][] mySimpleGrid = new SquareLayer[numXBins][numYBins];
		
		if(resultMap == null);
			System.err.println("ResultMap is null. Can' init Grid...");
		
		// create square layer array and init it with accessibility results for square centroids
		for(double xAxis = xmin; xAxis <= xmax; xAxis +=res){
			for(double yAxis = ymin; yAxis <= ymax; yAxis += res){
				
				// determine right array bin
				int xBin = (int)Math.floor(xAxis / res);
				int yBin = (int)Math.floor(yAxis / res);
				
				// determine square centroid
				double xCenter = xAxis + (res / 2.);
				double yCenter = yAxis + (res / 2.);
				Coord coord = new CoordImpl(xCenter, yCenter);
				
				// get nearest node on network
				Node node = network.getNearestNode( coord );
				
				SquareLayer sq = new SquareLayer();
				sq.setSquareCentroidV2(node.getId(), resultMap.get(node.getId()), coord);
				
				mySimpleGrid[xBin][yBin] = sq;
			}
		}
		
		
		Iterator<Node> nodeIterator = network.getNodes().values().iterator();		
		// assign nodes to right square
		for(;nodeIterator.hasNext();){
			Node node = nodeIterator.next();
			double xNodeCoord = node.getCoord().getX();
			double yNodeCoord = node.getCoord().getY();
			
			// determine right array bin
			int xBin = (int)Math.floor(xNodeCoord / res);
			int yBin = (int)Math.floor(yNodeCoord / res);
			
			SquareLayer sq = mySimpleGrid[xBin][yBin];
			sq.addNodeV2( node, resultMap.get(node.getId()) );
		}
		
		return mySimpleGrid;
	}
	
	private static void dumpResults(SquareLayer[][] mySimpleGrid){
		
		String fileName = "/Users/thomas/Desktop/test/";
		BufferedWriter layer1 = IOUtils.getBufferedWriter(fileName + "_Centroid" + InternalConstants.FILE_TYPE_TXT);
		BufferedWriter layer2 = IOUtils.getBufferedWriter(fileName + "_Mean" + InternalConstants.FILE_TYPE_TXT);
		BufferedWriter layer3 = IOUtils.getBufferedWriter(fileName + "_Derivation" + InternalConstants.FILE_TYPE_TXT);
		
		int numXBins = mySimpleGrid.length;
		int numYBins = mySimpleGrid[0].length;
		
		try{
			
			for(int xBin = 0; xBin < numXBins; xBin++){
				// write x coordinates
				double xCoord = xmin + (xBin * res);
				
				layer1.write("\t");
				layer1.write(String.valueOf(xCoord));
				layer2.write("\t");
				layer2.write(String.valueOf(xCoord));
				layer3.write("\t");
				layer3.write(String.valueOf(xCoord));
			}
			layer1.newLine();
			layer2.newLine();
			layer3.newLine();
			
			
			for(int yBin = numYBins - 1; yBin >= 0; yBin--){
				// write y coordinates 
				double yCoord = ymax - (yBin * res);
				
				layer1.write(String.valueOf(yCoord));
				layer2.write(String.valueOf(yCoord));
				layer3.write(String.valueOf(yCoord));
				
				for(int xBin = 0; xBin < numXBins; xBin++){
					layer1.write("\t");
					Double centroidValue = mySimpleGrid[xBin][yBin].getCentroidAccessibility();
					if(centroidValue != null)
						layer1.write( String.valueOf(centroidValue));
					else
						layer1.write("NA");
					
					layer2.write("\t");
					Double meanValue = mySimpleGrid[xBin][yBin].getMeanAccessibilityV2();
					if(meanValue != null)
						layer2.write(String.valueOf(meanValue));
					else
						layer2.write("NA");
					
					layer3.write("\t");
					Double derivationValue = mySimpleGrid[xBin][yBin].getAccessibilityDerivationV2();
					if(derivationValue != null)
						layer3.write(String.valueOf(derivationValue));
					else
						layer3.write("NA");
				}
				layer1.newLine();
				layer2.newLine();
				layer3.newLine();
			}
			layer1.flush();
			layer1.close();
			layer2.flush();
			layer2.close();
			layer3.flush();
			layer3.close();
		}
		catch(Exception e){
			e.printStackTrace();
		}

//		
//		for(int i = grid.getNumRows() - 1; i >=0 ; i--) {
//			layer1.write(String.valueOf(grid.getYmax() - i * grid.getResolution()));
//			layer2.write(String.valueOf(grid.getYmax() - i * grid.getResolution()));
//			layer3.write(String.valueOf(grid.getYmax() - i * grid.getResolution()));
//			
//			for(int j = 0; j < grid.getNumCols(i); j++) {
//				layer1.write("\t");
//				Double centroid = grid.getValue(i, j).getCentroidAccessibility();
//				if(centroid != null)
//					layer1.write(String.valueOf(centroid));
//				else
//					layer1.write("NA");
//				
//				layer2.write("\t");
//				Double interpolation = grid.getValue(i, j).getMeanAccessibility();
//				if(interpolation != null)
//					layer2.write(String.valueOf(interpolation));
//				else
//					layer2.write("NA");
//								
//				layer3.write("\t");
//				Double derivation = grid.getValue(i, j).getAccessibilityDerivation();
//				if(derivation != null)
//					layer3.write(String.valueOf(derivation));
//				else
//					layer3.write("NA");
//			}
//			layer1.newLine();
//			layer2.newLine();
//			layer3.newLine();
//		}
//		layer1.flush();
//		layer1.close();
//		layer2.flush();
//		layer2.close();
//		layer3.flush();
//		layer3.close();
//	}
	}	
	
}
