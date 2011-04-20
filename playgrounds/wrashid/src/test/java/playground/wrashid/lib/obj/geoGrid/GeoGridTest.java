package playground.wrashid.lib.obj.geoGrid;

import org.matsim.core.utils.geometry.CoordImpl;
import org.opengis.coverage.grid.GridCell;

import playground.wrashid.lib.obj.Coord;

import junit.framework.TestCase;

public class GeoGridTest extends TestCase{

	public void testSingleDataItem(){
		GeoGrid geoGrid=new GeoGrid(10);
		CoordImpl sampleCoord = new CoordImpl(55,55);
		GridDataItem gridDataItem=new GridDataItem(5.0, 1.0, sampleCoord);
		geoGrid.addGridInformation(gridDataItem);
		geoGrid.markDataCollectionPhaseAsFishished();
		
		assertEquals(5.0, geoGrid.getValue(sampleCoord));
		assertEquals(5.0, geoGrid.getValue(new CoordImpl(50.0,50.0)));
		assertEquals(5.0, geoGrid.getValue(new CoordImpl(100.0,100.0)));
	}
	
	public void testTwoDataItemsAndWeights(){
		GeoGrid geoGrid=new GeoGrid(10);
		CoordImpl firstCoord = new CoordImpl(55,55);
		CoordImpl secondCoord = new CoordImpl(56,56);
		geoGrid.addGridInformation(new GridDataItem(2.0, 1.0, firstCoord));
		geoGrid.addGridInformation(new GridDataItem(10.0, 3.0, secondCoord));
		geoGrid.markDataCollectionPhaseAsFishished();
		
		assertEquals(8.0, geoGrid.getValue(firstCoord));
		assertEquals(8.0, geoGrid.getValue(secondCoord));
	}
	
	public void testGridBordersAndMissingValues(){
		GeoGrid geoGrid=new GeoGrid(10);
		geoGrid.addGridInformation(new GridDataItem(1.0, 1.0, new CoordImpl(5.0,5.0)));
		geoGrid.addGridInformation(new GridDataItem(2.0, 1.0, new CoordImpl(15.0,5.0)));
		geoGrid.addGridInformation(new GridDataItem(3.0, 1.0, new CoordImpl(15.0,15.0)));
		geoGrid.addGridInformation(new GridDataItem(4.0, 1.0, new CoordImpl(5.0,15.0)));

		geoGrid.markDataCollectionPhaseAsFishished();
		
		assertEquals(1.0, geoGrid.getValue(new CoordImpl(5.0,5.0)));
		assertEquals(2.0, geoGrid.getValue(new CoordImpl(10.0,5.0)));
		assertEquals(3.0, geoGrid.getValue(new CoordImpl(15.0,10.0)));
		assertEquals(3.0, geoGrid.getValue(new CoordImpl(10.0,10.0)));
		assertEquals(3.0, geoGrid.getValue(new CoordImpl(10.0,15.0)));
		assertEquals(4.0, geoGrid.getValue(new CoordImpl(5.0,10.0)));
		assertEquals(2.5, geoGrid.getValue(new CoordImpl(25.0,5.0)));
	}
	
}
