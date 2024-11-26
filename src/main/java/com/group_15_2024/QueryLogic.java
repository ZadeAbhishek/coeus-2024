package com.group_15_2024;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class QueryLogic {

    public static class QueryLib {
        private static final Path CURRENT_RELATIVE_PATH = Paths.get("").toAbsolutePath();
        private static final String QUERIES_FILE_PATH = CURRENT_RELATIVE_PATH + "/topics.401-450";

        /**
         * Loads queries from the file and returns a list of QueryObject instances.
         *
         * @return List of QueryObject instances.
         */
        public static List<QueryObject> loadQueriesFromFile() {
            List<QueryObject> queries = new ArrayList<>();
            QueryObject queryObject = null;
            String currentTag = null;

            try (BufferedReader reader = new BufferedReader(new FileReader(QUERIES_FILE_PATH))) {
                String line;

                while ((line = reader.readLine()) != null) {
                    String detectedTag = detectTag(line.trim());

                    if (detectedTag != null) {
                        if (detectedTag.equals(QueryTags.TOP_START.getTag())) {
                            queryObject = new QueryObject();
                        } else if (detectedTag.equals(QueryTags.TOP_END.getTag())) {
                            if (queryObject != null) {
                                queries.add(queryObject);
                            }
                            queryObject = null;
                            currentTag = null;
                        } else {
                            currentTag = detectedTag;
                            populateQueryFields(currentTag, line, queryObject);
                        }
                    } else if (currentTag != null && queryObject != null) {
                        populateQueryFields(currentTag, line, queryObject);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return queries;
        }

        /**
         * Detects if a line starts with a recognized tag.
         *
         * @param line The line to check.
         * @return The tag if detected, otherwise null.
         */
        private static String detectTag(String line) {
            for (QueryTags tag : QueryTags.values()) {
                if (line.startsWith(tag.getTag())) {
                    return tag.getTag();
                }
            }
            return null;
        }

        /**
         * Populates fields of a QueryObject based on the current tag and line content.
         *
         * @param tag         The tag of the current line.
         * @param line        The content of the line.
         * @param queryObject The QueryObject being populated.
         */
        private static void populateQueryFields(String tag, String line, QueryObject queryObject) {
            if (queryObject == null) return;

            String content = line.replaceFirst(Pattern.quote(tag), "").trim();

            switch (tag) {
                case "<num> Number:":
                    queryObject.setQueryNum(content.replaceAll("\\D", ""));
                    break;

                case "<title>":
                    queryObject.appendTitle(content);
                    break;

                case "<desc> Description:":
                    queryObject.appendDescription(content);
                    break;

                case "<narr> Narrative:":
                    queryObject.appendNarrative(content);
                    break;

                default:
                    if (tag != null) {
                        switch (tag) {
                            case "<title>":
                                queryObject.appendTitle(line.trim());
                                break;
                            case "<desc> Description:":
                                queryObject.appendDescription(line.trim());
                                break;
                            case "<narr> Narrative:":
                                queryObject.appendNarrative(line.trim());
                                break;
                        }
                    }
                    break;
            }
        }
    }

    public static class QueryObject {
        private String queryNum;
        private final StringBuilder title;
        private final StringBuilder description;
        private final StringBuilder narrative;

        public QueryObject() {
            this.queryNum = "";
            this.title = new StringBuilder();
            this.description = new StringBuilder();
            this.narrative = new StringBuilder();
        }

        public String getQueryNum() {
            return queryNum;
        }

        public void setQueryNum(String queryNum) {
            this.queryNum = queryNum;
        }

        public String getTitle() {
            return title.toString().trim();
        }

        public void appendTitle(String titlePart) {
            this.title.append(" ").append(titlePart);
        }

        public String getDescription() {
            return description.toString().trim();
        }

        public void appendDescription(String descriptionPart) {
            this.description.append(" ").append(descriptionPart);
        }

        public String getNarrative() {
            return narrative.toString().trim();
        }

        public void appendNarrative(String narrativePart) {
            this.narrative.append(" ").append(narrativePart);
        }
    }

    public enum QueryTags {
        TOP_START("<top>"),
        TOP_END("</top>"),
        QUERY_NUMBER("<num> Number:"),
        QUERY_TITLE("<title>"),
        QUERY_DESCRIPTION("<desc> Description:"),
        QUERY_NARRATIVE("<narr> Narrative:");

        private final String tag;

        QueryTags(String tag) {
            this.tag = tag;
        }

        public String getTag() {
            return this.tag;
        }
    }
}
