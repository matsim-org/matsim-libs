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
 * Created on 06-Jan-2006
 * 
 * This source code is released under the GNU Lesser General Public License Version 3, 29 June 2007
 * see http://www.gnu.org/licenses/lgpl.html or the plain text version of the LGPL included with this project
 * 
 * It comes with no warranty whatsoever
 */

import java.util.Random;


/**
 * The environment holds the random seed (for reproducible simulations), the population, the mutationrate, keeps track of fitness
 * and runs the genetic algorithm. 
 * 
 * Since it was written as a proof of concept, it currently has too many dependencies on the TravelingSalesman class. It would
 * be desireable to user more abstractions, so that the GA can be used for a wider class of problems. Also parameters like
 * population size and mutation rate shouldn't be hardcoded. As a proof of concept it works, though.
 * 
 * In each step, the algorithm picks two parents and replaces a random gene with the offspring of the two parents. The likelihood
 * for a parent to be picked depends on it's fitness 
 *  
 * @author Bjoern Guenzel - http://blog.blinker.net
 */
public class Environment {

        public final static float MUTATION_RATE = 0.003f;
        
        public final static int RANDOM_SEED = 1234;
        
        public final static int POPULATION_SIZE = 500;//must be even number
        
        public static Random random = new Random(RANDOM_SEED);

        public int bitsPerAllele;
        
        public int allelesCount;
        public int bitsPerGene;
        public int bytesPerGene;
        
        public Gene[] genes;
        
        public Gene[] oldGenes;
        
        public double[] fitnesses;
        
        public double fitnessSum;
        
        private TravelingSalesman travelingSalesman;
        
        private int[] route;//temporary variable for calculating the fitness
        
        private double minCost;
        int fittestGeneIndex;
        
        public Environment(TravelingSalesman travelingSalesman){
                this.travelingSalesman = travelingSalesman;
                
                initGenes();
                
                route = new int[travelingSalesman.n];
                
                fitnesses = new double[POPULATION_SIZE];
        }
        
        public void run(){
                
                int generation = 0;
                calculateFitnesses();
                
                //main loop
                while(true){
                        if(generation % POPULATION_SIZE == 0){
                                System.out.println("\nGeneration: "+generation+", cost: "+minCost);
                                System.out.print("\nminimum gene: ");
                                printGene(genes[fittestGeneIndex]);
                                initRouteFromGene(genes[fittestGeneIndex]);
                                System.out.print("\nminimum route: ");
                                printRoute();
                                System.out.print("\n");
                        }
                        
                        createNewOffspring();
                        
                        generation++;
                }
        }
        
        
        private void initGenes(){
                //0 to n-1 for the cities, n to denote the seperator. Superfluous numbers are being ignored
                bitsPerAllele = (int) (Math.log(travelingSalesman.n)/Math.log(2));
                
                //2n alleles, as we use pair permutations
                allelesCount = 2*travelingSalesman.n;
                bitsPerGene = (allelesCount*bitsPerAllele);
                bytesPerGene = (bitsPerGene+Byte.SIZE-1)/Byte.SIZE;
                
                genes = new Gene[POPULATION_SIZE];
                oldGenes = new Gene[POPULATION_SIZE];
                
                //random initial genes
                for(int i = 0;i<genes.length;i++){
                        byte[] geneData = new byte[bytesPerGene];
                        
                        Environment.random.nextBytes(geneData);
                        
                        genes[i] = new Gene(geneData, this);
                        oldGenes[i] = new Gene(new byte[bytesPerGene], this);
                }
        }       
        
        private void createNewOffspring(){
                //indices of parent genes
                int parent1 = pickGene();
                int parent2 = pickGene();
                
                //index of gene to be replaced
                int replacedGene = 0;
                
                //choose gene to be replaced - preserve parents and fittest gene
                do{
                        replacedGene = Math.abs(random.nextInt()%genes.length);
                } while (replacedGene == parent1 || replacedGene == parent2 || replacedGene == fittestGeneIndex);
                
                //replace old gene with offspring of i1 and i2
                genes[parent1].createOffspring(genes[parent2], genes[replacedGene]);
                
                //update fitnessSum: remove fitness of old gene, add fitness of new gene
                
                fitnessSum -= fitnesses[replacedGene];
                
                initRouteFromGene(genes[replacedGene]);
                
                double cost = travelingSalesman.calculateCosts(route);
                
                if(minCost < 0 || cost < minCost){
                        minCost = cost;
                        fittestGeneIndex = replacedGene;
                }
                
                fitnesses[replacedGene] = 1/cost;
                
                fitnessSum += fitnesses[replacedGene];
        }

        /**
         * Picking a gene works like a roulette wheel: a random nummer between 0 and the total population fitness
         * is chosen. Then starting from the first index, the fitnesses of the genes are being added up sequentially, 
         * until the sum is greater than the chosen number. The index for the last gene is the pick. 
         * By this algorithm, the probability for a gene to be picked is proportional to it's fitness.
         * 
         * @return index into the population, pointing to a gene
         */
        private int pickGene() {
                double selector = Math.abs(random.nextDouble())%fitnessSum;//random number between 0 and fitnessSum
                
                int j = 0;
                
                double count = fitnesses[j];
                
                while(count < selector && j<fitnesses.length-1){
                        count += fitnesses[++j];
                }
                
                return j;
        }
        
        private void calculateFitnesses(){
                
                fitnessSum = 0;
                
                minCost = -1;
                
                for(int i = 0;i<POPULATION_SIZE;i++){
                        initRouteFromGene(genes[i]);
                        double cost = travelingSalesman.calculateCosts(route);
                        
                        if(minCost < 0 || cost < minCost){
                                minCost = cost;
                                fittestGeneIndex = i;
                        }
                        
                        fitnesses[i] = 1/cost;
                
                        fitnessSum += fitnesses[i];
                        
//                      System.out.print("\ngene: ");printGene(genes[i]);
//                      System.out.print("\nroute: ");printRoute();
//                      System.out.println("\ncost: "+cost+", fitness: "+fitnesses[i]);
                }
        }
        
        private void initRouteFromGene(Gene gene){
                
                //init to ordered route
                for(int i = 0;i<route.length;i++){
                        route[i] = i;
                }
                
                int lastAllele = travelingSalesman.n;
                
                //apply transpositions
                
                for(int i = 0;i<allelesCount;i++){
                        //differeing strategies to deal with superfluous alleles - modulo n favours some numbers, 
                        //but presumably so would any other scheme
                        int allele = gene.getAllele(i)%travelingSalesman.n;
                        
                        if(i > 0){
                                int tmpCity = route[allele];
                                route[allele] = route[lastAllele];
                                route[lastAllele] = tmpCity;
                        }
                        
                        lastAllele = allele;
                }
                
        }
        
        private void printRoute(){
                for(int i = 0;i<route.length;i++){
                        System.out.print(route[i]+" ");
                }
        }
        
        private void printGene(Gene gene){
                for(int i = 0;i<allelesCount;i++){
                        //differeing strategies to deal with superfluous alleles - modulo n favours some numbers, 
                        //but presumably so would any other scheme
                        int allele = gene.getAllele(i)%travelingSalesman.n;
                        
                        if(i%2 == 0){
                                System.out.print("(");
                        } 
                        System.out.print(allele);
                        if(i%2 == 0){
                                System.out.print(" ");
                        } else {
                                System.out.print(") ");
                        }
                }
        }
        
}
