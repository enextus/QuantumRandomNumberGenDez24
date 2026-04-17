package org.ThreeDotsSierpinski;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class ToggleSwitch extends JComponent {
    private boolean selected;
    private final int width = 50;
    private final int height = 26;
    private final List<ActionListener> actionListeners = new ArrayList<>();

    public ToggleSwitch(boolean isSelected) {
        this.selected = isSelected;
        setPreferredSize(new Dimension(width, height));
        updateCursor();
        setToolTipText("Переключить источник случайных чисел");

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                // Игнорируем клики, если кнопка заморожена
                if (!isEnabled()) return;

                selected = !selected;
                repaint();
                fireActionPerformed();
            }
        });
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        updateCursor();
        repaint(); // Перерисовываем, чтобы изменить цвет
    }

    private void updateCursor() {
        setCursor(isEnabled() ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
    }

    public void addActionListener(ActionListener listener) {
        actionListeners.add(listener);
    }

    private void fireActionPerformed() {
        ActionEvent event = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "toggle");
        for (ActionListener listener : actionListeners) {
            listener.actionPerformed(event);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color trackColor;
        Color knobColor;

        if (!isEnabled()) {
            // ЗАМОРОЖЕННОЕ СОСТОЯНИЕ: Все серое
            trackColor = new Color(200, 200, 200);
            knobColor = new Color(230, 230, 230);
        } else {
            // АКТИВНОЕ СОСТОЯНИЕ
            trackColor = selected ? new Color(34, 139, 34) : new Color(180, 180, 180);
            knobColor = Color.WHITE;
        }

        // Фон
        g2.setColor(trackColor);
        g2.fillRoundRect(0, 0, width, height, height, height);

        // Ползунок
        int knobDiameter = height - 6;
        int x = selected ? width - knobDiameter - 3 : 3;
        g2.setColor(knobColor);
        g2.fillOval(x, 3, knobDiameter, knobDiameter);

        g2.dispose();
    }

    public boolean isSelected() { return selected; }
    public void setSelected(boolean selected) {
        this.selected = selected;
        repaint();
    }
}
