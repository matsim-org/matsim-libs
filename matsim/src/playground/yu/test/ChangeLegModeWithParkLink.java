/**
 * 
 */
package playground.yu.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.population.Activity;
import org.matsim.core.api.population.Leg;
import org.matsim.core.api.population.Plan;
import org.matsim.core.api.population.PlanElement;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.population.algorithms.PlanAlgorithm;

/**
 * this class contains some codes from ChangeLegMode
 * 
 * @author ychen
 * 
 */
public class ChangeLegModeWithParkLink extends AbstractMultithreadedModule {
	private TransportMode[] availableModes = new TransportMode[] {
			TransportMode.car, TransportMode.pt };

	/**
	 * @param availableModes
	 *            an array for TransportMode
	 */
	public ChangeLegModeWithParkLink(final TransportMode[] availableModes) {
		super();
		this.availableModes = availableModes.clone();
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
						changeCar2UnCar(plan, legIdx, newMode);
					} else {
						if (newMode.equals(TransportMode.car))
							if (!changeUnCar2Car(plan, legIdx))
								done = false;
							else
								changeUnCar2UnCar(plan, legIdx, newMode);
					}
				}
			}
		}

		private void changeUnCar2UnCar(Plan plan, int legIdx, TransportMode mode) {
			// TODO
		}

		private boolean changeCar2UnCar(Plan plan, int legIdx,
				TransportMode mode) {
			// TODO
			return true;
		}

		private boolean changeUnCar2Car(Plan plan, int legIdx) {
			boolean changable = false;
			CarLegChain clc = new CarLegChain(plan);
			List<PlanElement> pes = plan.getPlanElements();
			int firstCarActIdx = clc.getFirstActIdx();
			int lastCarActIdx = clc.getLastActIdx();
			if (firstCarActIdx < 0) {// no car legs in this plan
				for (int l = legIdx - 1; l >= 0; l -= 2)
					for (int r = legIdx + 1; r < pes.size(); r += 2)
						if (new ParkLocation((Activity) pes.get(l))
								.equals(new ParkLocation((Activity) pes.get(r))))
							return setLegChainMode(plan, l, r,
									TransportMode.car);
				System.out
						.println("2 park locations with the same parklocation can not be found in this plan");
				return false;
			} else if (legIdx < firstCarActIdx || legIdx > lastCarActIdx) {
				boolean done = false;
				for (int l = firstCarActIdx; l >= 0; l -= 2)
					for (int r = lastCarActIdx; r < pes.size(); r += 2)
						if (new ParkLocation((Activity) pes.get(l))
								.equals(new ParkLocation((Activity) pes.get(r))))
							if (l != firstCarActIdx || r != lastCarActIdx) {
								setLegChainMode(plan, l, firstCarActIdx,
										TransportMode.car);
								setLegChainMode(plan, lastCarActIdx, r,
										TransportMode.car);
								done = true;
							}
				return false;
			} else {
				// TODO A2
			}

			return false;
		}

		private static boolean setLegChainMode(Plan plan, int leftActIdx,
				int rightActIdx, TransportMode mode) {
			for (int i = leftActIdx + 1; i < rightActIdx; i += 2)
				((Leg) plan.getPlanElements().get(i)).setMode(mode);
			return true;
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
		private Plan plan;

		private List<Tuple<Integer, Integer>> actIdxs4CarLegs = new ArrayList<Tuple<Integer, Integer>>();

		private int firstActIdx = -1, lastActIdx = -1;

		public CarLegChain(Plan plan) {
			this.plan = plan;
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
