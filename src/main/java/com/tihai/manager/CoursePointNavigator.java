package com.tihai.manager;

import com.tihai.common.ChapterPoint;
import com.tihai.common.CoursePoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Normalizes the upstream course-point response into one ordered chapter list.
 */
public final class CoursePointNavigator {

    private CoursePointNavigator() {
    }

    public static List<ChapterPoint> flatten(List<CoursePoint> coursePoints) {
        if (coursePoints == null || coursePoints.isEmpty()) {
            return Collections.emptyList();
        }

        List<ChapterPoint> chapters = new ArrayList<>();
        for (CoursePoint coursePoint : coursePoints) {
            if (coursePoint != null && coursePoint.getPoints() != null) {
                chapters.addAll(coursePoint.getPoints());
            }
        }
        return chapters;
    }

    public static boolean isComplete(int nextChapterIndex, int chapterCount) {
        return nextChapterIndex >= chapterCount;
    }
}
