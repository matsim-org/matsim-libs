package teach.matsim08.sheet2;
import org.matsim.utils.vis.netvis.DrawableAgentI;



public class CAVehicle implements DrawableAgentI {

	private double pos;

	public void setPosition(double pos) {
		this.pos = pos;
	}

	public int getLane() {
		return 1;
	}

	public double getPosInLink_m() {
		return pos;
	}



}
