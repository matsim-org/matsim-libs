package playground.david.vis.data;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.matsim.utils.collections.QuadTree.Rect;

import playground.david.vis.gui.OTFDrawable;
import playground.david.vis.interfaces.OTFDrawer;


class LayerDrawingOrderComparator implements Comparator<SceneLayer> {

	public int compare(SceneLayer o1, SceneLayer o2) {
		int diff = (int)Math.signum(o1.getDrawOrder() - o2.getDrawOrder());

		return diff;
	}
	
}


public class SceneGraph {
	private final Rect rect;
	private final Map<Class, SceneLayer> layers = new LinkedHashMap<Class, SceneLayer>();
	private final List<SceneLayer> drawingLayers = new LinkedList<SceneLayer>();

	private final OTFDrawer drawer;
	
	public SceneGraph(Rect rect, OTFConnectionManager connect, OTFDrawer drawer) {
		this.rect = rect;
		this.drawer = drawer;
		
		// default layer, might be overridden from connect!
		layers.put(Object.class, new SimpleSceneLayer());
		
		connect.fillLayerMap(layers);

		// do initialising action if necessary
		for (SceneLayer layer : layers.values()) {
			layer.init(this);
			drawingLayers.add(layer);
		}
		
	}
	
	public Rect getRect() {
		return this.rect;
	}

	public OTFDrawer getDrawer() {
		return drawer;
	}
	
	public Object newInstance(Class clazz) throws InstantiationException, IllegalAccessException {
		SceneLayer layer = layers.get(clazz);
		if (layer == null)layer = layers.get(Object.class); //DS must exist: default handling
		return layer.newInstance(clazz);
	}

	public void addItem(OTFData.Receiver item) {
		SceneLayer layer = layers.get(item.getClass());
		if (layer == null)layer = layers.get(Object.class); //DS must exist: default handling
		
		layer.addItem(item);
	}
	
	public void finish() {
		Collections.sort(drawingLayers, new LayerDrawingOrderComparator());
		// do finishing action if necessary
		for (SceneLayer layer : drawingLayers) layer.finish();
	}
	
	public SceneLayer getLayer(Class clazz) {
		SceneLayer layer = layers.get(clazz);
		if (layer == null)layer = layers.get(Object.class); //DS must exist: default handling
		return layer;
	}
	
	public void draw() {
		// do initialising action if necessary
		for (SceneLayer layer : drawingLayers) layer.draw();
	}
}

