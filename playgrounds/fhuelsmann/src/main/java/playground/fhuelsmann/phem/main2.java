package playground.fhuelsmann.phem;
import java.io.IOException;


public class main2 {

	public static void main(String arg[]) throws IOException{
		
		DrivingCycleTime fr = new DrivingCycleTime();
		fr.read("../../detailedEval/teststrecke/sim/inputEmissions/FahrzyklenUhrzeit");
		System.out.println(fr.getDataStructure());
		ModaleOutput mot = new ModaleOutput();
		mot.read("../../detailedEval/teststrecke/sim/inputEmissions/ModaleAusgabe");
			
		DepatureTime dt = new DepatureTime();
		dt.read("../../detailedEval/teststrecke/sim/inputEmissions/departuretimes");
		dt.getDataStructure();
		//System.out.println(dt.getDataStructure());
		
		merge me = new merge(mot.getModaleOutputDataStructure(),fr.getDataStructure(),dt.getDataStructure());
		me.doit();
		
	}
}
