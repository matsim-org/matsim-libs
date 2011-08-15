package vrp.algorithms.ruinAndRecreate.ruin;

import java.util.ArrayList;
import java.util.List;

import org.matsim.core.gbl.MatsimRandom;

import vrp.VRPTestCase;
import vrp.algorithms.ruinAndRecreate.api.TourAgent;
import vrp.algorithms.ruinAndRecreate.basics.Solution;
import vrp.api.Customer;
import vrp.api.VRP;
import vrp.basics.TourActivity;

public class RadialRuinTest extends VRPTestCase{
	
	VRP vrp;
	
	Solution solution;
	
	RadialRuin radialRuin;
	
	public void setUp(){
		init();
		vrp = getVRP();
		solution = getInitialSolution(vrp);
		radialRuin = new RadialRuin(vrp);
		radialRuin.setFractionOfAllNodes(0.5);
		MatsimRandom.reset();
		/*
		 * fraction=0.5
		 * picks 0,10
		 * rem 0,10
		 * rem 1,5
		 */
	}
	
	public void testIniSolution(){
		assertEquals(3, solution.getTourAgents().size());
	}
	
	public void testSizeOfRuinedSolution(){
		radialRuin.run(solution);
		assertEquals(1, solution.getTourAgents().size());
	}
	
	public void testRemainingSolution(){
		radialRuin.run(solution);
		TourAgent tourAgent = solution.getTourAgents().iterator().next();
		List<TourActivity> acts = new ArrayList<TourActivity>(tourAgent.getTourActivities());
		assertEquals(3, tourAgent.getTourActivities().size());
		assertEquals(customerMap.get(makeId(10,10)),acts.get(1).getCustomer());
	}
	
	public void testRuinedSolutionWithoutRelation(){
		removeRelations();
		radialRuin.run(solution);
		assertEquals(2, solution.getTourAgents().size());
	}

	private void removeRelations() {
		for(Customer c : customerMap.values()){
			if(c.hasRelation()){
				c.removeRelation();
			}
		}
	}
	
	public void testRemainingSolutionWithoutRelation(){
		removeRelations();
		radialRuin.run(solution);
		List<TourAgent> agents = new ArrayList<TourAgent>(solution.getTourAgents());
		List<TourActivity> acts = new ArrayList<TourActivity>(agents.get(1).getTourActivities());
		assertEquals(3, agents.get(1).getTourActivities().size());
		assertEquals(customerMap.get(makeId(1,4)),acts.get(1).getCustomer());
	}
	
	public void testIncreasingFraction2BeRemovedSolutionWithoutRelation(){
		removeRelations();
		radialRuin.setFractionOfAllNodes(0.75);
		radialRuin.run(solution);
		assertEquals(1, solution.getTourAgents().size());
		TourAgent tourAgent = solution.getTourAgents().iterator().next();
		List<TourActivity> acts = new ArrayList<TourActivity>(tourAgent.getTourActivities());
		assertEquals(3, tourAgent.getTourActivities().size());
		assertEquals(customerMap.get(makeId(10,10)),acts.get(1).getCustomer());
	}
	
	public void testRuinOnEmptySolution(){
		assertTrue(false);
	}

}
