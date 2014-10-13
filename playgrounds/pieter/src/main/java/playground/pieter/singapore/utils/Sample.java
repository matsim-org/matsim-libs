package playground.pieter.singapore.utils;

/*************************************************************************
 *  Compilation:  javac Sample.java
 *  Execution:    java Sample M N
 *
 *  This program takes two command-line arguments M and N and produces
 *  a random sample of M of the integers from 0 to N-1.
 *
 *  % java Sample 6 49
 *  10 20 0 46 40 6
 *
 *  % java Sample 10 1000
 *  656 488 298 534 811 97 813 156 424 109
 *
 *************************************************************************/

public class Sample {
    
	public static int[] sampleMfromN(int M, int N) {
		
        // create permutation 0, 1, ..., N-1
        int[] perm = new int[N];
        for (int i = 0; i < N; i++)
            perm[i] = i;

        // create random sample in perm[0], perm[1], ..., perm[M-1]
        for (int i = 0; i < M; i++)  {

            // random integer between i and N-1
            int r = i + (int) (Math.random() * (N-i));

            // swap elements at indices i and r
            int t = perm[r];
            perm[r] = perm[i];
            perm[i] = t;
        }

        
        int[] out = new int[M];
        System.arraycopy(perm, 0, out, 0, M);
        return out;
    }
}