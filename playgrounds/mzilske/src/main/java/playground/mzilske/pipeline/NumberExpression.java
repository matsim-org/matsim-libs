package playground.mzilske.pipeline;

/* 
 * NumberExpression.java - a simple number expression parser
 * 
 * Copyright (c) 2010 Michael Schierl
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * - Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 *   
 * - Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *   
 * - Neither name of the copyright holders nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *   
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND THE CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR THE CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/**
 * An expression that matches nonnegative numbers. This supports cron-like
 * expressions, like <code>1,3-6,100-200,666,1000-3000/5,400-/7</code>,
 * <code>-100,102-</code> or <code>*</code>. Odd or even numbers can be
 * matched either by cron's step syntax, or by suffixing a simple range
 * (without step values) with <code>e</code> or <code>o</code>.
 * 
 * @author Michael Schierl
 */
public class NumberExpression {

    private final NumberRange[] ranges;
    private final int min, max;

    /**
     * Create a new {@link NumberExpression}.
     * 
     * @param pattern
     *            the expression pattern.
     * @throws IllegalArgumentException
     *             if the pattern is malformed
     */
    public NumberExpression(String pattern) {
        String[] parts = pattern.toLowerCase().split(",",-1);
        ranges = new NumberRange[parts.length];
        int min = Integer.MAX_VALUE, max = 0;
        for (int i = 0; i < ranges.length; i++) {
            String part = parts[i];
            try {
                if (part.equals("*")) {
                    ranges[i] = new NumberRange(0, Integer.MAX_VALUE, 0, 1);
                } else if (part.matches("\\*/\\d+")) {
                    ranges[i] = new NumberRange(0, Integer.MAX_VALUE, 0, Integer.parseInt(part.substring(2)));
                } else if (part.matches("\\d+")) {
                    int value = Integer.parseInt(part);
                    ranges[i] = new NumberRange(value, value, 0, 1);
                } else if (part.matches("\\d*-\\d*")) {
                    String[] limits = part.split("-", -1);
                    int from = limits[0].length() == 0 ? 0 : Integer.parseInt(limits[0]);
                    int to = limits[1].length() == 0 ? Integer.MAX_VALUE : Integer.parseInt(limits[1]);
                    if (to < from)
                        throw new IllegalArgumentException("Invalid pattern: " + part);
                    ranges[i] = new NumberRange(from, to, 0, 1);
                } else if (part.matches("\\d*-\\d*/\\d+")) {
                    String[] rangeAndModulus = part.split("/", -1);
                    String[] limits = rangeAndModulus[0].split("-", -1);
                    int from = limits[0].length() == 0 ? 0 : Integer.parseInt(limits[0]);
                    int to = limits[1].length() == 0 ? Integer.MAX_VALUE : Integer.parseInt(limits[1]);
                    int modulus = Integer.parseInt(rangeAndModulus[1]);
                    if (to < from)
                        throw new IllegalArgumentException("Invalid pattern: " + part);
                    ranges[i] = new NumberRange(from, to, from % modulus, modulus);
                } else if (part.matches("\\d*-\\d*[eo]")) {
                    String[] limits = part.substring(0, part.length() - 1).split("-", -1);
                    int from = limits[0].length() == 0 ? 0 : Integer.parseInt(limits[0]);
                    int to = limits[1].length() == 0 ? Integer.MAX_VALUE : Integer.parseInt(limits[1]);
                    if (to < from)
                        throw new IllegalArgumentException("Invalid pattern: " + part);
                    ranges[i] = new NumberRange(from, to, part.charAt(part.length() - 1) == 'o' ? 1 : 0, 2);
                } else {
                    throw new IllegalArgumentException("Invalid pattern: " + part);
                }
                max = Math.max(max, ranges[i].getMax());
                min = Math.min(min, ranges[i].getMin());
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Invalid pattern: " + part);
            }
        }
        this.max = max;
        this.min = min;
    }

    /**
     * Check whether this number expression matches the given number.
     * 
     * @param number
     *            the number to check against
     * @return whether the expression matches the number
     */
    public boolean matches(int number) {
        if (number < min || number > max)
            return false;
        for (int i = 0; i < ranges.length; i++) {
            if (ranges[i].matches(number))
                return true;
        }
        return false;
    }

    /**
     * Return the minimum number that can be matched.
     */
    public int getMinimum() { return min; }

    /**
     * Return the maximum number that can be matched.
     */
    public int getMaximum() { return max; }

    private static class NumberRange {
        private final int min, max, remainder, modulus;

        NumberRange(int min, int max, int remainder, int modulus) {
            this.min = min;
            this.max = max;
            this.remainder = remainder;
            this.modulus = modulus;
        }

        boolean matches(int number) {
            return number >= min && number <= max && number % modulus == remainder;
        }

        int getMin() { return min; }  
        int getMax() { return max; }
    }
}