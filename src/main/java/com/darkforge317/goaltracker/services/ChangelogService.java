package com.darkforge317.goaltracker.services;

import lombok.Getter;

import javax.inject.Singleton;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads changelog data bundled as plugin resources under /changelogs/. Plugin Hub jars
 * are not unpacked on disk, so this never lists a directory - it reads a small manifest
 * (versions.txt) that declares which versions exist, in order (oldest first), then loads
 * each version's own markdown file from /changelogs/logs/ by its exact known name (dots
 * converted to hyphens, e.g. "2.1.1" -> logs/v2-1-1.md).
 */
@Singleton
public final class ChangelogService
{
    private static final String CHANGELOG_ROOT = "/changelogs/";
    private static final String LOGS_SUBDIR = CHANGELOG_ROOT + "logs/";
    private static final String VERSIONS_MANIFEST = CHANGELOG_ROOT + "versions.txt";

    @Getter
    public static final class ChangelogEntry
    {
        private final String version;
        private final String markdown;

        public ChangelogEntry(String version, String markdown)
        {
            this.version = version;
            this.markdown = markdown;
        }

    }

    /**
     * All changelog entries, newest first, built from versions.txt (oldest-first in the
     * file) plus each version's own logs/v{version}.md content. A version listed in the
     * manifest whose .md file is missing/unreadable is silently skipped rather than
     * producing a broken entry.
     */
    public List<ChangelogEntry> getAllEntries()
    {
        List<ChangelogEntry> entries = new ArrayList<>();
        List<String> versions = getAllVersions(); // oldest first, as stored
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

    /** The most recent version declared in versions.txt (its last line), or null if none. */
    public ChangelogEntry getLatestEntry()
    {
        List<ChangelogEntry> entries = getAllEntries();
        return entries.isEmpty() ? null : entries.get(0);
    }

    /**
     * Raw version strings (e.g. "2.1.1"), oldest first, as declared in versions.txt.
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
}