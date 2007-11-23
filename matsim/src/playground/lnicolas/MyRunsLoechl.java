/* *********************************************************************** *
 * project: org.matsim.*
 * MyRunsLoechl.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.lnicolas;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.GZIPOutputStream;

import org.matsim.events.algorithms.TravelTimeCalculator;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.plans.Route;
import org.matsim.router.AStarEuclidean;
import org.matsim.router.AStarLandmarks;
import org.matsim.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.router.util.LeastCostPathCalculator;
import org.matsim.router.util.PreProcessEuclidean;
import org.matsim.router.util.PreProcessLandmarks;
import org.matsim.utils.identifiers.IdI;
import org.matsim.world.Location;

public class MyRunsLoechl extends MyRuns {

	/**
	 * The path where the resulting files should be saved.
	 */
	final static String outputPath = "/data/matsim-t/lnicolas/loechl/";

	/**
	 * @param args If args[2] == "v", for each facility, the distances and hop counts
	 * (number of nodes on the fastest path to) all facilities within
	 * a predefined radius are calculated. if args[2] == "d", the distances and hop counts
	 * to the nearest point of interest (poi) is calculated (if args[3] == "p", the poi is a
	 * public transport stop, if args[3] == "h" the poi is a highway drive up,
	 * if args[3] == "c" the poi is a city centre, if args[3] == "r" the poi is a
	 * railstation.
	 */
	public static void main(final String[] args) {
		readNetwork();
		network.connect();
		Facilities facilities = readFacilities();
		PreProcessLandmarks preRouter = new PreProcessLandmarks(
				new FreespeedTravelTimeCost());
		preRouter.run(network);
//		Gbl.createConfig(args);
		if (args[2].equals("v")) {

			TreeMap<IdI, Link> nearestLinks = new TreeMap<IdI, Link>();
			for (Location fac : facilities.getLocations().values()) {
				Link nearestLink = network.getNearestRightEntryLink(fac.getCenter());
				nearestLinks.put(fac.getId(), nearestLink);
			}

			int threadCount = Gbl.getConfig().global().getNumberOfThreads();
			Thread[] threads = new Thread[threadCount];
			for (int i = 0; i < threadCount; i++) {
				threads[i] = new CalcFacilitiesInVicinityThread(i, threadCount, outputPath, network, preRouter, nearestLinks);
			}
			for (Thread thread : threads) {
				thread.run();
			}
			System.out.println("writeFacilitiesInVicinity");
		} else if (args[2].equals("d")) {
			System.out.println("writeDistanceToDriveway");
			writeLeastDistanceToPointOfInterest(args[3]);
			System.exit(0);
		}
	}

	/**
	 * Calculates the distances to the nearest
	 * point of interest (poi) (if arg == "p", the poi is a public
	 * transport stop (assuming that the ids of public transport stops start with "99"),
	 * if arg == "h" the poi is a highway drive up (assuming that the ids
	 * of highway drive ups start with "8888"), if arg == "c"
	 * the poi is a city centre (assuming that there are 2 city centres with
	 * ids "1" and "2"), if arg == "r" the poi is a railstation (assuming
	 * that the ids of railstations start with "777").
	 * @param arg
	 */
	private static void writeLeastDistanceToPointOfInterest(final String arg) {
		Collection<Location> facs = (Gbl.getWorld().getLayer(Facilities.LAYER_TYPE)).getLocations().values();
		TreeMap<IdI, Link> nearestLinks = new TreeMap<IdI, Link>();

		// get Autobahnauffahrten, ptStops, railstations and CDBs
		ArrayList<Facility> highwayDriveUp = new ArrayList<Facility>();
		ArrayList<Facility> ptStop = new ArrayList<Facility>();
		ArrayList<Facility> railstation = new ArrayList<Facility>();
		ArrayList<Facility> cityCenter = new ArrayList<Facility>();
		ArrayList<Facility> facilities = new ArrayList<Facility>();

		for (Location loc : facs) {
			Facility fac = (Facility)loc;
			int facId = Integer.parseInt(fac.getId().toString());
			if (facId > CalcDistanceToDrivewayThread.highwayDriveUpOffset
					&& fac.getId().toString().startsWith("8888")) {
				highwayDriveUp.add(fac);
			} else if (facId > CalcDistanceToDrivewayThread.ptStopOffset
					&& fac.getId().toString().startsWith("99")) {
				ptStop.add(fac);
			} else if (facId > CalcDistanceToDrivewayThread.railstationOffset
					&& fac.getId().toString().startsWith("777")) {
				railstation.add(fac);
			} else if (facId == 1 || facId == 2) {
				cityCenter.add(fac);
			} else {
				facilities.add(fac);
			}
			Link nearestLink = network.getNearestRightEntryLink(fac.getCenter());
			nearestLinks.put(fac.getId(), nearestLink);
		}

		System.out.println("highwayDriveUps: " + highwayDriveUp.size());
		System.out.println("ptStops: " + ptStop.size());
		System.out.println("railstations: " + railstation.size());
		System.out.println("cityCenters: " + cityCenter.size());

		PreProcessLandmarks preRouter = new PreProcessLandmarks(
				new FreespeedTravelTimeCost());
		preRouter.run(network);
		LeastCostPathCalculator router = new AStarLandmarks(network, preRouter,
				new TravelTimeCalculator(network));

		String filename = outputPath
				+ (new File(Gbl.getConfig().facilities().getInputFile())).getName() + "_" + arg
				+ "_d.txt";

		String statusString = "|----------+-----------|";
		System.out.println(statusString);

		ArrayList<Facility> pointsOfInterest = null;
		int pointOfInterestIdOffset = 0;
		if (arg.equals("h")) {
			pointsOfInterest = highwayDriveUp;
			pointOfInterestIdOffset = CalcDistanceToDrivewayThread.highwayDriveUpOffset;
		} else if (arg.equals("c")) {
			pointsOfInterest = cityCenter;
			pointOfInterestIdOffset = 0;
		} else if (arg.equals("p")) {
			pointsOfInterest = ptStop;
			pointOfInterestIdOffset = CalcDistanceToDrivewayThread.ptStopOffset;
		} else if (arg.equals("r")) {
			pointsOfInterest = railstation;
			pointOfInterestIdOffset = CalcDistanceToDrivewayThread.railstationOffset;
		} else {
			System.out.println("Wrong parameter " + arg + ". Should be h, c, p or r");
			return;
		}
		BufferedWriter out;
		try {
			out = new BufferedWriter(new FileWriter(filename));
			out.write("FROM-ID" + "\t" + "TO-ID" + "\t" + "DISTANCE");
			out.newLine();

			int i = 0;
			for (Facility fac : facilities) {
				// Search nearest pointOfInterest
				double shortestDistance = Double.POSITIVE_INFINITY;
				Facility nearestEntry = null;
				for (Facility p : pointsOfInterest) {
					double routeDist = getShortestPathDistanceAndHopCount(fac, p,
							nearestLinks, router)[0];
					if (routeDist < shortestDistance) {
						shortestDistance = routeDist;
						nearestEntry = p;
					}
				}
				out.write(fac.getId() + "\t"
					+ (Integer.parseInt(nearestEntry.getId().toString()) -
							pointOfInterestIdOffset)
					+ "\t" + shortestDistance);

				i++;
				if (i % (facilities.size() / statusString.length()) == 0) {
					System.out.print(".");
					System.out.flush();
				}
			}

			out.close();
		} catch (IOException e) {
			System.out.println("Error writing to file " + filename + ": "
					+ e.getMessage());
			// e.printStackTrace();
		}
		System.out.println("Facilities written to " + filename);
	}

	private static void writeFacilitiesInVicinity() {
		Collection<Location> facs = (Gbl.getWorld().getLayer(Facilities.LAYER_TYPE)).getLocations().values();

		TreeMap<IdI, Link> nearestLinks = new TreeMap<IdI, Link>();

		for (Location fac : facs) {
			Link nearestLink = network.getNearestRightEntryLink(fac.getCenter());
			nearestLinks.put(fac.getId(), nearestLink);
		}

		PreProcessLandmarks preRouter = new PreProcessLandmarks(new FreespeedTravelTimeCost());
		preRouter.run(network);
		LeastCostPathCalculator router = new AStarLandmarks(network,
				preRouter, new TravelTimeCalculator(network));

		BufferedWriter out;
		String filename = outputPath
			+ (new File(Gbl.getConfig().facilities().getInputFile())).getName() + ".txt" + ".gz";
		try {
			out = new BufferedWriter(new OutputStreamWriter(
					new GZIPOutputStream(new FileOutputStream(filename))));
			out.write("FROM-ID" + "\t" + "TO-ID" + "\t" + "DISTANCE" + "\t" + "HOPCOUNT");
			out.newLine();

			String statusString = "|----------+-----------|";
    		System.out.println(statusString);
    		int modFac = facs.size()*facs.size() / statusString.length();
    		int i = 0;
	    	for (Location fac1 : facs) {
				for (Location fac2 : facs) {
					if (fac1.getId() != fac2.getId()
							&& fac1.getCenter().calcDistance(fac2.getCenter()) <= 5000) {
						double[] routeData = getShortestPathDistanceAndHopCount(fac1, fac2,
								nearestLinks, router);
						double routeDist = routeData[0];
						int hopCount = (int)routeData[1];

						out.write(fac1.getId() + "\t" + fac2.getId() + "\t" + routeDist + "\t" + hopCount);
						out.newLine();
					}
					i++;
					if (i % modFac == 0) {
						System.out.print(".");
						System.out.flush();
					}
				}
	    	}

			out.close();
			System.out.println("Facilities written to " + filename);
		} catch (IOException e) {
			System.out.println("Error writing to file " + filename + ": "
					+ e.getMessage());
		}
	}

	synchronized static double[] getShortestPathDistanceAndHopCount(final Location fac1,
			final Location fac2, final Map<IdI, Link> nearestLinks, final LeastCostPathCalculator router) {
		Link fromLink = nearestLinks.get(fac1.getId());
		Link toLink = nearestLinks.get(fac2.getId());
		Node fromNode = fromLink.getToNode();
		Node toNode = toLink.getFromNode();
		Route route = router.calcLeastCostPath(fromNode, toNode, 0);
		double routeDist = route.getDist();
		routeDist += fromLink.calcDistance(fac1.getCenter());
		routeDist += toLink.calcDistance(fac2.getCenter());
		routeDist += fromLink.getLength();
		double[] result = new double[2];
		result[0] = routeDist;
		result[1] = route.getRoute().size() - 2;
		return result;
	}

	static class CalcDistanceToDrivewayThread extends Thread {

		Collection<Facility> facilities = null;

		Collection<Facility> highwayDriveUp = null;

		Collection<Facility> ptStop = null;

		Collection<Facility> railstation = null;

		Collection<Facility> cityCenter = null;

		Map<IdI, Link> nearestLinks = null;

		LeastCostPathCalculator router = null;

		int id = -1;

		ArrayList<String> resultStrings = new ArrayList<String>();

		String type = null;

		public static final int highwayDriveUpOffset = 8888000;

		public static final int ptStopOffset = 9900000;

		public static final int railstationOffset = 7770000;

		CalcDistanceToDrivewayThread(final Collection<Facility> facilities,
				final Collection<Facility> highwayDriveUp,
				final Collection<Facility> ptStop, final Collection<Facility> railstation,
				final Collection<Facility> cityCenter,
				final Map<IdI, Link> nearestLinks, final PreProcessEuclidean preRouter,
				final NetworkLayer network, final int id, final String type) {
			this.facilities = facilities;
			this.nearestLinks = nearestLinks;
			this.highwayDriveUp = highwayDriveUp;
			this.ptStop = ptStop;
			this.railstation = railstation;
			this.cityCenter = cityCenter;
			this.router = new AStarEuclidean(network, preRouter, new TravelTimeCalculator(network));
			this.id = id;
			this.type = type;
		}

		// This method is called when the thread runs
		@Override
		public void run() {
			String statusString = "|----------+-----------|";
			if (this.id == 0) {
				System.out.println(statusString);
			}

			int i = 0;
			for (Facility fac : this.facilities) {
				// Search nearest highwayDriveUp
				double shortestDistance = Double.POSITIVE_INFINITY;
				Facility nearestEntry = null;
				if (this.type.equals("h")) {
					for (Facility p : this.highwayDriveUp) {
						double routeDist = getShortestPathDistanceAndHopCount(
								fac, p, this.nearestLinks, this.router)[0];
						if (routeDist < shortestDistance) {
							shortestDistance = routeDist;
							nearestEntry = p;
						}
					}
					this.resultStrings.add(fac.getId() + "\t"
							+ (Integer.parseInt(nearestEntry.getId().toString()) -
									highwayDriveUpOffset)
							+ "\t" + shortestDistance);

					shortestDistance = Double.POSITIVE_INFINITY;
					for (Facility p : this.cityCenter) {
						double routeDist = getShortestPathDistanceAndHopCount(fac, p,
										this.nearestLinks, this.router)[0];
						if (routeDist < shortestDistance) {
							shortestDistance = routeDist;
							nearestEntry = p;
						}
					}
					this.resultStrings.add(fac.getId() + "\t" + nearestEntry.getId()
							+ "\t" + shortestDistance);
				} else if (this.type.equals("p")) {
					shortestDistance = Double.POSITIVE_INFINITY;
					for (Facility p : this.ptStop) {
						double routeDist = getShortestPathDistanceAndHopCount(fac, p,
										this.nearestLinks, this.router)[0];
						if (routeDist < shortestDistance) {
							shortestDistance = routeDist;
							nearestEntry = p;
						}
					}
					this.resultStrings.add(fac.getId() + "\t"
							+ (Integer.parseInt(nearestEntry.getId().toString()) -
									ptStopOffset) + "\t"
							+ shortestDistance);

					shortestDistance = Double.POSITIVE_INFINITY;
					for (Facility p : this.railstation) {
						double routeDist = getShortestPathDistanceAndHopCount(fac, p,
										this.nearestLinks, this.router)[0];
						if (routeDist < shortestDistance) {
							shortestDistance = routeDist;
							nearestEntry = p;
						}
					}
					this.resultStrings.add(fac.getId() + "\t"
							+ (Integer.parseInt(nearestEntry.getId().toString()) -
									railstationOffset) + "\t"
							+ shortestDistance);
				}

				i++;
				if (this.id == 0
						&& i % (this.facilities.size() / statusString.length()) == 0) {
					System.out.print(".");
					System.out.flush();
				}
			}
		}

		String[] getResult() {
			return this.resultStrings.toArray(new String[this.resultStrings.size()]);
		}
	}
}

/**
 * for each facility, all facilities within a predefined  distance
 * <code>vicinityRadius</code> are calculated.
 * @author lnicolas
 *
 */
class CalcFacilitiesInVicinityThread extends Thread {

	int id = -1;

	int threadCount = -1;

	final static int vicinityRadius = 5000;

	private final String outputPath;

	private final NetworkLayer network;

	private final PreProcessLandmarks preRouter;

	private final TreeMap<IdI, Link> nearestLinks;

	CalcFacilitiesInVicinityThread(final int id, final int threadCount, final String outputPath, final NetworkLayer network,
			final PreProcessLandmarks preRouter, final TreeMap<IdI, Link> nearestLinks) {
		super(Integer.toString(id));
		this.id = id;
		this.threadCount = threadCount;
		this.outputPath = outputPath;
		this.network = network;
		this.preRouter = preRouter;
		this.nearestLinks = nearestLinks;
	}

	/**
	 * This method is called when the thread runs
	 */
	@Override
	public void run() {
		Facilities facs = (Facilities)Gbl.getWorld().createLayer(Facilities.LAYER_TYPE, null);

		String[] tmp = (new File(Gbl.getConfig().facilities().getInputFile())).getName().split("/");
		String filename = this.outputPath + tmp[tmp.length - 1] + this.id + ".txt.gz";
		System.out.println("Output file name: " + filename);

		Collection<Location> facilities = facs.getLocations().values();
		String statusString = "|----------+-----------|";

		LeastCostPathCalculator router = new AStarLandmarks(this.network, this.preRouter,
				new FreespeedTravelTimeCost());

		int chunkSize = (facilities.size()) / this.threadCount;
		int startIndex = (this.id * chunkSize);
		int endIndex = ((this.id + 1) * chunkSize) - 1;
		if (this.id == this.threadCount - 1) {
			endIndex = facilities.size();
		}
		System.out.println("Thread " + this.id + ": " + " starting at the "
				+ startIndex + "th facility and ending at the " + endIndex
				+ "th facility (nof facilities: " + facilities.size() + ")");
		System.out.println(statusString);
		int i = 0;
		int facCnt = 0;
		ByteArrayOutputStream os = null;
		try {
			os = new ByteArrayOutputStream();
			GZIPOutputStream gzos = new GZIPOutputStream(os);
			String line = "FROM-ID" + "\t" + "TO-ID" + "\t" + "DISTANCE" + "\t"
			+ "HOPCOUNT" + "\n";
			gzos.write(line.getBytes(), 0, line.length());
			for (Location fac1 : facilities) {
				if (i >= startIndex && i < endIndex) {
					for (Location fac2 : facilities) {
						if (fac1.getId() != fac2.getId()
								&& fac1.getCenter()
								.calcDistance(fac2.getCenter()) <= CalcFacilitiesInVicinityThread.vicinityRadius) {
							double routeDist = MyRunsLoechl.getShortestPathDistanceAndHopCount(fac1,
									fac2, this.nearestLinks, router)[0];

							line = fac1.getId() + "\t" + fac2.getId() + "\t"
							+ routeDist + "\n";
							gzos.write(line.getBytes(), 0, line.length());
						}
					}
					facCnt++;
					if (this.id == 0
							&& facCnt % (chunkSize / statusString.length()) == 0) {
						System.out.print(".");
						System.out.flush();
					}
				}

				i++;
			}
			gzos.finish();
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}

		BufferedWriter out;
		try {
			out = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(filename)));

			out.write(os.toString());

			out.close();
		} catch (FileNotFoundException e) {
			Gbl.errorMsg(e);
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
		System.out.println("Facilities written to " + filename);
	}
}

