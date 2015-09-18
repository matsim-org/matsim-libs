package playground.wrashid.lib.tools.events.network.obj;

import org.matsim.api.core.v01.Coord;

import playground.wrashid.lib.tools.network.obj.RectangularArea;
import junit.framework.TestCase;

public class RectangularAreaTest extends TestCase {

	public void testBasic(){
		RectangularArea rectangleOne = new RectangularArea(new Coord(1.0, 1.0), new Coord(2.0, 2.0));

		assertTrue(rectangleOne.isInArea(new Coord(1.5, 1.5)));

		final double x = -1.0;
		assertFalse(rectangleOne.isInArea(new Coord(x, 1.5)));
		assertTrue(rectangleOne.isInArea(new Coord(2.0, 1.5)));
		assertFalse(rectangleOne.isInArea(new Coord(1.5, 2.5)));
		assertFalse(rectangleOne.isInArea(new Coord(2.0, 0.5)));
		assertFalse(rectangleOne.isInArea(new Coord(2.5, 2.5)));

		RectangularArea rectangleTwo = new RectangularArea(new Coord(0.0, 1.0), new Coord(1.0, 0.0));
		assertTrue(rectangleTwo.isInArea(new Coord(0.5, 0.5)));
		assertFalse(rectangleTwo.isInArea(new Coord(1.5, 1.5)));
	}
	
}
