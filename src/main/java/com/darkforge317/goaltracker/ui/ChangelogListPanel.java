package com.darkforge317.goaltracker.ui;

import com.darkforge317.goaltracker.services.ChangelogService;
import net.runelite.client.ui.ColorScheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Consumer;

/**
 * Simple read-only, clickable list of all bundled changelog versions, newest first.
 * Deliberately not built on ListPanel/ListItemPanel - this has no reordering, removal,
 * or context menu needs, so the full reorderable-list machinery would be unnecessary
 * complexity for a plain "click a version to view it" list.
 */
public class ChangelogListPanel extends JPanel
{
    public ChangelogListPanel(List<ChangelogService.ChangelogEntry> entries,
                              Consumer<ChangelogService.ChangelogEntry> onSelect,
                              Runnable onClose)
    {
        super(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        JPanel headerBar = new JPanel(new BorderLayout());
        headerBar.setBorder(new EmptyBorder(4, 4, 4, 4));
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> onClose.run());
        headerBar.add(closeButton, BorderLayout.EAST);

        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

        if (entries.isEmpty())
        {
            JLabel emptyLabel = new JLabel("No changelogs available.");
            emptyLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
            emptyLabel.setBorder(new EmptyBorder(8, 8, 8, 8));
            listPanel.add(emptyLabel);
        }
        else
        {
            for (ChangelogService.ChangelogEntry entry : entries)
            {
                listPanel.add(buildRow(entry, onSelect));
            }
        }

        JScrollPane scrollPane = new JScrollPane(listPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(ColorScheme.DARK_GRAY_COLOR);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        add(headerBar, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    private JComponent buildRow(ChangelogService.ChangelogEntry entry, Consumer<ChangelogService.ChangelogEntry> onSelect)
    {
        JLabel row = new JLabel("V" + entry.getVersion() + " Changes");
        row.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        row.setBorder(new EmptyBorder(6, 8, 6, 8));
        row.setCursor(new Cursor(Cursor.HAND_CURSOR));
        row.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e) { onSelect.accept(entry); }

            @Override
            public void mouseEntered(MouseEvent e) { row.setForeground(Color.WHITE); }

            @Override
            public void mouseExited(MouseEvent e) { row.setForeground(ColorScheme.LIGHT_GRAY_COLOR); }
        });
        return row;
    }
}