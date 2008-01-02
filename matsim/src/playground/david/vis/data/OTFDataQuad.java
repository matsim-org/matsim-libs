package playground.david.vis.data;

public interface OTFDataQuad extends OTFData{
	
	public static interface Receiver extends OTFData.Receiver{
		public void setQuad(float startX, float startY, float endX, float endY);
		public void setColor(float coloridx);
	}

}
