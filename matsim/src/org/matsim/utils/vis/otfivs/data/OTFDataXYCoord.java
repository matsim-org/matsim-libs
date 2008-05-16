package org.matsim.utils.vis.otfivs.data;


public class OTFDataXYCoord implements OTFData{
	public static interface Receiver extends OTFData.Receiver{
		public void setXYCoord(float x, float y);
	}

}
