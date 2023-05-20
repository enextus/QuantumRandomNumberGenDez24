package org.ThreeDotsSerpinski;

import java.util.Random;

class DiceRoller {
    public static final int BOUND = 6;
    private Random random;

    public DiceRoller() {
        this.random = new Random();
    }

    public int rollDice() {
        return 1 + random.nextInt(BOUND);
    }

}
