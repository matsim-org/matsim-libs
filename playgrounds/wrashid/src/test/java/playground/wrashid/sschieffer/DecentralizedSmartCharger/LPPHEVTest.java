package playground.wrashid.sschieffer.DecentralizedSmartCharger;

import junit.framework.TestCase;
import lpsolve.LpSolveException;

public class LPPHEVTest extends TestCase{
	
	
	private Schedule s;
	LPPHEV lp= new LPPHEV();
	
	public LPPHEVTest() {		
		
	}
		
	
	
	/*
	 * calcEnergyUsageFromCombustionEngine
	 * check energy use from engine/battery for one case
	 */
	public void testRunLPPHEV() throws LpSolveException{
		
		double [] solution = setUpTestLPPHEV();
		lp.setSchedule(s);
		double eEngine= lp.calcEnergyUsageFromCombustionEngine(solution);
		assertEquals(eEngine, 15000.0);
		// * parking times-->  the required charging times is adjusted;
		// dependent on charing speed.. currently 3500 but should be flexible in future..
		// so not implemented right now test for this
		
		
		// * driving times --> consumption from engine for an interval is reduced 
		DrivingInterval d= (DrivingInterval) s.timesInSchedule.get(1);
		
		assertEquals(15000.0, d.getExtraConsumption());
		assertEquals(35000.0, d.getConsumption());
		
	}
	
	
	
	
	public double [] setUpTestLPPHEV(){
		
		s= new Schedule();
		
		s.addTimeInterval(new ParkingInterval(0, 10, null));
		
		s.addTimeInterval(new DrivingInterval(10, 20, 50000));
		
		s.addTimeInterval(new ParkingInterval(20, 30, null));
		s.addTimeInterval(new ParkingInterval(30, 40, null));
		
		for (int i=0; i<s.getNumberOfEntries(); i++){
			if (s.timesInSchedule.get(i).isParking()){
				
				ParkingInterval p= (ParkingInterval) s.timesInSchedule.get(i);
				p.setRequiredChargingDuration(0.0);
			}
		}
		
		double[] solution={0.0, 10.0, 1.0, 0.0, 0.0};
		return solution;
	}
	
	
}
