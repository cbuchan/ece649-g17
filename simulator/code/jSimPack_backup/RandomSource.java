/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package jSimPack;

import java.util.Random;

/**
 * Wrapper class for Random that lets us manage the random seed.  Random cannot
 * tell you what random seed was used after it is created, and you cannot reseed
 * it later.  This provides both features.
 *
 * @author Justin Ray
 */
public class RandomSource {
    protected Random r;
    protected long seed;
    
    /**
     * Create a random source using the current unix time as a seed.
     */
    public RandomSource() {
        seed = System.currentTimeMillis();
        r = new Random(seed);
    }

    /**
     * @return the Random object
     */
    public Random getRandom() {
        return r;
    }

    /**
     * Reset the random object with the specified seed
     * @param seed
     */
    public void setSeed(long seed) {
        this.seed = seed;
        r = new Random(seed);
    }

    /**
     *
     * @return the seed used to create the current random object
     */
    public long getSeed() {
        return seed;
    }
}
