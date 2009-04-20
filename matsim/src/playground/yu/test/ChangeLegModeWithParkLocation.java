/**
 * 
 */
package playground.yu.test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.BasicScenario;
import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.api.facilities.Facilities;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.population.Activity;
import org.matsim.core.api.population.Leg;
import org.matsim.core.api.population.Plan;
import org.matsim.core.api.population.PlanElement;
import org.matsim.core.api.replanning.PlanStrategyModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.StrategyManagerConfigLoader;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.replanning.modules.ChangeLegMode;
import org.matsim.core.replanning.modules.ExternalModule;
import org.matsim.core.replanning.modules.PlanomatModule;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.modules.ReRouteDijkstra;
import org.matsim.core.replanning.modules.ReRouteLandmarks;
import org.matsim.core.replanning.modules.TimeAllocationMutator;
import org.matsim.core.replanning.selectors.BestPlanSelector;
import org.matsim.core.replanning.selectors.ExpBetaPlanChanger;
import org.matsim.core.replanning.selectors.ExpBetaPlanSelector;
import org.matsim.core.replanning.selectors.KeepSelected;
import org.matsim.core.replanning.selectors.PathSizeLogitSelector;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.PreProcessLandmarks;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.StringUtils;
import org.matsim.locationchoice.LocationChoice;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.socialnetworks.replanning.RandomFacilitySwitcherF;
import org.matsim.socialnetworks.replanning.RandomFacilitySwitcherK;
import org.matsim.socialnetworks.replanning.SNPickFacilityFromAlter;

/**
 * this class contains some codes from ChangeLegMode
 * 
 * @author ychen
 * 
 */
public class ChangeLegModeWithParkLocation extends AbstractMultithreadedModule {
	private final static String CONFIG_MODULE = "changeLegModeWithParkLocation";
	private final static String CONFIG_PARAM_MODES = "modes";
	private TransportMode[] availableModes = new TransportMode[] {
			TransportMode.car, TransportMode.pt };

	public ChangeLegModeWithParkLocation() {
	}

	/**
	 * @param availableModes
	 *            an array for TransportMode
	 */
	public ChangeLegModeWithParkLocation(final TransportMode[] availableModes) {
		super();
		this.availableModes = availableModes.clone();
	}

	public ChangeLegModeWithParkLocation(final Config config) {
		String modes = config.findParam(CONFIG_MODULE, CONFIG_PARAM_MODES);
		if (modes != null) {
			String[] parts = StringUtils.explode(modes, ',');
			this.availableModes = new TransportMode[parts.length];
			for (int i = 0, n = parts.length; i < n; i++) {
				this.availableModes[i] = TransportMode.valueOf(parts[i].trim());
			}
		}
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		return new ChooseRandomLegModeWithParkLink(availableModes, MatsimRandom
				.getLocalInstance());
	}

	public static class ChooseRandomLegModeWithParkLink implements
			PlanAlgorithm {
		private TransportMode[] possibleModes;

		private Random rnd;

		public ChooseRandomLegModeWithParkLink(TransportMode[] possibleModes,
				Random rnd) {
			this.possibleModes = possibleModes;
			this.rnd = rnd;
		}

		public void run(Plan plan) {
			int peSize = plan.getPlanElements().size();
			if (peSize > 1) {
				boolean done = false;
				while (!done) {
					// Idx for leg in PlanElements, whose TransportMode would be
					// changed.
					int legIdx = rnd.nextInt(peSize / 2) * 2 + 1;

					TransportMode tm = ((Leg) plan.getPlanElements()
							.get(legIdx)).getMode();

					int crtModeIdx = -1;
					for (int i = 0; i < possibleModes.length; i++) {
						if (possibleModes[i].equals(tm))
							crtModeIdx = i;
					}

					int newModeIdx = -1;
					while (newModeIdx < 0 || newModeIdx == crtModeIdx) {
						newModeIdx = rnd.nextInt(possibleModes.length);
					}

					TransportMode newMode = possibleModes[newModeIdx];
					if (tm.equals(TransportMode.car)) {
						done = changeCar2UnCar(plan, legIdx, newMode);
					} else {
						if (newMode.equals(TransportMode.car)) {
							done = changeUnCar2Car(plan, legIdx);
						} else
							done = changeUnCar2UnCar(plan, legIdx, newMode);
					}
				}
			}
		}

		private boolean changeUnCar2UnCar(Plan plan, int legIdx,
				TransportMode mode) {
			Leg leg = (Leg) plan.getPlanElements().get(legIdx);
			leg.setMode(mode);
			return leg != null;
		}

		private boolean changeCar2UnCar(Plan plan, int legIdx,
				TransportMode mode) {
			CarLegChain clc = new CarLegChain(plan);
			int l = -1, r = -1;// boundary of the CarLegChain, where the legIdx
			// stands.
			for (Tuple<Integer, Integer> tuple : clc.getActIdxs4CarLegs()) {
				if (tuple.getFirst() < legIdx && tuple.getSecond() > legIdx) {
					l = tuple.getFirst();
					r = tuple.getSecond();
					break;
				}
			}
			List<PlanElement> pes = plan.getPlanElements();

			ParkLocation leftPl = new ParkLocation((Activity) pes.get(l));
			List<Tuple<Integer, Integer>> linkTuples = new ArrayList<Tuple<Integer, Integer>>();
			int tmpL = l;
			for (int i = l + 2; i < r; i += 2) {
				if (new ParkLocation((Activity) pes.get(i)).equals(leftPl)) {
					linkTuples.add(new Tuple<Integer, Integer>(tmpL, i));
					tmpL = i;
				}
			}

			ParkLocation rightPl = new ParkLocation((Activity) pes.get(l));
			List<Tuple<Integer, Integer>> rightTuples = new ArrayList<Tuple<Integer, Integer>>();
			int tmpR = r;
			for (int j = r - 2; j > l; j -= 2)
				if (new ParkLocation((Activity) pes.get(j)).equals(rightPl)) {
					rightTuples.add(new Tuple<Integer, Integer>(j, tmpR));
					tmpR = j;
				}

			List<Tuple<Integer, Integer>> midTuples = new ArrayList<Tuple<Integer, Integer>>();
			for (int i = l + 2; i <= r - 4; i += 2)
				for (int j = r - 2; j > i; j -= 2)
					if (new ParkLocation((Activity) pes.get(i))
							.equals(new ParkLocation((Activity) pes.get(j))))
						midTuples.add(new Tuple<Integer, Integer>(i, j));

			Set<Tuple<Integer, Integer>> tuples = new HashSet<Tuple<Integer, Integer>>();
			tuples.addAll(linkTuples);
			tuples.addAll(rightTuples);
			tuples.addAll(midTuples);
			// Is there changeable leg chain, which contains auch leg with
			// legIdx
			Set<Tuple<Integer, Integer>> legTuples = new HashSet<Tuple<Integer, Integer>>();
			for (Tuple<Integer, Integer> tpl : tuples) {
				if (tpl.getFirst() < legIdx && tpl.getSecond() > legIdx)
					legTuples.add(tpl);
			}
			// which is the shortest?
			List<Tuple<Integer, Integer>> tmpTpls = computeShortestLegTuples(legTuples);
			if (tmpTpls.size() > 0) {
				Tuple<Integer, Integer> tuple = tmpTpls.get(rnd.nextInt(tmpTpls
						.size()));
				return setLegChainMode(plan, tuple.getFirst(), tuple
						.getSecond(), mode);
			} else {
				tmpTpls = computeShortestLegTuples(tuples);
				Tuple<Integer, Integer> tuple = tmpTpls.get(rnd.nextInt(tmpTpls
						.size()));
				return setLegChainMode(plan, tuple.getFirst(), tuple
						.getSecond(), mode);
			}
		}

		private List<Tuple<Integer, Integer>> computeShortestLegTuples(
				Set<Tuple<Integer, Integer>> legTuples) {
			List<Tuple<Integer, Integer>> tmpTpls = new ArrayList<Tuple<Integer, Integer>>();
			int shortestLeg = 10000;
			if (legTuples.size() > 1)
				for (Tuple<Integer, Integer> tpl : legTuples) {
					int diff = tpl.getSecond() - tpl.getFirst();
					if (diff < shortestLeg) {
						shortestLeg = diff;
						tmpTpls.clear();
						tmpTpls.add(tpl);
					} else if (diff == shortestLeg) {
						tmpTpls.add(tpl);
					}
				}
			return tmpTpls;
		}

		private boolean changeUnCar2Car(Plan plan, int legIdx) {
			Plan copyPlan = new PlanImpl(plan.getPerson());
			copyPlan.copyPlan(plan);
			CarLegChain clc = new CarLegChain(copyPlan);
			List<PlanElement> pes = copyPlan.getPlanElements();
			int firstCarActIdx = clc.getFirstActIdx();
			int lastCarActIdx = clc.getLastActIdx();
			ParkLocation firstPark = new ParkLocation((Activity) pes
					.get(firstCarActIdx));

			if (firstCarActIdx < 0) {// no car legs in this copyPlan
				for (int l = legIdx - 1; l >= 0; l -= 2)
					for (int r = legIdx + 1; r < pes.size(); r += 2)
						if (new ParkLocation((Activity) pes.get(l))
								.equals(new ParkLocation((Activity) pes.get(r)))) {
							if (setLegChainMode(copyPlan, l, r,
									TransportMode.car)) {
								plan = copyPlan;
								return true;
							}
						}
				System.out
						.println("2 park locations with the same parklocation can not be found in this copyPlan");
				return false;
			} else if (legIdx < firstCarActIdx || legIdx > lastCarActIdx) {
				boolean done = false;
				int cnt = 0;
				int firstL = -1, firstR = -1;
				for (int l = firstCarActIdx; l >= 0; l -= 2)
					for (int r = lastCarActIdx; r < pes.size(); r += 2)
						if (new ParkLocation((Activity) pes.get(l))
								.equals(new ParkLocation((Activity) pes.get(r))))
							if (l != firstCarActIdx || r != lastCarActIdx) {
								if (cnt == 0) {
									firstL = l;
									firstR = r;
								}
								if ((legIdx > l && legIdx < firstCarActIdx)
										|| (legIdx > lastCarActIdx && legIdx < r)) {

									boolean leftDone = false;
									for (int ll = l + 2; ll < firstCarActIdx; ll += 2)
										if (firstPark.equals(new ParkLocation(
												(Activity) pes.get(ll)))) {
											leftDone = setLegChainMode(
													copyPlan, l, ll,
													TransportMode.car);
											break;
										}
									if (!leftDone)
										leftDone = setLegChainMode(copyPlan, l,
												firstCarActIdx,
												TransportMode.car);
									boolean rightDone = false;
									for (int rr = r - 2; rr > lastCarActIdx; rr -= 2)
										if (firstPark.equals(new ParkLocation(
												(Activity) pes.get(rr)))) {
											rightDone = setLegChainMode(
													copyPlan, rr, r,
													TransportMode.car);
											break;
										}
									if (!rightDone)
										rightDone = setLegChainMode(copyPlan,
												lastCarActIdx, r,
												TransportMode.car);
									done = leftDone || rightDone;
									if (done) {
										plan = copyPlan;
										return true;
									}
								}
								cnt++;
							}
				copyPlan.copyPlan(plan);
				if (firstL != -1 && firstR != -1)
					done = setLegChainMode(copyPlan, firstL, firstCarActIdx,
							TransportMode.car)
							|| setLegChainMode(copyPlan, lastCarActIdx, firstR,
									TransportMode.car);
				if (done)
					plan = copyPlan;
				return done;
			} else if (legIdx > firstCarActIdx && legIdx < lastCarActIdx) {
				int l = -1, r = -1;// boundary for the legIdx
				for (Tuple<Integer, Integer> tuple : clc.getActIdxs4CarLegs()) {
					int s = tuple.getSecond();
					if (legIdx > s)
						l = s;
					else {
						r = tuple.getFirst();
						break;
					}
				}

				List<Integer> sameParkActIdxs = new ArrayList<Integer>();
				for (int i = l; i <= r; i += 2) {
					if (firstPark
							.equals(new ParkLocation((Activity) pes.get(i))))
						sameParkActIdxs.add(i);
				}
				sameParkActIdxs.add(legIdx);
				Collections.sort(sameParkActIdxs);
				int legIdxInList = sameParkActIdxs.indexOf(legIdx);
				if (setLegChainMode(copyPlan, sameParkActIdxs
						.get(legIdxInList - 1), sameParkActIdxs
						.get(legIdxInList + 1), TransportMode.car)) {
					plan = copyPlan;
					return true;
				}
			}
			return false;
		}

		private static boolean setLegChainMode(Plan plan, int leftActIdx,
				int rightActIdx, TransportMode mode) {
			for (int i = leftActIdx + 1; i < rightActIdx; i += 2)
				((Leg) plan.getPlanElements().get(i)).setMode(mode);
			return rightActIdx >= leftActIdx + 2;
		}

		// reserve
		private static boolean checkParkSensible(Plan plan) {
			int carLegCnt = 0;
			Id OrigPark = null, lastNextPark = null;

			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Leg) {
					Leg leg = (Leg) pe;
					if (leg.getMode().equals(TransportMode.car)) {
						Id prePark = plan.getPreviousActivity(leg).getLinkId();
						Id nextPark = plan.getNextActivity(leg).getLinkId();

						if (carLegCnt == 0)
							OrigPark = prePark;
						else if (!prePark.equals(lastNextPark))
							return false;

						lastNextPark = nextPark;
						carLegCnt++;
					}
				}
			}
			return OrigPark.equals(lastNextPark);
		}
	}

	/**
	 * contains act/ park location information
	 * 
	 * @author ychen
	 * 
	 */
	public static class ParkLocation {
		private Activity act;

		// private Coord coord=null;
		// private Link link=null;

		public ParkLocation(Activity act) {
			this.act = act;
			// coord=act.getCoord();
			// link=act.getLink();
		}

		public boolean equals(ParkLocation pl) {
			Coord plCoord = pl.act.getCoord();
			Coord thisCoord = this.act.getCoord();

			Link plLink = pl.act.getLink();
			Link thisLink = this.act.getLink();

			if (plCoord != null && thisCoord != null) {// they both have
				// coordinates.
				return thisCoord.equals(plCoord);
			} else if (plLink != null && thisLink != null) {// they both have
				// Links
				if (plLink.equals(thisLink)) {
					return true;
				} else {
					return (plLink.getFromNode().equals(thisLink.getToNode()) && plLink
							.getToNode().equals(thisLink.getFromNode()));
				}
			} else {
				System.err
						.println("the 2 acts don't simultaneously have comparable location information!");
				return false;
			}
		}
		// reserve
		// public Activity getAct() {
		// return act;
		// }
	}

	/**
	 * contains activity indexs of car-leg-chain, e.g. a plan has leg chain:
	 * car-car-pt-walk-car, then the activity indexs looks like: [0-4, 8-10]
	 * 
	 * @author ychen
	 * 
	 */
	private static class CarLegChain {

		private List<Tuple<Integer, Integer>> actIdxs4CarLegs = new ArrayList<Tuple<Integer, Integer>>();

		private int firstActIdx = -1, lastActIdx = -1;

		public CarLegChain(Plan plan) {
			int actIdxA = -1, actIdxB = -1;
			List<PlanElement> pes = plan.getPlanElements();
			for (int i = 1; i < pes.size(); i += 2) {
				Leg leg = (Leg) pes.get(i);
				if (leg.getMode().equals(TransportMode.car)) {
					if (actIdxA == -1)
						actIdxA = i - 1;
					actIdxB = i + 1;
				} else {
					if (actIdxA != -1)
						actIdxs4CarLegs.add(new Tuple<Integer, Integer>(
								actIdxA, actIdxB));
					actIdxA = -1;
				}
			}
			if (actIdxA != -1)// by the last "car"-leg
				actIdxs4CarLegs.add(new Tuple<Integer, Integer>(actIdxA,
						actIdxB));
			int actIdxs4CarLegsSize = actIdxs4CarLegs.size();
			if (actIdxs4CarLegsSize > 0) {
				firstActIdx = actIdxs4CarLegs.get(0).getFirst();
				lastActIdx = actIdxs4CarLegs.get(actIdxs4CarLegsSize - 1)
						.getSecond();
			}
		}

		public List<Tuple<Integer, Integer>> getActIdxs4CarLegs() {
			return actIdxs4CarLegs;
		}

		public int getFirstActIdx() {
			return firstActIdx;
		}

		public int getLastActIdx() {
			return lastActIdx;
		}
	}
}
