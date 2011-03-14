package playground.wrashid.lib.tools.events.network.obj;

import org.matsim.core.utils.geometry.CoordImpl;

import playground.wrashid.lib.tools.network.obj.RectangularArea;
import junit.framework.TestCase;

public class RectangularAreaTest extends TestCase {

	public void testBasic(){
		RectangularArea rectangleOne = new RectangularArea(new CoordImpl(1.0,1.0),new CoordImpl(2.0,2.0));
	
		assertTrue(rectangleOne.isInArea(new CoordImpl(1.5, 1.5)));
		
		assertFalse(rectangleOne.isInArea(new CoordImpl(-1.0, 1.5)));
		assertFalse(rectangleOne.isInArea(new CoordImpl(2.0, 1.5)));
		assertFalse(rectangleOne.isInArea(new CoordImpl(1.5, 2.5)));
		assertFalse(rectangleOne.isInArea(new CoordImpl(2.0, 0.5)));
		assertFalse(rectangleOne.isInArea(new CoordImpl(2.5, 2.5)));
		
		RectangularArea rectangleTwo = new RectangularArea(new CoordImpl(0.0,1.0),new CoordImpl(1.0,0.0));
		assertTrue(rectangleTwo.isInArea(new CoordImpl(0.5, 0.5)));
		assertFalse(rectangleTwo.isInArea(new CoordImpl(1.5, 1.5)));
	}
	
}
