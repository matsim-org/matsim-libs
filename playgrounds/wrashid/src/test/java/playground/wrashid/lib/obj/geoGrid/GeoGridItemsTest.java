package playground.wrashid.lib.obj.geoGrid;

import org.matsim.core.utils.geometry.CoordImpl;

import junit.framework.TestCase;

public class GeoGridItemsTest extends TestCase {

	public void testBasic(){
		GeoGridItems geoGridList=new GeoGridItems<Integer>(10);
		geoGridList.addElement(1.0, new CoordImpl(10.0, 10.0));
		
		assertEquals(0, geoGridList.getElementsWithinDistance(new CoordImpl(0,0), 0).size());
		assertEquals(1, geoGridList.getElementsWithinDistance(new CoordImpl(0,0), 1).size());
		assertEquals(1, geoGridList.getElementsWithinDistance(new CoordImpl(0,0), 2).size());
	}
	
	public void testBasic2(){
		GeoGridItems geoGridList=new GeoGridItems<Integer>(10);
		geoGridList.addElement(1.0, new CoordImpl(9.0, 9.0));
		geoGridList.addElement(1.0, new CoordImpl(18.0, 18.0));
		geoGridList.addElement(1.0, new CoordImpl(27.0, 27.0));
		geoGridList.addElement(1.0, new CoordImpl(36.0, 36.0));
		geoGridList.addElement(1.0, new CoordImpl(45.0, 45.0));
		
		assertEquals(1, geoGridList.getElementsWithinDistance(new CoordImpl(0,0), 0).size());
		assertEquals(2, geoGridList.getElementsWithinDistance(new CoordImpl(0,0), 1).size());
		assertEquals(3, geoGridList.getElementsWithinDistance(new CoordImpl(0,0), 2).size());
		assertEquals(4, geoGridList.getElementsWithinDistance(new CoordImpl(0,0), 3).size());
		assertEquals(5, geoGridList.getElementsWithinDistance(new CoordImpl(0,0), 4).size());
	}
	
}
