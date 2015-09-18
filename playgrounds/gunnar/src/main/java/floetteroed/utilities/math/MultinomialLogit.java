/*
 * Copyright 2015 Gunnar Flötteröd
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.floetteroed@abe.kth.se
 *
 */ 
package floetteroed.utilities.math;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;



/**
 * Implements a basic multinomial logit choice model where all alternatives have
 * the same coefficients plus an alternative specific constant.
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class MultinomialLogit {

	// -------------------- CONSTANTS --------------------

	private final List<Integer> ALL_ATTRIBUTE_INDICES;

	private final List<Integer> ALL_ASC_INDICES;

	// -------------------- EXOGENEOUS PARAMETERS --------------------

	private double utilityScale;

	private final Vector coeff;

	private final Vector asc;

	private final Matrix attr;

	// -------------------- ENDOGENEOUS PARAMETERS --------------------

	private final Vector utilities;

	private final Vector choiceProbs;

	private final Matrix dProbs_dCoeffs;

	private final Matrix dProbs_dASCs;

	private boolean consistent;

	// -------------------- CONSTRUCTION --------------------

	public MultinomialLogit(final int choiceSetSize, final int attributeCount) {

		this.utilityScale = 1.0;
		this.coeff = new Vector(attributeCount);
		this.asc = new Vector(choiceSetSize);
		this.attr = new Matrix(choiceSetSize, attributeCount);
		this.utilities = new Vector(choiceSetSize);
		this.choiceProbs = new Vector(choiceSetSize);
		this.dProbs_dCoeffs = new Matrix(choiceSetSize, attributeCount);
		this.dProbs_dASCs = new Matrix(choiceSetSize, choiceSetSize);
		this.consistent = false;

		final List<Integer> allAttrInd = new ArrayList<Integer>(attributeCount);
		for (int i = 0; i < attributeCount; i++) {
			allAttrInd.add(i);
		}
		this.ALL_ATTRIBUTE_INDICES = Collections.unmodifiableList(allAttrInd);

		final List<Integer> allASCInd = new ArrayList<Integer>(choiceSetSize);
		for (int i = attributeCount; i < attributeCount + choiceSetSize; i++) {
			allASCInd.add(i);
		}
		this.ALL_ASC_INDICES = Collections.unmodifiableList(allASCInd);
	}

	// -------------------- SETTERS --------------------

	public void setUtilityScale(final double value) {
		this.consistent = false;
		this.utilityScale = value;
	}

	public void setCoefficient(final int attrIndex, final double value) {
		this.consistent = false;
		this.coeff.set(attrIndex, value);
	}

	public void setASC(final int choiceIndex, final double value) {
		this.consistent = false;
		this.asc.set(choiceIndex, value);
	}

	public void setAttribute(final int choiceIndex, final int attrIndex,
			final double value) {
		this.consistent = false;
		this.attr.getRow(choiceIndex).set(attrIndex, value);
	}

	// -------------------- UPDATE --------------------

	public void enforcedUpdate() {

		/*
		 * (1) update utilities
		 */
		double vMax = Double.NEGATIVE_INFINITY; // double vAvg = 0;
		for (int i = 0; i < this.getChoiceSetSize(); i++) {
			final double v = this.coeff.innerProd(this.attr.getRow(i))
					+ this.asc.get(i);
			this.utilities.set(i, v);
			vMax = Math.max(vMax, v);
		}

		/*
		 * (2) update choice probabilities
		 */
		double pSum = 0;
		for (int i = 0; i < this.getChoiceSetSize(); i++) {
			final double p = Math.exp(this.utilityScale
					* (this.utilities.get(i) - vMax));
			this.choiceProbs.set(i, p);
			pSum += p;
		}
		this.choiceProbs.mult(1.0 / pSum);

		/*
		 * (3) update derivatives of choice probabilities w.r.t. coefficients
		 */
		this.dProbs_dCoeffs.clear();
		final Vector probsTimesAttr = this.attr
				.timesVectorFromLeft(this.choiceProbs);
		for (int i = 0; i < this.getChoiceSetSize(); i++) {
			final Vector dProbi_dCoeff = this.dProbs_dCoeffs.getRow(i);
			final double probi = this.choiceProbs.get(i);
			final Vector attri = this.attr.getRow(i);
			for (int j = 0; j < this.getAttrCount(); j++) {
				dProbi_dCoeff.set(j, probi
						* (attri.get(j) - probsTimesAttr.get(j)));
			}
		}
		this.dProbs_dCoeffs.mult(this.utilityScale);

		/*
		 * (3) update derivatives of choice probabilities w.r.t. ASCs
		 */
		this.dProbs_dASCs.clear();
		for (int i = 0; i < this.getChoiceSetSize(); i++) {
			final double probi = this.choiceProbs.get(i);
			final Vector dProbi_dASC = this.dProbs_dASCs.getRow(i);
			dProbi_dASC.set(i, probi);
			dProbi_dASC.add(this.choiceProbs, -probi);
		}
		this.dProbs_dASCs.mult(this.utilityScale);

		this.consistent = true;
	}

	public void conditionalUpdate() {
		if (!this.consistent) {
			this.enforcedUpdate();
		}
	}

	// -------------------- GETTERS AND THE LIKE --------------------

	// TODO NEW
	public double getUtilityScale() {
		return this.utilityScale;
	}
	
	public int getChoiceSetSize() {
		return this.choiceProbs.size();
	}

	public int getAttrCount() {
		return this.coeff.size();
	}

	public Vector getCoeff() {
		return this.coeff.newImmutableView();
	}

	public Vector getASC() {
		return this.asc.newImmutableView();
	}

	public Vector getProbs() {
		this.conditionalUpdate();
		return this.choiceProbs.newImmutableView();
	}

	public Vector getUtils() {
		this.conditionalUpdate();
		return this.utilities.newImmutableView();
	}

	public Matrix get_dProbs_dCoeffs() {
		this.conditionalUpdate();
		return this.dProbs_dCoeffs.newImmutableView();
	}

	public Matrix get_dProbs_dASCs() {
		this.conditionalUpdate();
		return this.dProbs_dASCs.newImmutableView();
	}

	// -------------------- MISCELLANEOUS --------------------

	public int draw(final Random rnd) {
		return MathHelpers.draw(this.getProbs(), rnd);
	}

	// --------------- PARAMETER VECTOR EXTERNALIZATION ---------------

	public int getParameterSize(final List<Integer> attributeIndices,
			final boolean withASC) {
		return attributeIndices.size()
				+ (withASC ? this.getChoiceSetSize() : 0);
	}

	public int getParameterSize(final boolean withASC) {
		return this.getParameterSize(this.ALL_ATTRIBUTE_INDICES, withASC);
	}

	public double getParameter(final int j) {
		if (j < this.getAttrCount()) {
			return this.getCoeff().get(j);
		} else {
			return this.getASC().get(j - this.getAttrCount());
		}
	}

	public Vector getParameters(final boolean withASC) {
		final Vector result = new Vector(this.getParameterSize(withASC));
		for (int j = 0; j < result.size(); j++) {
			result.set(j, this.getParameter(j));
		}
		return result;
	}

	public void setParameter(final int j, final double value) {
		this.consistent = false;
		if (j < this.getAttrCount()) {
			this.setCoefficient(j, value);
		} else {
			this.setASC(j - this.getAttrCount(), value);
		}
	}

	public void setParameters(final Vector parameters) {
		this.consistent = false;
		for (int j = 0; j < parameters.size(); j++) {
			this.setParameter(j, parameters.get(j));
		}
	}

	public Matrix get_dProb_dParameters(final List<Integer> attributeIndices,
			final boolean withASC) {
		this.conditionalUpdate();
		final Matrix result = new Matrix(this.getChoiceSetSize(), this
				.getParameterSize(attributeIndices, withASC));
		for (int i = 0; i < this.getChoiceSetSize(); i++) {
			final Vector resulti = result.getRow(i);
			final Vector dProbi_dCoeff = this.get_dProbs_dCoeffs().getRow(i);
			final Vector dProbi_dASC = (withASC ? this.get_dProbs_dASCs()
					.getRow(i) : null);
			int l = 0;
			for (int j : attributeIndices) {
				resulti.set(l++, dProbi_dCoeff.get(j));
			}
			if (withASC) {
				for (int i2 = 0; i2 < this.getChoiceSetSize(); i2++) {
					resulti.set(l++, dProbi_dASC.get(i2));
				}
			}
		}
		return result;
	}

	public Matrix get_dProb_dParameters(final boolean withASC) {
		return this.get_dProb_dParameters(this.ALL_ATTRIBUTE_INDICES, withASC);
	}

	/**
	 * Provides a numerical approximation of the Hessian of each choice
	 * probability with respect to the model parameters.
	 * 
	 * @param delta
	 *            the step size of the finite differences
	 * 
	 * @param withASC
	 *            if the alternative specific constants are to be accounted for
	 * 
	 * @returns a numerical approximation of the Hessian of each choice
	 *          probability with respect to the model parameters
	 */
	public List<Matrix> get_d2P_dbdb(final double delta,
			final List<Integer> attributeIndices, final boolean withASC) {

		final int paramSize = this.getParameterSize(attributeIndices, withASC);
		final List<Integer> paramIndices = new ArrayList<Integer>(paramSize);
		paramIndices.addAll(attributeIndices);
		if (withASC) {
			paramIndices.addAll(this.ALL_ASC_INDICES);
		}

		final List<Matrix> result = new ArrayList<Matrix>(this
				.getChoiceSetSize());
		for (int i = 0; i < this.getChoiceSetSize(); i++) {
			result.add(new Matrix(paramSize, paramSize));
		}

		final Matrix dP_db0 = this.get_dProb_dParameters(attributeIndices,
				withASC);
		int resultIndex = 0;
		for (int r : paramIndices) {
			final double br0 = this.getParameter(r);
			this.setParameter(r, br0 + delta);
			final Matrix dP_dbVaried = this.get_dProb_dParameters(
					attributeIndices, withASC);
			for (int i = 0; i < this.getChoiceSetSize(); i++) {
				final Vector d2Pi_dbrdb = result.get(i).getRow(resultIndex);
				d2Pi_dbrdb.add(dP_dbVaried.getRow(i), +1.0);
				d2Pi_dbrdb.add(dP_db0.getRow(i), -1.0);
				d2Pi_dbrdb.mult(1.0 / delta);
			}

			this.setParameter(r, br0);
			resultIndex++;
		}

		return result;
	}

	public List<Matrix> get_d2P_dbdb(final double delta, final boolean withASC) {
		return this.get_d2P_dbdb(delta, this.ALL_ATTRIBUTE_INDICES, withASC);
	}

	// TODO NEW
	public Matrix getAttributesView() {
		this.conditionalUpdate();
		return this.attr.copy();
	}

	// TODO NEW
	public void setAttributes(final Matrix attr) {
		this.attr.clear();
		this.attr.add(attr, 1.0);
		this.consistent = false;
	}
}
