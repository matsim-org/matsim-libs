package playground.vbmh.einzel_klassen_tests;

import playground.vbmh.vmParking.AdvancedParkingChoice;
import playground.vbmh.vmParking.AdvancedParkingChoice.Option;

public class testParkingChoice {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		AdvancedParkingChoice choice = new AdvancedParkingChoice();
		choice.startUp();
//		choice.calcLMSOC(0.0);
//		choice.calcLMSOC(0.05);
		System.out.println(choice.calcLMSOC(15));
//		choice.calcLMSOC(0.25);
//		choice.calcLMSOC(0.5);
//		choice.calcLMSOC(0.6);
//		System.out.println(choice.calcLMSOC(0.75));
//		System.out.println(choice.calcLMReserve(0.5, 0.55));
//		System.out.println((choice.calcLMSOC(1)-choice.calcLMSOC(0.75))*4.5);
//		
//		choice.calcLMReserve(0.5, 0.3);
//		choice.calcLMReserve(0.5, 0.48);
//		choice.calcLMReserve(0.5, 0.5);
//		choice.calcLMReserve(0.5, 0.51);
//		choice.calcLMReserve(0.5, 0.55);
//		choice.calcLMReserve(0.5, 0.6);
//		choice.calcLMReserve(0.5, 0.8);
		choice.setBetaSOC(0.2759);
		System.out.println("-----------");
		System.out.println(choice.calcUtil(1206, 20.5627, -1, 0));
		System.out.println("-----------");
//		System.out.println(choice.calcUtil(600, 7, 0, 1));
//		choice.calcUtil(500, 5, 0, 0.6);
//		choice.calcUtil(500, 5, -1, 0.6);
//		
		System.out.println("fall test");
		choice = new AdvancedParkingChoice();
		choice.startUp();
		
		choice.setBetaSOC(0.237);
		choice.setRequiredRestOfDayBatPerc(0.002);

		//choice.addOption(choice.new Option(null, 3.416, 2*1736, 0.784));
		choice.addOption(choice.new Option(null, 8.416, 2*1736, 0.99));

		System.out.println(choice.selectBestOption().toString());
		System.out.println("beta SOC :" +choice.getBetaSOC());
	}

}
