package playground.kai.urbansim.ids;



public class JobIdFactory implements IdFactory {
	public JobId createId(String str) {
		return new JobId(str) ;
	}
	public JobId createId(long ii) {
		return new JobId(ii) ;
	}
}
