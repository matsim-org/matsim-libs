package playground.gregor.sim2d.otfdebug.layer;

import org.matsim.vis.otfvis.caching.SimpleSceneLayer;

public class ForceArrowLayer extends SimpleSceneLayer{
	@Override
	public int getDrawOrder() {
		return 200;
	}
}