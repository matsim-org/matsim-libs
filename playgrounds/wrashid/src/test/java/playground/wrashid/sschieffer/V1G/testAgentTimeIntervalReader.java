package playground.wrashid.sschieffer.V1G;

import org.matsim.api.core.v01.Id;
import org.matsim.testcases.MatsimTestCase;
import org.objenesis.instantiator.basic.NewInstanceInstantiator;


public class testAgentTimeIntervalReader extends MatsimTestCase{
	
	
	AgentTimeIntervalReader myAgentReader= new AgentTimeIntervalReader();
	
	Schedule s1 = new Schedule();
	
	public void initTestCheckTimesWithHubSuubAndOptimalTimes(){
		ParkingInterval p1= new ParkingInterval(0, 1000, null);
		DrivingInterval d1= new DrivingInterval(1000, 4000, 10);
		ParkingInterval p2= new ParkingInterval(4000, 5000, null);
		
		s1.addTimeInterval(p1);
		s1.addTimeInterval(d1);
		s1.addTimeInterval(p2);		
		
	}
	
	
	
	//public Schedule checkTimesWithHubSubAndOptimalTimes(Id id, Schedule schedule){
}
