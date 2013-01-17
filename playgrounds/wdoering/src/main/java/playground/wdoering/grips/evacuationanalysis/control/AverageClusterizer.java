package playground.wdoering.grips.evacuationanalysis.control;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;

import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.Tuple;

public class AverageClusterizer implements Clusterizer {

	@Override
	public LinkedList<Tuple<Id,Double>> getClusters(LinkedList<Tuple<Id, Double>> data, int n)
	{
		LinkedList<Tuple<Id,Double>> clusters = new LinkedList<Tuple<Id,Double>>(); 
				
		Collections.sort(data, new Comparator<Tuple<Id,Double>>() {

			@Override
			public int compare(Tuple<Id, Double> o1, Tuple<Id, Double> o2) {
				if (o1.getSecond()>o2.getSecond())
					return 1;
				else if (o1.getSecond()<o2.getSecond())
					return -1;
				return 0;
			}
		});
		
//		System.out.println();
//		System.out.println("~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~");
//		System.out.println("clusters:");
//		for (Tuple<Id,Double>cluster : data)
//			System.out.println(cluster.getFirst() + ":" + cluster.getSecond());
//		System.out.println();
//		System.out.println("~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~");
		
		int m = (data.size()/n)>0?(data.size()/n):1; int i = 0;
		
//		System.out.println("m:" + m + "|n:" + n + "|size:" + data.size());
		
		for (Tuple<Id,Double> element : data)
		{
			if ((i++%m==0) && clusters.size()<n)
			{
				clusters.add(element);
//				System.out.println(element.getFirst() + ":" + element.getSecond());
			}
				
		}
		
		// just in case (this is a lazy solution, just for now)
		while (clusters.size()<n)
			clusters.add(clusters.get(clusters.size()-1));
		
		
		return clusters;
	}

	
	


	
	
}
