# Three Dots Serpinski

## Introduction

This Java application creates a simulation of the Sierpinski Triangle. The Sierpinski Triangle is a fractal with the overall shape of an equilateral triangle, subdivided recursively into smaller equilateral triangles.

The application is built with basic Java classes and Swing for the GUI. The simulation works by repeatedly "rolling a dice" and moving a "dot" accordingly, leaving a trace behind, which eventually creates a Sierpinski triangle.

## How it Works

The application consists of the following key classes:

1. **App:** The main entry point of the application. It creates a JFrame and adds a DotMover component to it. It also starts a Timer that triggers the dot's movement every millisecond.

2. **Dot:** A simple class that represents a dot with a position (Point) and a creation date.

3. **DiceRoller:** This class encapsulates a random number generator, simulating a dice roll. The `rollDice` method returns an integer between 1 and 6.

4. **DotMover:** This class extends a JPanel and is responsible for the logic of moving the dot and painting the dots on the panel. The dot's movement direction depends on the dice roll result.

## How to Use

1. Clone the repository or download the Java files to your local machine.

2. Compile and run the `App` class. A GUI window will open, showing the moving dot and the Sierpinski Triangle being formed in real time.

## Implementation Details

The DotMover's `moveDot` method is called every millisecond by a Swing Timer, initiated in the `App` class. The `moveDot` method:

- Rolls a dice (1-6).
- Moves the dot to a new position, according to the roll result (1-2: to the lower left, 3-4: to the lower right, 5-6: up).
- Adds the new dot to a list of dots.
- Calls `repaint` to trigger the Swing repaint process.

In the `paintComponent` method, all dots are drawn on the JPanel, with their color's alpha channel adjusted based on their age.

Please note that because the dots are not removed from the list, the list can become quite large after a while, depending on your system's performance.

## Requirements

To run the project, you need a Java Runtime Environment (JRE) installed on your machine. The project was developed using Java SE 11, but it should run on newer versions as well.

## Future Improvements

Potential future improvements for this project could be:

- Optimizations to handle a large number of dots.
- Options to customize the simulation (e.g., speed, color).
- Pause/Resume functionality.
- Saving and loading simulation states.

## Conclusion

This project provides an interesting visualization of the Sierpinski Triangle. It's a simple application demonstrating the power of basic mathematical concepts, randomness, and fractals.
