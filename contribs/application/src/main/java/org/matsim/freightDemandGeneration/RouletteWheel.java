package org.matsim.freightDemandGeneration;


import org.apache.commons.math3.stat.Frequency;

import java.util.Iterator;
import java.util.Random;


class RouletteWheel {

    static class Builder{


        private Random random = new Random(Long.MAX_VALUE);

        private final Frequency frequency;

        public static Builder newInstance(Frequency frequency){
            return new Builder(frequency);
        }

        private Builder(Frequency frequency) {
            this.frequency = frequency;
        }

        public Builder setRandom(Random random){
            this.random = random;
            return this;
        }

        public RouletteWheel build(){
            return new RouletteWheel(this);
        }
    }


    private final Random random;

    private final Frequency frequency;

    private RouletteWheel(Builder builder) {
       this.frequency = builder.frequency;
        this.random = builder.random;
    }


    /*package-private*/ Long nextLong(){
        double randomNumber = random.nextDouble();
        double sum = 0;
        Iterator iterator = frequency.valuesIterator();
        while(iterator.hasNext()){
            Long value = (Long) iterator.next();
            sum += frequency.getPct(value);
            if(randomNumber < sum){
                return value;
            }
        }
        throw new IllegalStateException("no item found. this must not be.");
    }

    /*package-private*/ char nextChar(){
        double randomNumber = random.nextDouble();
        double sum = 0;
        Iterator iterator = frequency.valuesIterator();
        while(iterator.hasNext()){
            char value = (Character) iterator.next();
            sum += frequency.getPct(value);
            if(randomNumber < sum){
                return value;
            }
        }
        throw new IllegalStateException("no item found. this must not be.");
    }



}
