package playground.kai.urbansim.ids;


public class BldIdFactory implements IdFactory {
	public BldId createId(String str) {
		return new BldId(str) ;
	}
	public BldId createId(long ii) {
		return new BldId(ii) ;
	}
}
