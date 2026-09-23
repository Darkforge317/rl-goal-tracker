package com.darkforge317.goaltracker.utils;

public final class ChangelogMarkdownRenderer
{
    private ChangelogMarkdownRenderer() {}

    public static String toHtml(String markdown)
    {
        StringBuilder html = new StringBuilder(
                "<html><head><style>"
                        + "body { margin: 0; padding: 0; }"
                        + "p { margin: 0 0 8px 0; }"
                        + "h2, h3 { margin: 4px 0; }"
                        + "ul { margin: 0 0 8px 0; padding-left: 16px; }"
                        + "li { margin: 0; }"
                        + "</style></head><body>");
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
        if (bold) result.append("</b>");

        return result.toString();
    }
}