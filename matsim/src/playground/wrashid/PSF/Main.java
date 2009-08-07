package playground.wrashid.PSF;

import org.matsim.core.controler.Controler;

import playground.wrashid.PSF.energy.LogParkingTimes;
import playground.wrashid.PSF.energy.ScoreEnergyCharged;

public class Main {
	public static void main(String[] args) {
		Controler controler=new Controler(args);
		controler.addControlerListener(new ScoreEnergyCharged());
		
		
		controler.run();
	}
}
