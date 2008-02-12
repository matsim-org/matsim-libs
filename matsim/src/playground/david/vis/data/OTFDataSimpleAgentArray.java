package playground.david.vis.data;

public interface OTFDataSimpleAgentArray extends OTFData{
	
	public static interface Receiver extends OTFData.Receiver{
		public void setMaxSize(int size);
		public void addAgent(char[] id, float startX, float startY, int state, float color);
	}

}
