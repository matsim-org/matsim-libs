package playground.wrashid.lib.tools.events.network.obj;

import java.util.LinkedList;

import junit.framework.TestCase;

import org.matsim.api.core.v01.Coord;

import playground.wrashid.lib.tools.network.obj.ComplexRectangularSelectionArea;
import playground.wrashid.lib.tools.network.obj.RectangularArea;

public class ComplexRectangularSelectionAreaTest extends TestCase {

	public void testBasic(){
		LinkedList<RectangularArea> includeInSection= new LinkedList<RectangularArea>();
		LinkedList<RectangularArea> excludeFromSelection = new LinkedList<RectangularArea>();

		includeInSection.add(new RectangularArea(new Coord(0.0, 0.0), new Coord(5.0, 5.0)));
		excludeFromSelection.add(new RectangularArea(new Coord(1.0, 1.0), new Coord(2.0, 2.0)));
		
		ComplexRectangularSelectionArea complexRectangularSelectionArea = new ComplexRectangularSelectionArea(includeInSection, excludeFromSelection);

		assertTrue(complexRectangularSelectionArea.isInArea(new Coord(3.0, 3.0)));
		assertFalse(complexRectangularSelectionArea.isInArea(new Coord(1.5, 1.5)));
		assertFalse(complexRectangularSelectionArea.isInArea(new Coord(7.0, 7.0)));
	}
	
}
