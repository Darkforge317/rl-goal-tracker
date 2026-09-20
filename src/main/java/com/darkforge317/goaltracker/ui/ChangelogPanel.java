package com.darkforge317.goaltracker.ui;

import com.darkforge317.goaltracker.services.ChangelogService;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.GridLayout;

public class ChangelogPanel extends JPanel
{
    public ChangelogPanel(ChangelogService.ChangelogEntry entry, boolean isNewUpdate,
                          Runnable onSeeAll, Runnable onClose)
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

        JLabel titleLabel = new JLabel(isNewUpdate ? "New Update!" : "Goal Tracker Changelog");
        titleLabel.setFont(FontManager.getRunescapeBoldFont());
        titleLabel.setForeground(ColorScheme.BRAND_ORANGE);
        titleLabel.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);

        JLabel versionLabel = new JLabel("V" + entry.getVersion() + " Changes");
        versionLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        versionLabel.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);

        JPanel titleSection = new JPanel();
        titleSection.setLayout(new javax.swing.BoxLayout(titleSection, javax.swing.BoxLayout.Y_AXIS));
        titleSection.setBackground(ColorScheme.DARK_GRAY_COLOR);
        titleSection.setBorder(new EmptyBorder(8, 8, 8, 8));
        titleSection.add(titleLabel);
        titleSection.add(javax.swing.Box.createVerticalStrut(4));
        titleSection.add(versionLabel);

        add(headerBar, BorderLayout.NORTH);
        add(titleSection, BorderLayout.CENTER);
    }
}