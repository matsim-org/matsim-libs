package playground.pieter.singapore.utils;

import java.util.*;
class FindCombination
{
    public static void main(String args[])
    {
        Scanner s=new Scanner(System.in);

        System.out.println("Enter the value of n");
        int n=s.nextInt();


        System.out.println("Enter the combination suffix (r)");
        int r=s.nextInt();

        long res=1;

        if(n>=r)
        {
            res=getFact(n)/(getFact(n-r)*getFact(r));
            System.out.println("The result is "+res);
        }
        else System.out.println("r cannot be greater than n");

    }

    public static long getFact(int n)
    {
        long f=1;

        for(int i=n;i>=1;i--)
        {
            f*=i;
        }

        return f;
    }
}