package com.toofifty.goaltracker.ui.inputs;

import com.toofifty.goaltracker.GoalTrackerPlugin;
import com.toofifty.goaltracker.models.Goal;
import com.toofifty.goaltracker.models.task.KillCountTask;
import com.toofifty.goaltracker.ui.SimpleDocumentListener;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.components.FlatTextField;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.regex.Pattern;

/**
 * Input panel for creating Kill Count tasks.
 * The player enters the NPC name, NPC ID, and the target kill count.
 *
 * NPC ID lookup tip: search the NPC on the OSRS wiki and grab the ID from the
 * infobox, or use a NPC ID finder plugin in RuneLite.
 */
public final class KillCountTaskInput extends TaskInput
{
    private final FlatTextField npcNameField = new FlatTextField();
    private final FlatTextField npcIdField   = new FlatTextField();
    private final FlatTextField targetField  = new FlatTextField();

    private String npcNameValue  = "";
    private String npcIdValue    = "";
    private String targetValue   = "100";

    private final Pattern numberPattern = Pattern.compile("^(?:\\d+)?$");

    public KillCountTaskInput(GoalTrackerPlugin plugin, Goal goal)
    {
        super(plugin, goal, "Kill count");

        // NPC name field (left, takes remaining space)
        npcNameField.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        npcNameField.setBorder(new EmptyBorder(0, 8, 0, 4));
        npcNameField.getDocument().addDocumentListener(
                (SimpleDocumentListener) e -> npcNameValue = npcNameField.getText());

        // NPC ID field (centre, fixed width)
        npcIdField.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        npcIdField.setBorder(new EmptyBorder(0, 4, 0, 4));
        npcIdField.getTextField().setHorizontalAlignment(SwingConstants.CENTER);
        npcIdField.setPreferredSize(new Dimension(60, PREFERRED_INPUT_HEIGHT));
        npcIdField.getDocument().addDocumentListener(
                (SimpleDocumentListener) e -> SwingUtilities.invokeLater(() -> {
                    String value = npcIdField.getText();
                    if (!numberPattern.matcher(value).find()) {
                        npcIdField.setText(npcIdValue);
                        return;
                    }
                    npcIdValue = value;
                }));

        // Target kills field (right, fixed width)
        targetField.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        targetField.setBorder(new EmptyBorder(0, 4, 0, 8));
        targetField.getTextField().setHorizontalAlignment(SwingConstants.RIGHT);
        targetField.setText(targetValue);
        targetField.setPreferredSize(new Dimension(56, PREFERRED_INPUT_HEIGHT));
        targetField.getDocument().addDocumentListener(
                (SimpleDocumentListener) e -> SwingUtilities.invokeLater(() -> {
                    String value = targetField.getText();
                    if (!numberPattern.matcher(value).find()) {
                        targetField.setText(targetValue);
                        return;
                    }
                    targetValue = value;
                }));

        // Layout: [npcName (flex)] [npcId (60px)] [target (56px)]
        JPanel fields = new JPanel(new BorderLayout(4, 0));
        fields.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        fields.add(npcNameField, BorderLayout.CENTER);

        JPanel rightFields = new JPanel(new BorderLayout(4, 0));
        rightFields.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        rightFields.add(npcIdField, BorderLayout.WEST);
        rightFields.add(targetField, BorderLayout.CENTER);
        fields.add(rightFields, BorderLayout.EAST);

        getInputRow().add(fields, BorderLayout.CENTER);

        // Placeholder-style hints via tooltip
        npcNameField.getTextField().setToolTipText("NPC name (e.g. Ogress Warrior)");
        npcIdField.getTextField().setToolTipText("NPC ID");
        targetField.getTextField().setToolTipText("Target kills");
    }

    @Override
    protected void submit()
    {
        if (npcNameValue.isBlank() || npcIdValue.isBlank() || targetValue.isBlank())
        {
            return;
        }

        int npcId;
        int target;
        try
        {
            npcId  = Integer.parseInt(npcIdValue);
            target = Integer.parseInt(targetValue);
        }
        catch (NumberFormatException e)
        {
            return;
        }

        if (npcId <= 0 || target <= 0)
        {
            return;
        }

        addTask(KillCountTask.builder()
                .npcId(npcId)
                .npcName(npcNameValue.trim())
                .targetKills(target)
                .build());
    }

    @Override
    protected void reset()
    {
        npcNameValue = "";
        npcIdValue   = "";
        targetValue  = "100";
        npcNameField.setText("");
        npcIdField.setText("");
        targetField.setText(targetValue);
    }
}