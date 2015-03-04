package playground.balac.analysis.income;

import java.io.BufferedReader;
import java.io.IOException;

import org.matsim.core.utils.io.IOUtils;

public class IncomeFreeFloating {

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		
		final BufferedReader readLink = IOUtils.getBufferedReader("C:/Users/balacm/Desktop/STRC_Temp/FF_Stats.txt");


		String s = readLink.readLine();
		s = readLink.readLine();
				
		int c = 0;
		double time = 0.0;
		double specialStartTime = 36000;
		double specialEndTime = 57600;

		double specialTime = 0.0;
		while(s != null) {
			String[] arr = s.split("\\s");
			if (Double.parseDouble(arr[5]) != 0.0) {
			double startTime = Double.parseDouble(arr[1]);

			double endTime = Double.parseDouble(arr[2]);		
			
				
					if (startTime > specialEndTime || endTime < specialStartTime)
					
						time += (endTime - startTime);
					else {
						
						boolean startBefore = startTime < specialStartTime;
						boolean endBefore = endTime < specialEndTime;
						
						if (startBefore && endBefore) {
							c++;
							specialTime += endTime - specialStartTime;
							time += specialStartTime - startTime;
						}
						else if (!startBefore && endBefore) {
							specialTime += endTime - startTime;
						}
						else if (!startBefore && !endBefore) {
							
							specialTime += specialEndTime - startTime;
							time += endTime - specialEndTime;
						}
						else {
							
							specialTime += specialEndTime - specialStartTime;
							time += specialStartTime - startTime;
							time += endTime - specialEndTime;
						}
				
				
			}
			
			
			}
			s = readLink.readLine();		
			
		}
		double fee = 0.185;
		double specialFee = 0.37;
		double turnover = time /60.0 * fee + specialTime / 60 * specialFee;
		
		System.out.println(turnover);
		

	}

}
