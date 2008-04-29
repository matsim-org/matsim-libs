package teach.matsim08.sheet3;
import java.io.IOException;

import org.matsim.interfaces.networks.basicNet.BasicNet;

import teach.matsim08.network.CANetStateWriter;

public class CASim {

	public CANetwork network;
	private CANetStateWriter netVis;
	
	public CASim(BasicNet net) {
		network = new CANetwork(net);
		netVis = CANetStateWriter.createWriter(network, MyControler.NETFILENAME, MyControler.VISFILENAME );
	}
	
	public void runSimulation(int starttime, int endtime) {
		try {
			// simulate movement
			for (int time = starttime; time < endtime; time++) {
				network.move(time);
				netVis.dump(time);
				if (time % 3600 == 0 )
					System.out.println("Simulating: Time: " + time /3600 + "h 0min");
			}
			// finish netVis
			netVis.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
