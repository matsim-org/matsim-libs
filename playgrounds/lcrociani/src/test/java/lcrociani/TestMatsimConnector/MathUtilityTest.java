package lcrociani.TestMatsimConnector;

import static org.junit.Assert.assertEquals;
import matsimConnector.utility.MathUtility;

import org.junit.Test;

import pedCA.environment.grid.GridPoint;

public class MathUtilityTest {
	
	@Test
	public void testRotate(){
		GridPoint pos = new GridPoint(5,1);
		String check = pos.toString();
		double xc = 0;
		double yc = 0;
		MathUtility.rotate(pos, 90, xc, yc);
		System.out.println("["+pos.getX()+","+pos.getY()+"]");
		MathUtility.rotate(pos, 90, xc, yc);
		System.out.println("["+pos.getX()+","+pos.getY()+"]");
		MathUtility.rotate(pos, 90, xc, yc);
		System.out.println("["+pos.getX()+","+pos.getY()+"]");
		MathUtility.rotate(pos, 90, xc, yc);
		System.out.println("["+pos.getX()+","+pos.getY()+"]");
		assertEquals(check,pos.toString());
	}
}
