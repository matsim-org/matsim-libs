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
import java.util.Map.Entry;

import org.matsim.api.core.v01.network.Link;

import ch.ethz.idsc.queuey.math.HungarianAlgorithm;
import ch.ethz.idsc.queuey.util.GlobalAssert;
import playground.clruch.dispatcher.utils.NetworkDistanceFunction;
import playground.sebhoerl.avtaxi.passenger.AVRequest;

/**
 * @author Nicolo Ormezzano
 *
 */
public class StackerCraneTour {
	// Fields
	private List<AVRequest> scTAVrequests = new ArrayList<AVRequest>();
	private final NetworkDistanceFunction distancer;
	
	public StackerCraneTour(List<AVRequest> scTAVrequests, NetworkDistanceFunction distancer) {
		super();
		this.scTAVrequests = scTAVrequests;
		this.distancer = distancer;
	}
	
	// External
	// Calculate Stacker crane tour
	@SuppressWarnings("unused")
    public List<AVRequest> calculate(Collection<AVRequest> requests) {

		// since Collection::iterator does not make guarantees about the order we store
		// the pairs in a list
		final List<AVRequest> ordered_requests = new ArrayList<>(requests);

		// cost of assigning dropoff i to pickup j, i.e. distance from link i to
		// link j
		final int n = ordered_requests.size(); // workers
		final int m = ordered_requests.size(); // jobs

		final double[][] requestsDistMatrix = new double[n][m];
        double lengthAvReq=0.0; // 
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
					// Set distance from i to j (if equal i.e. same dropoff and pickup of same
					// request) to inf,
					// not to create single subtours.
					requestsDistMatrix[i][++j] = 99999999; 
					lengthAvReq += distancer.getDistance(DropOff, PickUp); // unused for now
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
		// if many subt ---> rewire, else convert hashmap to array
		if (subtMap.size() > 1) {

			scTour = rewire(subtMap, requestsDistMatrix);
		} else {
			scTour = fromHashtoIntArray(subtMap);

		}
		
		scTAVrequests = fromIntSctToAVsct (scTour, ordered_requests);
		return scTAVrequests;

	}
	
	
	//--------------------------- Internal
	// Get indexes of requests every 2 as scTour (arraylist) is in the format 0,2,2,3,3,6,6,...
	private List<AVRequest> fromIntSctToAVsct (ArrayList<Integer> scTour, List<AVRequest> ordered_requests) {
		ArrayList<AVRequest> scTAVrequests = new ArrayList<AVRequest>();
			for(int i = 0; i< scTour.size(); i = i+2) {
				scTAVrequests.add(ordered_requests.get(scTour.get(i)));
			}
		return scTAVrequests;	
	}

	
	private HashMap<Integer, ArrayList<Integer>> checkForSubtours(double[][] requestsDistMatrix,
			int[] matchinghungarianAlgorithm, Collection<AVRequest> requests) {

		// Create HasMap<KEY,VALUE> to store key and value pairs. 
		HashMap<Integer, ArrayList<Integer>> subtourMap = new HashMap<Integer, ArrayList<Integer>>();

		ArrayList<Integer> bpMatchingOut = new ArrayList<Integer>();
		ArrayList<Integer> expandedRequests = new ArrayList<Integer>();
		
		// --------Could also create arrays of requests directly instead of integers
//		ArrayList<AVRequest> ordered_requests = new ArrayList<>(requests);
//		// Array to hold requests forming up a subtour
//		ArrayList<AVRequest> requestsSubT = new ArrayList<AVRequest>();
		// --------
		
		// Fill in array lists with Av request indexes
for (int m = 0; m < matchinghungarianAlgorithm.length; m++) {
			// one to hold hungarian algorithm output intact
			bpMatchingOut.add(matchinghungarianAlgorithm[m]);
			// one to keep track of requests already expanded
			expandedRequests.add(matchinghungarianAlgorithm[m]);

		}
//		System.out.println("Currently the indecesArray list has following elements:" + bpMatchingOut);

		boolean subtourend = false;
		int k = 0;
		int j = 0;
				
		// First write array lists and then add them to the hashmap
		// i.e. subtour 1 (=key) and arraylist (= av requests dropoff, pickup)
		ArrayList<Integer> intSTour = new ArrayList<Integer>();

		while (subtourend == false) {

			// set index to -1 to accessed requests in 2nd array of indexes.
			expandedRequests.set(k, -1);
//			System.out.println("Removed " + k + " from indecesArray2 which has now" + "the following elements:"
//					+ expandedRequests);

			// Add requests as integers 
			intSTour.add(k);
			intSTour.add(bpMatchingOut.get(k));
			// System.out.println("HashMap Elements: " + subtourMap);

//			System.out.println("First index of array " + bpMatchingOut.get(0));
//			System.out.println("New one:  " + intSTour);

			// If arraylist has equal first and last value --> Subtour
			if (intSTour.get(0) == intSTour.get(intSTour.size() - 1)) {
				// Add Subtour to integer map.
				subtourMap.put(j, intSTour);
				// Clear subt 
				intSTour = new ArrayList<Integer>();

				boolean found = false;
				// Loop through indexes and find first one that is not -1.
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
				// Indexes and values are related. i.e. at index 0 you could have value 2;
				// meaning the subtour consists of starting from the 0th request dropoff and
				// moving to the second one for pickup. Hence at index = 2 there will be eiher a 0 i.e.
				// the subtour is finished, or another number, meaning it continues to another
				// request.
				k = bpMatchingOut.get(k);
			}

			// Check if all expandedRequests are = to -1 i.e. all nodes have been checked.
			if (expandedRequests.stream().distinct().limit(2).count() <= 1) {
				subtourend = true;

			}

		}

		System.out.println("Subtour Map with subtours of requests as integers: " + subtourMap);
		System.out.println("Found " + subtourMap.size() + " Subtours!!!");

		return subtourMap;
	}

	// Need this method for rewiring after creating subtours ie. stitching subtours together
	private ArrayList<Integer> rewire(HashMap<Integer, ArrayList<Integer>> subtourMap, double[][] requestsDistMatrix) {

		// ArrayList<AVRequest> totalSubt = new ArrayList<AVRequest>();
		// Initialize set S that will hold all the subtours one after the other.
		ArrayList<Integer> setS = new ArrayList<Integer>();
		// Get random number to choose randomly link to be opened to connect subtours.
		//final Random randomGenerator;
		//randomGenerator = new Random();
		
		//Not used, using arbitrary index choice

		int i = 0;
		int sizeStours[] = new int[subtourMap.size()]; // To hold sizes of subtours
		
		for (Entry<Integer, ArrayList<Integer>> entry : subtourMap.entrySet()) {
			ArrayList<Integer> v = entry.getValue();
			GlobalAssert.that(v.size() >= 4); // Check that subtours have at least 2 requests
			sizeStours[i] = v.size();
			setS.addAll(v); // Add them all up to in the set S
			i++;
		}

		// index of initial arbitrary delivery site in S1
		int index = 2;//randomGenerator.nextInt(sizeStours[1] / 2) * 2;
		// Base is the request index from which the 1st subtour (S1) is opened
		int base = setS.get(index);
		System.out.println("Subtour set S : " + setS);
		int prev = base; //
		// Start subtour index
		int startSInd = sizeStours[0];
		// Array with distance to pickup from chosen delivery point & temp lists
		ArrayList<Integer> dtoNext = new ArrayList<Integer>();
		ArrayList<Integer> tmpSTList = new ArrayList<Integer>();
		ArrayList<Integer> tmpSTListOrder = new ArrayList<Integer>();
		ArrayList<Integer> scTour = new ArrayList<Integer>(); // Final Stacker crane tour stored here
		scTour.addAll(setS.subList(0, sizeStours[0])); // Add S1 in scTour

		// Do for all subtours
		for (int k = 0; k < subtourMap.size() - 1; k++) {

			// Loop through the S (k+1) subtour within the setS array.. need to start from [1]
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
			
			// Find index of pickup site closest to y prev (dropoff site) by looping through the
			// distances to y_prev i.e.
			// prev variable. Add the +1)*2-1 to correct for the different indexing as in
			// for loop -->i = i+2.
			// reorder_index --> next in paper doi. 10.1109/CDC.2011.6161406
			int reorder_index = (dtoNext.indexOf(Collections.min(dtoNext)) + 1) * 2 - 1;

			// create a temporary subtour list to have that can be reordered depending on
			// the chosen x_next.
			tmpSTList.addAll(setS.subList(startSInd, startSInd + sizeStours[k + 1]));
			tmpSTListOrder = reorder(reorder_index, tmpSTList); // Use method to reorder subtour.
			// Merge to current arraylist
			scTour.addAll(index + 1, tmpSTListOrder);
			// System.out.println("Current SCT: " + scTour);
			
			// set prev -- i.e. by getting x_next i.e. where the next branch is added.
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
