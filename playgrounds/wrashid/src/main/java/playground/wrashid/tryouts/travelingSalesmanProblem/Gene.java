/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.wrashid.tryouts.travelingSalesmanProblem;

/*
 * Created on 05-Jan-2006
 * 
 * This source code is released under the GNU Lesser General Public License Version 3, 29 June 2007
 * see http://www.gnu.org/licenses/lgpl.html or the plain text version of the LGPL included with this project
 *
 * It comes with no warranty whatsoever
 */

/**
 * The gene data is a byte array, but really each allele is represented by a number of bits of fixed length (specified in the Environment). 
 * This class takes care of translating between gene data and alleles, and of the mating operator.
 * 
 * @author Bjoern Guenzel - http://blog.blinker.net
 */
public class Gene {
        
        private Environment environment;
        
        private byte[] geneData;

        public Gene(byte[] geneData, Environment envrionment){
                this.geneData = geneData;
                this.environment = envrionment;
        }
        
        
        public byte[] getGeneData() {
                return geneData;
        }

        public void setGeneData(byte[] geneData) {
                this.geneData = geneData;
        }
        
        public void createOffspring(Gene gene2, Gene offspring){
                int crossoverBit = Math.abs(Environment.random.nextInt()%environment.bitsPerGene);
                int crossoverByte = crossoverBit/Byte.SIZE;
                
                byte[] firstGene, lastGene;//who provides the first bits?
                
                if(Environment.random.nextInt()%2 == 0){
                        firstGene = geneData;
                        lastGene = gene2.geneData;
                } else {
                        firstGene = gene2.geneData;
                        lastGene = geneData;
                }
                
//              System.out.println("#### mate ###");
//              
//              System.out.println("gene1: ");
//              printGene(gene1);
//              System.out.println("gene2: ");
//              printGene(gene2);
//              System.out.println("crossoverByte:"+crossoverByte);
                
                System.arraycopy(firstGene, 0, offspring.geneData, 0, crossoverByte);
                System.arraycopy(lastGene, crossoverByte, offspring.geneData, crossoverByte, lastGene.length-crossoverByte);
                
                int crossoverBitFromRight = Byte.SIZE-crossoverBit%Byte.SIZE;
                
                byte mask = (byte) (Math.pow(2,crossoverBitFromRight) -1);
                
                offspring.geneData[crossoverByte] &= mask;
                
                offspring.geneData[crossoverByte] |= (byte)(firstGene[crossoverByte] & ~mask);
                
//              System.out.println("offspring: ");
//              printGene(offspring);
                
                //mutate
                
                for(int i = 0;i<environment.bitsPerGene;i++){
                        if(Environment.random.nextFloat() < Environment.MUTATION_RATE){
//                              System.out.println("mutate bit "+i+", byte before: "+Integer.toBinaryString(offspring[i/Byte.SIZE]  & 0xff));
                                offspring.geneData[i/Byte.SIZE] ^= (byte)((1<<(Byte.SIZE-i%Byte.SIZE-1)));
//                              System.out.println("byte after: "+Integer.toBinaryString(offspring[i/Byte.SIZE] & 0xff));
                        }
                }
        }
        
        public int getAllele(int index){
                int allele = 0;
                
                int firstBitIndex = index*environment.bitsPerAllele;
                
                int bitsToGo = environment.bitsPerAllele;
                
                int byteIndexInGene = firstBitIndex/Byte.SIZE;
                int inBitsFromRight = Byte.SIZE-firstBitIndex%Byte.SIZE;
                
                allele |= geneData[byteIndexInGene];
                
                //remove heading bits
                
                allele &= (int)(Math.pow(2, inBitsFromRight))-1;
                
                bitsToGo -= inBitsFromRight;
                
                byte nextByte = 0;
                
                while(bitsToGo > 0){
                        
                        byteIndexInGene++;
                        nextByte = geneData[byteIndexInGene];
                        
                        if(bitsToGo > Byte.SIZE){
                                allele <<=  Byte.SIZE;
                        } else {
                                allele <<= bitsToGo;
                                nextByte >>>= Byte.SIZE-bitsToGo;
                        }
                        
                        allele |= (nextByte & 0xff);
                        
                        bitsToGo -= Byte.SIZE;
                }
                
                return allele;
                
        }
        
}

