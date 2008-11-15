package playground.kai.urbansim;


public class BldIdBuilder implements IdBuilder {
	public BldId createId(String str) {
		return new BldId(str) ;
	}
	public BldId createId(long ii) {
		return new BldId(ii) ;
	}
}
