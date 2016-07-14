package playground.singapore.springcalibration.preprocess;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;
import org.matsim.counts.MatsimCountsReader;
import org.matsim.counts.Volume;

public class CountsComposer {
	
	private static final Logger log = Logger.getLogger(CountsComposer.class);
	
	private Scenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
	private String busfile = "countsBuses";
	private String carsfile = "countsCars";
	private String hgvfile = "countsHGV";
	private String lgvfile = "countsLGV";
	private String taxifile = "countsTaxis";
	private String composedfile = "countsComposed";
	
	private Counts<Link> buscounts = new Counts<>();
	private Counts<Link> carcounts = new Counts<>();
	private Counts<Link> hgvcounts = new Counts<>();
	private Counts<Link> lgvcounts = new Counts<>();
	private Counts<Link> taxicounts = new Counts<>();
	
	private Counts<Link> composedcounts = new Counts<>();
	private double scaleFactor = 1.0;
	
	// countsMotorcycle, countsPrivateBuses are teleported modes
	
	public static void main(String[] args) {
		CountsComposer cleaner = new CountsComposer();
		cleaner.run(args[0], args[1], Double.parseDouble(args[2]));
	}
	
	
	public void run(String path, String networkfile, double scale) {
		double buses = 0;
		double cars = 0;
		double freight = 0;
		
		this.scaleFactor = scale;
		
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkfile);
		new MatsimCountsReader(buscounts).readFile(path + this.busfile + ".xml");
		log.info("Number of bus stations: " + this.buscounts.getCounts().size());
		new MatsimCountsReader(carcounts).readFile(path + this.carsfile + ".xml");
		new MatsimCountsReader(hgvcounts).readFile(path + this.hgvfile + ".xml");
		new MatsimCountsReader(lgvcounts).readFile(path + this.lgvfile + ".xml");
		new MatsimCountsReader(taxicounts).readFile(path + this.taxifile + ".xml");
		
		for (Id<Link> countedlink : buscounts.getCounts().keySet()) {
			Count<Link> count = buscounts.getCount(countedlink);
			buses += this.addCountValues(count, "bus");
		}
		for (Id<Link> countedlink : carcounts.getCounts().keySet()) {
			Count<Link> count = carcounts.getCount(countedlink);
			cars += this.addCountValues(count, TransportMode.car);
		}
		for (Id<Link> countedlink : hgvcounts.getCounts().keySet()) {
			Count<Link> count = hgvcounts.getCount(countedlink);
			freight += this.addCountValues(count, "freight");
		}
		for (Id<Link> countedlink : lgvcounts.getCounts().keySet()) {
			Count<Link> count = lgvcounts.getCount(countedlink);
			freight += this.addCountValues(count, "freight");
		}
		for (Id<Link> countedlink : taxicounts.getCounts().keySet()) {
			Count<Link> count = taxicounts.getCount(countedlink);
			this.addCountValues(count, "taxi");
		}

		log.info("Writing counts file to " + path + this.composedfile + ".xml");
		log.info("Cars: " + cars + " buses: " + buses + " freight: " + freight);
		new CountsWriter(composedcounts).write(path + this.composedfile + ".xml");		
	}
	
	private double addCountValues(Count<Link> countstoadd, String mode) {
				
		double scale = 1.0;
		if (mode.equals("bus"))  scale = this.scaleFactor;
						
		Count<Link> composedCount = this.composedcounts.getCount(countstoadd.getLocId());
		if (composedCount == null) {
			composedCount = this.composedcounts.createAndAddCount(countstoadd.getLocId(), countstoadd.getCsId());
		}
		
		double totalCount = 0;
		for (int h = 1; h <= 24; h++) {
			double countval2add = 0.0;
			Volume volume2add = countstoadd.getVolume(h);
			if (volume2add != null) countval2add = volume2add.getValue();
									
			double oldcountval = 0.0;
			Volume oldVolume = composedCount.getVolume(h);
			if (oldVolume != null) oldcountval = oldVolume.getValue();
			
			totalCount += countval2add;
			
			double newCount = scale * countval2add + oldcountval;
			
			if (volume2add != null || oldVolume != null) composedCount.createVolume(h, newCount);		
		}
		return totalCount;
	}
	
}
