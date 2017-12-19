package playground.clruch.io.fleet;

import java.util.Arrays;
import java.util.List;

public class AVStatusSF {

	public static List<Object> status(List<String> coord1, List<String> coord2, List<String> occup, List<String> time,
			int x) {
		System.out.println("x= " + x);
		Object[] array = new Object[x];

		for (int s = 1; s < x; s++) {
			Long timing = Long.parseLong(time.get(s)) - Long.parseLong(time.get(s - 1));
			double space1 = Math.abs((Double.parseDouble(coord1.get(s)) - Double.parseDouble(coord1.get(s - 1))));
			double space2 = Math.abs(Double.parseDouble(coord2.get(s)) - Double.parseDouble(coord2.get(s - 1)));

			if (isOccupied(occup, s) == true)
				array[s] = "DRIVEWITHCOSTUMER";
			else {
				if (moving(timing, space1, space2, x) == true) {
					array[s] = "REBALANCE";
				} else {
					if (stayLongTime(timing) == true)
						array[s] = "OFFSERVICE";
					else
						array[s] = "STAY";
				}
			}
		}
		List<Object> lista = Arrays.asList(array);
		return lista;
	}

	private static boolean isOccupied(List<String> occup, int s) {
		if (occup.get(s).equals("1"))
			return true;
		else
			return false;
	}

	private static boolean moving(Long timing, double space1, double space2, int x) {
		if (space1 > 0.0001 && space2 > 0.0001 && timing < 100)
			return true;
		else
			return false;
	}

	private static boolean stayLongTime(Long timing) {
		if (timing > 3600)
			return true;
		else
			return false;

	}
}