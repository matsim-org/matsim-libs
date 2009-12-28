/**
 *
 */
package playground.johannes.eut;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;

/**
 * @author illenberger
 *
 * K-Shortest Path algorithm that uses the link penalty approach.
 */
public class KSPPenalty {

	public static final double DEFAULT_ALPHA_1 = 0.05;

	public static final double DEFAULT_ALPHA_2 = 0.03;

	public static final double DEFAULT_BETA = 5;

	// ===========================================================================
    // private fields
    // ===========================================================================

//	private final NetworkDecorator decoratedNet;

	private final LeastCostPathCalculator algorithm;

	private final PenaltyLinkcost penaltyLinkcost;

//	private Set<LinkDecorator> penalizedLinks;
	private java.util.Map<Link, Double> linkPenalties;

//	private static boolean logging = true;
//
	private static PrintWriter logWriter;

	// ===========================================================================
    // constants
    // ===========================================================================

	private double alpha_2 = DEFAULT_ALPHA_2;

	private double alpha_1 = DEFAULT_ALPHA_1;

	private double beta = DEFAULT_BETA;

	// ===========================================================================
    // constructors
    // ===========================================================================

	public KSPPenalty(Network network) {
//		decoratedNet = new NetworkDecorator(network);
		this.penaltyLinkcost = new PenaltyLinkcost();
		this.algorithm = newAlgorithm(network, this.penaltyLinkcost);

		try {
			if(logWriter == null)
				logWriter = new PrintWriter(EUTController.getOutputFilename("ksp.log"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

//	public KSPPenalty(NetworkLayer network, double alpha_1, double alpha_2, double beta) {
//		this(network);
//		this.alpha_1 = alpha_1;
//		this.alpha_2 = alpha_2;
//		this.beta = beta;
//	}

	// ===========================================================================
    // instance methods
    // ===========================================================================

	/**
	 * Allows subclasses to use other best path algorithms than Dijkstra.
	 * @param network The network on which the best path search is done.
	 * @return a new best path algorithm object.
	 */
	protected LeastCostPathCalculator newAlgorithm(Network network, PenaltyLinkcost pLinkcost) {
		return new Dijkstra(network, pLinkcost, pLinkcost);
	}

	/**
	 * Enables logging. Logs the number of iterations and the number of
	 * identified paths on each run. The log file will be written to the default
	 * output directory {@link Gbl#getOuputDirectory()}.
	 *
	 * @param flag
	 *            <tt>true</tt> to enable loggin, <tt>false</tt> otherwise.
	 */
//	public void enableLogging(boolean flag) {
//		logging = flag;
//		if(logging) {
//			File outDir = Gbl.getOuputDirectory();
//			if (outDir != null) {
//				try {
//					String path = Gbl.getOuputDirectory().getAbsolutePath() + "/KSPPenalty.log";
//					logWriter = new PrintWriter(new FileWriter(path));
//					logWriter.println("runs\tfound");
//				} catch (IOException e) {
//					Gbl.getLogger().log(Level.SEVERE, "IOException occured!", e);
//				}
//			} else {
//				Gbl.getLogger().warning("No output directory is specified!");
//			}
//		}
//	}

	/**
	 * Returns a list of paths identified by the K-Shortest Path algorithm. It
	 * is tried to identify <tt>count</tt> different paths, but with a maximum
	 * number of iterations of <tt>count</tt>*2.
	 *
	 * @param departure
	 *            the departure link.
	 * @param destination
	 *            the destination link.
	 * @param time
	 *            the departure time.
	 * @param count
	 *            the number of different paths that should be identified.
	 * @param linkcost
	 *            the linkcost object the best path search uses.
	 * @return a link of different paths. Can be empty if no paths have been
	 *         identified. The first path in the list is always the best path!
	 */
	public List<Path> getPaths(Node departure, Node destination, double time,
			int count, TravelTime travelTimes) {
		/*
		 * (1) Set the plain linkcost object and set the extended linkcost
		 * object for the bestpath algo.
		 */
		//penalizedLinks = new LinkedHashSet<LinkDecorator>();
		this.linkPenalties = new HashMap<Link, Double>();
		this.penaltyLinkcost.linkcost = travelTimes;
		//algorithm.setLinkCost(penaltyLinkcost);
		/*
		 * (2) Iterate until we found the required number of different paths or
		 * we exceeded the maxruns number.
		 */
		List<Path> paths = new ArrayList<Path>(count);
		int maxruns = count * 2;
		int runcount = 0;
		do {
			Path path = this.algorithm.calcLeastCostPath(departure, destination, time);
			if (path != null) {
				/*
				 * (2a) Increase the impedance on the links in the identified
				 * path.
				 */
				penalizeLinks(path);
				/*
				 * (2b) If we have not identified this path before, added to the
				 * list of different paths.
				 */
				boolean found = false;
				if (paths.isEmpty())
					paths.add(path);
				else {
					for (Path foundpaths : paths) {
						if (foundpaths.nodes.equals(path.nodes))
							found = true;
					}
					if (!found)
						paths.add(path);
				}
			}
			runcount++;
		} while ((paths.size() < count) && (runcount < maxruns));
		/*
		 * (3) Do some logging...
		 */
//		synchronized (this) {
//			if (logging)
				logWriter.println(runcount + "\t" + paths.size());
				logWriter.flush();
//		}
		/*
		 * (4) The paths we identified contain decorated nodes. So "undecorate"
		 * the paths.
		 */
//		List<Route> plainPaths = new ArrayList<Route>(paths.size());
//		for (Route path : paths)
//			plainPaths.add(undecorizePath(path));
		/*
		 * (5) Reset the impedances on the penalized links.
		 */
//		for(LinkDecorator link : penalizedLinks)
//			link.setImpedance(0);
//
		return paths;
	}

//	private Path<N> undecorizePath(Path<BasicNodeDecorator<N, L>> path) {
//		Path<N> plainPath = new Path<N>(path.getStartTime());
//		for (int i = 0; i < path.nodes().size(); i++) {
//			plainPath.appendNode(path.nodes().get(i).getDecoratedNode(),
//					path.getCosts(i), path.getTravelTime(i));
//		}
//		return plainPath;
//	}

	private void penalizeLinks(Path path) {
		/*
		 * (1) Convert the path to a list of links.
		 */
		List<Link> links = path.links;
		/*
		 * (2) Get the total length of the route.
		 */
		double length = 0;
		for (Link link : links)
			length += link.getLength();
		/*
		 * (3) Calculate the new impedance for each link in the route.
		 *
		 * (3a) Calculate all values that are not dependent on the distance.
		 */
		double gradient = (this.alpha_1 - this.alpha_2) / (length - (0.5 * length));
		double shift_x = 0.5 * length;
		double length_factor = (1 + (1 / length)) * this.beta;
		/*
		 * (3b) Loop through all links and calculate the impedance increment.
		 */
		double s = 0;
		for (Link link : links) {
			s += link.getLength();
			double increment = (this.alpha_1 - Math.abs(gradient * (shift_x - s)))
					* length_factor;

			Double impedance = this.linkPenalties.get(link);
			if(impedance != null)
				this.linkPenalties.put(link, impedance + increment);
			else
				this.linkPenalties.put(link, increment);

//			link.setImpedance(link.getImpedance() + increment);
//			penalizedLinks.add(link);
		}
	}

	/**
	 * Dummy class that for the best path algo. Applies the impedance to certain
	 * links.
	 */
	private class PenaltyLinkcost implements TravelTime, TravelCost {

		private TravelTime linkcost;

		public double getLinkTravelTime(Link link, double time) {
			Double impedance = KSPPenalty.this.linkPenalties.get(link);
			if(impedance != null)
				return this.linkcost.getLinkTravelTime(link, time) * (1+impedance);
			else
				return this.linkcost.getLinkTravelTime(link, time);
		}

		public double getLinkTravelCost(Link link, double time) {
			return getLinkTravelTime(link, time);
		}

	}

//	/**
//	 * Decorator class...
//	 */
//	private class NetworkDecorator extends BasicNetDecorator<BasicNetI, N, L> {
//
//		public NetworkDecorator(BasicNetI network) {
//			super(network);
//		}
//
//		@Override
//		protected BasicLinkDecorator<N, L> newLinkDecorator(L link, LabelId id) {
//			return new LinkDecorator(link, id, this);
//		}
//
//	}
//
//	/**
//	 * Decorator class, adds an imedance field for each link.
//	 */
//	private class LinkDecorator extends BasicLinkDecorator<N, L> {
//
//		private double impedance = 0;
//
//		protected LinkDecorator(L link, LabelId id, BasicNet network) {
//			super(link, id, network);
//		}
//
//		public void setImpedance(double impedance) {
//			this.impedance = impedance;
//		}
//
//		public double getImpedance() {
//			return impedance;
//		}
//
//	}
}
