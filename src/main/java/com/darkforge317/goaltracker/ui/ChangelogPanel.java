package com.darkforge317.goaltracker.ui;

import net.runelite.client.ui.ColorScheme;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.GridLayout;

public class ChangelogPanel extends JPanel
{
    public ChangelogPanel(Runnable onSeeAll, Runnable onClose)
    {
        super(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        JPanel headerBar = new JPanel(new GridLayout(1, 2, 4, 0));
        headerBar.setBorder(new EmptyBorder(4, 4, 4, 4));

        JButton seeAllButton = new JButton("See All");
        seeAllButton.addActionListener(e -> onSeeAll.run());

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> onClose.run());

        headerBar.add(seeAllButton);
        headerBar.add(closeButton);

        add(headerBar, BorderLayout.NORTH);
    }
}