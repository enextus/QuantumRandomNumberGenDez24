# Project: Sierpinski Fractal Visualization Using Random Numbers

## Overview

This application is designed to visualize the Sierpinski fractal using random numbers obtained from an external API. The application consists of several classes, each performing specific functions to ensure the generation, processing, and display of fractal points.

---

## Classes

### 1. App.java

**Description:**
The `App` class serves as the entry point of the application. It is responsible for initializing the main components, launching the graphical user interface (GUI), and managing the timer that periodically updates the fractal points.

**Additional Comments and Explanations:**

- **Class Fields:**
    - `CLOSING_PARENTHESIS`: Used for formatting the window title.
    - `DOT_MOVER`: The name of the application.
    - `DOT_MOVER_DOTS`: A template for the window title that displays the number of dots.
    - `DELAY`: The interval time between dot updates in milliseconds.

- **Method `main`:**
    - Creates instances of `RandomNumberProvider` and `DotController`.
    - Launches the GUI in a separate thread using `SwingUtilities.invokeLater`.
    - Configures the `JFrame` using the `BorderLayout` layout manager.
    - Adds `DotController` to the center of the window.
    - Sets the default close operation to `EXIT_ON_CLOSE`.
    - Maximizes the window to full screen.
    - Creates and starts a `Timer` that, every `DELAY` milliseconds, calls the `moveDot` method of `DotController` to update the dots.
    - Updates the window title with the current number of dots.
    - If an error occurs (`errorMessage` is not `null`), stops the timer and displays an error message to the user using `JOptionPane`.

---

### 2. Dot.java

**Description:**
The `Dot` class represents an immutable point with coordinates on a plane. It is used to store the positions of points that will be drawn on the panel.

**Additional Comments and Explanations:**

- **Constructor:**
    - Accepts the initial coordinates of the dot and creates a copy of the passed `Point` to ensure immutability.

- **Method `point()`:**
    - Returns a copy of the point to prevent external modification of the original `Point` object.

- **Immutability:**
    - Although the `record` itself is immutable, additional copying of the `Point` object ensures safety since `Point` is mutable.

---

### 3. DotController.java

**Description:**
The `DotController` class is responsible for managing and displaying the points of the Sierpinski fractal. It handles the generation of new points, their rendering, and managing the application's state when errors occur.

**Additional Comments and Explanations:**

- **Class Fields:**
    - `SIZE`: Defines the size of the panel in pixels.
    - `DOT_SIZE`: The size of each dot on the panel.
    - `dots`: A thread-safe list for storing all drawn dots.
    - `randomNumberProvider`: An object responsible for providing random numbers from the API.
    - `dotCounter`: A counter tracking the total number of drawn dots.
    - `errorMessage`: Stores the error message if an error occurs.
    - `currentPoint`: The current position of the dot from which a new position will be calculated.
    - `offscreenImage`: A buffered image for more efficient dot rendering.

- **Method `moveDot()`:**
    - Launches a background task using `SwingWorker` to generate and render new dots.
    - Attempts to add up to 10,000 dots per update cycle.
    - If a `NoSuchElementException` occurs (e.g., due to reaching the API request limit), sets the `errorMessage` and stops adding new dots.

- **Method `calculateNewDotPosition()`:**
    - Calculates the new position of a dot based on a random number.
    - Divides the range of random numbers into three parts, each corresponding to one of the vertices of the Sierpinski triangle.
    - Depending on which range the random number falls into, the dot moves towards the corresponding vertex.

- **Method `drawDots()`:**
    - Draws new dots on the buffered image to enhance performance.
    - Uses `Graphics2D` to draw rectangles representing the dots.

- **Method `paintComponent()`:**
    - Renders the buffered image onto the panel.
    - If there is an error message, it displays the message over the triangle.

- **Method `getDotCounter()`:**
    - Returns the current number of drawn dots.

**Error Handling:**
- When an error occurs (e.g., reaching the API request limit), the application stops adding new dots and displays an error message to the user.
- This prevents further attempts to fetch random numbers after the set limit is reached.

**Thread Safety:**
- The use of `CopyOnWriteArrayList` for the list of dots ensures thread safety, which is crucial when working with `SwingWorker`.

**Performance Optimization:**
- A buffered image (`BufferedImage`) is used for drawing dots, significantly speeding up the visualization process, especially with a large number of dots.

---

### 4. RandomNumberProvider.java

**Description:**
The `RandomNumberProvider` class is responsible for fetching random numbers from an external API and providing them to other parts of the application. It handles API requests, parses responses, and manages a queue of random numbers.

**Additional Comments and Explanations:**

- **Class Fields:**
    - `API_URL`: The URL of the API for fetching random numbers.
    - `MAX_API_REQUESTS`: The maximum number of API requests allowed. After reaching this limit, no new requests are sent.
    - `randomNumbersQueue`: A thread-safe queue for storing fetched random numbers.
    - `objectMapper`: An object from the Jackson library for handling JSON responses.
    - `apiRequestCount`: A counter for the number of API requests made.

- **Constructor:**
    - Initializes the `randomNumbersQueue` and the `objectMapper`.
    - Calls the `loadInitialData()` method to load the initial set of random numbers.

- **Method `loadInitialData()`:**
    - Checks if the maximum number of API requests has been reached.
    - Forms the request URL and sends an HTTP GET request.
    - Reads and processes the API response.
    - Parses the JSON response to extract the `"qrn"` field (a HEX string of numbers) and the `"length"` field.
    - Converts the HEX string into a byte array and adds each number (0 to 255) to the `randomNumbersQueue`.
    - Increments the `apiRequestCount`.
    - Handles potential errors and exceptions, outputting relevant messages to the console.

- **Method `hexStringToByteArray(String s)`:**
    - Converts a HEX string into a byte array.
    - Every two characters of the HEX string are converted into one byte.

- **Method `getNextRandomNumber()`:**
    - Attempts to retrieve the next random number from the `randomNumbersQueue`, waiting up to 5 seconds.
    - If no number is available and the API request limit has been reached, throws a `NoSuchElementException` with an appropriate message.
    - If no number is available but the request limit has not been reached, attempts to load more data and tries again.
    - If there are few numbers left in the queue (less than 1000) and the request limit has not been reached, automatically loads additional data.

- **Method `getNextRandomNumberInRange(long min, long max)`:**
    - Retrieves a random number between 0 and 255.
    - Normalizes it to the range [0.0, 1.0].
    - Scales it to the specified range [min, max].
    - Returns the scaled number as a `long`.

**Error Handling:**
- When an error occurs (e.g., reaching the API request limit or lack of available numbers), the `getNextRandomNumber()` method throws a `NoSuchElementException` with an informative message.
- These exceptions are handled in the `DotController` class, which stops further attempts to add dots and displays an error message to the user.

**Thread Safety:**
- The use of `LinkedBlockingQueue` ensures safe access to the queue of random numbers from different threads, which is important when working with `SwingWorker` and multithreading in the application.

**Use of Jackson Library:**
- `ObjectMapper` is used to parse JSON responses from the API, simplifying the processing and extraction of necessary data.

---

## Recommendations for Further Improvement

1. **Logging Implementation:**
    - For more flexible and robust logging, consider using logging libraries such as `java.util.logging` or `Log4j`.
    - This will allow configuring logging levels, outputting logs to files, and managing messages more efficiently.

2. **Error Handling:**
    - Instead of throwing `NoSuchElementException`, consider creating your own exception classes for more precise error management.
    - This will facilitate handling different types of errors in other parts of the application.

3. **Configuration Flexibility:**
    - Make parameters such as `MAX_API_REQUESTS`, `n` (number of bytes), and `DELAY` (update interval) configurable through an external configuration file or command-line arguments.
    - This will allow easier modification of the application's behavior without the need to change the code.

4. **Performance Optimization:**
    - If the application requires processing large volumes of data, consider optimizing the algorithms for processing and storing random numbers.
    - For example, you can use more efficient data structures or data processing methods.

5. **Testing:**
    - Develop unit tests for key components of the application to ensure their correct operation and facilitate future modifications.
    - This will enhance the reliability and resilience of the application.

6. **Documentation:**
    - In addition to code comments, consider creating comprehensive documentation (e.g., using Javadoc) that describes the architecture and components of the application.
    - This will help new developers quickly understand the project.

---

## Conclusion

We have successfully developed a Sierpinski fractal visualization application that efficiently utilizes random numbers obtained from an external API. The application includes well-structured classes with detailed comments in both Russian and English, facilitating its understanding and further development.

If you have any additional questions or require assistance with other parts of the project, please feel free to reach out!

---

*This README was automatically generated using comments and explanations provided by the developer and the assistant.*
