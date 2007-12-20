package playground.david.vis.data;


public class OTFDataXYCoord implements OTFData{
	public static interface Receiver extends OTFData.Receiver{
		public void setXYCoord(float x, float y);
	}

}
