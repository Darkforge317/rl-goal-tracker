package com.darkforge317.goaltracker.services;

import com.darkforge317.goaltracker.GoalTrackerPlugin;
import com.darkforge317.goaltracker.ui.ChangelogListPanel;
import com.darkforge317.goaltracker.ui.ChangelogPanel;

import javax.swing.*;
import java.awt.*;

public class PanelService extends JPanel{
    private final GoalTrackerPlugin plugin;

    public PanelService(GoalTrackerPlugin plugin)
    {
        super(new BorderLayout());

        this.plugin = plugin;
    }

    public void showHome()
    {

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
        ChangelogPanel panel = new ChangelogPanel(this, entry, isNewUpdate, true);
        add(panel, BorderLayout.CENTER);
        revalidate();
        repaint();
    }
}
