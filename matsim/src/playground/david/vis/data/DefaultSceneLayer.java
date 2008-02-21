package playground.david.vis.data;

import playground.david.vis.data.OTFData.Receiver;

public abstract class DefaultSceneLayer implements SceneLayer {

	public void addItem(Receiver item) {
	}

	public void draw() {
	}

	public void finish() {
	}

	public void init(SceneGraph graph) {
	}

	public Object newInstance(Class clazz) throws InstantiationException, IllegalAccessException {
		return clazz.newInstance();
	}
}
