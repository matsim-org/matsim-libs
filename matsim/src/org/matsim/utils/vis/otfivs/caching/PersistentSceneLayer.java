package org.matsim.utils.vis.otfivs.caching;

import java.util.ArrayList;
import java.util.List;

import org.matsim.utils.vis.otfivs.data.OTFData;
import org.matsim.utils.vis.otfivs.data.OTFData.Receiver;
import org.matsim.utils.vis.otfivs.gui.OTFDrawable;


/***
 * This class will draw its items on every frame,
 * therefore it is good for drawing background images or
 * a simple net representation, that does not change over the time
 * 
 * @author dstrippgen
 *
 */
public abstract class PersistentSceneLayer extends DefaultSceneLayer {
	
	private final static List<OTFDrawable> items = new ArrayList<OTFDrawable>();

	@Override
	public void addItem(Receiver item) {
		items.add((OTFDrawable)item);
	}

	@Override
	public void draw() {
		for(OTFDrawable item : items) item.draw();
	}
	

}
