package playground.acmarmol.matsim2030.forecasts.timeSeriesUpdate.loaders.weges;

import java.util.ArrayList;
import java.util.TreeMap;

import playground.acmarmol.matsim2030.forecasts.timeSeriesUpdate.loaders.etappes.Etappe;
import playground.acmarmol.matsim2030.forecasts.timeSeriesUpdate.loaders.etappes.EtappenLoader;

public class TestMain {

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub

		TreeMap<String, ArrayList<Wege>> weges = WegeLoader.loadData(2000);
		
		System.out.println();
	}

}
