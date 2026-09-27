package com.darkforge317.goaltracker.services;

import com.darkforge317.goaltracker.GoalTrackerPlugin;
import com.darkforge317.goaltracker.ui.ChangelogListPanel;
import com.darkforge317.goaltracker.ui.ChangelogPanel;
import com.darkforge317.goaltracker.ui.GoalTrackerPanel;
import com.darkforge317.goaltracker.ui.Refreshable;
import com.google.inject.Inject;
import net.runelite.client.ui.PluginPanel;

import javax.swing.*;
import java.awt.*;

public class PanelService extends PluginPanel {
    private final GoalTrackerPlugin plugin;
    private boolean goalsChangedListenerRegistered = false;

    // In PanelService.java
    @Inject
    public PanelService(GoalTrackerPlugin plugin)
    {
        super(false); // Explicitly required for custom layouts
        this.plugin = plugin;

        setLayout(new BorderLayout()); // Anchors layout components precisely
        setBorder(null);
    }


    public void showHome()
    {
        removeAll();
        GoalTrackerPanel panel = new GoalTrackerPanel(this, plugin, plugin.getGoalManager());
        panel.onGoalUpdated(plugin::onGoalUpdatedCallback);
        panel.onTaskAdded(plugin::onTaskAddedCallback);
        panel.onTaskUpdated(plugin::onTaskUpdatedCallback);
        add(panel, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    public void showGoalPanel()
    {

    }

    public void showChangelogListPanel()
    {
        removeAll();
        ChangelogListPanel panel = new ChangelogListPanel(this);
        add(panel, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    public void showChangelogPanel(ChangelogService.ChangelogEntry entry, boolean isNewUpdate, boolean createdByChangelogListPanel)
    {
        removeAll();
        ChangelogPanel panel = new ChangelogPanel(this, entry, isNewUpdate, createdByChangelogListPanel);
        add(panel, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    /**
     * Refreshes whatever content is currently displayed, if it's Refreshable
     * Callers don't need to know which panel happens to be showing.
     */
    public void refreshCurrent()
    {
        for (Component component : getComponents())
        {
            if (component instanceof Refreshable)
            {
                ((Refreshable) component).refresh();
            }
        }
    }

    public void registerGoalsChangedListener()
    {
        plugin.getGoalManager().addGoalsChangedListener(() -> SwingUtilities.invokeLater(this::refreshCurrent));
    }
}
