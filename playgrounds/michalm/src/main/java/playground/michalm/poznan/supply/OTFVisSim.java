package playground.michalm.poznan.supply;

import java.util.Arrays;

import org.matsim.contrib.otfvis.OTFVis;

public class OTFVisSim {

	public static void main(String[] args) {
		String dir;
		String mviFile;

		if (args.length == 1 && args[0].equals("test")) {// for testing
			dir = "d:\\eclipse-matsim\\bartekp\\";

			// mviFile = "output\\config-verB\\ITERS\\it.10\\10.otfvis.mvi";
			mviFile = "output\\poznan\\ITERS\\it.20\\20.otfvis.mvi";
		} else if (args.length == 2) {
			dir = args[0];
			mviFile = args[1];
		} else {
			throw new IllegalArgumentException("Incorrect program arguments: " + Arrays.toString(args));
		}

		OTFVis.playMVI(dir + mviFile);
	}
}
