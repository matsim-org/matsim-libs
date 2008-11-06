package playground.kai.urbansim;

import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;

import playground.kai.IdBuilder;

public class JobIdBuilder implements IdBuilder {
	public JobIdImpl createId(String str) {
		return new JobIdImpl(str) ;
	}
	public JobIdImpl createId(long ii) {
		return new JobIdImpl(ii) ;
	}
}
