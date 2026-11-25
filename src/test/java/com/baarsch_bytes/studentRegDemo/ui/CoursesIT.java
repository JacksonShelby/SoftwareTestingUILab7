package com.baarsch_bytes.studentRegDemo.ui;

import com.baarsch_bytes.studentRegDemo.ui.pages.CoursesPage;
import com.baarsch_bytes.studentRegDemo.ui.pages.StudentListPage;
import com.baarsch_bytes.studentRegDemo.ui.UserMessages;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class CoursesIT extends UiTestBase {

    private static final Duration SETTLE = Duration.ofSeconds(5);

    private long createCourse(CoursesPage page, String name) {
        return createCourse(page, name, "40");
    }

    private long createCourse(CoursesPage page, String name, String maxSize) {
        page.open();
        page.submitNewCourse(name, "5", maxSize, "338");
        return page.waitForCourseNamed(name);
    }

    private long createStudent(String name) {
        StudentListPage students = new StudentListPage(driver, wait, BASE_URL);
        students.open();
        students.submitNewStudent(name, "CS", "3.5");
        return students.waitForStudentNamed(name);
    }

    @Test // TC5, TC6
    void createCourse_showsAllFieldsAfterReload() {
        CoursesPage page = new CoursesPage(driver, wait, BASE_URL);
        String name = unique("Course");
        long id = createCourse(page, name);
        page.open();
        page.waitForCourseNamed(name);
        assertEquals(name, page.nameOf(id));
        assertEquals("5", page.instructorOf(id));
        assertEquals("40", page.maxSizeOf(id));
        assertEquals("338", page.roomOf(id));
        assertEquals("0", page.enrolledCountOf(id));
    }

    @Test // TC7
    void editCourse_changesPersistAfterReload() {
        CoursesPage page = new CoursesPage(driver, wait, BASE_URL);
        long id = createCourse(page, unique("Course"));
        String newName = unique("Renamed");
        page.clickEdit(id);
        page.setEditFields(newName, "7", "30", "340");
        page.saveEdit();
        page.open();
        page.waitForCourseNamed(newName);
        assertEquals("7", page.instructorOf(id));
        assertEquals("30", page.maxSizeOf(id));
        assertEquals("340", page.roomOf(id));
    }

    @Test // TC8
    void deleteCourse_removesOnlyThatRow() {
        CoursesPage page = new CoursesPage(driver, wait, BASE_URL);
        long keepId = createCourse(page, unique("Keep"));
        long deleteId = createCourse(page, unique("Delete"));
        page.clickDelete(deleteId);
        page.waitForRowGone(deleteId);
        assertFalse(driver.findElements(By.id("course-row-" + keepId)).isEmpty());
    }

    @Test // TC9
    void addStudent_updatesCountAndRoster() {
        String studentName = unique("Enrollee");
        long studentId = createStudent(studentName);
        CoursesPage page = new CoursesPage(driver, wait, BASE_URL);
        long courseId = createCourse(page, unique("Course"));
        page.addStudent(courseId, studentId);
        page.waitForEnrolledCount(courseId, "1");
        assertTrue(page.rosterOf(courseId).contains(studentName));
    }

    @Test // TC10: removing takes effect immediately, with no Save
    void removeStudent_updatesCountAndRosterAfterReload() {
        String studentName = unique("Leaver");
        long studentId = createStudent(studentName);
        CoursesPage page = new CoursesPage(driver, wait, BASE_URL);
        String courseName = unique("Course");
        long courseId = createCourse(page, courseName);
        page.addStudent(courseId, studentId);
        page.waitForEnrolledCount(courseId, "1");

        page.clickEdit(courseId);
        page.removeStudent(studentId);
        page.waitForEnrolledCount(courseId, "0");

        page.open();
        page.waitForCourseNamed(courseName);
        assertEquals("0", page.enrolledCountOf(courseId));
        assertFalse(page.rosterOf(courseId).contains(studentName));
    }

    // TC17: course with max size 1. Adds one student, then tries a second.
    private long fillCourseThenTrySecond(CoursesPage page, String courseName,
                                         String firstName, String secondName) {
        long first = createStudent(firstName);
        long second = createStudent(secondName);
        long courseId = createCourse(page, courseName, "1");
        page.addStudent(courseId, first);
        page.waitForEnrolledCount(courseId, "1");
        page.addStudent(courseId, second);
        return courseId;
    }

    @Test // TC17
    void fullCourse_secondStudentIsNotEnrolled() {
        CoursesPage page = new CoursesPage(driver, wait, BASE_URL);
        String courseName = unique("Tiny");
        String firstName = unique("First");
        String secondName = unique("Second");
        long courseId = fillCourseThenTrySecond(page, courseName, firstName, secondName);

        assertFalse(page.enrolledCountChangesFromWithin(courseId, "1", SETTLE),
                "the count changed even though the course is full");
        page.open();
        page.waitForCourseNamed(courseName);
        assertEquals("1", page.enrolledCountOf(courseId));
        String roster = page.rosterOf(courseId);
        assertTrue(roster.contains(firstName));
        assertFalse(roster.contains(secondName));
    }

    // =====================  DEFECT CANDIDATES  =====================

    @Tag("defect")
    @Test // TC17: the user must be told
    void fullCourse_showsMessage() {
        CoursesPage page = new CoursesPage(driver, wait, BASE_URL);
        fillCourseThenTrySecond(page, unique("Tiny"), unique("First"), unique("Second"));
        assertTrue(UserMessages.appearsWithin(driver, SETTLE), "no message was shown to the user");
    }

    @Tag("defect")
    @Test // Cancel must not change anything, but removal is already saved (see report question)
    void cancelEdit_afterRemove_keepsStudentEnrolled() {
        String studentName = unique("Stayer");
        long studentId = createStudent(studentName);
        CoursesPage page = new CoursesPage(driver, wait, BASE_URL);
        String courseName = unique("Course");
        long courseId = createCourse(page, courseName);
        page.addStudent(courseId, studentId);
        page.waitForEnrolledCount(courseId, "1");

        page.clickEdit(courseId);
        page.removeStudent(studentId);
        page.cancelEdit();

        page.open();
        page.waitForCourseNamed(courseName);
        assertEquals("1", page.enrolledCountOf(courseId));
    }

    @Test //TC22: a successful add clears the form
    void addCourse_clearsFormAfterSuccess() {
        CoursesPage page = new CoursesPage(driver, wait, BASE_URL);
        createCourse(page, unique("Cleared"));
        assertTrue(page.formClearedWithin(SETTLE),
                "the add-course form still holds the typed values after a successful add");
    }
}