package freight;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;

public class OfferRecorder {

	public static class OfferRecord {
		public Id id;
		public Id from;
		public Id to;
		public int size;
		public double price;
		public String omStrat;
		public OfferRecord(Id id, Id from, Id to, int size, double price,
				String omStrat) {
			super();
			this.id = id;
			this.from = from;
			this.to = to;
			this.size = size;
			this.price = price;
			this.omStrat = omStrat;
		}
		
	}
	List<OfferRecord> records = new ArrayList<OfferRecord>();
	
	public void add(Id id, Id from, Id to, int shimpentSize, Double price, String omStrat) {
		records.add(new OfferRecord(id,from,to,shimpentSize,price,omStrat));
	}

	public List<OfferRecord> getRecords() {
		
		return records;
	}
	
	

}
