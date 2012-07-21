package playground.sergioo.bestTravelTimeRouter;


import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
/*import org.matsim.core.router.BestTravelTimePathCalculatorImpl;
import org.matsim.core.router.util.BestTravelTimePathCalculator;
import org.matsim.core.router.util.BestTravelTimePathCalculator.Path;*/
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;

public class TestBestTravelTimeRouter {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).parse("./data/networks/singapart.xml");
		TravelTime tt = new TravelTime() {
			@Override
			public double getLinkTravelTime(Link link, double time) {
				return link.getLength();
			}
		};
		//BestTravelTimePathCalculator bttpc = new BestTravelTimePathCalculatorImpl(scenario.getNetwork(), tt);
		Node o = scenario.getNetwork().getNodes().get(new IdImpl(1380010516));
		Node d = scenario.getNetwork().getNodes().get(new IdImpl(1380021814));
		long time = System.currentTimeMillis();
		//Path path = bttpc.calcBestTravelTimePath(o, d, 3, 0);
		System.out.println(System.currentTimeMillis()-time);
		//System.out.println(path);
		//System.out.println(path.travelTime);
	}

	private static boolean isIn(Node node) {
		double x=node.getCoord().getX(), y=node.getCoord().getY(); 
		return x>103.8372 && x<103.8544 && y>1.2741 && y<1.2867;
	}

}
