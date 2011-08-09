package vrp.algorithms.ruinAndRecreate.basics;

import vrp.VRPTestCase;

public class BestTourBuilderTest extends VRPTestCase{
	
	BestTourBuilder tourBuilder;
	
	public void setUp(){
		init();
		tourBuilder = new BestTourBuilder();
		tourBuilder.setCosts(costs);
		tourBuilder.setTourActivityStatusUpdater(new TourActivityStatusUpdaterImpl(costs));
	}
	
	

}
