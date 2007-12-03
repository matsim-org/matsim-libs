/**
 * 
 */
package org.matsim.utils.geometry.transformations;

import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.geometry.CoordI;
import org.matsim.utils.geometry.CoordinateTransformationI;
import org.matsim.utils.geometry.shared.Coord;

/**
 * @author laemmel
 *
 */
public class GeotoolsTransformationTest extends MatsimTestCase {
	
	public void testTransform(){
		String toCRS = "WGS84";
		String fromCRS = "WGS84_UTM47S";
		
		double x = 638748.9000000004;
		double y = 9916839.69;
		
		double targetX = 100.24690901110905;
		double targetY = -0.7521976363533539;
		double delta = 1e-16;
		
		CoordI coordWGS84UTM47S = new Coord(x,y);
		
		CoordinateTransformationI transform = new GeotoolsTransformation(fromCRS,toCRS);
		CoordI coordWGS84 = transform.transform(coordWGS84UTM47S);
		double xWGS84 = coordWGS84.getX();
		double yWGS84 = coordWGS84.getY();
		

		junit.framework.Assert.assertEquals(targetX, xWGS84, delta);
		junit.framework.Assert.assertEquals(targetY, yWGS84, delta);
		
	}

}
