package com.darkforge317.goaltracker.services;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads changelog data bundled as plugin resources under /changelogs/. Plugin Hub jars
 * are not unpacked on disk, so this never lists a directory - it reads a small manifest
 * (versions.txt) that declares which versions exist, one per line in any order (versions
 * are compared numerically, not by file position - see README-changelogs.md for the
 * recommended human-readable ordering convention), then loads each version's own
 * markdown file from /changelogs/logs/ by its exact known name (dots converted to
 * hyphens, e.g. "2.1.1" -> logs/v2-1-1.md).
 */
public final class ChangelogService
{
    private static final String CHANGELOG_ROOT = "/changelogs/";
    private static final String LOGS_SUBDIR = CHANGELOG_ROOT + "logs/";
    private static final String VERSIONS_MANIFEST = CHANGELOG_ROOT + "versions.txt";

    public static final class ChangelogEntry
    {
        private final String version;
        private final String markdown;

        public ChangelogEntry(String version, String markdown)
        {
            this.version = version;
            this.markdown = markdown;
        }

        public String getVersion() { return version; }
        public String getMarkdown() { return markdown; }
    }

    /**
     * All changelog entries, newest first, regardless of what order versions.txt lists
     * them in - versions are compared numerically. A version listed in the manifest
     * whose .md file is missing/unreadable is silently skipped rather than producing
     * a broken entry.
     */
    public List<ChangelogEntry> getAllEntries()
    {
        List<ChangelogEntry> entries = new ArrayList<>();
        List<String> versions = new ArrayList<>(getAllVersions());
        versions.sort(ChangelogService::compareVersions); // ascending: oldest first

        for (int i = versions.size() - 1; i >= 0; i--) // newest first for display
        {
            String version = versions.get(i);
            String markdown = getChangelogMarkdown(version);
            if (markdown != null)
            {
                entries.add(new ChangelogEntry(version, markdown));
            }
        }
        return entries;
    }

    /** The numerically highest version among those declared in versions.txt, or null if none. */
    public ChangelogEntry getLatestEntry()
    {
        List<ChangelogEntry> entries = getAllEntries();
        return entries.isEmpty() ? null : entries.get(0);
    }

    /**
     * Raw version strings (e.g. "2.1.1"), in whatever order versions.txt lists them.
     * Returns an empty list if the manifest is missing or unreadable.
     */
    private List<String> getAllVersions()
    {
        List<String> versions = new ArrayList<>();
        try (InputStream in = ChangelogService.class.getResourceAsStream(VERSIONS_MANIFEST))
        {
            if (in == null) return versions;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)))
            {
                String line;
                while ((line = reader.readLine()) != null)
                {
                    line = line.trim();
                    if (!line.isEmpty()) versions.add(line);
                }
            }
        }
        catch (IOException e)
        {
            // Leave versions empty; callers treat this the same as "no changelogs bundled"
        }
        return versions;
    }

    /**
     * Raw markdown content for a given version (e.g. "2.1.1" -> /changelogs/logs/v2-1-1.md),
     * or null if the resource is missing or unreadable.
     */
    private String getChangelogMarkdown(String version)
    {
        String fileName = "v" + version.replace('.', '-') + ".md";
        String path = LOGS_SUBDIR + fileName;
        try (InputStream in = ChangelogService.class.getResourceAsStream(path))
        {
            if (in == null) return null;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)))
            {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null)
                {
                    sb.append(line).append('\n');
                }
                return sb.toString();
            }
        }
        catch (IOException e)
        {
            return null;
        }
    }

    /**
     * Numerically compares two dotted version strings component by component
     * (e.g. "2.1.10" > "2.1.9", unlike a plain string comparison). A missing or
     * non-numeric component is treated as 0.
     */
    private static int compareVersions(String a, String b)
    {
        String[] partsA = a.split("\\.");
        String[] partsB = b.split("\\.");
        int len = Math.max(partsA.length, partsB.length);
        for (int i = 0; i < len; i++)
        {
            int numA = i < partsA.length ? parseIntSafe(partsA[i]) : 0;
            int numB = i < partsB.length ? parseIntSafe(partsB[i]) : 0;
            if (numA != numB) return Integer.compare(numA, numB);
        }
        return 0;
    }

    private static int parseIntSafe(String s)
    {
        try { return Integer.parseInt(s.trim()); }
        catch (NumberFormatException e) { return 0; }
    }
}