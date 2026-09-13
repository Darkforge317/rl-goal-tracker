package com.darkforge317.goaltracker.ui;

import com.darkforge317.goaltracker.services.ChangelogService;
import com.darkforge317.goaltracker.utils.ChangelogMarkdownRenderer;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Displays a single changelog entry: a header bar with "See All Changelogs" and
 * "Close Changelog" buttons, and the rendered Markdown content below.
 * NOTE: net.runelite.client.ui.FontManager.getRunescapeBoldFont() and
 * ColorScheme.BRAND_ORANGE are used below on the assumption they exist in the
 * RuneLite API version this project targets - worth a quick compile check, since
 * this couldn't be verified against the actual client jar in this environment.
 */
public class ChangelogPanel extends JPanel
{
    public ChangelogPanel(ChangelogService.ChangelogEntry entry, boolean isNewUpdate,
                          Runnable onSeeAll, Runnable onClose)
    {
        super(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        // Header bar: See All Changelogs | Close Changelog
        JPanel headerBar = new JPanel(new GridLayout(1, 2, 4, 0));
        headerBar.setBorder(new EmptyBorder(4, 4, 4, 4));

        JButton seeAllButton = new JButton("See All Changelogs");
        seeAllButton.addActionListener(e -> onSeeAll.run());

        JButton closeButton = new JButton("Close Changelog");
        closeButton.addActionListener(e -> onClose.run());

        headerBar.add(seeAllButton);
        headerBar.add(closeButton);

        // Title + version subtitle + rendered content
        JPanel contentWrapper = new JPanel();
        contentWrapper.setLayout(new BoxLayout(contentWrapper, BoxLayout.Y_AXIS));
        contentWrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
        contentWrapper.setBorder(new EmptyBorder(8, 8, 8, 8));

        JLabel titleLabel = new JLabel(isNewUpdate ? "New Update!" : "Goal Tracker Changelog");
        titleLabel.setFont(FontManager.getRunescapeBoldFont());
        titleLabel.setForeground(ColorScheme.BRAND_ORANGE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel versionLabel = new JLabel("V" + entry.getVersion() + " Changes");
        versionLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        versionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel contentLabel = new JLabel(ChangelogMarkdownRenderer.toHtml(entry.getMarkdown()));
        contentLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        contentLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        contentWrapper.add(titleLabel);
        contentWrapper.add(Box.createVerticalStrut(4));
        contentWrapper.add(versionLabel);
        contentWrapper.add(Box.createVerticalStrut(8));
        contentWrapper.add(contentLabel);

        JScrollPane scrollPane = new JScrollPane(contentWrapper);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(ColorScheme.DARK_GRAY_COLOR);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        add(headerBar, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }
}