package com.group_15_2024;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class QueryLogic {

    // Static inner class QueryLib to handle loading queries from a file
    public static class QueryLib {

        private final static Path currentRelativePath = Paths.get("").toAbsolutePath();
        private static final String absPathToQueries = currentRelativePath + "/topics.401-450";

        public static List<QueryObject> loadQueriesFromFile() {
            List<QueryObject> queries = new ArrayList<>();
            QueryObject queryObject = null;
            String tempTag = null;

            try (BufferedReader bf = new BufferedReader(new FileReader(absPathToQueries))) {
                String queryLine;
                int counter = 0;

                while ((queryLine = bf.readLine()) != null) {
                    String queryLineTag = checkIfDocLineHasTag(queryLine);

                    if (queryLineTag != null) {
                        if (queryLineTag.equals(QueryTags.TOP_START.getTag())) {
                            queryObject = new QueryObject();
                            counter++;
                        } else if (queryLineTag.equals(QueryTags.TOP_END.getTag())) {
                            if (queryObject != null) {
                                queries.add(queryObject);
                            }
                            queryObject = null;
                            tempTag = null; // Reset tempTag
                        } else {
                            tempTag = queryLineTag;
                            // Immediately call populateQueryFields to handle tag and content
                            populateQueryFields(tempTag, queryLine, queryObject);
                        }
                    } else if (tempTag != null && queryObject != null) {
                        populateQueryFields(tempTag, queryLine, queryObject);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return queries;
        }

        private static String checkIfDocLineHasTag(String docLine) {
            docLine = docLine.trim();
            for (QueryTags tag : QueryTags.values()) {
                if (docLine.startsWith(tag.getTag())) {
                    return tag.getTag();
                }
            }
            return null;
        }

        private static void populateQueryFields(String tempTag, String queryLine, QueryObject queryObject) {
            String content = queryLine.replaceFirst(Pattern.quote(tempTag), "").trim();

            switch (tempTag) {
                case "<num> Number:":
                    String queryNum = content.replaceAll("[^\\d]", "");
                    queryObject.setQueryNum(queryNum);
                    break;
                case "<title>":
                    queryObject.setTitle(queryObject.getTitle() + " " + content);
                    break;
                case "<desc> Description:":
                    queryObject.setDescription(queryObject.getDescription() + " " + content);
                    break;
                case "<narr> Narrative:":
                    queryObject.setNarrative(queryObject.getNarrative() + " " + content);
                    break;
                default:
                    if (tempTag != null) {
                        switch (tempTag) {
                            case "<title>":
                                queryObject.setTitle(queryObject.getTitle() + " " + queryLine.trim());
                                break;
                            case "<desc> Description:":
                                queryObject.setDescription(queryObject.getDescription() + " " + queryLine.trim());
                                break;
                            case "<narr> Narrative:":
                                queryObject.setNarrative(queryObject.getNarrative() + " " + queryLine.trim());
                                break;
                        }
                    }
                    break;
            }
        }
    }

    public static class QueryObject {

        private String queryNum;
        private String queryId;
        private String title;
        private String description;
        private String narrative;

        public QueryObject() {
            this.queryNum = "";
            this.queryId = "";
            this.title = "";
            this.narrative = "";
            this.description = "";
        }

        public String getTitle() {
            return title;
        }

        public String getNarrative() {
            return narrative;
        }

        public String getQueryId() {
            return queryId;
        }

        public String getQueryNum() {
            return queryNum;
        }

        public String getDescription() {
            return description;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public void setNarrative(String narrative) {
            this.narrative = narrative;
        }

        public void setQueryId(String queryId) {
            this.queryId = queryId;
        }

        public void setQueryNum(String queryNum) {
            this.queryNum = queryNum;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    // Enum QueryTags to represent tags used in query files
    public enum QueryTags {

        TOP_START("<top>"), TOP_END("</top>"), QUERY_NUMBER("<num> Number:"), QUERY_TITLE("<title>"),
        QUERY_DESCRIPTION("<desc> Description:"), QUERY_NARRATIVE("<narr> Narrative:");

        private final String tag;

        QueryTags(final String tag) {
            this.tag = tag;
        }

        public String getTag() {
            return this.tag;
        }
    }
}