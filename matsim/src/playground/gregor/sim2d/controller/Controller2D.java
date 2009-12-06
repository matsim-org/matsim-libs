package playground.gregor.sim2d.controller;


import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkLayer;

import playground.gregor.sim2d.network.NetworkLoader;

public class Controller2D extends Controler {
	
	public Controller2D(String[] args) {
		super(args);
		this.setOverwriteFiles(true);
	}

	@Override
	protected NetworkLayer loadNetwork() {
		NetworkLayer net = this.scenarioData.getNetwork();
		new NetworkLoader(net).loadNetwork();

		this.getWorld().setNetworkLayer(net);
		this.getWorld().complete();

		return net;
	}

	
	public static void main(String [] args){
		Controler controller = new Controller2D(args);
		controller.run();
		
	}

}
