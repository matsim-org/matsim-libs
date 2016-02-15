package playground.tschlenther.Cottbus.Network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;

import playground.jbischoff.lsacvs2kml.LSA;

public class Converter {

	public static void main(String[] args){
		
//		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
//		NetworkImpl network = (NetworkImpl) scenario.getNetwork();
//		CoordinateTransformation ct = 
//				TransformationFactory.getCoordinateTransformation(TransformationFactory.GK4,TransformationFactory.WGS84);
//		OsmNetworkReader reader = new OsmNetworkReader(scenario.getNetwork(), ct);
//		reader.parse("C:/Users/Tille/WORK/Cottbus/Cottbus-pt/Demand_input/network_pt_cap60.xml");
//		
//		NetworkWriter writer = new NetworkWriter(scenario.getNetwork());
//		writer.write("C:/Users/Tille/WORK/Cottbus/Cottbus-pt/Demand_input/transformiert.xml");
//				
		
		
		String input = "C:/Users/Tille/WORK/Cottbus/Cottbus-pt/Demand_input/bearbeitetWGS84.xml";
		String output = "C:/Users/Tille/WORK/Cottbus/Cottbus-pt/Demand_input/RUECKtransformiert.xml";
		
		
		/* Read the network. */
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).parse(input);
		
		/* Transform each node. */
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84,"EPSG:25833");
//		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84,TransformationFactory.DHDN_GK4);

//		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("EPSG:25833",TransformationFactory.WGS84);
//		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.DHDN_GK4,TransformationFactory.WGS84);
		
		for(Node node : scenario.getNetwork().getNodes().values()){
			((NodeImpl)node).setCoord(ct.transform(node.getCoord()));
		}
		
		/* Write the resulting network. */
		new NetworkWriter(scenario.getNetwork()).write(output);
		
	}
		
		
	
	
}
