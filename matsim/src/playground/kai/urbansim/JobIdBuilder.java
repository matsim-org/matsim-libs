package playground.kai.urbansim;

import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;


public class JobIdBuilder implements IdBuilder {
	public JobId createId(String str) {
		return new JobId(str) ;
	}
	public JobId createId(long ii) {
		return new JobId(ii) ;
	}
}
