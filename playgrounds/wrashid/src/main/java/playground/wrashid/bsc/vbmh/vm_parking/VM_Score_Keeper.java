package playground.wrashid.bsc.vbmh.vm_parking;
public class VM_Score_Keeper {
	double score = 0;
	
	public void add(double add){
		score = score + add;
	}
	
	public double get_score(){
		return score;
	}

}
