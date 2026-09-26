package com.darkforge317.goaltracker.ui;

import com.darkforge317.goaltracker.GoalTrackerPlugin;
import com.darkforge317.goaltracker.services.ChangelogService;
import com.darkforge317.goaltracker.services.PanelService;
import com.darkforge317.goaltracker.utils.ChangelogMarkdownRenderer;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.BorderFactory;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;

public class ChangelogPanel extends JPanel
{
    private final PanelService panelService;
    private final boolean createdByChangelogListPanel;
    private static final int CONTENT_PADDING = 8;

    public ChangelogPanel(PanelService panelService, ChangelogService.ChangelogEntry entry, boolean isNewUpdate, boolean createdByChangelogListPanel)
    {
        super(new BorderLayout());
        this.panelService = panelService;
        this.createdByChangelogListPanel = createdByChangelogListPanel;

        setBackground(ColorScheme.DARK_GRAY_COLOR);

        JPanel headerBar = new JPanel(new GridLayout(1, 2, 4, 0));
        headerBar.setBorder(new EmptyBorder(4, 4, 4, 4));

        // If the ChangelogListPanel opened this changelog
        if(createdByChangelogListPanel){
            // Show a Back and Home button
            JButton backButton = new JButton();
            JButton homeButton = new JButton();

            backButton.setText("Back");
            backButton.addActionListener(e -> onBackButtonClicked());
            homeButton.setText("Home");
            homeButton.addActionListener(e -> onHomeButtonClicked());

            headerBar.add(backButton);
            headerBar.add(homeButton);
        }
        // If something else opened this changelog, like a new update
        else {
            // Show the Close and All Changelogs buttons
            JButton closeButton = new JButton();
            JButton seeAllButton = new JButton();

            closeButton.setText("Close");
            closeButton.addActionListener(e -> onCloseButtonClicked());

            seeAllButton.setText("All Changelogs");
            seeAllButton.addActionListener(e -> onSeeAllButtonClicked());

            headerBar.add(closeButton);
            headerBar.add(seeAllButton);
        }

        JLabel titleLabel = new JLabel(isNewUpdate ? "New Update!" : "Goal Tracker Changelog");
        titleLabel.setFont(FontManager.getRunescapeBoldFont());
        titleLabel.setForeground(ColorScheme.BRAND_ORANGE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel versionLabel = new JLabel("V" + entry.getVersion() + " Changes");
        versionLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        versionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel titleSection = new JPanel();
        titleSection.setLayout(new BoxLayout(titleSection, BoxLayout.Y_AXIS));
        titleSection.setBackground(ColorScheme.DARK_GRAY_COLOR);
        titleSection.setBorder(new EmptyBorder(8, 8, 4, 8));
        titleSection.add(titleLabel);
        titleSection.add(Box.createVerticalStrut(4));
        titleSection.add(versionLabel);

        JPanel topSection = new JPanel(new BorderLayout());
        topSection.setBackground(ColorScheme.DARK_GRAY_COLOR);
        topSection.add(headerBar, BorderLayout.NORTH);
        topSection.add(titleSection, BorderLayout.CENTER);

        // JEditorPane as the scroll pane's DIRECT child - getScrollableTracksViewportWidth()
        // only takes effect when checked against the viewport's immediate view, so this
        // can't be nested inside another panel. This forces wrapping against the real
        // viewport width, no pixel-guessing needed.
        JEditorPane contentPane = new JEditorPane()
        {
            @Override
            public boolean getScrollableTracksViewportWidth()
            {
                return true;
            }
        };
        contentPane.setContentType("text/html");
        contentPane.setEditable(false);
        contentPane.setFocusable(false);
        contentPane.setOpaque(false);
        contentPane.setBorder(new EmptyBorder(0, CONTENT_PADDING, CONTENT_PADDING, CONTENT_PADDING));
        contentPane.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        contentPane.setText(ChangelogMarkdownRenderer.toHtml(entry.getMarkdown()));

        JScrollPane scrollPane = new JScrollPane(contentPane);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(ColorScheme.DARK_GRAY_COLOR);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        add(topSection, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void onCloseButtonClicked() {
        panelService.showHome();
    }

    private void onSeeAllButtonClicked() {
        panelService.showChangelogListPanel();
    }

    private void onBackButtonClicked() {
        panelService.showChangelogListPanel();
    }

    private void onHomeButtonClicked() {
        panelService.showHome();
    }
}