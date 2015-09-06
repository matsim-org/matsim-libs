package playground.wrashid.lib.obj.geoGrid;

import org.matsim.api.core.v01.Coord;

import junit.framework.TestCase;

public class GeoGridItemsTest extends TestCase {

	public void testBasic(){
		GeoGridItems geoGridList=new GeoGridItems<Integer>(10);
		geoGridList.addElement(1.0, new Coord(10.0, 10.0));

		assertEquals(0, geoGridList.getElementsWithinDistance(new Coord((double) 0, (double) 0), 0).size());
		assertEquals(1, geoGridList.getElementsWithinDistance(new Coord((double) 0, (double) 0), 1).size());
		assertEquals(1, geoGridList.getElementsWithinDistance(new Coord((double) 0, (double) 0), 2).size());
	}
	
	public void testBasic2(){
		GeoGridItems geoGridList=new GeoGridItems<Integer>(10);
		geoGridList.addElement(1.0, new Coord(9.0, 9.0));
		geoGridList.addElement(1.0, new Coord(18.0, 18.0));
		geoGridList.addElement(1.0, new Coord(27.0, 27.0));
		geoGridList.addElement(1.0, new Coord(36.0, 36.0));
		geoGridList.addElement(1.0, new Coord(45.0, 45.0));

		assertEquals(1, geoGridList.getElementsWithinDistance(new Coord((double) 0, (double) 0), 0).size());
		assertEquals(2, geoGridList.getElementsWithinDistance(new Coord((double) 0, (double) 0), 1).size());
		assertEquals(3, geoGridList.getElementsWithinDistance(new Coord((double) 0, (double) 0), 2).size());
		assertEquals(4, geoGridList.getElementsWithinDistance(new Coord((double) 0, (double) 0), 3).size());
		assertEquals(5, geoGridList.getElementsWithinDistance(new Coord((double) 0, (double) 0), 4).size());
	}
	
}
