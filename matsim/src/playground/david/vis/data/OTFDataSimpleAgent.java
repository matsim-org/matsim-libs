package playground.david.vis.data;

public interface OTFDataSimpleAgent extends OTFData{
	
	public static interface Receiver extends OTFData.Receiver{
		public void setAgent(String id, float startX, float startY, int state, float color);
	}

}
