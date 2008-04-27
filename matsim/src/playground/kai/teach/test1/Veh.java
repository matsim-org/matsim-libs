package playground.kai.teach.test1;

import org.matsim.utils.vis.netvis.DrawableAgentI;

class Veh implements DrawableAgentI {
	
	private double pos ;
	public void setPosition ( double tmp ) {
		pos = tmp ;
	}

	public int getLane() {
		return 1;
	}

	public double getPosInLink_m() {
		return pos ;
	}

}