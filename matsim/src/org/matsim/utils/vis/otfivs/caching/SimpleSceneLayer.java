package org.matsim.utils.vis.otfivs.caching;

import java.util.ArrayList;
import java.util.List;

import org.matsim.utils.vis.otfivs.data.OTFData;
import org.matsim.utils.vis.otfivs.data.OTFData.Receiver;
import org.matsim.utils.vis.otfivs.gui.OTFDrawable;


public class SimpleSceneLayer extends DefaultSceneLayer {
	protected final List<OTFDrawable> items = new ArrayList<OTFDrawable>();

	@Override
	public void addItem(Receiver item) {
		items.add((OTFDrawable)item);
	}

	@Override
	public void draw() {
		for(OTFDrawable item : items) item.draw();
	}
	
	@Override
	public void finish() {
	}

	@Override
	public void init(SceneGraph graph) {
	}

	public int getDrawOrder() {
		return 100;
	}

	
	
}