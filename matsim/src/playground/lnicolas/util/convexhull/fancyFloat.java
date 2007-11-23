/* *********************************************************************** *
 * project: org.matsim.*
 * fancyFloat.java
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
//  File: fancyFloat.java
//  
//  This separates the sign, exponent, and mantissa
//  from a 32-bit floating point number.
//
//  It also contains the function ldexp which java does
//  not appear have.
//
//
//IEEE 754 floating-point "single format" bit layout:  (32 bit)
//       seeeeeee emmmmmmm mmmmmmmm mmmmmmmm
//
// s = sign bit
// e = exponent bits
// m = mantissa bits
//
//---------------------------------------------------------------
// To extract the sign (bit 31)
//       seeeeeee emmmmmmm mmmmmmmm mmmmmmmm
// >>31  00000000 00000000 00000000 0000000s
//       -----------------------------------
//       00000000 00000000 00000000 0000000s
//---------------------------------------------------------------
// To extract the exponent (bits 30-23)
//       seeeeeee emmmmmmm mmmmmmmm mmmmmmmm
// >>23  00000000 00000000 0000000s eeeeeeee
//
//   &   00000000 00000000 00000000 11111111    (0x000000FF)
//       -----------------------------------
//       00000000 00000000 00000000 eeeeeeee
//
//---------------------------------------------------------------
// To extract the mantissa (bits 22-0)
//       seeeeeee emmmmmmm mmmmmmmm mmmmmmmm
//   &   00000000 01111111 11111111 11111111    (0x007FFFFF)
//       -----------------------------------
//       00000000 0mmmmmmm mmmmmmmm mmmmmmmm
//---------------------------------------------------------------
//
//  Programmed by Lynn Tetreault,  1998
//***********************************************

package playground.lnicolas.util.convexhull;

public class fancyFloat
{   
    int sign=0;
    int exponent=0;
    int mantissa=0;
    float fnum = 0.0f;
    int inum = 0;
 
    fancyFloat(float value)
    {   fnum = value; 
        inum = Float.floatToIntBits(fnum);

        sign = (inum >> 31);

        exponent = (inum >> 23) & 0x000000ff; 
        exponent -= 128;                      // bias the exponent.

        mantissa = inum & 0x007fffff;         
        mantissa |= 0x00800000;               // add the implicit leading 1 bit
     };

     int getSign()
     {   if(fnum == 0) return 0;
         return (sign == 1) ? 1 : -1;
     };

     int getExp(){   return exponent;   };

     int getMantissa(){   return mantissa;   };   


     static int getExp(float value)
     {  
         fancyFloat temp = new fancyFloat(value);
         return temp.getExp();
     };

     static int getMantissa(float value)
     {   
         fancyFloat temp = new fancyFloat(value);
         return temp.getMantissa();
     };   

     public static float ldexp(float value, int exp)
     {   Double temp = new Double(Math.pow(2, exp));  
         return value * temp.floatValue();
     };

};



