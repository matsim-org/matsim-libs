package playground.anhorni.counts;

import java.util.Iterator;
import java.util.List;

import org.matsim.core.basic.v01.IdImpl;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;

public class Converter {
	
	Counts countsAre = new Counts();
	Counts countsIVTCH = new Counts();
	Counts countsNavteq = new Counts();
	Counts countsTeleatlas = new Counts();
	
//	private final static Logger log = Logger.getLogger(Converter.class);
	
	public void convert(List<CountStation> incounts) {
		
		Iterator<CountStation> countStation_it = incounts.iterator();
		while (countStation_it.hasNext()) {
			CountStation countStation = countStation_it.next();
			
			this.createCounts(countStation, countsIVTCH, 
					new IdImpl(countStation.getLink1().getLinkidIVTCH()),
					new IdImpl(countStation.getLink2().getLinkidIVTCH()));
			
			this.createCounts(countStation, countsAre, 
					new IdImpl(countStation.getLink1().getLinkidAre()),
					new IdImpl(countStation.getLink2().getLinkidAre()));
						
			this.createCounts(countStation, countsNavteq, 
					new IdImpl(countStation.getLink1().getLinkidNavteq()),
					new IdImpl(countStation.getLink2().getLinkidNavteq()));
			
			this.createCounts(countStation, countsTeleatlas, 
					new IdImpl(countStation.getLink1().getLinkidTeleatlas()),
					new IdImpl(countStation.getLink2().getLinkidTeleatlas()));			
		}		
	}
	
	private void createCounts(CountStation countStation, Counts counts, IdImpl locId1, IdImpl locId2) {
		
//		if (countStation.getId().equals("ASTRA066")) {
//			log.info("SIZE ----------: " + countStation.getLink1().getAggregator().getSize(0));
//			log.info("locs ----------: " + locId1.toString());
//			log.info("STUNDE0: " + countStation.getLink1().getAggregator().getAvg()[0]);
//			log.info("STUNDE7: " + countStation.getLink1().getAggregator().getAvg()[7]);
//		}
				
		if (locId1.compareTo(new IdImpl("-")) == 0 || locId2.compareTo(new IdImpl("-")) == 0) {
			return;
		}
		
		Count count0 = counts.createCount(locId1, countStation.getId());		
		if (count0 != null) {
			for (int i = 0; i < 24; i++) {
				count0.createVolume(i+1, countStation.getLink1().getAggregator().getAvg()[i]);
			}
		}
		
		Count count1 = counts.createCount(locId2, countStation.getId());
		if (count1 != null) {
			for (int i = 0; i < 24; i++) {
				count1.createVolume(i+1, countStation.getLink2().getAggregator().getAvg()[i]);
			}
		}
	}

	public Counts getCountsAre() {
		return countsAre;
	}

	public void setCountsAre(Counts countsAre) {
		this.countsAre = countsAre;
	}

	public Counts getCountsIVTCH() {
		return countsIVTCH;
	}

	public void setCountsIVTCH(Counts countsIVTCH) {
		this.countsIVTCH = countsIVTCH;
	}

	public Counts getCountsNavteq() {
		return countsNavteq;
	}

	public void setCountsNavteq(Counts countsNavteq) {
		this.countsNavteq = countsNavteq;
	}

	public Counts getCountsTeleatlas() {
		return countsTeleatlas;
	}

	public void setCountsTeleatlas(Counts countsTeleatlas) {
		this.countsTeleatlas = countsTeleatlas;
	}	
}
