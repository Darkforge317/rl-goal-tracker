package com.darkforge317.goaltracker.services;

import net.runelite.client.input.KeyListener;

import javax.inject.Singleton;
import javax.swing.SwingUtilities;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Tracks Shift key state within the RuneLite client window via KeyManager
 * (client-scoped, plugin-hub approved) rather than java.awt.KeyboardFocusManager
 * (JVM-wide, disallowed). UI components subscribe to be notified on change.
 */
@Singleton
public class KeyInputService implements KeyListener
{
    private volatile boolean shiftDown = false;
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    public boolean isShiftDown() { return shiftDown; }

    public void addListener(Runnable listener) { listeners.add(listener); }
    public void removeListener(Runnable listener) { listeners.remove(listener); }

    @Override public void keyTyped(KeyEvent e) { }

    @Override
    public void keyPressed(KeyEvent e)
    {
        if (e.getKeyCode() == KeyEvent.VK_SHIFT && !shiftDown)
        {
            shiftDown = true;
            fireChanged();
        }
    }

    @Override
    public void keyReleased(KeyEvent e)
    {
        if (e.getKeyCode() == KeyEvent.VK_SHIFT && shiftDown)
        {
            shiftDown = false;
            fireChanged();
        }
    }

    private void fireChanged()
    {
        SwingUtilities.invokeLater(() -> listeners.forEach(Runnable::run));
    }
}