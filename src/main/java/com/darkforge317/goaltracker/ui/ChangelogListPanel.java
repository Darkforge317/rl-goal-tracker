package com.darkforge317.goaltracker.ui;

import com.darkforge317.goaltracker.GoalTrackerPlugin;
import com.darkforge317.goaltracker.services.ChangelogService;
import com.darkforge317.goaltracker.services.ChangelogService.ChangelogEntry;
import com.darkforge317.goaltracker.services.PanelService;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

import javax.swing.JTextPane;
import javax.swing.border.EmptyBorder;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import java.awt.*;
import java.util.List;

//region LAYOUT HIERARCHY
/*   ChangelogListPanel (JPanel, BorderLayout)
     │
     ├── topSection (JPanel, BorderLayout)
     │   ├── headerBar (JPanel, GridLayout)
     │   │   └── backButton (JButton)
     │   └── titleSection (JPanel, Vertical BoxLayout)
     │       └── titleLabel (JLabel)
     └── scrollPane (JScrollPane)
         └── JViewport (Internal Viewport Area)
             └── contentPane (JTextPane)
                 │<──<──<──<──<──<──<──<──<──<──<──<──<──<──<──<──<──<──<──<──<──<──┐
                 │── [Element N] ComponentView ── changelogEntryButton (JButton)    │
                 │── [Text Context] "\n" (Line Break)                               │
                 └──>──>─ LOOP THROUGH ALL CHANGELOG ENTRIES ─>──>──>──>──>──>──>──>┘
*/
//endregion

public class ChangelogListPanel extends JPanel
{
    private final PanelService panelService;
    private static final int CONTENT_PADDING = 8;
    private ChangelogService changelogService = new ChangelogService();
    private List<ChangelogService.ChangelogEntry> changelogEntries = changelogService.getAllEntries();

    public ChangelogListPanel(PanelService panelService)
    {
        super(new BorderLayout());
        this.panelService = panelService;
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        JPanel headerBar = new JPanel(new GridLayout(1, 2, 4, 0));
        headerBar.setBorder(new EmptyBorder(4, 4, 4, 4));
        JButton backButton = new JButton("Back");
        backButton.addActionListener(e -> onBackButtonClicked());
        headerBar.add(backButton);

        JLabel titleLabel = new JLabel("All Changelogs");
        titleLabel.setFont(FontManager.getRunescapeBoldFont());
        titleLabel.setForeground(ColorScheme.BRAND_ORANGE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel titleSection = new JPanel();
        titleSection.setLayout(new BoxLayout(titleSection, BoxLayout.Y_AXIS));
        titleSection.setBackground(ColorScheme.DARK_GRAY_COLOR);
        titleSection.setBorder(new EmptyBorder(8, 8, 4, 8));
        titleSection.add(titleLabel);

        JPanel topSection = new JPanel(new BorderLayout());
        topSection.setBackground(ColorScheme.DARK_GRAY_COLOR);
        topSection.add(headerBar, BorderLayout.NORTH);
        topSection.add(titleSection, BorderLayout.CENTER);

        JTextPane contentPane = new JTextPane()
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

        for (ChangelogService.ChangelogEntry entry : changelogEntries) {
            JButton changelogEntryButton = new JButton("Version " + entry.getVersion());
            changelogEntryButton.setBackground(ColorScheme.DARK_GRAY_COLOR);
            changelogEntryButton.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
            changelogEntryButton.addActionListener( e -> onChangelogEntryClicked(entry));

            // Explicitly clamp the size so the HTML layout engine respects it
            changelogEntryButton.setAlignmentX(Component.CENTER_ALIGNMENT);
            changelogEntryButton.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 30));

            // Force insert directly into the text layout flow
            contentPane.setCaretPosition(contentPane.getDocument().getLength());
            contentPane.insertComponent(changelogEntryButton);

            // Insert a native HTML line break so the buttons stack vertically
            try {
                contentPane.getDocument().insertString(contentPane.getDocument().getLength(), "\n", null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        JScrollPane scrollPane = new JScrollPane(contentPane);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(ColorScheme.DARK_GRAY_COLOR);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        add(topSection, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }


    private void onChangelogEntryClicked(ChangelogEntry entry)
    {
        Boolean isNewestEntry = changelogService.isNewestEntry(entry);

        panelService.showChangelogPanel(entry, isNewestEntry, true);
    }

    private void onBackButtonClicked(){
        panelService.showHome();
    }
}