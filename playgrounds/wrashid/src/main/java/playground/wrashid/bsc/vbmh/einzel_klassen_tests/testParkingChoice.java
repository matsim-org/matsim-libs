package playground.wrashid.bsc.vbmh.einzel_klassen_tests;

import playground.wrashid.bsc.vbmh.vmParking.AdvancedParkingChoice;
import playground.wrashid.bsc.vbmh.vmParking.AdvancedParkingChoice.Option;

public class testParkingChoice {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		AdvancedParkingChoice choice = new AdvancedParkingChoice();
		choice.startUp();
		choice.calcLMSOC(0.0);
		choice.calcLMSOC(0.05);
		choice.calcLMSOC(0.1);
		choice.calcLMSOC(0.25);
		choice.calcLMSOC(0.5);
		choice.calcLMSOC(0.6);
		choice.calcLMSOC(1);
		
		choice.clalcLMReserve(0.5, 0.3);
		choice.clalcLMReserve(0.5, 0.48);
		choice.clalcLMReserve(0.5, 0.5);
		choice.clalcLMReserve(0.5, 0.51);
		choice.clalcLMReserve(0.5, 0.55);
		choice.clalcLMReserve(0.5, 0.6);
		choice.clalcLMReserve(0.5, 0.8);
		
		choice.calcUtil(500, 7, 0, 0.98);
		choice.calcUtil(500, 5, 0, 0.6);
		choice.calcUtil(500, 5, -1, 0.6);
		
		choice = new AdvancedParkingChoice();
		choice.setRequiredRestOfDayBatPerc(0.0);
		System.out.println("fall test");
		choice.addOption(choice.new Option(null, 7.1, 500, 0.0));
		choice.addOption(choice.new Option(null, 7, 540, 0.0));
		choice.addOption(choice.new Option(null, 8.5, 700, 0.0));
		System.out.print(choice.selectBestOption().toString());
	}

}
