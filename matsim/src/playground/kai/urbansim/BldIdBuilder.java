package playground.kai.urbansim;

import playground.kai.IdBuilder;

public class BldIdBuilder implements IdBuilder {
	public BldIdImpl createId(String str) {
		return new BldIdImpl(str) ;
	}
	public BldIdImpl createId(long ii) {
		return new BldIdImpl(ii) ;
	}
}
