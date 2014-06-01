package playground.vbmh.paramterschaetzer;

import java.util.LinkedList;

import playground.vbmh.util.CSVReader;

public class doIt {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		LinkedList<ChoiceSet> sets = new LinkedList<ChoiceSet>();
		LinkedList<Parameterset> params = new LinkedList<Parameterset>();
		
		sets.add(new ChoiceSet());
		sets.getLast().add(-1, 2.5, 1, 1);
		sets.getLast().add(-1, -20, 1, 0);
		
		CSVReader reader = new CSVReader();
		LinkedList<String[]> liste = reader.readCSV("input/logit/dings.csv");
		sets.add(new ChoiceSet());
		for(String[] zeile : liste){
			if (Double.parseDouble(zeile[3])>-1) {
				sets.getLast().add(Double.parseDouble(zeile[0]),
						Double.parseDouble(zeile[1]),
						Double.parseDouble(zeile[2]),
						Double.parseDouble(zeile[3]));
			} else{
				sets.add(new ChoiceSet());
			}
		}
		
		
		
		for(double constant = -5; constant<5.0; constant+=0.25){
			for(double betaMatsim = 0; betaMatsim<7.0; betaMatsim+=0.25){
				for(double betaReserve = 0; betaReserve<7.0; betaReserve+=0.25){
					for(double betaSOC = 0; betaSOC<5.0; betaSOC+=0.25){
						params.add(new Parameterset(betaMatsim, betaReserve, betaSOC, constant));
					}
				}
			}
		}
		System.out.println("Parameter Sets vorbereitet");
		for(Parameterset param : params){
			param.score=0.0;
			for(ChoiceSet set : sets){
				param.score=param.score+set.check(param);
			}
			//System.out.println("param score : "+param.score);
			System.out.println("pruefe naechstes Set");
		}
		
		double bestScore=-3000;
		Parameterset bestParam = null;
		for (Parameterset param : params){
			if(param.score>=bestScore){
				//System.out.println("besseres");
				bestScore=param.score;
				bestParam = param;
			}
		}
		System.out.println(bestParam.toString());
	}

}
