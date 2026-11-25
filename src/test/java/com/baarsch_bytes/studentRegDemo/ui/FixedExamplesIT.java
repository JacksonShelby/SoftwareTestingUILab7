package com.baarsch_bytes.studentRegDemo.ui;

import com.baarsch_bytes.studentRegDemo.ui.pages.StudentListPage;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.junit.jupiter.api.Tag;

import static org.junit.jupiter.api.Assertions.*;

class FixedExamplesIT extends UiTestBase {

    private long createStudent(StudentListPage page, String name, String major, String gpa) {
        page.open();
        page.submitNewStudent(name, major, gpa);
        return page.waitForStudentNamed(name);
    }

    @Test
    void createStudent_showsAllThreeFields() {
        StudentListPage page = new StudentListPage(driver, wait, BASE_URL);
        String name = unique("Grace Hopper");
        long id = createStudent(page, name, "Computer Science", "3.8");
        assertEquals(name, page.nameOf(id));
        assertEquals("Computer Science", page.majorOf(id));
        assertEquals("3.8", page.gpaOf(id));
    }

    @Test
    void editStudent_majorPersistsAfterReload() {
        StudentListPage page = new StudentListPage(driver, wait, BASE_URL);
        String name = unique("Edit Me");
        long id = createStudent(page, name, "Physics", "3.0");
        page.clickEdit(id);
        page.setEditMajor("Mathematics");
        page.saveEdit();
        page.open();                        // reload: proves it was saved
        page.waitForStudentNamed(name);     // wait for the rows to load
        assertEquals("Mathematics", page.majorOf(id));
    }

    @Test
    void cancelEdit_changesNothing() {
        StudentListPage page = new StudentListPage(driver, wait, BASE_URL);
        String name = unique("Cancel Me");
        long id = createStudent(page, name, "Physics", "3.0");
        page.clickEdit(id);
        page.setEditMajor("Chemistry");
        page.cancelEdit();
        assertEquals("Physics", page.majorOf(id));
        page.open();
        page.waitForStudentNamed(name);
        assertEquals("Physics", page.majorOf(id));
    }

    @Test
    void deleteStudent_removesOnlyThatRow() {
        StudentListPage page = new StudentListPage(driver, wait, BASE_URL);
        long keepId = createStudent(page, unique("Keep"), "Art", "3.1");
        long deleteId = createStudent(page, unique("Delete"), "Art", "3.2");
        page.clickDelete(deleteId);
        page.waitForRowGone(deleteId);
        assertTrue(driver.findElements(By.id("student-row-" + deleteId)).isEmpty());
        assertFalse(driver.findElements(By.id("student-row-" + keepId)).isEmpty());
    }

    @Tag("examples") // Part 1 rewrite of studentTableHasData
    @Test
    void studentTable_showsCreatedStudentData() {
        StudentListPage page = new StudentListPage(driver, wait, BASE_URL);
        String name = unique("Table Check");
        long id = createStudent(page, name, "History", "2.5");
        assertEquals(name, page.nameOf(id));
        assertEquals("History", page.majorOf(id));
        assertEquals("2.5", page.gpaOf(id));
    }
}