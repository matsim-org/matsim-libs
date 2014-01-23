package org.matsim.contrib.matsim4urbansim.grid;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.contrib.accessibility.gis.SpatialGrid;
import org.matsim.contrib.matrixbasedptrouter.utils.CreateTestNetwork;
import org.matsim.contrib.matrixbasedptrouter.utils.MyBoundingBox;
import org.matsim.core.network.NetworkImpl;
import org.matsim.testcases.MatsimTestCase;

public class SpatialGridTest extends MatsimTestCase{
	
	private double cellSize = 10.;

	@Test
	public void testSpatialGrid() {
		
		// get network
		NetworkImpl network = CreateTestNetwork.createTestNetwork();
		
		// get boundaries of network, i.e. x and y coordinates
		MyBoundingBox nbb = new MyBoundingBox();
		nbb.setDefaultBoundaryBox(network);		
		
		// create spatial grid		
		SpatialGrid testGrid = new SpatialGrid(cellSize, nbb.getBoundingBox());
		
		// get number of rows
		int rows = testGrid.getNumRows();
		double numOfExpectedRows = ((nbb.getYMax() - nbb.getYMin()) / cellSize) + 1;
		Assert.assertTrue(rows == numOfExpectedRows);
		
		// get number of columns
		int cols = testGrid.getNumCols(0);
		double numOfExpectedCols = ((nbb.getXMax() - nbb.getXMin()) / cellSize) + 1;
		Assert.assertTrue(cols == numOfExpectedCols);
	}	
}
