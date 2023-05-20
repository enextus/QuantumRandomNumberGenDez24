package org.ThreeDotsSerpinski;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

class DiceRoller {
    private Random random;

    public DiceRoller() {
        this.random = new Random();
    }

    public int rollDice() {
        return 1 + random.nextInt(6); // Генерирует число от 1 до 6
    }
}