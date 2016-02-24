package floetteroed.utilities.math;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class GramSchmidtOrthonormalization {

	// -------------------- CONSTANTS --------------------
	
	private final double eps;

	// -------------------- MEMBERS --------------------

	private List<Vector> basis = new LinkedList<>();

	// -------------------- CONSTRUCTION --------------------

	public GramSchmidtOrthonormalization(final double eps) {
		this.eps = Math.max(1e-8, eps);
	}

	// -------------------- IMPLEMENTATION --------------------

	public List<Vector> getBasisView() {
		return Collections.unmodifiableList(this.basis);
	}

	private Vector project(final Vector projectThis, final Vector ontoThis) {
		final Vector result = ontoThis.copy();
		result.mult(projectThis.innerProd(ontoThis)
				/ ontoThis.innerProd(ontoThis));
		return result;
	}

	public boolean add(final Vector x) {
		final Vector newBasisVector = x.copy();
		for (Vector basisVector : this.basis) {
			newBasisVector.add(this.project(x, basisVector), -1.0);
		}
		if (newBasisVector.euclNorm() >= this.eps * x.euclNorm()) {
			newBasisVector.normalize();
			this.basis.add(newBasisVector);
			return true;
		} else {
			return false;
		}
	}

	// -------------------- MAIN-FUNCTION --------------------

	public static void main(String[] args) {
		for (int dim = 1; dim <= 100; dim++) {
			final GramSchmidtOrthonormalization gso = new GramSchmidtOrthonormalization(1e-3);
			for (int i = 0; i < 1000; i++) {
				gso.add(Vector.newGaussian(dim));
			}
			System.out.println("dim = " + dim + ", gso basis has dim " + gso.getBasisView().size());
		}
	}
	
}
