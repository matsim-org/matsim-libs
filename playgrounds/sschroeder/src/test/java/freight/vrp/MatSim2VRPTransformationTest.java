package freight.vrp;

import junit.framework.TestCase;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;

public class MatSim2VRPTransformationTest extends TestCase{
	
	MatSim2VRPTransformation matsim2vrp;
	
	public void setUp(){
		matsim2vrp = new MatSim2VRPTransformation(new Locations(){

			@Override
			public Coord getCoord(Id id) {
				// TODO Auto-generated method stub
				return null;
			}
			
		});
	}
	
	public void testAddAndCreateCustomer(){
		matsim2vrp.addAndCreateCustomer(makeId("myCustomer"), makeId("myCustomersLocation"), 10, 0.0, Double.MAX_VALUE, 5);
	}

	private Id makeId(String string) {
		return new IdImpl(string);
	}

}
