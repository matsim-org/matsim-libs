package playground.pieter.roadpricing;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.roadpricing.RoadPricingSchemeImpl;
import org.matsim.roadpricing.RoadPricingWriterXMLv1;

public class RoadPriceFromCapacity {
	private final Scenario sc;
	private final Network network;
	private final RoadPricingSchemeImpl rps;
	private final double startTime = Time.convertHHMMInteger(700);
	private final double endTime = Time.convertHHMMInteger(900);
	private final double roadCostPerMeter = 0.0025;
	private final double linkMinimumCapacity = 4000;

	private RoadPriceFromCapacity(String networkFileName) {
		sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader nwr = new MatsimNetworkReader(sc);
		nwr.readFile(networkFileName);
		rps = new RoadPricingSchemeImpl();
		network = sc.getNetwork();
	}
	
	void run(String rpsFileName){
		for(Link link:network.getLinks().values()){
			if(link.getCapacity()>= linkMinimumCapacity){
				rps.addLinkCost(link.getId(), startTime, endTime, link.getLength()*roadCostPerMeter);
			}
		}
		rps.setType(RoadPricingSchemeImpl.TOLL_TYPE_LINK);
		rps.setName("Prospective toll scenario for Zurich");
		RoadPricingWriterXMLv1 rpw = new RoadPricingWriterXMLv1(rps);
		rpw.writeFile(rpsFileName);
	}

	public static void main(String[] args){
		RoadPriceFromCapacity rpfc = new RoadPriceFromCapacity(args[0]);
		rpfc.run(args[1]);
	}
}
