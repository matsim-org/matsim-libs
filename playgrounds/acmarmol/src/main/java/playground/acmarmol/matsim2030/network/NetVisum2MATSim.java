/**
 * 
 */
package playground.acmarmol.matsim2030.network;


import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.TransitScheduleWriterV1;
import org.matsim.vehicles.VehicleWriterV1;
import playground.acmarmol.matsim2030.network.MyVisumNetwork.ModeType;
import playground.mrieser.pt.converter.Visum2TransitSchedule;




public class NetVisum2MATSim {
	private Network network;
	private MyVisumNetwork visumNet;

	public NetVisum2MATSim() {
		this.network = ScenarioUtils.createScenario(ConfigUtils.createConfig())
				.getNetwork();
	}

	public void readVisumNets(String inputFilename) {
		visumNet = new MyVisumNetwork();
		System.out.println(">>>>>Visum network reading began!");
		MyVisumNetworkReader reader = new MyVisumNetworkReader(visumNet);
		reader.read(inputFilename);
		visumNet.setLanguage(reader.getLanguage());
		System.out.println(">>>>>Visum network reading ended!");
	}

	/**
	 * @param inputFilename
	 *            visum network filename
	 * @param outputZoneFile
	 *            in which the zone information can be saved
	 */
	public void convertNetwork(String inputFilename, String outputZoneFile) {
		System.out.println(">>>>>Network converting began!");
		StreamingVisumNetworkReader streamingVisumNetworkReader = new StreamingVisumNetworkReader();

		// convert nodes
		VisumNetworkRowHandler nodeRowHandler = new MyVisumNodesRowHandler(
				(NetworkImpl) this.network);
		String[] NODES = {"NODE", "KNOTEN"};
		streamingVisumNetworkReader.addRowHandler(NODES[visumNet.getLanguage()], nodeRowHandler);

		// convert links
		VisumNetworkRowHandler linkRowHandler = new MyVisumLinksRowHandler(
				(NetworkImpl) this.network, visumNet);
		String[] LINKS = {"LINK", "STRECKE"};
		streamingVisumNetworkReader.addRowHandler(LINKS[visumNet.getLanguage()], linkRowHandler);

		// extract zone informationes
		//VisumZonesRowHandler zoneRowHandler = new VisumZonesRowHandler(
		//		outputZoneFile);
		//streamingVisumNetworkReader.addRowHandler("BEZIRK", zoneRowHandler);

		streamingVisumNetworkReader.read(inputFilename);

		((NetworkImpl) network).setCapacityPeriod(24d * 3600d);

		//zoneRowHandler.finish();
		System.out.println(">>>>>Network converting ended!");
	}

	public void writeMATSimNetwork(String outputFilename) {
		System.out.println(">>>>>This network has "
				+ this.network.getNodes().size() + " nodes and "
				+ this.network.getLinks().size() + " links!");
		System.out.println(">>>>>MATSim network writing began!");
		new NetworkWriter(network).write(outputFilename);
		System.out.println(">>>>>MATSim network writing ended!");
	}

	public void cleanNetwork() {
		new NetworkCleaner().run(this.network);
	}
	
	public void writeTransitScheduleAndVehiclesFiles(String outputTransitScheduleFile,String outputPTVehiclesFile){
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Config config = scenario.getConfig();
		config.scenario().setUseTransit(true);
		config.scenario().setUseVehicles(true);
		Visum2TransitSchedule converter = new Visum2TransitSchedule(visumNet, scenario.getTransitSchedule(), scenario.getTransitVehicles());	
		for(ModeType mode: visumNet.getModeTypes().values()){
			converter.registerTransportMode(mode.id.toString(), mode.name);
		}
		converter.convert();
		System.out.println("writing TransitSchedule to file.");
		new TransitScheduleWriterV1(scenario.getTransitSchedule()).write(outputTransitScheduleFile);
		System.out.println("writing vehicles to file.");
		new VehicleWriterV1(scenario.getTransitVehicles()).writeFile(outputPTVehiclesFile);
	}
	
	
	

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		String[] networks = {"01-MIV_2030+_DWV_Ref_Mit_Iteration_MasterGerman",
							 "02-OeV_2030+_DWV_Ref_Mit_IterationGerman"};
		int index;
		
		String inputBase = "C:/local/marmolea/input/UVEK-Network2030/";
		String outputBase = "C:/local/marmolea/output/UVEK-Network2030/";
		
		
		String outputZoneFile =  outputBase + "Zone.log";

		
		//MIV NETWORK
		NetVisum2MATSim n2mMIV = new NetVisum2MATSim();
		index = 0;
		String outputMATSimNetworkFile = outputBase +  networks[index] + ".xml.gz";
		String inputVisumNetFile = inputBase + networks[index] + ".net";
		n2mMIV.readVisumNets(inputVisumNetFile);
		n2mMIV.convertNetwork(inputVisumNetFile, outputZoneFile);
		
		NetworkPostProcessor postProcessor = new NetworkPostProcessor(n2mMIV.network);
		postProcessor.process();
		n2mMIV.cleanNetwork();
		n2mMIV.writeMATSimNetwork(outputMATSimNetworkFile);
		
		//OeV NETWORK
		NetVisum2MATSim n2mOeV = new NetVisum2MATSim();
		index = 1;
		outputMATSimNetworkFile = outputBase +  networks[index] + ".xml.gz";
		inputVisumNetFile = inputBase + networks[index] + ".net";
		n2mOeV.readVisumNets(inputVisumNetFile);
		n2mOeV.convertNetwork(inputVisumNetFile, outputZoneFile);
		NetworkPostProcessor postProcessor2 = new NetworkPostProcessor(n2mOeV.network);
		postProcessor2.process();
		n2mOeV.cleanNetwork();

		n2mOeV.writeMATSimNetwork(outputMATSimNetworkFile);
		String outputMATSimTransitScheduleFile = outputBase + networks[index] + "_TransitSchedule.xml";
		String outputMATSimPTVehiclesFile = outputBase + networks[index] + "_PTVehicles.xml";
		n2mOeV.writeTransitScheduleAndVehiclesFiles(outputMATSimTransitScheduleFile, outputMATSimPTVehiclesFile );
		

		
		
	}

}
