package org.matsim.utils.vis.otfivs.data;

public interface OTFDataSimpleAgent extends OTFData{
	
	public static interface Receiver extends OTFData.Receiver{
		public void setAgent(char[] id, float startX, float startY, int state, int user, float color);
	}

}
