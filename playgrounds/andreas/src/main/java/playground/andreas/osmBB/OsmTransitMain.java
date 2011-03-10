package playground.andreas.osmBB;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.core.api.experimental.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.vehicles.VehicleWriterV1;
import org.openstreetmap.osmosis.core.filter.common.IdTrackerType;
import org.openstreetmap.osmosis.core.xml.common.CompressionMethod;

import playground.mzilske.osm.JOSMTolerantFastXMLReader;
import playground.mzilske.osm.NetworkSink;
import playground.mzilske.osm.TransitNetworkSink;

/**
 * 
 * @author aneumann, mzilske
 *
 */
public class OsmTransitMain {
	
	private final static Logger log = Logger.getLogger(OsmTransitMain.class);
	
	String inFile;
	String fromCoordSystem;
	String toCoordSystem;
	String networkOutFile;
	String transitScheduleOutFile;
	String vehiclesOutFile;
	
	public OsmTransitMain(String inFile, String fromCoordSystem, String toCoordSystem, String networkOutFile, String transitScheduleOutFile, String vehiclesOutFile){
		this.inFile = inFile;
		this.fromCoordSystem = fromCoordSystem;
		this.toCoordSystem = toCoordSystem;
		this.networkOutFile = networkOutFile;
		this.transitScheduleOutFile = transitScheduleOutFile;
		this.vehiclesOutFile = vehiclesOutFile;
	}
	
	public static void main(String[] args) throws IOException {
		new OsmTransitMain("/Users/michaelzilske/Desktop/wurst/neu.osm", TransformationFactory.WGS84, TransformationFactory.DHDN_GK4, "/Users/michaelzilske/Desktop/wurst/net.osm", "/Users/michaelzilske/Desktop/wurst/transit.osm", "vehiclesOutFile").convertOsm2Matsim();
	}
	
	public void convertOsm2Matsim(){
		convertOsm2Matsim(null);
	}
		
	public void convertOsm2Matsim(String[] transitFilter){
		
		log.info("Start...");		
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().scenario().setUseTransit(true);
		scenario.getConfig().scenario().setUseVehicles(true);
		JOSMTolerantFastXMLReader reader = new JOSMTolerantFastXMLReader(new File(inFile), false, CompressionMethod.None);		

		CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(this.fromCoordSystem, this.toCoordSystem);
		NetworkSink networkGenerator = new NetworkSink(scenario.getNetwork(), coordinateTransformation);
		
		// Anmerkung trunk, primary und secondary sollten in Bln als ein Typ behandelt werden
		
		// Autobahn
		networkGenerator.setHighwayDefaults(1, "motorway",      3,  100.0/3.6, 1.2, 2000, true); // 70
		networkGenerator.setHighwayDefaults(1, "motorway_link", 2,  60.0/3.6, 1.2, 1500, true); // 60
		// Pseudoautobahn
		networkGenerator.setHighwayDefaults(2, "trunk",         2,  50.0/3.6, 0.75, 1000, false); // 45
		networkGenerator.setHighwayDefaults(2, "trunk_link",    1,  50.0/3.6, 0.5, 1000, false); // 40
		// Durchgangsstrassen
		networkGenerator.setHighwayDefaults(3, "primary",       2,  50.0/3.6, 0.75, 1000, false); // 35
		networkGenerator.setHighwayDefaults(3, "primary_link",  1,  50.0/3.6, 0.5, 1000, false); // 30
		
		// Hauptstrassen
		networkGenerator.setHighwayDefaults(4, "secondary",     1,  50.0/3.6, 0.5, 1000, false); // 30
		// Weitere Hauptstrassen
		networkGenerator.setHighwayDefaults(5, "tertiary",      1,  30.0/3.6, 0.8,  600, false); // 25 
		// bis hier ca wip
		
		// Nebenstrassen
		networkGenerator.setHighwayDefaults(6, "minor",         1,  30.0/3.6, 0.8,  600, false); // nix
		// Alles Moegliche, vor allem Nebenstrassen auf dem Land, meist keine 30er Zone 
		networkGenerator.setHighwayDefaults(6, "unclassified",  1,  30.0/3.6, 0.8,  600, false);
		// Nebenstrassen, meist 30er Zone
		networkGenerator.setHighwayDefaults(6, "residential",   1,  30.0/3.6, 0.6,  600, false);
		// Spielstrassen
		networkGenerator.setHighwayDefaults(6, "living_street", 1,  15.0/3.6, 1.0,  300, false);
		
		log.info("Reading " + this.inFile);
		TransitNetworkSink transitNetworkSink = new TransitNetworkSink(scenario.getNetwork(), scenario.getTransitSchedule(), coordinateTransformation, IdTrackerType.BitSet);
		transitNetworkSink.setTransitModes(transitFilter);
		reader.setSink(networkGenerator);
		networkGenerator.setSink(transitNetworkSink);
		reader.run();
		log.info("Writing network to " + this.networkOutFile);
		new NetworkWriter(scenario.getNetwork()).write(this.networkOutFile);
		try {
			log.info("Writing transit schedule to " + this.transitScheduleOutFile);
			new TransitScheduleWriter(scenario.getTransitSchedule()).writeFile(this.transitScheduleOutFile);
			log.info("Writing vehicles to " + this.vehiclesOutFile);
			new VehicleWriterV1(scenario.getVehicles()).writeFile(this.vehiclesOutFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		log.info("Done...");
	}

}
