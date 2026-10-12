package com.darkforge317.goaltracker.utils;

import net.runelite.client.ui.ColorScheme;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A small, dependency-free Markdown renderer for the syntax used by changelogs. */
public final class ChangelogMarkdownRenderer
{
    private static final Pattern HEADING = Pattern.compile("^(#{1,6})\\s+(.+?)\\s*#*$");
    private static final Pattern LIST_ITEM = Pattern.compile("^(\\s*)([-+*]|\\d+[.)])\\s+(.+)$");
    private static final Pattern IMAGE_WIDTH = Pattern.compile("^\\s*\\{width=(\\d{1,10})px\\}");

    private ChangelogMarkdownRenderer() {}

    public static String toHtml(String markdown)
    {
        return toHtml(markdown, null);
    }

    public static String toHtml(String markdown, String version)
    {
        String headingColor = String.format("#%06x", ColorScheme.BRAND_ORANGE.getRGB() & 0xFFFFFF);
        StringBuilder html = new StringBuilder(
                "<html><head><style>"
                        + "body { margin: 0; padding: 0; }"
                        + "p { margin: 0 0 8px 0; }"
                        + "h1, h2, h3, h4, h5, h6 { margin: 4px 0; color: " + headingColor + "; }"
                        + "ul, ol { margin: 0 0 8px 0; padding-left: 12px; }"
                        + "li { margin: 0; }"
                        + "blockquote { margin: 4px 0 8px 12px; padding-left: 8px; border-left: 2px solid #888; }"
                        + "hr { border: 0; border-top: 1px solid #666; margin: 8px 0; }"
                        + "code { font-family: monospace; background: #333; }"
                        + "img { margin: 4px 0; }"
                        + "</style></head><body>");

        if (markdown == null || markdown.isEmpty())
        {
            return html.append("</body></html>").toString();
        }

        String[] lines = markdown.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        int i = 0;
        while (i < lines.length)
        {
            String line = lines[i].trim();
            if (line.isEmpty())
            {
                i++;
                continue;
            }

            Matcher heading = HEADING.matcher(line);
            if (heading.matches())
            {
                int level = heading.group(1).length();
                html.append("<h").append(level).append('>')
                        .append(inlineFormat(heading.group(2), version))
                        .append("</h").append(level).append('>');
                i++;
            }
            else if (isHorizontalRule(line))
            {
                html.append("<hr>");
                i++;
            }
            else if (isBlockQuote(line))
            {
                List<String> quote = new ArrayList<>();
                while (i < lines.length && isBlockQuote(lines[i].trim()))
                {
                    String quoteLine = lines[i].trim().replaceFirst("^> ?", "");
                    quote.add(quoteLine);
                    i++;
                }
                html.append("<blockquote>").append(renderBlocks(quote, version)).append("</blockquote>");
            }
            else if (isListItem(line))
            {
                i = renderList(lines, i, html, version, 1);
            }
            else
            {
                List<String> paragraph = new ArrayList<>();
                while (i < lines.length)
                {
                    String paragraphLine = lines[i].trim();
                    if (paragraphLine.isEmpty() || startsBlock(lines, i)) break;
                    paragraph.add(paragraphLine);
                    i++;
                }
                html.append("<p>").append(inlineFormat(String.join(" ", paragraph), version)).append("</p>");
            }
        }
        return html.append("</body></html>").toString();
    }

    private static String renderBlocks(List<String> lines, String version)
    {
        return toHtml(String.join("\n", lines), version).replaceFirst("(?s)^.*?<body>", "").replaceFirst("</body></html>$", "");
    }

    private static boolean startsBlock(String[] lines, int index)
    {
        String line = lines[index].trim();
        return line.isEmpty() || HEADING.matcher(line).matches() || isHorizontalRule(line)
                || isBlockQuote(line) || isListItem(line);
    }

    private static boolean isHorizontalRule(String line)
    {
        return line.matches("(?:\\*\\s*){3,}|(?:-\\s*){3,}|(?:_\\s*){3,}");
    }

    private static boolean isBlockQuote(String line)
    {
        return line.startsWith(">");
    }

    private static boolean isListItem(String line)
    {
        return LIST_ITEM.matcher(line).matches();
    }

    private static int indentation(String line)
    {
        int width = 0;
        while (width < line.length() && Character.isWhitespace(line.charAt(width)))
        {
            width += line.charAt(width) == '\t' ? 4 : 1;
        }
        return width;
    }

    private static int renderList(String[] lines, int start, StringBuilder html, String version, int depth)
    {
        Matcher first = LIST_ITEM.matcher(lines[start]);
        first.matches();
        int baseIndent = indentation(first.group(1));
        if (depth > 2)
        {
            return renderFlattenedList(lines, start, baseIndent, html, version);
        }

        boolean ordered = isOrderedMarker(first.group(2));
        String tag = ordered ? "ol" : "ul";
        html.append('<').append(tag).append('>');

        int i = start;
        while (i < lines.length)
        {
            Matcher item = LIST_ITEM.matcher(lines[i]);
            if (!item.matches() || indentation(item.group(1)) != baseIndent
                    || isOrderedMarker(item.group(2)) != ordered)
            {
                break;
            }

            html.append("<li>").append(inlineFormat(item.group(3), version));
            i++;
            if (i < lines.length && isListItem(lines[i].trim()) && indentation(lines[i]) > baseIndent)
            {
                i = renderList(lines, i, html, version, depth + 1);
            }
            html.append("</li>");
            if (i >= lines.length || !isListItem(lines[i].trim())) break;
            Matcher next = LIST_ITEM.matcher(lines[i]);
            next.matches();
            if (indentation(next.group(1)) < baseIndent) break;
            if (indentation(next.group(1)) == baseIndent && isOrderedMarker(next.group(2)) != ordered) break;
            if (indentation(next.group(1)) > baseIndent) break;
        }

        html.append("</").append(tag).append('>');
        return i;
    }

    /** Keeps very deep nesting readable without letting indentation consume the panel. */
    private static int renderFlattenedList(String[] lines, int start, int baseIndent, StringBuilder html, String version)
    {
        int i = start;
        while (i < lines.length && isListItem(lines[i].trim()) && indentation(lines[i]) >= baseIndent)
        {
            Matcher item = LIST_ITEM.matcher(lines[i]);
            item.matches();
            html.append("<br><span>&#8627; </span>").append(inlineFormat(item.group(3), version));
            i++;
        }
        return i;
    }

    private static boolean isOrderedMarker(String marker)
    {
        return Character.isDigit(marker.charAt(0));
    }

    private static String inlineFormat(String text, String version)
    {
        StringBuilder result = new StringBuilder();
        int i = 0;
        while (i < text.length())
        {
            if (text.charAt(i) == '\\' && i + 1 < text.length())
            {
                result.append(escapeHtml(text.substring(i + 1, i + 2)));
                i += 2;
                continue;
            }

            int tokenEnd = appendInlineToken(text, i, result, version);
            if (tokenEnd > i)
            {
                i = tokenEnd;
                continue;
            }

            result.append(escapeHtml(text.substring(i, i + 1)));
            i++;
        }
        return result.toString();
    }

    private static int appendInlineToken(String text, int start, StringBuilder result, String version)
    {
        boolean image = text.startsWith("![", start);
        int labelStart = image ? start + 2 : text.startsWith("[", start) ? start + 1 : -1;
        if (labelStart >= 0)
        {
            int labelEnd = text.indexOf("](", labelStart);
            int urlEnd = labelEnd < 0 ? -1 : text.indexOf(')', labelEnd + 2);
            if (labelEnd >= 0 && urlEnd >= 0)
            {
                String label = text.substring(labelStart, labelEnd);
                String url = text.substring(labelEnd + 2, urlEnd).trim();
                int tokenEnd = urlEnd + 1;
                int requestedImageWidth = 0;
                if (image)
                {
                    Matcher widthMatcher = IMAGE_WIDTH.matcher(text.substring(tokenEnd));
                    if (widthMatcher.find())
                    {
                        try
                        {
                            requestedImageWidth = (int) Math.min(100000L, Long.parseLong(widthMatcher.group(1)));
                            tokenEnd += widthMatcher.end();
                        }
                        catch (NumberFormatException ignored)
                        {
                            // Leave an invalid width annotation as ordinary text.
                        }
                    }
                }
                if (image)
                {
                    String imagePath = getBundledImagePath(url, version);
                    if (imagePath != null)
                    {
                        java.net.URL imageResource = ChangelogMarkdownRenderer.class.getResource("/" + imagePath);
                        if (imageResource != null)
                        {
                            String altText = requestedImageWidth > 0
                                    ? "[goaltracker-width=" + requestedImageWidth + "]" + label
                                    : label;
                            result.append("<img src=\"").append(escapeHtml(imageResource.toExternalForm()))
                                    .append("\" alt=\"").append(escapeHtml(altText)).append("\">");
                        }
                        else
                        {
                            result.append("[Image: ").append(escapeHtml(label)).append(']');
                        }
                    }
                    else
                    {
                        result.append("[Image: ").append(escapeHtml(label)).append(']');
                    }
                }
                else if (isSafeLink(url))
                {
                    result.append("<a href=\"").append(escapeHtml(url)).append("\">")
                            .append(inlineFormat(label, version)).append("</a>");
                }
                else
                {
                    result.append(inlineFormat(label, version)).append(" (").append(escapeHtml(url)).append(')');
                }
                return tokenEnd;
            }
        }

        if (text.charAt(start) == '`')
        {
            int end = text.indexOf('`', start + 1);
            if (end > start + 1)
            {
                result.append("<code>").append(escapeHtml(text.substring(start + 1, end))).append("</code>");
                return end + 1;
            }
        }

        String[][] delimiters = {{"***", "<strong><em>", "</em></strong>"},
                {"___", "<strong><em>", "</em></strong>"},
                {"**", "<strong>", "</strong>"}, {"__", "<strong>", "</strong>"},
                {"*", "<em>", "</em>"}, {"_", "<em>", "</em>"}};
        for (String[] delimiter : delimiters)
        {
            String token = delimiter[0];
            if (!text.startsWith(token, start)) continue;
            int end = text.indexOf(token, start + token.length());
            if (end > start + token.length())
            {
                result.append(delimiter[1]).append(inlineFormat(text.substring(start + token.length(), end), version))
                        .append(delimiter[2]);
                return end + token.length();
            }
        }
        return start;
    }

    private static boolean isSafeLink(String url)
    {
        String lower = url.toLowerCase();
        return lower.startsWith("https://") || lower.startsWith("http://") || lower.startsWith("mailto:");
    }

    private static String getBundledImagePath(String path, String version)
    {
        if (version == null || !version.matches("[0-9]+(?:\\.[0-9]+)*")) return null;
        String normalized = path.startsWith("/") ? path.substring(1) : path;
        String versionDirectory = "changelogs/images/V" + version.replace('.', '-') + "/";
        if (!normalized.startsWith(versionDirectory) || normalized.contains("..")
                || !normalized.matches("[A-Za-z0-9_./-]+\\.(?:png|gif|jpe?g)"))
        {
            return null;
        }
        return normalized;
    }

    private static String escapeHtml(String text)
    {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
