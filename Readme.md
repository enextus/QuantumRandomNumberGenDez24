# Актуальное описание проекта

## Проект: Визуализация фрактала Серпинского с использованием случайных чисел

### Обзор

Это приложение разработано для визуализации фрактала Серпинского с использованием случайных чисел, получаемых от внешнего API. Оно включает в себя несколько классов, каждый из которых выполняет специфические функции для генерации, обработки и отображения точек фрактала.

### Классы

- **App.java** - класс-входная точка приложения. Отвечает за инициализацию основных компонентов, запуск графического интерфейса пользователя (GUI) и управление таймером для периодического обновления точек фрактала.
- **Dot.java** - класс представляет неизменяемую точку на плоскости. Используется для хранения позиций точек, которые будут нарисованы на панели.
- **DotController.java** - класс управляет отображением и обновлением точек фрактала Серпинского. Обрабатывает генерацию новых точек, их рендеринг и управление состоянием приложения при возникновении ошибок.
- **RandomNumberProvider.java** - класс отвечает за получение случайных чисел от внешнего API и предоставление их другим частям приложения. Управляет запросами к API, анализирует ответы и управляет очередью случайных чисел.

---

# Current Project Description

## Project: Visualization of the Sierpinski Fractal Using Random Numbers

### Overview

This application is designed to visualize the Sierpinski fractal using random numbers obtained from an external API. It consists of several classes, each performing specific functions for the generation, processing, and display of fractal points.

### Classes

- **App.java** - the entry point of the application. Responsible for initializing the main components, launching the graphical user interface (GUI), and managing the timer for periodically updating the fractal points.
- **Dot.java** - a class that represents an immutable point on a plane. It is used to store the positions of points that will be drawn on the panel.
- **DotController.java** - a class that manages the display and updating of Sierpinski fractal points. It handles the generation of new points, rendering, and managing the application's state in case of errors.
- **RandomNumberProvider.java** - a class responsible for fetching random numbers from an external API and providing them to other parts of the application. It manages API requests, parses responses, and handles the queue of random numbers.
