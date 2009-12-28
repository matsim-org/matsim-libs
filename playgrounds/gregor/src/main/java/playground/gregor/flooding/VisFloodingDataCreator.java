package playground.gregor.flooding;

import java.io.IOException;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.AbstractMatsimWriter;
import org.matsim.evacuation.flooding.FloodingInfo;
import org.matsim.evacuation.flooding.FloodingReader;

public class VisFloodingDataCreator extends AbstractMatsimWriter {
	
	private static final Logger log = Logger.getLogger(VisFloodingDataCreator.class);
	
	
	private static final double THRESHOLD = 0.05;
	
	public void write(String file, Collection<FloodingInfo> events, double t0) {
		try {
			openFile(file);
			int steps = events.iterator().next().getFloodingSeries().size();
			for (int i = 0; i < steps; i++) {
				double time = t0 + i * 60;
				for (FloodingInfo c : events) {
					double flooding = c.getFloodingSeries().get(i);
					if (flooding < THRESHOLD) {
						continue;
					}
					this.writer.write(c.getCoordinate().x + "\t" + c.getCoordinate().y + "\t" + flooding + "\t" + time + NL);
				}
				
			}
			

			
			
			
			close();
		} catch (IOException e) {
			log.fatal("Error during writing network change events!", e);
		}
	}
	
	public static void main(String [] args) {
		String netcdf = "../../inputs/flooding/flooding01.sww";
		String outfile = "../../inputs/flooding/flooding01.txt.gz";
		FloodingReader f = new FloodingReader(netcdf);
		Collection<FloodingInfo> infos = f.getFloodingInfos();
		
		new VisFloodingDataCreator().write(outfile, infos, 3*3600);
		
				
	}

}
