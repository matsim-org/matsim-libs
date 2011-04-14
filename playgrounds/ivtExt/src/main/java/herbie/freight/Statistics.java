package herbie.freight;

import utils.Bins;

public class Statistics {
	private Bins bins = new Bins(1.0, 24.0, "departures");
		
	public void addDeparture(double departure) {
		this.bins.addVal(departure / 3600.0, 1.0);
	}
	
	public void writeDepartures(String outpath) {	
		this.bins.plotBinnedDistribution(outpath, "", " h");
	}
}