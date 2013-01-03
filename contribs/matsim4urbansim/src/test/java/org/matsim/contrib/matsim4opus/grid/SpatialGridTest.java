package org.matsim.contrib.matsim4opus.grid;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.matsim4opus.gis.GridUtils;
import org.matsim.contrib.matsim4opus.gis.SpatialGrid;
import org.matsim.contrib.matsim4opus.gis.ZoneLayer;
import org.matsim.contrib.matsim4opus.utils.CreateTestNetwork;
import org.matsim.contrib.matsim4opus.utils.network.NetworkBoundaryBox;
import org.matsim.core.network.NetworkImpl;
import org.matsim.testcases.MatsimTestCase;

public class SpatialGridTest extends MatsimTestCase{
	
	private double cellSize = 10.;

	@Test
	public void testSpatialGrid() {
		
		// get network
		NetworkImpl network = CreateTestNetwork.createTestNetwork();
		
		// get boundaries of network, i.e. x and y coordinates
		NetworkBoundaryBox nbb = new NetworkBoundaryBox();
		nbb.setDefaultBoundaryBox(network);		
		
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

	@Test
	public void testCellCentroids() {
		
		// get network
		NetworkImpl network = CreateTestNetwork.createTestNetwork();
		
		// get boundaries of network, i.e. x and y coordinates
		NetworkBoundaryBox nbb = new NetworkBoundaryBox();
		nbb.setDefaultBoundaryBox(network);

		ZoneLayer<Id> measuringPoints = GridUtils.createGridLayerByGridSizeByNetwork(cellSize, nbb.getBoundingBox());
	}
	
}
