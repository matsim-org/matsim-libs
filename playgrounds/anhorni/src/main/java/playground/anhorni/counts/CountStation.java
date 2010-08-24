package playground.anhorni.counts;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.matsim.core.utils.geometry.CoordImpl;

public class CountStation {
	
	//private final static Logger log = Logger.getLogger(CountStation.class);
	
	private String csId;
	private LinkInfo link1;
	private LinkInfo link2;
	private CoordImpl coord;
	private List<RawCount> counts = new Vector<RawCount>();
	
	public CountStation(String id, CoordImpl coord) {
		this.csId = id;
		this.coord = coord;
	}
	
	public void mapCounts() {
		Iterator<RawCount> count_it = counts.iterator();
		while (count_it.hasNext()) {
			RawCount rawCount = count_it.next();	
			link1.addYearCountVal(rawCount.getHour(), rawCount.getVol1());
			link2.addYearCountVal(rawCount.getHour(), rawCount.getVol2());
		}
	}
	
	public boolean addSimValforLinkId(String networkName, String linkId, int hour, double simVal) {
		if (this.link1.addSimValforLinkId(networkName, linkId, hour, simVal) || 
				this.link2.addSimValforLinkId(networkName, linkId, hour, simVal)) {
			return true;
		}
		return false;
	}
	
	public void filter(DateFilter filter) {
		this.counts = filter.filter(this.counts);
	}
	public void addCount(RawCount count) {
		this.counts.add(count);
	}		
	// aggregate 0..24
	public void aggregate() {
		this.link1.aggregate();
		this.link2.aggregate();	
	}	
	public List<RawCount> getCounts() {
		return counts;
	}
	public String getId() {
		return csId;
	}
	public void setId(String id) {
		this.csId = id;
	}
	public CoordImpl getCoord() {
		return this.coord;
	}
	public void setCoord(CoordImpl coord) {
		this.coord = coord;
	}
	public LinkInfo getLink1() {
		return link1;
	}
	public void setLink1(LinkInfo link1) {
		this.link1 = link1;
	}
	public LinkInfo getLink2() {
		return link2;
	}
	public void setLink2(LinkInfo link2) {
		this.link2 = link2;
	}
	
	
/*	public void finish() {
		Collections.sort(this.counts, new CountsComparator());
	}
	
	static class CountsComparator implements Comparator<RawCount>, Serializable {
		private static final long serialVersionUID = 1L;

		public int compare(final RawCount rc0, final RawCount rc1) {
			if (rc0.getHour() < rc1.getHour()) return -1;
			else if (rc0.getHour() > rc1.getHour()) return +1;
			return 0;
		}
	}*/
}
