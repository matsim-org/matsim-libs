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
		//System.out.println("Previous Score :"+score);
		score = score + add;
		//System.out.println("Added value to ScoreKeeper :"+add);
		if(score>0){
			System.out.println("Positve parkingScore !!");
			try {
				wait(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public double getScore(){
		return score;
	}

}
