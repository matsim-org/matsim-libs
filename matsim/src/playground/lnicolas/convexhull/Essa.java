/* *********************************************************************** *
 * project: org.matsim.*
 * Essa.java
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

//***********************************************
//  File: Essa.java 
//  
//  Calculates the exact sign of the sum of a vector 
//  of floating point numbers.
//
//  Returns the sign of the sum
//
//                       -1 -> sum negative
//                        0 -> sum 0
//                       +1 -> sum positive

//  Programmed by Lynn Tetreault,  1998
//***********************************************

package playground.lnicolas.convexhull;


public class Essa
{
    floatVector a = new floatVector();       // Vector of positive summands
    floatVector b = new floatVector();       // Vector of negative summands
    int m=0;                                 // Length of Vector a
    int n=0;                                 // Length of Vector b

    public Essa(floatVector S)
    {
        // Splitting of S into positive and negative summands.
        // a[1].....a[m] positive
        // b[1].....b[n] negative

        S.trimToSize();
        int l = S.size();
        a.append(Float.MAX_VALUE); //forces element[0] to be MAXVALUE;
        b.append(Float.MAX_VALUE); //just a place holder in the reverse sort

        for (int i=0;i<l;i++)
           if (S.get(i)>0) 
           {   m++; a.append(S.get(i));
           }
           else if(S.get(i)<0)
           {   n++; b.append(-S.get(i));
           }

        a.trimToSize();
        b.trimToSize();

        // Sorting of the lists a and b in descending order.
        if ( m>1 ) a.reverseHeapSort();
        if ( n>1 ) b.reverseHeapSort();
    };

    public int sgsum()
    {
        int E=0;              // E is the exponent of a[1];
        int F=0;              // F is the exponent of b[1];
        int sg=-2;
        float as=0.0f, ass=0.0f;
        float bs=0.0f, bss=0.0f;
        float uu=0.0f, u=0.0f, v=0.0f;
        
        while(sg==-2)   //  Main loop (the proper algorithm ESSA).
        {
            //===================================
            // Step 1: (Termination Criteria)
            //===================================
            if( n==0 && m==0 ) { sg=0; continue; }
            if( n==0 ) { sg=1; continue; }
            if( m==0 ) { sg=-1; continue; }

	    F = fancyFloat.getExp( b.get(1) );
            if(a.get(1) >= n*fancyFloat.ldexp(0.5f,F+1)) 
            {  sg=1; continue;  }

	    E = fancyFloat.getExp( a.get(1) );
            if(  b.get(1) >= m*fancyFloat.ldexp(0.5f,E+1)  ) 
            {  sg=-1; continue;  }


            //===================================
            // Step 2: (Auxiliary variables)
            //===================================
            as=ass=bs=bss=0;


            //===================================
            // Step 3: (Compare and process the leading summands of the lists
            // E contains the exponent of a[1] in base 2.  
            // F contains the exponent of b[1] in base 2.
            //===================================

            if (E==F)                                   // Step 3, case (i):
            {
                if (a.get(1)>=b.get(1)) as=a.get(1)-b.get(1);
                else bs= b.get(1)-a.get(1);
            }
            else if (E>F)                               // Step 3, case (ii):
            {
	        uu=fancyFloat.ldexp(0.5f, F);
                u = (b.get(1)==uu ? uu : uu*2);

                as=a.get(1)-u;
                ass=u-b.get(1);
            }
            else if (F>E)                               // Step 3, case (iii):
            {
	        uu=fancyFloat.ldexp(0.5f, E);
                v = (a.get(1)==uu ? uu : uu*2);

                bs= b.get(1)-v;
                bss= v-a.get(1);
            }


            //===================================
            // Step 4: Rearrangement of the lists, keeping S constant.
            //===================================
            if(as == 0.0f)
	    {
                if(ass==0.0f)
                {
                    a.set(1, a.get(m));
                    m--;
                }
                else a.set(1,ass);

                a.buildHeapFromTop(m);
            }
            else    
	    {
                a.set(1,as);
                a.buildHeapFromTop(m);
 
                if(ass!=0.0f)
                {
                    a.set(++m, ass);
                    a.buildHeapFromBelow(m);
                }
            };

            if(bs == 0.0f)
	    {
                if(bss==0.0f)
                {
                    b.set(1, b.get(n));
                    n--;
                }
                else b.set(1, bss);

                b.buildHeapFromTop(n);

            }
            else    
	    {
                b.set(1, bs);
                b.buildHeapFromTop(n);
                if(bss!=0.0f)
                {
                    b.set(++n, bss);
                    b.buildHeapFromBelow(n);
                }
            };
        };
        return sg;
    };

};





