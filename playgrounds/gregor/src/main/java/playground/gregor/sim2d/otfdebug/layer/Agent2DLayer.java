package playground.gregor.sim2d.otfdebug.layer;

import org.matsim.vis.otfvis.caching.SimpleSceneLayer;

public class Agent2DLayer extends SimpleSceneLayer{
	@Override
	public int getDrawOrder() {
		return 199;
	}
}
