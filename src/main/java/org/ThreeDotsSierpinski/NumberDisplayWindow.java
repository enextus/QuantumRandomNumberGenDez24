package org.ThreeDotsSierpinski;

import javax.swing.*;
import java.awt.*;

public class NumberDisplayWindow extends JFrame {
    private JTextArea textArea;

    public NumberDisplayWindow() {
        setTitle("Random Numbers");
        setSize(300, 400);
        setLocationRelativeTo(null);  // Center the window
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);  // Close only this window on close
        initUI();
    }

    private void initUI() {
        textArea = new JTextArea();
        textArea.setEditable(false);  // Make text area non-editable
        JScrollPane scrollPane = new JScrollPane(textArea);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void addNumber(int number) {
        SwingUtilities.invokeLater(() -> {
            textArea.append(number + "\n");
        });
    }
}
