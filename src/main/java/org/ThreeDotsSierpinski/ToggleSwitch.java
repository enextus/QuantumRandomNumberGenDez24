package org.ThreeDotsSierpinski;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Современный UI компонент: переключатель-слайдер (Toggle Switch).
 */
public class ToggleSwitch extends JComponent {
    private boolean selected;
    private final int width = 50;
    private final int height = 26;

    // Ручное хранилище слушателей (так как JComponent не имеет встроенной поддержки ActionListener)
    private final List<ActionListener> actionListeners = new ArrayList<>();

    public ToggleSwitch(boolean isSelected) {
        this.selected = isSelected;
        setPreferredSize(new Dimension(width, height));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setToolTipText("Переключить источник случайных чисел");

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                selected = !selected;
                repaint();
                fireActionPerformed();
            }
        });
    }

    // Добавляем метод addActionListener вручную
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

        // Фон слайдера (зеленый если включен, серый если выключен)
        g2.setColor(selected ? new Color(34, 139, 34) : new Color(180, 180, 180));
        g2.fillRoundRect(0, 0, width, height, height, height);

        // Белый кружок-ползунок
        int knobDiameter = height - 6;
        int x = selected ? width - knobDiameter - 3 : 3;
        g2.setColor(Color.WHITE);
        g2.fillOval(x, 3, knobDiameter, knobDiameter);

        g2.dispose();
    }

    public boolean isSelected() { return selected; }
    public void setSelected(boolean selected) { this.selected = selected; repaint(); }
}
