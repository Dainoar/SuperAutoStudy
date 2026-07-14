package com.tihai.manager;

import com.tihai.common.ChapterPoint;
import com.tihai.common.CoursePoint;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoursePointNavigatorTest {

    @Test
    void traversesEveryCoursePointAndFinishesOnlyAfterTheLastChapter() {
        CoursePoint firstGroup = coursePoint("chapter-1");
        CoursePoint secondGroup = coursePoint("chapter-2");

        List<ChapterPoint> chapters = CoursePointNavigator.flatten(Arrays.asList(firstGroup, secondGroup));

        assertEquals(Arrays.asList("chapter-1", "chapter-2"), Arrays.asList(
                chapters.get(0).getId(), chapters.get(1).getId()));
        assertFalse(CoursePointNavigator.isComplete(1, chapters.size()));
        assertTrue(CoursePointNavigator.isComplete(2, chapters.size()));
    }

    private CoursePoint coursePoint(String chapterId) {
        ChapterPoint chapter = new ChapterPoint();
        chapter.setId(chapterId);
        CoursePoint point = new CoursePoint();
        point.setPoints(Arrays.asList(chapter));
        return point;
    }
}
