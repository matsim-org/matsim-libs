package playground.david.vis.data;

import java.util.ArrayList;
import java.util.List;

import playground.david.vis.data.OTFData.Receiver;
import playground.david.vis.gui.OTFDrawable;

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
	
	public List<OTFDrawable> getAllItemsKILLTHIS() {
		return items;
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