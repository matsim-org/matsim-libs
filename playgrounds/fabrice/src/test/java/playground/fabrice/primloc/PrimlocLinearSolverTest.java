package playground.fabrice.primloc;

import org.matsim.testcases.MatsimTestCase;

import Jama.Matrix;

public class PrimlocLinearSolverTest extends MatsimTestCase {

	// FIXME this test-case has no Assert-statement, so it will always succeed!

	public void testLinearSolver(){
		testSolver( 10 );
		testSolver( 100 );
		testSolver( 1000 );
	}

	static void testSolver( int n){
		System.out.println("Testing matrix solver");
		System.out.println("Solving A.X = B for size:"+n);
		System.out.println("Comp. time is O(N3)");
		Matrix A = Matrix.random( n, n );
		Matrix b = Matrix.random( n, 1 );
		long timeMill = System.currentTimeMillis();
		Matrix x = A.solve( b );
		timeMill = System.currentTimeMillis() - timeMill;
		double duration = timeMill / 1000.0;
		System.out.println("Solved in:\t"+duration+" s");
		
        Matrix Residual = A.times(x).minus(b); 
        double rnorm = Residual.normInf();
        System.out.println( "Residual:\t"+rnorm );
        System.out.println("Test over");
	}
}
