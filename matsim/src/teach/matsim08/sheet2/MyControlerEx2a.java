package teach.matsim08.sheet2;
import java.util.Iterator;
import java.util.Map;

import org.matsim.interfaces.networks.basicNet.BasicLink;
import org.matsim.interfaces.networks.basicNet.BasicNet;
import org.matsim.interfaces.networks.basicNet.BasicNode;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;



public class MyControlerEx2a {

	public static void main(String[] args) {
		String netFileName = "equilNet.xml" ;
		String visFileName = "output/ABC" ;

		BasicNet net = new NetworkLayer() ;
		MatsimNetworkReader reader = new MatsimNetworkReader((NetworkLayer)net);
		reader.readFile(netFileName) ;
		// from here on, net can do everything that is guaranteed in BasicNetI

		Map nodes = net.getNodes();
		Map links = net.getLinks();

		System.out.println("Nodes: ");

		for ( Iterator it = nodes.values().iterator(); it.hasNext();) {
			BasicNode nn = (BasicNode) it.next();
			System.out.print( nn.getId() + " ") ;
		}

		System.out.println();
		System.out.println("Links: ");

		for (Iterator it = links.values().iterator(); it.hasNext();) {
			BasicLink ll = (BasicLink) it.next();
			System.out.print( ll.getId() + " ");
		}
	}

}
