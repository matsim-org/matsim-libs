/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * Run.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2015 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package michael;

import java.util.Arrays;
import java.util.HashSet;

import optdyts.DecisionVariable;
import optdyts.ObjectiveFunction;
import optdyts.SimulatorState;
import optdyts.algorithms.DecisionVariableSetEvaluator;
import floetteroed.utilities.math.Vector;

public class Run {

	public static void main(String[] args) {
		new Run().run();
	}

	double statex, statey = 0.0;

	private void run() {
		ObjectiveFunction<MySimulatorState> objectiveFunction = new ObjectiveFunction<MySimulatorState>() {
			double targetx = 20;
			double targety = -37;

			@Override
			public double evaluateState(MySimulatorState state) {
				double dx2 = (targetx - state.x) * (targetx - state.x);
				double dy2 = (targety - state.y) * (targety - state.y);
				return Math.sqrt(dx2 + dy2);
			}
		};
		DecisionVariableSetEvaluator<MySimulatorState, DecisionVariable> decisionVariableSetEvaluator = new DecisionVariableSetEvaluator<>(
				new HashSet<>(Arrays.asList(new DecisionVariable() {
					@Override
					public void implementInSimulation() {
						statex = (0.9 * statex) + (0.1 * 100) + Math.random()
								- 0.5;
						statey = (0.9 * statey) + (0.1 * 100) + Math.random()
								- 0.5;
					}

					@Override
					public String toString() {
						return "(100, 100)";
					}
				}, new DecisionVariable() {
					@Override
					public void implementInSimulation() {
						statex = (0.9 * statex) + (0.1 * 20) + Math.random()
								- 0.5;
						statey = (0.9 * statey) + (0.1 * -37) + Math.random()
								- 0.5;
					}

					@Override
					public String toString() {
						return "(20, -37)";
					}
				}, new DecisionVariable() {
					@Override
					public void implementInSimulation() {
						statex = (0.9 * statex) + (0.1 * 0) + Math.random()
								- 0.5;
						statey = (0.9 * statey) + (0.1 * 0) + Math.random()
								- 0.5;
					}

					@Override
					public String toString() {
						return "(0, 0)";
					}
				}, new DecisionVariable() {
					@Override
					public void implementInSimulation() {
						statex = (0.9 * statex) + (0.1 * -37) + Math.random()
								- 0.5;
						statey = (0.9 * statey) + (0.1 * 20) + Math.random()
								- 0.5;
					}

					@Override
					public String toString() {
						return "(-37, 20)";
					}
				}, new DecisionVariable() {
					@Override
					public void implementInSimulation() {
						statex = (0.9 * statex) + (0.1 * 21) + Math.random()
								- 0.5;
						statey = (0.9 * statey) + (0.1 * -38) + Math.random()
								- 0.5;
					}

					@Override
					public String toString() {
						return "(21, -38)";
					}
				})), objectiveFunction, 5, 0.05);
		// 2 * (1.0 / 12.0),
		// 1.);
		decisionVariableSetEvaluator.setLogFileName("testlog.txt");
		
		while (objectiveFunction.evaluateState(new MySimulatorState(statex,
				statey)) > 1.0) {
			decisionVariableSetEvaluator.afterIteration(new MySimulatorState(
					statex, statey));
			System.out.printf("%f, %f\n", statex, statey);
		}
	}

	class MySimulatorState implements SimulatorState {

		double x, y;

		public MySimulatorState(double x, double y) {
			this.x = x;
			this.y = y;
		}

		// @Override
		// public MySimulatorState deepCopy() {
		// return new MySimulatorState(x, y);
		// }

		// @Override
		// public void takeOverConvexCombination(List<MySimulatorState> states,
		// List<Double> weights) {
		// x = 0;
		// y = 0;
		// for (int i = 0; i < states.size(); i++) {
		// x += states.get(i).x * weights.get(i);
		// y += states.get(i).y * weights.get(i);
		// }
		// }

		@Override
		public Vector getReferenceToVectorRepresentation() {
			return new Vector(new double[] { x, y });
		}

		@Override
		public void implementInSimulation() {
			statex = x;
			statey = y;
		}

		// @Override
		// public void releaseDeepMemory() {
		//
		// }

	}

}
