package playground.pieter.distributed;

import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;

public class ControlerReference {
	Controler delegate;

	public ControlerReference(String config) {
		this.delegate = new Controler(config);
		delegate.setOverwriteFiles(true);
	}

	public static void main(String args[]) {
		new ControlerReference(args[0]).run();
	}

	private void run() {
		delegate.run();

	}
}
