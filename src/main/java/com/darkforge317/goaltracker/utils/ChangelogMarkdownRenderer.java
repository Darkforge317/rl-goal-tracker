package com.darkforge317.goaltracker.utils;

/**
 * Converts a deliberately narrow subset of markdown into the HTML 3.2-ish dialect
 * Swing's built-in JLabel/JEditorPane HTML rendering understands. Handles only what
 * changelog entries actually need: '#'/'##' headers, '**bold**', '- ' bullet lists,
 * and plain paragraphs. Not a general-purpose markdown parser - no tables, links,
 * nested lists, code blocks, etc. If richer formatting is ever needed, that's the
 * point to evaluate a real dependency rather than extend this by hand.
 */
public final class ChangelogMarkdownRenderer
{
    private ChangelogMarkdownRenderer() {}

    public static String toHtml(String markdown)
    {
        return toHtml(markdown, -1);
    }

    /**
     * Same as toHtml(String), but constrains the rendered content to widthPx pixels
     * wide (pass -1 for no constraint). Swing's JLabel HTML renderer does not wrap
     * text unless given an explicit pixel width via a body style - without this,
     * content just runs off the edge of the panel instead of wrapping to new lines.
     * margin/padding are explicitly zeroed since the default HTMLEditorKit stylesheet
     * otherwise adds its own body margin, which would eat into the specified width.
     */
    public static String toHtml(String markdown, int widthPx)
    {
        String bodyTag = widthPx > 0
                ? "<body style='width: " + widthPx + "px; margin: 0; padding: 0'>"
                : "<body style='margin: 0; padding: 0'>";
        StringBuilder html = new StringBuilder("<html>").append(bodyTag);
        boolean inList = false;

        for (String rawLine : markdown.split("\n"))
        {
            String line = rawLine.trim();

            if (line.isEmpty())
            {
                if (inList) { html.append("</ul>"); inList = false; }
                continue;
            }

            if (line.startsWith("## "))
            {
                if (inList) { html.append("</ul>"); inList = false; }
                html.append("<h3>").append(inlineFormat(line.substring(3))).append("</h3>");
            }
            else if (line.startsWith("# "))
            {
                if (inList) { html.append("</ul>"); inList = false; }
                html.append("<h2>").append(inlineFormat(line.substring(2))).append("</h2>");
            }
            else if (line.startsWith("- "))
            {
                if (!inList) { html.append("<ul>"); inList = true; }
                html.append("<li>").append(inlineFormat(line.substring(2))).append("</li>");
            }
            else
            {
                if (inList) { html.append("</ul>"); inList = false; }
                html.append("<p>").append(inlineFormat(line)).append("</p>");
            }
        }

        if (inList) html.append("</ul>");
        html.append("</body></html>");
        return html.toString();
    }

    /**
     * Escapes raw HTML special characters, then converts **bold** markers to <b> tags.
     * Escaping happens first so changelog text containing a literal '<' or '&' can't
     * corrupt the generated markup.
     */
    private static String inlineFormat(String text)
    {
        String escaped = text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");

        StringBuilder result = new StringBuilder();
        boolean bold = false;
        int i = 0;
        while (i < escaped.length())
        {
            if (escaped.startsWith("**", i))
            {
                result.append(bold ? "</b>" : "<b>");
                bold = !bold;
                i += 2;
            }
            else
            {
                result.append(escaped.charAt(i));
                i++;
            }
        }
        if (bold) result.append("</b>"); // unterminated ** in source; close gracefully

        return result.toString();
    }
}