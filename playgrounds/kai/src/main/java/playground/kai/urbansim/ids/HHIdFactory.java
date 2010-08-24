package playground.kai.urbansim.ids;



public class HHIdFactory implements IdFactory {
	public HHId createId(String str) {
		return new HHId(str) ;
	}
	public HHId createId(long ii) {
		return new HHId(ii) ;
	}
}
