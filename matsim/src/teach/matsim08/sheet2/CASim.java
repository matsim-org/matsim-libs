package teach.matsim08.sheet2;
import java.io.IOException;

import org.matsim.interfaces.networks.basicNet.BasicNet;

import teach.matsim08.network.CANetStateWriter;

public class CASim {

	public CANetwork network;

	public CASim(BasicNet net) {

		network = new CANetwork(net);
		CANetStateWriter netVis = CANetStateWriter.createWriter(network, MyControler.NETFILENAME, MyControler.VISFILENAME );

		try {
			netVis.dump(0) ;
			netVis.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
