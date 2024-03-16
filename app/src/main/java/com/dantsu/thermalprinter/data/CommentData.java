package com.dantsu.thermalprinter.data;

import java.util.ArrayList;
import java.util.List;

public class CommentData {
    private static List<Comment> comments = new ArrayList<>();

    // Function to get a comment by ID
    public static Comment getCommentById(String id) {
        for (Comment comment : comments) {
            if (comment.uniqueId.equals(id)) {
                return comment;
            }
        }
        return null;
    }

}
