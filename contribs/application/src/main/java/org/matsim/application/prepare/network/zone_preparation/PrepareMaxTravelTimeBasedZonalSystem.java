package org.matsim.application.prepare.network.zone_preparation;

import org.matsim.api.core.v01.network.Network;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.network.NetworkUtils;
import picocli.CommandLine;

public class PrepareMaxTravelTimeBasedZonalSystem implements MATSimAppCommand {
	@CommandLine.Option(names = "--input", required = true, description = "input network path")
	private String inputNetworkPath;

	@CommandLine.Option(names = "--output", required = true, description = "output network path")
	private String outputNetworkPath;

	@CommandLine.Option(names = "--max-travel-time", defaultValue = "300", description = "max time distance away from zone centroid [second]")
	private double maxTimeDistance;

	@CommandLine.Option(names = "--iterations", defaultValue = "20", description = "number of iterations to improve zonal system")
	private int iterations;

	@Override
	public Integer call() throws Exception {
		Network network = NetworkUtils.readNetwork(inputNetworkPath);
		MaxTravelTimeBasedZoneGenerator.Builder builder = new MaxTravelTimeBasedZoneGenerator.Builder(network, outputNetworkPath);
		MaxTravelTimeBasedZoneGenerator generator = builder.setTimeRadius(maxTimeDistance).setZoneIterations(iterations).build();
		generator.compute();
		return 0;
	}

	public static void main(String[] args) {
		new PrepareMaxTravelTimeBasedZonalSystem().execute(args);
	}
}
