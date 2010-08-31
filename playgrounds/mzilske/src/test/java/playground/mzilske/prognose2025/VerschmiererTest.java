package playground.mzilske.prognose2025;

import junit.framework.Assert;

import org.junit.Test;
import org.matsim.core.utils.geometry.CoordImpl;

public class VerschmiererTest {
	
	@Test
	public void test() {
		Verschmierer verschmierer = new Verschmierer();
		verschmierer.prepare();
		CoordImpl notInCell = new CoordImpl(3.0, 4.0);
		Assert.assertSame(notInCell, verschmierer.shootIntoSameZoneOrLeaveInPlace(notInCell));
		CoordImpl inCell = new CoordImpl(4326122, 5756578);
		Assert.assertNotSame(inCell, verschmierer.shootIntoSameZoneOrLeaveInPlace(inCell));
	}

}
