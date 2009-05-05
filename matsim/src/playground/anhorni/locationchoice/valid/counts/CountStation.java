package playground.anhorni.locationchoice.valid.counts;

import java.util.List;
import java.util.Vector;
import org.apache.log4j.Logger;
import org.matsim.core.utils.geometry.CoordImpl;

public class CountStation {
	
	private final static Logger log = Logger.getLogger(CountStation.class);
	
	private String csId;
	private LinkInfo link0;
	private LinkInfo link1;
	private CoordImpl coord;
	private List<RawCount> counts = new Vector<RawCount>();
	private Aggregator aggregator = new Aggregator();
	
	public CountStation(String id, CoordImpl coord) {
		this.csId = id;
		this.coord = coord;
	}
	
	public void filter(DateFilter filter) {
		this.counts = filter.filter(this.counts);
	}
	public void addCount(RawCount count) {
		this.counts.add(count);
	}		
	// aggregate 0..24
	public void aggregate() {
		aggregator.aggregate(this.counts);		
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
	public LinkInfo getLink0() {
		return link0;
	}
	public void setLink0(LinkInfo link0) {
		this.link0 = link0;
	}
	public LinkInfo getLink1() {
		return link1;
	}
	public void setLink1(LinkInfo link1) {
		this.link1 = link1;
	}
	public Aggregator getAggregator() {
		return aggregator;
	}
	public void setAggregator(Aggregator aggregator) {
		this.aggregator = aggregator;
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
