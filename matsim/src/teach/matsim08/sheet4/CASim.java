package teach.matsim08.sheet4;
import java.io.IOException;

import org.matsim.interfaces.networks.basicNet.BasicNet;

import teach.matsim08.network.CANetStateWriter;

public class CASim {

	static int TIMESTEP;

	public static int getTimeStep() {
		return TIMESTEP;
	}

	public CANetwork network;
	private CANetStateWriter netVis;
	
	
	public CASim(BasicNet net) {
		network = new CANetwork(net);
		netVis = CANetStateWriter.createWriter(network, MyControler.NETFILENAME, MyControler.VISFILENAME );
	}
	
	public void runSimulation() {
		try {
			TIMESTEP = 0;
			// simulate movement
			while (TIMESTEP < 7200) {
				TIMESTEP++;
				network.move();
				netVis.dump(TIMESTEP);
				if (TIMESTEP % 3600 == 0 )
					System.out.println("Simulating: Time: " + TIMESTEP /3600 + "h 0min");
			}
			// finish netVis
			netVis.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
