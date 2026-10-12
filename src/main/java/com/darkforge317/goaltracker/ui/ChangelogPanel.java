package com.darkforge317.goaltracker.ui;

import com.darkforge317.goaltracker.GoalTrackerPlugin;
import com.darkforge317.goaltracker.services.ChangelogService;
import com.darkforge317.goaltracker.services.PanelService;
import com.darkforge317.goaltracker.utils.ChangelogMarkdownRenderer;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.LinkBrowser;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.HyperlinkEvent;
import javax.swing.border.EmptyBorder;
import javax.swing.BorderFactory;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.Shape;
import javax.swing.text.Element;
import javax.swing.text.StyleConstants;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import javax.swing.text.html.HTML;
import javax.swing.text.html.ImageView;

public class ChangelogPanel extends JPanel
{
    private final PanelService panelService;
    private final GoalTrackerPlugin plugin;
    private final boolean createdByChangelogListPanel;
    private static final int CONTENT_PADDING = 8;
    private final ChangelogService.ChangelogEntry entry;

    public ChangelogPanel(PanelService panelService, GoalTrackerPlugin plugin, ChangelogService.ChangelogEntry entry, boolean isNewUpdate, boolean createdByChangelogListPanel)
    {
        super(new BorderLayout());
        this.createdByChangelogListPanel = createdByChangelogListPanel;
        this.entry = entry;
        this.panelService = panelService;
        this.plugin = plugin;

        setBackground(ColorScheme.DARK_GRAY_COLOR);

        JPanel headerBar = new JPanel(new GridLayout(1, 2, 4, 0));
        headerBar.setBorder(new EmptyBorder(4, 4, 4, 4));

        // If the ChangelogListPanel opened this changelog
        if(createdByChangelogListPanel){
            // Show a Back and Home button
            JButton backButton = new JButton();
            JButton homeButton = new JButton();

            backButton.setText("Back");
            backButton.setToolTipText("Return to the changelog list");
            backButton.addActionListener(e -> onBackButtonClicked());
            homeButton.setText("Home");
            homeButton.setToolTipText("Return to Goal Tracker home");
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
            closeButton.setToolTipText("Close this changelog and return to Goal Tracker");
            closeButton.addActionListener(e -> onCloseButtonClicked());

            seeAllButton.setText("All Changelogs");
            seeAllButton.setToolTipText("Mark this update as seen and browse all changelog entries");
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
        contentPane.setEditorKit(new ResponsiveImageEditorKit());
        contentPane.setEditable(false);
        contentPane.setFocusable(false);
        contentPane.setOpaque(false);
        contentPane.setBorder(new EmptyBorder(0, CONTENT_PADDING, CONTENT_PADDING, CONTENT_PADDING));
        contentPane.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        HTMLEditorKit htmlKit = (HTMLEditorKit) contentPane.getEditorKit();
        HTMLDocument htmlDocument = (HTMLDocument) htmlKit.createDefaultDocument();
        // A classpath root URL may be null for resources inside the plugin jar. Use a
        // known bundled image as the base so relative Markdown image paths resolve in
        // both exploded development builds and the packaged plugin.
        htmlDocument.setBase(ChangelogPanel.class.getResource("/add_child.png"));
        contentPane.setDocument(htmlDocument);
        contentPane.addHyperlinkListener(event ->
        {
            if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED && event.getURL() != null)
            {
                LinkBrowser.browse(event.getURL().toString());
            }
        });
        contentPane.setText(ChangelogMarkdownRenderer.toHtml(entry.getMarkdown(), entry.getVersion()));

        JScrollPane scrollPane = new JScrollPane(contentPane);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(ColorScheme.DARK_GRAY_COLOR);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        add(topSection, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void onCloseButtonClicked() {
        plugin.getConfig().highestChangelogVersionSeen(entry.getVersion());
        panelService.showHome();
    }

    private void onSeeAllButtonClicked() {
        plugin.getConfig().highestChangelogVersionSeen(entry.getVersion());
        panelService.showChangelogListPanel();
    }

    private void onBackButtonClicked() {
        panelService.showChangelogListPanel();
    }

    private void onHomeButtonClicked() {
        panelService.showHome();
    }

    private static final class ResponsiveImageEditorKit extends HTMLEditorKit
    {
        private final ViewFactory viewFactory = new HTMLFactory()
        {
            @Override
            public View create(Element element)
            {
                Object name = element.getAttributes().getAttribute(StyleConstants.NameAttribute);
                if (name == HTML.Tag.IMG)
                {
                    return new ResponsiveImageView(element);
                }
                return super.create(element);
            }
        };

        @Override
        public ViewFactory getViewFactory()
        {
            return viewFactory;
        }
    }

    /** Scales oversized changelog images to the editor width while preserving aspect ratio. */
    private static final class ResponsiveImageView extends ImageView
    {
        private static final String WIDTH_METADATA_PREFIX = "[goaltracker-width=";
        private float allocatedWidth = -1;

        private ResponsiveImageView(Element element)
        {
            super(element);
        }

        @Override
        public String getAltText()
        {
            String altText = super.getAltText();
            if (altText != null && altText.startsWith(WIDTH_METADATA_PREFIX))
            {
                int end = altText.indexOf(']');
                if (end >= 0) return altText.substring(end + 1);
            }
            return altText;
        }

        @Override
        public float getPreferredSpan(int axis)
        {
            if (axis != View.X_AXIS && axis != View.Y_AXIS)
            {
                return super.getPreferredSpan(axis);
            }

            float imageWidth = super.getPreferredSpan(View.X_AXIS);
            float imageHeight = super.getPreferredSpan(View.Y_AXIS);
            if (imageWidth <= 0 || imageHeight <= 0)
            {
                return super.getPreferredSpan(axis);
            }

            float targetWidth = getRequestedWidth();
            if (targetWidth <= 0) targetWidth = imageWidth;

            Container container = getContainer();
            float availableWidth = 360;
            if (container != null && container.getWidth() > 0)
            {
                availableWidth = container.getWidth() - container.getInsets().left - container.getInsets().right
                        - CONTENT_PADDING;
            }

            targetWidth = Math.min(targetWidth, Math.max(1f, availableWidth));
            if (allocatedWidth > 0)
            {
                targetWidth = Math.min(targetWidth, allocatedWidth);
            }
            float scale = targetWidth / imageWidth;
            return axis == View.X_AXIS ? targetWidth : imageHeight * scale;
        }

        private float getRequestedWidth()
        {
            Object altAttribute = getElement().getAttributes().getAttribute(HTML.Attribute.ALT);
            if (altAttribute == null) return 0;

            String altText = altAttribute.toString();
            if (!altText.startsWith(WIDTH_METADATA_PREFIX)) return 0;
            int end = altText.indexOf(']');
            if (end < 0) return 0;
            try
            {
                return Integer.parseInt(altText.substring(WIDTH_METADATA_PREFIX.length(), end));
            }
            catch (NumberFormatException ignored)
            {
                return 0;
            }
        }

        @Override
        public void paint(Graphics graphics, Shape allocation)
        {
            Image image = getImage();
            if (image == null)
            {
                super.paint(graphics, allocation);
                return;
            }

            Rectangle bounds = allocation.getBounds();
            if (bounds.width > 0 && (allocatedWidth < 0 || Math.abs(allocatedWidth - bounds.width) > 1f))
            {
                allocatedWidth = bounds.width;
                preferenceChanged(null, true, true);
            }
            float preferredWidth = getPreferredSpan(View.X_AXIS);
            float preferredHeight = getPreferredSpan(View.Y_AXIS);
            if (preferredWidth <= 0 || preferredHeight <= 0)
            {
                super.paint(graphics, allocation);
                return;
            }
            float scale = Math.min(1f, Math.min(bounds.width / preferredWidth, bounds.height / preferredHeight));
            int width = Math.round(preferredWidth * scale);
            int height = Math.round(preferredHeight * scale);
            if (width <= 0 || height <= 0)
            {
                super.paint(graphics, allocation);
                return;
            }

            Graphics scaledGraphics = graphics.create(bounds.x, bounds.y, width, height);
            scaledGraphics.drawImage(image, 0, 0, width, height, null);
            scaledGraphics.dispose();
        }
    }
}
