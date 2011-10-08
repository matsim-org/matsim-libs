package freight.vrp;

import junit.framework.TestCase;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;

import playground.mzilske.freight.carrier.CarrierShipment;
import playground.mzilske.freight.carrier.CarrierUtils;
import vrp.api.Constraints;
import vrp.api.Costs;
import vrp.api.Node;
import vrp.basics.Tour;
import vrp.basics.Vehicle;
import vrp.basics.VehicleType;

/**
 * VrpBuilder should build the vrp problem required for the algorithms to solve it. 
 * 1. Depots must be determined, i.e. where vehicles come from and plan tours from
 * 2. Vehicle types at each depot must be determined
 * 3. Customers to serve from the depots must be determined
 * 4. Relations among customers must be determined
 * 5. Constraints like time-constraints must be determined
 * 6. Costs and cost-functions must be determined
 * @author stefan
 *
 */

public class MatSimSingleDepotVRPBuilderTest extends TestCase{
	
	MatSimSingleDepotVRPBuilder builder;
	
	MatSim2VRPTransformation vrpTransformation;
	
	Costs costs;
	
	Constraints constraints;
	
	public void setUp(){
		//eigentlich sollte das auch in den VRPBuilder, hier wird schließlich das Problem zusammen gebastelt.
		//locations must contain all depot and customer locations
		Locations locations = new Locations(){

			@Override
			public Coord getCoord(Id id) {
				return null;
			}
			
		};
		vrpTransformation = new MatSim2VRPTransformation(locations);
		builder = new MatSimSingleDepotVRPBuilder(makeId("depot"), makeId("depotLocation"), makeVehicleType(20), vrpTransformation);
		costs = new Costs(){

			@Override
			public Double getCost(Node from, Node to) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Double getDistance(Node from, Node to) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Double getTime(Node from, Node to) {
				// TODO Auto-generated method stub
				return null;
			}
			
		};
		constraints = new Constraints() {
			
			@Override
			public boolean judge(Tour tour, Vehicle vehicle) {
				// TODO Auto-generated method stub
				return false;
			}
			
		};
	}
	
	public void testNoDepotsAssigned(){
		try{
			builder.buildVRP();
			assertTrue(false);
		}
		catch(IllegalStateException e){
			assertTrue(true);
		}
	}
	
	public void testAssignDepotAndVehicleTypeButNoConstraintsNoCosts(){
		try{
			builder.buildVRP();
			assertTrue(false);
		}
		catch(IllegalStateException e){
			System.out.println(e.toString());
			assertTrue(true);
		}
	}
	
	public void testAssignDepotVehicleTypeConstraintsButNoCosts(){
		builder.setConstraints(constraints);
		try{
			builder.buildVRP();
			assertTrue(false);
		}
		catch(IllegalStateException e){
			System.out.println(e.toString());
			assertTrue(true);
		}
	}
	
	public void testAssignDepotVehicleTypeConstraintsCosts(){
		builder.setConstraints(constraints);
		builder.setCosts(costs);
		try{
			builder.buildVRP();
			assertTrue(true);
		}
		catch(IllegalStateException e){
			System.out.println(e.toString());
			assertTrue(false);
		}
	}
	
	public void testAddingAShipmentToDepot(){
		builder.setConstraints(constraints);
		builder.setCosts(costs);
		CarrierShipment shipment = createShipment("customerLocation","depotLocation",20);
		try{
			builder.addPickupForDepotShipment(shipment);
			assertTrue(true);
		}
		catch(IllegalStateException e){
			System.out.println(e.toString());
			assertTrue(false);
		}
	}
	
	public void testAddingAShipmentToDepotEvenItIsNoSuchRelation(){
		builder.setConstraints(constraints);
		builder.setCosts(costs);
		CarrierShipment shipment = createShipment("customerLocation","fooLocation",20);
		try{
			builder.addPickupForDepotShipment(shipment);
			assertTrue(false);
		}
		catch(IllegalStateException e){
			System.out.println(e.toString());
			assertTrue(true);
		}
	}
	
	public void testAddingADepotToCustomerShipment(){
		builder.setConstraints(constraints);
		builder.setCosts(costs);
		CarrierShipment shipment = createShipment("depotLocation","customerLocation",20);
		try{
			builder.addDeliveryFromDepotShipment(shipment);
			assertTrue(true);
		}
		catch(IllegalStateException e){
			System.out.println(e.toString());
			assertTrue(false);
		}
	}
	
	public void testAddingADepotToCustomerShipmentEventItIsNoSuchRelation(){
		builder.setConstraints(constraints);
		builder.setCosts(costs);
		CarrierShipment shipment = createShipment("fooLocation","customerLocation",20);
		try{
			builder.addDeliveryFromDepotShipment(shipment);
			assertTrue(false);
		}
		catch(IllegalStateException e){
			System.out.println(e.toString());
			assertTrue(true);
		}
	}
	
	public void testAddingEnRoutePickupAndDelivery(){
		builder.setConstraints(constraints);
		builder.setCosts(costs);
		CarrierShipment shipment = createShipment("depotLocation","customerLocation",20);
		try{
			builder.addEnRoutePickupAndDeliveryShipment(shipment);
			assertTrue(true);
		}
		catch(IllegalStateException e){
			System.out.println(e.toString());
			assertTrue(false);
		}
	}
	
	private CarrierShipment createShipment(String from, String to, int size) {
		return CarrierUtils.createShipment(makeId(from), makeId(to), size, 0.0, Double.MAX_VALUE, 0.0, Double.MAX_VALUE);
	}

	private VehicleType makeVehicleType(int i) {
		return new VehicleType(i);
	}

	private Id makeId(String string) {
		return new IdImpl(string);
	}
}
