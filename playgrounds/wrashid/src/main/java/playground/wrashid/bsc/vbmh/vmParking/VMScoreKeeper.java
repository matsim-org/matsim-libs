package playground.wrashid.bsc.vbmh.vmParking;

/**
 * Is added as an agents Attribute to keep for example the money which is paid for parking.
 * !! Could probably be replaced by a matsim on board solution.
 * 
 * 
 * @author Valentin Bemetz & Moritz Hohenfellner
 *
 */


public class VMScoreKeeper {
	double score = 0;
	
	public void add(double add){
		score = score + add;
	}
	
	public double getScore(){
		return score;
	}

}
