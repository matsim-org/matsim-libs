package teach.matsim08.sheet4;
import java.util.Map;

import org.matsim.gbl.Gbl;
import org.matsim.interfaces.networks.basicNet.BasicNet;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.MatsimPlansReader;
import org.matsim.plans.Plans;
import org.matsim.utils.vis.netvis.NetVis;



public class MyControler {

	public static final String NETFILENAME = "/Volumes/data/work/lehre/matsim2008/input/equilNet.xml" ;
	public static final String VISFILENAME = "MyControlerOutput";

	public static final String PLANSFILENAME = "/Volumes/data/work/lehre/matsim2008/input/plans100.xml";
	
	public static void main(String[] args) {

		BasicNet net = new NetworkLayer() ;
		MatsimNetworkReader reader = new MatsimNetworkReader((NetworkLayer)net);
		reader.readFile(NETFILENAME) ;
		// from here on, net can do everything that is guaranteed in BasicNet

		Gbl.createConfig(null);	
		Gbl.getWorld().setNetworkLayer((NetworkLayer) net);
		
		Plans plans = new Plans();
		MatsimPlansReader plansParser = new MatsimPlansReader(plans);
		plansParser.readFile(PLANSFILENAME);
		
		Map nodes = net.getNodes();
		Map links = net.getLinks();

//		CASim sim = new CASim(net) ;
//		sim.runSimulation();
//
//		NetVis.start(VISFILENAME) ;
	}

}
