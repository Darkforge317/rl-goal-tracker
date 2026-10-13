package com.darkforge317.goaltracker.ui.inputs;

import com.darkforge317.goaltracker.models.Goal;
import org.junit.jupiter.api.Test;

import javax.swing.SpinnerNumberModel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CombatLevelTaskInputTest
{
    @Test
    void creationControlIsBoundedFromThreeThrough126()
    {
        CombatLevelTaskInput input = new CombatLevelTaskInput(null, Goal.builder().build());
        SpinnerNumberModel model = (SpinnerNumberModel) input.getTargetSpinner().getModel();

        assertEquals(126.0, input.getTargetSpinner().getValue());
        assertEquals(3.0, model.getMinimum());
        assertEquals(126.0, model.getMaximum());
        assertEquals(0.25, model.getStepSize());
        input.getTargetSpinner().setValue(85.25);
        assertEquals(85.25, input.getTargetSpinner().getValue());
        assertThrows(IllegalArgumentException.class, () -> input.getTargetSpinner().setValue(85.257));
    }
}
