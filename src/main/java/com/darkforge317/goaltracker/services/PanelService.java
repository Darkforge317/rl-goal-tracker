package com.darkforge317.goaltracker.services;

import com.darkforge317.goaltracker.GoalTrackerPlugin;
import com.darkforge317.goaltracker.ui.ChangelogListPanel;
import com.darkforge317.goaltracker.ui.ChangelogPanel;
import com.darkforge317.goaltracker.ui.GoalTrackerPanel;
import com.google.inject.Inject;
import net.runelite.client.ui.PluginPanel;

import javax.swing.*;
import java.awt.*;

public class PanelService extends PluginPanel {
    private final GoalTrackerPlugin plugin;

    @Inject
    public PanelService(GoalTrackerPlugin plugin)
    {
        super();

        this.plugin = plugin;
    }

    public void showHome()
    {
        removeAll();
        add(new GoalTrackerPanel(this, plugin, plugin.getGoalManager()), BorderLayout.CENTER);
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
}
