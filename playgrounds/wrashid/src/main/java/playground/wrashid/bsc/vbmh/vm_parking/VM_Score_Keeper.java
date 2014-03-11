package playground.wrashid.bsc.vbmh.vm_parking;

/**
 * Is added as an agents Attribute to keep for example the money which is paid for parking.
 * !! Could probably be replaced by a matsim on board solution.
 * 
 * 
 * @author Valentin Bemetz & Moritz Hohenfellner
 *
 */


public class VM_Score_Keeper {
	double score = 0;
	
	public void add(double add){
		score = score + add;
	}
	
	public double get_score(){
		return score;
	}

}
