package org.matsim.contrib.accessibility.grid;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.accessibility.gis.SpatialGrid;
import org.matsim.contrib.matrixbasedptrouter.utils.CreateTestNetwork;
import org.matsim.contrib.matrixbasedptrouter.utils.BoundingBox;
import org.matsim.testcases.MatsimTestCase;

public class SpatialGridTest extends MatsimTestCase{
	
	private double cellSize = 10.;

	@Test
	public void testSpatialGrid() {
		
		// get network
		Network network = CreateTestNetwork.createTestNetwork();
		
		// get boundaries of network, i.e. x and y coordinates
		BoundingBox nbb = BoundingBox.createBoundingBox(network);		
		
		// create spatial grid		
		SpatialGrid testGrid = new SpatialGrid(nbb.getBoundingBox(), cellSize);
		
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
