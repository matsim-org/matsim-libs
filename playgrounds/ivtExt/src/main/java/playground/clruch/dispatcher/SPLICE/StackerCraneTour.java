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

		// cost of assigning dropoff i to pickup j, i.e. distance from link i to
		// link j
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
					// Set distance from i to j (if equal i.e. same dropoff and pickup of same
					// request) to inf,
					// not to create single subtours.
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
		ArrayList<Integer> scTour = new ArrayList<Integer>();
		if (subtMap.size() > 1) {

			scTour = rewire(subtMap, requestsDistMatrix);
		} else {
			scTour = fromHashtoIntArray(subtMap);

		}
	

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

		ArrayList<Integer> bpMatchingOut = new ArrayList<Integer>();
		ArrayList<Integer> expandedRequests = new ArrayList<Integer>();

		ArrayList<AVRequest> ordered_requests = new ArrayList<>(requests);
		// Array to hold requests forming up a subtour
		ArrayList<AVRequest> requestsSubT = new ArrayList<AVRequest>();
		// Create array list with Av request indeces
		for (int m = 0; m < matchinghungarianAlgorithm.length; m++) {
			// array to hold hungarian algorithm output intact
			bpMatchingOut.add(matchinghungarianAlgorithm[m]);
			// array to keep track of requests already expanded
			expandedRequests.add(matchinghungarianAlgorithm[m]);

		}
		System.out.println("Currently the indecesArray list has following elements:" + bpMatchingOut);
		boolean subtourend = false;
		int k = 0;
		int j = 0;
		// Add new arraylist in map numbered with key
		// i.e. subtour 1 (=key) and arraylist (= av requests dropff, pickup)
		subtourMap.put(j, new ArrayList<Integer>());
		// Maybe should first write array lists and then add them to the hashmap
		ArrayList<Integer> intSTour = new ArrayList<Integer>();

		while (subtourend == false) {

			// set index to -1 to accessed requests in 2nd array of indeces.
			expandedRequests.set(k, -1);
			System.out.println("Removed " + k + " from indecesArray2 which has now" + "the following elements:"
					+ expandedRequests);

			// Add requests as AV requests and as integeres-- still have to decide which
			// format to use for later.
			// i.e. These arrays hold the subtours in different format
			requestsSubT.add(ordered_requests.get(k));
			requestsSubT.add(ordered_requests.get(matchinghungarianAlgorithm[k]));

			intSTour.add(k);
			intSTour.add(bpMatchingOut.get(k));
			// System.out.println("HashMap Elements: " + subtourMap);

			System.out.println("Firs index of array " + bpMatchingOut.get(0));
			System.out.println("New one:  " + intSTour);

			// If arraylist has equal first and last value --> Subtour
			if (intSTour.get(0) == intSTour.get(intSTour.size() - 1)) {
				// Add Subtour to integer and AVrequest maps.
				subtourMap.put(j, intSTour);
				subtourMap2.put(j, requestsSubT);
				// Clear subt and requestsSubT
				intSTour = new ArrayList<Integer>();
				requestsSubT = new ArrayList<AVRequest>();

				boolean found = false;
				// Loop through indeces and find first one that is not -1.
				for (Integer a : expandedRequests) {
					if (a > 0 && found == false) {
						// Set k equal to the index of the first value not equal to -1 in expanded req.
						k = expandedRequests.indexOf(a);
						found = true;
						++j;
					}
				}

			} else {
				// If subtour extraction is still ongoing then get next value.
				// Indeces and values are related. i.e. at index 0 you could have value 2;
				// meaning the subtour consists of starting from the 0th request dropoff and
				// moving to the second one. Hence at index = 2 there will be eiher a 0 i.e.
				// the subtour is finished, or another number, meaning it continues to another
				// request.
				k = bpMatchingOut.get(k);
			}

			// Check if all indecesArray2 are = to -1 i.e. all nodes have been checked.
			if (expandedRequests.stream().distinct().limit(2).count() <= 1) {
				subtourend = true;

			}

		}

		System.out.println("Subtour Map with subtours of requests as integers: " + subtourMap);
		// System.out.println("Subtour Map 2 with subtours of requests as AV requests: "
		// + subtourMap2);
		System.out.println("Found " + subtourMap2.size() + " Subtours!!!");

		return subtourMap;
	}

	// Need this method for rewiring after creating subtours
	private ArrayList<Integer> rewire(HashMap<Integer, ArrayList<Integer>> subtourMap, double[][] requestsDistMatrix) {

		// ArrayList<AVRequest> totalSubt = new ArrayList<AVRequest>();
		// Initialize set S that will hold all the subtours one after the other.
		ArrayList<Integer> setS = new ArrayList<Integer>();
		// Get random number to choose randomly link to be opened to connect subtours.
		final Random randomGenerator;
		randomGenerator = new Random();
		// int k = subtourMap.size(); // no of subtours

		int i = 0;
		int sizeStours[] = new int[subtourMap.size()]; // To hold sizes of subtours
		for (Entry<Integer, ArrayList<Integer>> entry : subtourMap.entrySet()) {
			ArrayList<Integer> v = entry.getValue();
			GlobalAssert.that(v.size() >= 4); // Check that subtours have at least 2 requests
			sizeStours[i] = v.size();
			setS.addAll(v); // Add them all up to for the set S
			i++;
		}

		// index oof arbitraty (random) delivery site in S1
		int index = randomGenerator.nextInt(sizeStours[1] / 2) * 2;
		// Base is the request index from which the 1st subtour (S1) is opened
		int base = setS.get(index);
		System.out.println("Subtour set S : " + setS);// + " , opened the link " + base);
		int prev = base; //
		// Start subtour index
		int startSInd = sizeStours[0];
		// Array with distance to pickup from chosen delivery point
		ArrayList<Integer> dtoNext = new ArrayList<Integer>();
		ArrayList<Integer> tmpSTList = new ArrayList<Integer>();
		ArrayList<Integer> tmpSTListOrder = new ArrayList<Integer>();
		ArrayList<Integer> scTour = new ArrayList<Integer>(); // Stacker crane tour stored here
		scTour.addAll(setS.subList(0, sizeStours[0])); // Add S1 in scTour

		int next = 0; // TO be use
		// Do for all subtours
		for (int k = 0; k < subtourMap.size() - 1; k++) {

			// Loop through the S (k+1) subtour within the setS array.. have to start from 1
			// rather than [0]
			// as subtours are always labelled D,P,D,P,D,P,D,P... where D = dropoff integer
			// and P = pickup.
			// Also, jump every 2 numbers so not too have duplicates i.e. 3,3 ---> (D,P)
			// ---> take only P.
			for (i = startSInd + 1; i < startSInd + sizeStours[k + 1]; i = i + 2) {
				dtoNext.add((int) requestsDistMatrix[prev][setS.get(i)]);
				// System.out.println("Chosen " + dtoNext);
			}
			// System.out.println("Min v: " + Collections.min(dtoNext));
			// Find index of pickup site closest to to y_prev by looping through the
			// distances to y_prev i.e.
			// prev variable. add the +1)*2-1 to correct for the different indexing as in
			// for loop -->i = i+2.
			// reorder_index --> next in paper.
			int reorder_index = (dtoNext.indexOf(Collections.min(dtoNext)) + 1) * 2 - 1;

			// create a temporary subtour list to have that can be reordered depending on
			// the chosen x_next.
			tmpSTList.addAll(setS.subList(startSInd, startSInd + sizeStours[k + 1]));
			tmpSTListOrder = reorder(reorder_index, tmpSTList); // Use method to reorder subtour.
			// Merge to current arraylist
			scTour.addAll(index + 1, tmpSTListOrder);
			// System.out.println("Current SCT: " + scTour);
			// set prev -- i.e. b getting x_next i.e. where the next branch is added.
			// This is found by getting the last value on the ordered subtour list i.e.
			// it corresponds to the request dropoff before x_next
			prev = tmpSTListOrder.get(tmpSTListOrder.size() - 1);
			// Update Starting Index in totalsubtour set and in the scTour arraylist.
			startSInd = startSInd + sizeStours[k + 1]; // update the Subtour index
			index = index + tmpSTListOrder.size(); // Index where the next subtour is branched from
			// Clear All arraylists
			dtoNext.clear();
			tmpSTList.clear();
			tmpSTListOrder.clear();

		}
		System.out.println("Current SCT: " + scTour);
		return scTour;
	}

	private ArrayList<Integer> reorder(int reorder_index, ArrayList<Integer> stretchST) {
		ArrayList<Integer> stretchST2 = new ArrayList<Integer>();
		for (int i = 0; i < reorder_index; i++) {
			stretchST.add(stretchST.get(i));
		}
		stretchST2.addAll(stretchST.subList(reorder_index, stretchST.size()));
		return stretchST2;

	}

	private ArrayList<Integer> fromHashtoIntArray(HashMap<Integer, ArrayList<Integer>> map){
		ArrayList<Integer> arrayl = new ArrayList<Integer>();
		for (Entry<Integer, ArrayList<Integer>> entry : map.entrySet()) {
			ArrayList<Integer> v = entry.getValue();
			arrayl.addAll(v); // Add them all up to for the set S
		}
		return arrayl;
	}
	
}
