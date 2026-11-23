package com.baarsch_bytes.studentRegDemo.ui;

import com.baarsch_bytes.studentRegDemo.ui.pages.CoursesPage;
import com.baarsch_bytes.studentRegDemo.ui.pages.StudentListPage;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import static org.junit.jupiter.api.Assertions.*;

class CoursesIT extends UiTestBase {

    private long createCourse(CoursesPage page, String name) {
        page.open();
        page.submitNewCourse(name, "5", "40", "338");
        return page.waitForCourseNamed(name);
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
        StudentListPage students = new StudentListPage(driver, wait, BASE_URL);
        String studentName = unique("Enrollee");
        students.open();
        students.submitNewStudent(studentName, "CS", "3.0");
        long studentId = students.waitForStudentNamed(studentName);

        CoursesPage page = new CoursesPage(driver, wait, BASE_URL);
        long courseId = createCourse(page, unique("Course"));
        page.addStudent(courseId, studentId);
        page.waitForEnrolledCount(courseId, "1");
        assertTrue(page.rosterOf(courseId).contains(studentName));
    }
}