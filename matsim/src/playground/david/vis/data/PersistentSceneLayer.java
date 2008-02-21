package playground.david.vis.data;

import java.util.ArrayList;
import java.util.List;

import playground.david.vis.data.OTFData.Receiver;
import playground.david.vis.gui.OTFDrawable;

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
