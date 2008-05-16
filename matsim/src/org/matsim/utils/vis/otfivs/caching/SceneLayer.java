package org.matsim.utils.vis.otfivs.caching;

import org.matsim.utils.vis.otfivs.data.OTFData;
import org.matsim.utils.vis.otfivs.data.OTFData.Receiver;

public interface SceneLayer {
	public void init(SceneGraph graph);
	public void finish();
	public void addItem(OTFData.Receiver item);
	public void draw();
	public Object newInstance(Class clazz) throws InstantiationException, IllegalAccessException;
	public int getDrawOrder();
	
}

