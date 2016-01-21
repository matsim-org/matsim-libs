/*
 * Opdyts - Optimization of dynamic traffic simulations
 *
 * Copyright 2015 Gunnar Flötteröd
 * 
 *
 * This file is part of Opdyts.
 *
 * Opdyts is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Opdyts is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Opdyts.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.floetteroed@abe.kth.se
 *
 */
package vind;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.analysis.MultivariateVectorFunction;
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.MaxIter;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.SimpleValueChecker;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunctionGradient;
import org.apache.commons.math3.optim.nonlinear.scalar.gradient.NonLinearConjugateGradientOptimizer;

import floetteroed.opdyts.DecisionVariable;
import floetteroed.opdyts.searchalgorithms.UpperBoundTuner;
import floetteroed.opdyts.trajectorysampling.SamplingStage;
import floetteroed.utilities.math.Vector;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
@Deprecated
public class TrajectorySamplingSelfTuner {

	// TODO
	private final double terminationPrecision = 1e-6;

	// -------------------- CONSTANTS --------------------

	private final int equilibriumGapWeightIndex = 0;

	private final int uniformityWeightIndex = 1;

	private final int posOffsetIndex = 2;

	private final int negOffsetIndex = 3;

	// -------------------- MEMBERS --------------------

	private final double inertia;

	private final LinkedList<Vector> inputs = new LinkedList<Vector>();

	private final LinkedList<Double> outputs = new LinkedList<Double>();

	private Vector coeffs;

	private PrintWriter log;

	// -------------------- CONSTRUCTION --------------------

	public TrajectorySamplingSelfTuner(
			final double initialEquilibriumGapWeight,
			final double initialUniformityWeight, final double initialOffset,
			final double regressionInertia, final double initialValueWeight) {

		try {
			this.log = new PrintWriter("./predvsreal.txt");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		this.log.close();

		this.inertia = regressionInertia;

		Vector input = new Vector(4);
		input.set(this.equilibriumGapWeightIndex, initialValueWeight);
		this.inputs.add(input);
		this.outputs.add(initialEquilibriumGapWeight * initialValueWeight);

		input = new Vector(4);
		input.set(this.uniformityWeightIndex, initialValueWeight);
		this.inputs.add(input);
		this.outputs.add(initialUniformityWeight * initialValueWeight);

		input = new Vector(4);
		input.set(this.posOffsetIndex, +initialValueWeight);
		this.inputs.add(input);
		this.outputs.add(initialOffset >= 0 ? initialOffset
				* initialValueWeight : 0.0);

		input = new Vector(4);
		input.set(this.negOffsetIndex, -initialValueWeight);
		this.inputs.add(input);
		this.outputs.add(initialOffset < 0 ? initialValueWeight : 0.0);

		this.run();
	}

	// -------------------- GETTERS --------------------

	public int size() {
		return this.inputs.size();
	}

	// -------------------- OPTIMIZATION --------------------

//	public <U extends DecisionVariable> void registerSamplingStageSequence(
//			final List<SamplingStage<U>> samplingStages,
//			final double finalObjectiveFunctionValue
//			,
////			final double gradientNorm, final 
//			U finalDecisionVariable) {
//
//		final SamplingStage<U> stage = samplingStages.get(0);
//		// final double alpha = stage.getAlphaSum(finalDecisionVariable);
//		final Vector input = new Vector(4);
//		input.set(this.equilibriumGapWeightIndex,
//				// gradientNorm * 
//				stage.getEquilibriumGap()
//		// * stage.decisionVariable2singletonEquilbriumGap
//		// .get(finalDecisionVariable)
//		);
//		input.add(this.uniformityWeightIndex,
//				// gradientNorm * 
//				stage.getAlphaNorm() * stage.getAlphaNorm()
//		// * stage.decisionVariable2singletonUniformityGap
//		// .get(finalDecisionVariable)
//		);
//		input.add(this.posOffsetIndex, +1.0);
//		input.add(this.negOffsetIndex, -1.0);
//		double output = finalObjectiveFunctionValue
//				- stage.getOriginalObjectiveFunctionValue();
//
//		// TODO NEW SCALING >>>>>
//		// final double completionProba = 1.0;
//		// // this.completionProba(stage, finalDecisionVariable,
//		// // samplingStages.size());
//		// System.out.println(completionProba + "\t" + input + " -> " + output);
//		// input.mult(Math.sqrt(completionProba));
//		// output *= Math.sqrt(completionProba);
//		// TODO NEW SCALING <<<<<
//
//		this.inputs.addFirst(input);
//		this.outputs.addFirst(output);
//
//		try {
//			this.log = new PrintWriter(new BufferedWriter(new FileWriter(
//					"./predvsreal.txt", true)));
//		} catch (IOException e) {
//			throw new RuntimeException(e);
//		}
//		this.log.println((stage.getOriginalObjectiveFunctionValue() + input
//				.innerProd(this.coeffs)) + "\t" + finalObjectiveFunctionValue);
//		this.log.close();
//
//		// final SamplingStage stage = samplingStages.get(0);
//		// final double alpha = stage.getAlphaSum(finalDecisionVariable);
//		// final Vector input = new Vector(4);
//		// input.set(
//		// this.equilibriumGapWeightIndex,
//		// gradientNorm
//		// * stage.decisionVariable2singletonEquilbriumGap
//		// .get(finalDecisionVariable));
//		// input.add(
//		// this.uniformityWeightIndex,
//		// gradientNorm
//		// * stage.decisionVariable2singletonUniformityGap
//		// .get(finalDecisionVariable));
//		// input.add(this.posOffsetIndex, +1.0);
//		// input.add(this.negOffsetIndex, -1.0);
//		// double output = finalObjectiveFunctionValue
//		// - stage.decisionVariable2singletonInterpolatedObjectiveFunction
//		// .get(finalDecisionVariable);
//		//
//		// System.out.println(alpha + "\t" + input + " -> " + output);
//		// // TODO NEW SCALING >>>>>
//		// input.mult(alpha);
//		// output *= alpha;
//		// // TODO NEW SCALING <<<<<
//		// this.inputs.addFirst(input);
//		// this.outputs.addFirst(output);
//
//		// final Vector input = new Vector(4);
//		// final SamplingStage stage = samplingStages
//		// .get(samplingStages.size() - 2); // the stage before convergence
//		// input.set(this.equilibriumGapWeightIndex,
//		// gradientNorm * stage.getEquilibriumGap());
//		// input.set(this.uniformityWeightIndex,
//		// gradientNorm * stage.getAlphaNorm());
//		// input.set(this.posOffsetIndex, +1.0);
//		// input.set(this.negOffsetIndex, -1.0);
//		// final double output = finalObjectiveFunctionValue
//		// - stage.getInterpolatedObjectiveFunctionValue();
//		// System.out.println(input + " -> " + output);
//		// this.inputs.addFirst(input);
//		// this.outputs.addFirst(output);
//
//		// final Vector input = new Vector(4);
//		// double output = 0.0; // final
//		// SamplingStage initialStage = samplingStages.get(0);
//		// for (SamplingStage stage : samplingStages) {
//		// input.add(this.equilibriumGapWeightIndex,
//		// gradientNorm * stage.getEquilibriumGap());
//		// input.add(this.uniformityWeightIndex,
//		// gradientNorm * stage.getAlphaNorm());
//		// input.add(this.posOffsetIndex, +1.0);
//		// input.add(this.negOffsetIndex, -1.0);
//		// output += finalObjectiveFunctionValue
//		// - stage.getInterpolatedObjectiveFunctionValue();
//		// }
//		// input.mult(1.0 / samplingStages.size());
//		// output /= samplingStages.size();
//		// System.out.println(input + " -> " + output);
//		// this.inputs.addFirst(input);
//		// this.outputs.addFirst(output);
//
//		// for (SamplingStage stage : samplingStages) {
//		// final Vector input = new Vector(4);
//		// double output = 0.0;
//		// input.add(this.equilibriumGapWeightIndex,
//		// gradientNorm * stage.getEquilibriumGap());
//		// input.add(this.uniformityWeightIndex,
//		// gradientNorm * stage.getAlphaNorm());
//		// input.add(this.posOffsetIndex, +1.0);
//		// input.add(this.negOffsetIndex, -1.0);
//		// output += finalObjectiveFunctionValue
//		// - stage.getInterpolatedObjectiveFunctionValue();
//		// this.inputs.addFirst(input);
//		// this.outputs.addFirst(output);
//		// }
//
//		final UpperBoundTuner myTuner = new UpperBoundTuner();
//		myTuner.registerSamplingStageSequence(samplingStages,
//				finalObjectiveFunctionValue
//				// , 
//				// gradientNorm,
//				// finalDecisionVariable
//				);
//
//		this.run();
//	}

	public <U extends DecisionVariable> void registerSamplingStageSequence(
			final double initialObjectiveFunctionValue,
			final double initialEquilibriumGap,
			final double initialUniformityGap,
			final double finalObjectiveFunctionValue,
			final U finalDecisionVariable) {

		final Vector input = new Vector(4);
		input.set(this.equilibriumGapWeightIndex, initialEquilibriumGap);
		input.add(this.uniformityWeightIndex, initialUniformityGap);
		input.add(this.posOffsetIndex, +1.0);
		input.add(this.negOffsetIndex, -1.0);
		double output = finalObjectiveFunctionValue
				- initialObjectiveFunctionValue;

		this.inputs.addFirst(input);
		this.outputs.addFirst(output);

		// try {
		// this.log = new PrintWriter(new BufferedWriter(new FileWriter(
		// "./predvsreal.txt", true)));
		// } catch (IOException e) {
		// throw new RuntimeException(e);
		// }
		// this.log.println((stage.getOriginalObjectiveFunctionValue() + input
		// .innerProd(this.coeffs)) + "\t" + finalObjectiveFunctionValue);
		// this.log.close();

		this.run();
	}

	private double[] initialPoint = new double[] { 1.0, 1.0, 1.0, 1.0 };

	private void run() {
		final NonLinearConjugateGradientOptimizer solver = new NonLinearConjugateGradientOptimizer(
				NonLinearConjugateGradientOptimizer.Formula.POLAK_RIBIERE,
				new SimpleValueChecker(this.terminationPrecision,
						this.terminationPrecision));
		final PointValuePair result = solver.optimize(new ObjectiveFunction(
				new MyObjectiveFunction()), new ObjectiveFunctionGradient(
				new MyGradient()), GoalType.MINIMIZE, new InitialGuess(
				this.initialPoint), new MaxEval(Integer.MAX_VALUE),
				new MaxIter(Integer.MAX_VALUE));
		this.initialPoint = result.getPoint();
		this.coeffs = point2coeffs(result.getPoint());
	}

	public double getEquilibriumGapWeight() {
		return this.coeffs.get(this.equilibriumGapWeightIndex);
	}

	public double getUniformityWeight() {
		return this.coeffs.get(this.uniformityWeightIndex);
	}

	public double getOffset() {
		final double posOffset = this.coeffs.get(this.posOffsetIndex);
		final double negOffset = this.coeffs.get(this.negOffsetIndex);
		return posOffset - negOffset;
	}

	// -------------------- OPTIMIZATION INTERNALS --------------------

	private Vector point2coeffs(final double[] point) {
		final Vector result = new Vector(point.length);
		for (int i = 0; i < point.length; i++) {
			result.set(i, point[i] * point[i]);
		}
		return result;
	}

	private class MyObjectiveFunction implements MultivariateFunction {
		@Override
		public double value(double[] point) {
			final Vector coeffs = point2coeffs(point);
			double result = 0.0;
			double weight = 1.0;
			for (int i = 0; i < size(); i++) {
				result += weight
						* Math.pow(
								outputs.get(i)
										- inputs.get(i).innerProd(coeffs), 2.0);
				weight *= inertia;
			}
			return result;
		}
	}

	private class MyGradient implements MultivariateVectorFunction {
		@Override
		public double[] value(double[] point) {
			final Vector result = new Vector(point.length);
			final Vector coeffs = point2coeffs(point);
			double weight = 1.0;
			for (int i = 0; i < size(); i++) {
				final Vector addend = inputs.get(i).copy();
				for (int k = 0; k < addend.size(); k++) {
					addend.mult(k, coeffs.get(k));
				}
				addend.mult(weight * 2.0
						* (outputs.get(i) - inputs.get(i).innerProd(coeffs))
						* (-2.0));
				result.add(addend);
				weight *= inertia;
			}
			double[] resultArray = new double[result.size()];
			for (int i = 0; i < result.size(); i++) {
				resultArray[i] = result.get(i);
			}
			return resultArray;
		}
	}
}
