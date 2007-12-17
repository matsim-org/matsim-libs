package playground.alexander;

import org.matsim.config.Config;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.QueueNetworkLayer;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkWriter;
import org.matsim.world.World;

public class NetworkModifier {
	    	   	
	public static void main( String[] args )
	{				
		
		String readFileNameNet;
		String writeFileNameNet;
		String fileNameNodes;
		String fileNameLinks;
		
		if (args.length == 4){
			readFileNameNet = args[0];
			writeFileNameNet = args[1];
			fileNameNodes = args[2];
			fileNameLinks = args[3];
		}
		
		else{
			readFileNameNet = "./padang/padang_net_new_261107.xml";
			writeFileNameNet = "./padang/padang_net_new_171207.xml";
			fileNameNodes = "./padang/padang_change_nodes.txt";
			fileNameLinks = "./padang/padang_change_links.txt";
		}
		
		World world = Gbl.createWorld();
		Config config = Gbl.createConfig(new String[] {"./padang/evacuationConf.xml"});
		QueueNetworkLayer network = new QueueNetworkLayer();
		new MatsimNetworkReader(network).readFile(readFileNameNet);
	
		Reader reader = new Reader(network);
		
		reader.readfile(fileNameNodes, "node");
		reader.readfile(fileNameLinks, "link");
		
		NetworkWriter nw = new NetworkWriter(network, writeFileNameNet);
		nw.write();
				
	}

}

