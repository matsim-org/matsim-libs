package playground.pieter.network;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;

public class NetworkCapacityChanger {

	void run(final String[] args) {
		Scenario scenario;
		MatsimRandom.reset(123);
		scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(args[0]);

		for (Link l : scenario.getNetwork().getLinks().values()) {
			// only apply rules to car links
			l.setLength(Math.max(l.getLength()*1000,getEuclidean(l.getFromNode().getCoord(),l.getToNode().getCoord())));
			l.setFreespeed(Math.max(17,Math.min(32,8*l.getCapacity()/Math.max(1, l.getNumberOfLanes())/l.getLength()/7.5)));


		}


		new NetworkWriter(scenario.getNetwork()).write(args[1]);
	}

	private double getEuclidean(Coord coord, Coord coord2) {
		double dx = coord2.getX()-coord.getX();
		double dy = coord2.getY()-coord.getY();
		double dist = Math.sqrt(Math.pow(dx, 2)+Math.pow(dy, 2));
		return 0;
	}

	/**
	 * @param args
	 *            - An array of String, Double, String:
	 *            <ol>
	 *            <li>The name of the network;</li>
	 *            <li>the flow capacity per lane;</li>
	 *            <li>and the name of the output network.</li>
	 *            </ol>
	 */
	public static void main(final String[] args) {
		new NetworkCapacityChanger().run(args);
	}

}
