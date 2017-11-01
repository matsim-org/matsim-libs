/**
 * 
 */
package playground.clruch.dispatcher.SPLICE;

import java.util.ArrayList;
//import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.matsim.api.core.v01.network.Link;

import ch.ethz.idsc.queuey.math.HungarianAlgorithm;
import ch.ethz.idsc.queuey.util.GlobalAssert;
import playground.clruch.dispatcher.core.RoboTaxi;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.avtaxi.routing.AVRoute;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import playground.clruch.dispatcher.utils.BipartiteMatchingUtils;
import playground.clruch.dispatcher.utils.DistanceFunction;
import playground.clruch.dispatcher.utils.EuclideanDistanceFunction;
import playground.clruch.dispatcher.core.UniversalDispatcher;
import playground.sebhoerl.avtaxi.framework.AVModule;

/**
 * @author Nicolo Ormezzano
 *
 */
public class StackerCraneTour {
	// Fields
	private final DistanceFunction distancer;

	// Constructor
	public StackerCraneTour(DistanceFunction distancer) {
		this.distancer = distancer;

	}

	// METHODS ------- CALLABLE

	public double calculate(Collection<AVRequest> requests) {
		// double length = BPM(requests);
		BPM(requests);

		double length = 5;
		System.out.println("Length of SCT = " + length);
		return length;
	}

	// INTERNAL

	private void BPM(Collection<AVRequest> requests) {

		// since Collection::iterator does not make guarantees about the order we store
		// the pairs in a list
		final List<AVRequest> ordered_requests = new ArrayList<>(requests);

		// cost of assigning vehicle i to dest j, i.e. distance from vehicle i to
		// destination j
		final int n = ordered_requests.size(); // workers
		final int m = ordered_requests.size(); // jobs

		final double[][] requestsDistMatrix = new double[n][m];
		double lengthAvReq = 0.0; // Added N

		int i = -1;
		for (AVRequest avRequest : ordered_requests) {
			++i;
			Link DropOff = avRequest.getToLink();
			int j = -1;
			// Loop through each one again
			for (AVRequest avRequest2 : ordered_requests) {
				Link PickUp = avRequest2.getFromLink();
				// Calculate distance between Dropoff and pickup also including same request -
				// i.e. sometimes might have only one request or create single subtours?
				if (avRequest.getId() == avRequest2.getId()) {
					// for (int k = 0; k <= m-1; k++){
					requestsDistMatrix[i][++j] = 99999999; // distancer.getDistance(DropOff, PickUp);
					// }
					lengthAvReq += distancer.getDistance(DropOff, PickUp);
				} else {
					requestsDistMatrix[i][++j] = distancer.getDistance(DropOff, PickUp);
				}
			}
		}

		// DropOff at position i is assigned to PickUp matchinghungarianAlgorithm[j]
		int[] matchinghungarianAlgorithm = new HungarianAlgorithm(requestsDistMatrix).executeClruch(); // O(n^3)

		double lengthMatchingLinks = 0.0;
		for (int l = 0; l <= m - 1; l++) {
			// Calculate Length of Matching Links between dropoffs and pickups.
			lengthMatchingLinks += requestsDistMatrix[l][matchinghungarianAlgorithm[l]];
			// lengthAvReq += requestsDistMatrix[l][l]; // Length of av requests only
		}

		HashMap<Integer, ArrayList<Integer>> subtMap = checkForSubtours(requestsDistMatrix, matchinghungarianAlgorithm,
				ordered_requests);

		rewire(subtMap, requestsDistMatrix);
		//
		// do the assignment according to the Hungarian algorithm (only for the
		// matched elements, otherwise keep current drive destination)
		// final Map<RoboTaxi, AVRequest> map = new HashMap<>();
		// i = -1;
		// for (RoboTaxi vehicleLinkPair : ordered_vehicleLinkPairs) {
		// ++i;
		// if (0 <= matchinghungarianAlgorithm[i]) {
		// map.put(vehicleLinkPair,
		// ordered_requests.get(matchinghungarianAlgorithm[i]));
		// }
		// }
		//
		// GlobalAssert.that(map.size() == Math.min(n, m));
		// return map;
	}

	private HashMap<Integer, ArrayList<Integer>> checkForSubtours(double[][] requestsDistMatrix,
			int[] matchinghungarianAlgorithm, Collection<AVRequest> requests) {

		// Create HasMap<KEY,VALUE> to store key and value pairs. Doesn't hold order.
		HashMap<Integer, ArrayList<Integer>> subtourMap = new HashMap<Integer, ArrayList<Integer>>();
		HashMap<Integer, ArrayList<AVRequest>> subtourMap2 = new HashMap<Integer, ArrayList<AVRequest>>();

		ArrayList<Integer> indecesArray = new ArrayList<Integer>();
		ArrayList<Integer> indecesArray2 = new ArrayList<Integer>();

		ArrayList<AVRequest> ordered_requests = new ArrayList<>(requests);
		ArrayList<AVRequest> requestsSubT = new ArrayList<AVRequest>();
		// Create array list with Av request indeces
		for (int m = 0; m < matchinghungarianAlgorithm.length; m++) {
			indecesArray.add(matchinghungarianAlgorithm[m]);
			indecesArray2.add(matchinghungarianAlgorithm[m]);

			// indecesArray3.add(requests.get(matchinghungarianAlgorithm[m]));
			// indecesArray4.add(matchinghungarianAlgorithm[m]);
		}
		System.out.println("Currently the indecesArray list has following elements:" + indecesArray);
		boolean subtourend = false;
		int k = 0;
		int j = 0;
		// Add new arraylist in map numbered with key
		// i.e. subtour 1 (=key) and arraylist (= av requests dropff, pickup)
		subtourMap.put(j, new ArrayList<Integer>());
		// Maybe should first write array lists and then add them to the hashmap
		ArrayList<Integer> subt = new ArrayList<Integer>();

		while (subtourend == false) {

			// Check if all the indeces have been used

			indecesArray2.set(k, -1); // set index to -1

			System.out.println(
					"Removed " + k + " from indecesArray2 which has now" + "the following elements:" + indecesArray2);

			requestsSubT.add(ordered_requests.get(k));
			requestsSubT.add(ordered_requests.get(matchinghungarianAlgorithm[k]));

			subt.add(k);
			subt.add(indecesArray.get(k));
			// System.out.println("HashMap Elements: " + subtourMap);

			System.out.println("Firs index of array " + indecesArray.get(0));
			System.out.println("New one:  " + subt);

			if (subt.get(0) == subt.get(subt.size() - 1)) {
				subtourMap.put(j, subt);
				subtourMap2.put(j, requestsSubT);
				subt = new ArrayList<Integer>();
				requestsSubT = new ArrayList<AVRequest>();

				boolean found = false;
				// Loop through indeces and find first one that is not -1.
				for (Integer a : indecesArray2) {
					if (a > 0 && found == false) {
						k = indecesArray2.indexOf(a);
						found = true;
						++j;
					}
				}

			} else {

				k = indecesArray.get(k);
			}

			// Check if all indecesArray2 are = to -1 i.e. all nodes have been checked.
			if (indecesArray2.stream().distinct().limit(2).count() <= 1) {
				subtourend = true;

			}

		}

		System.out.println("Subtour Map with AV requests: " + subtourMap);
		System.out.println("Subtour Map 2 with AV requests: " + subtourMap2);
		System.out.println("Subtour Map 2 with AV requests: " + subtourMap2.size());

		return subtourMap;
	}

	// Need this method for rewiring after creating subtours
	private void rewire(HashMap<Integer, ArrayList<Integer>> subtourMap, double[][] requestsDistMatrix) {
		// ArrayList<AVRequest> totalSubt = new ArrayList<AVRequest>();
		ArrayList<Integer> totalSubt2 = new ArrayList<Integer>();
		final Random randomGenerator;
		randomGenerator = new Random();
		// int k = subtourMap.size(); // no of subtours

		int i = 0;
		int sizesubt[] = new int[subtourMap.size()];
		for (Entry<Integer, ArrayList<Integer>> entry : subtourMap.entrySet()) {
			ArrayList<Integer> v = entry.getValue();
			sizesubt[i] = v.size();
			totalSubt2.addAll(v);
			i++;
		}

		int index = randomGenerator.nextInt(sizesubt[1] / 2) * 2;
		int base = totalSubt2.get(index);
		System.out.println("From subtour Array : " + totalSubt2 + " taken " + base);
		int prev = base; // Pre is y_prev
		int startl = sizesubt[0];
		// int chosenNode = totalSubt2.get(prev-1);
		ArrayList<Integer> dtoPickupSubt = new ArrayList<Integer>();
		ArrayList<Integer> singleSTList = new ArrayList<Integer>();
		ArrayList<Integer> singleSTListOrder = new ArrayList<Integer>();
		ArrayList<Integer> scTour = new ArrayList<Integer>();
		scTour.addAll(totalSubt2.subList(0, sizesubt[0]));
		// double[sizesubt[k+1]];
		int next = 0;
		int reorderindex = 0;
		for (int k = 0; k < subtourMap.size() - 1; k++) {

			for (i = startl + 1; i < startl + sizesubt[k + 1]; i = i + 2) {
				dtoPickupSubt.add((int) requestsDistMatrix[prev][totalSubt2.get(i)]);
				// xPickUp.add(totalSubt2.get(i));
				// yDropOff.add(totalSubt2.get(i-1));
				// if (dtoPickup[i-startl]== dtoPickup[i-startl-1] && i-startl>0) {
				// dtoPickup[i-startl] = 999999;
				// }
				System.out.println("Chosen " + dtoPickupSubt);

			}
			System.out.println("Min v: " + Collections.min(dtoPickupSubt));
			reorderindex = (dtoPickupSubt.indexOf(Collections.min(dtoPickupSubt)) + 1) * 2 - 1; // index of pickup site closest
																					// to yprev within subtour
		singleSTList.addAll(totalSubt2.subList(startl, startl+sizesubt[k+1]));
		singleSTListOrder = reorder(reorderindex,singleSTList);
		scTour.addAll(index+1,singleSTListOrder);
		System.out.println("Current SCT: " + scTour);
		prev = singleSTListOrder.get(singleSTListOrder.size()-1);
		startl = startl + sizesubt[k+1];
		index = index+singleSTListOrder.size();
		dtoPickupSubt.clear();
		singleSTList.clear();
		singleSTListOrder.clear();

		}


	}

	private ArrayList<Integer> reorder(int reorder_index, ArrayList<Integer> stretchST) {
		ArrayList<Integer> stretchST2 = new ArrayList<Integer>();
		for (int i = 0; i < reorder_index; i++) {
			stretchST.add(stretchST.get(i));
		}
		stretchST2.addAll(stretchST.subList(reorder_index,stretchST.size()));
		return stretchST2;
	
	
	}

}
