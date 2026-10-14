package com.darkforge317.goaltracker.ui.inputs;

import com.darkforge317.goaltracker.models.Goal;
import com.darkforge317.goaltracker.models.task.QuestPointsTask;
import org.junit.jupiter.api.Test;

import javax.swing.JFormattedTextField;
import javax.swing.JSpinner;
import javax.swing.SwingUtilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class QuestPointsTaskInputTest
{
    @Test
    void submitCommitsTypedTargetWithoutWaitingForFocusToChange() throws Exception
    {
        QuestPointsTaskInput[] input = new QuestPointsTaskInput[1];
        Goal goal = Goal.builder().build();

        SwingUtilities.invokeAndWait(() -> {
            input[0] = new QuestPointsTaskInput(null, goal);
            JSpinner spinner = input[0].getTargetSpinner();
            JFormattedTextField field = ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField();

            assertEquals(QuestPointsTask.MAX_QUEST_POINTS, spinner.getValue());
            field.setText("15.5");
            assertNotEquals("15.5", field.getText(), "The editor should reject decimal text while typing.");
            field.setText("217");
            input[0].submit();
        });

        assertEquals(1, goal.getTasks().size());
        assertEquals(217, ((QuestPointsTask) goal.getTasks().get(0)).getTargetQuestPoints());
    }
}
