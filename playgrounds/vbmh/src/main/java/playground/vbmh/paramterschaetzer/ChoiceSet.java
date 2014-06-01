package playground.vbmh.paramterschaetzer;

import java.util.LinkedList;

public class ChoiceSet {
	public LinkedList<Double[]> set = new LinkedList<Double[]>();
	//0: Matsim Score
	//1: Reserve
	//2: SOC
	//3: Wie oft genommen
	//4: exp
	//5: Wkeit, dass so gewaehlt
	//6: ln(4)
	//7: ln Summe
	

	public double check(Parameterset parameter){
		//System.out.println("ja ja ");
		double expSumme = 0;
		int summeEntscheidungen = 0;
		double likeliHood =1.0;
		for(Double[] choice : set){
			for(int i = 4; i<8; i++){
				choice[i]=0.0;
			}
			choice [4] = parameter.calcExp(choice[0], choice[1], choice[2]);	
			expSumme+=choice[4];
			summeEntscheidungen += choice[3];
			//System.out.println(expSumme);
		}

		for(Double[] choice : set){
			choice[7] = 0.0;
			choice [5] = choice[4]/expSumme;
			//System.out.println("einzel wkeit"+choice[5]);
			double dafuer = choice[3];
			double dagegen = summeEntscheidungen - dafuer;
			//System.out.println("dagegen "+ dagegen);
//			choice[7]=Math.pow(Math.log(choice[5]), dafuer);
//			choice[7]=choice[7]*Math.pow(Math.log(1-choice[5]),dagegen);
			choice[7]+=Math.log(choice[5])*dafuer;
			choice[7]+=Math.log(1-choice[5])*dagegen;
			likeliHood=likeliHood+choice[7];
		}
		//System.out.println("likelihood "+likeliHood);
		return likeliHood;
	}
	
	

	public void add(double matsim, double reserve, double soc, double anzahl){
		Double[] neu = {matsim, reserve, soc, anzahl, 0.0, 0.0, 0.0, 0.0};
		set.add(neu);
	}

}
