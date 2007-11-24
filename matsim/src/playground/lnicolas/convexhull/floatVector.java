/* *********************************************************************** *
 * project: org.matsim.*
 * floatVector.java
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
//  File: floatVector.java
//
// - floatVector is a vector of Floats.
// - Its functionality includes:
//   .appending Float values to the vect
//   .setting values at certain positions
//   .getting the value at certain positions
//   .reverse sorting this vector using a
//    reverse heap sort algorithm
//
//  Programmed by Lynn Tetreault,  1998
//***********************************************

package playground.lnicolas.convexhull;

import java.util.Vector;

public class floatVector extends Vector<Float>
{
     public floatVector(){   super(5,10);   };

     @Override
		public Float get(int i)
     {
         return elementAt(i);
     };

     public void set(int i, float value)
     {
         setElementAt(new Float(value), i);
     };

     public void append(float value)
     {
         addElement( new Float(value) );
     };

     public String show()
     {   String temp = new String("Size = " + size() + "\n");
         String temp2 = new String("\n");

         for(int i=0; i<size(); i++)
         {
             temp2 = new String(i + ": " + get(i) + "   ");
             if((i%5) == 4) temp2 += "\n";
             temp += temp2;
         };
         return temp;
     };


     // Heapsort, modified from ``Numerical Recipes in C'' by Press, Flannery,
     // Teukolsky und Vetterling, Cambridge University Press, page 247.
     //
     // Sorts this Vector of floating point numbers in descending order.
     //
     // You need to place a dummy value in element 0 of the vector.
     // Element 0 remains untouched.
     // This function was tested and works fine.
     //
     public void reverseHeapSort()
     {
         trimToSize();
         int n = size()-1;

         int l,j,ir,i;
         float rra;

         l=(n >> 1)+1;
         ir=n;
         for(;;)
         {
             if (l>1) rra=get(--l);
             else
             {
                 rra=get(ir);
                 set(ir, get(1));
                 if (--ir==1)
                 {
                     set(1, rra);
                     return;
                 }
             }
             i=l;
             j=l<<1;
             while (j<=ir)
             {
                 if ( j < ir && get(j) > get(j+1) ) ++j;
                 if ( rra > get(j) )
                 {
                     set(i, get(j));
                     j+=(i=j);
                 }
                 else j=ir+1;
             }
             set(i, rra);
         }
     };



     // Description: The heap property for ra is reestablished under
     // the assumption that the property is only violated at the
     // root (a[1]) of the heap.
     //
     // Uncertain as to the source and accuracy of this function
     //
     public void buildHeapFromTop(int n)
     {
         int i=1,m;
         float top=get(1);

         while (2*i < n)    //Originally "while (2*i <= n)"
         {                  //Changed to "while (2*i < n) to avoid"
             m= 2*i;        //IndexOutOfBounds Errors at get(m+1)

             if( get(m) < get(m+1) )
                 if (m<n) m++;

             if(top < get(m)){ set(i, get(m)); i=m; }
             else break;
         }
         set(i, top);
     };


     // Function buildHeapFromBelow:
     // Description: The heap property for ra is reestablished under
     // the assumption that the property is only violated at the
     // place n in the heap.
     //
     // Uncertain as to the source and accuracy of this function
     //
     public void buildHeapFromBelow(int n)
     {
         int i=n,m;
         float last=get(n);

         while (i/2>0)
         {
             m= i/2;
             if ( get(m)<last ) { set(i, get(m)); i=m;}
             else break;
         }
         set(i, last);
     };
};


