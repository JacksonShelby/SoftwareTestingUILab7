package com.baarsch_bytes.studentRegDemo.ui;

import com.baarsch_bytes.studentRegDemo.ui.pages.StudentListPage;
import com.baarsch_bytes.studentRegDemo.ui.UserMessages;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class StudentsIT extends UiTestBase {

    //Longer than the app's worst-case response time
    private static final Duration SETTLE = Duration.ofSeconds(5);

    private StudentListPage page() {
        return new StudentListPage(driver, wait, BASE_URL);
    }

    private long createStudent(StudentListPage page, String name, String major, String gpa) {
        page.open();
        page.submitNewStudent(name, major, gpa);
        return page.waitForStudentNamed(name);
    }

    //Pads a unique base name with 'x' up to the wanted length
    private static String padTo(String base, int length) {
        return length <= base.length() ? base : base + "x".repeat(length - base.length());
    }

    // TC1, TC2
    @Test
    void createStudent_showsAllFieldsAfterReload() {
        StudentListPage page = page();
        String name = unique("Create");
        long id = createStudent(page, name, "CS", "3.5");
        assertEquals("CS", page.majorOf(id));
        assertEquals("3.5", page.gpaOf(id));

        page.open();
        assertEquals(id, page.waitForStudentNamed(name));
        assertEquals(name, page.nameOf(id));
        assertEquals("CS", page.majorOf(id));
        assertEquals("3.5", page.gpaOf(id));
    }

    //TC3
    @Test
    void editStudent_majorAndGpaPersistAfterReload() {
        StudentListPage page = page();
        String name = unique("Edit");
        long id = createStudent(page, name, "CS", "3.5");
        page.clickEdit(id);
        page.setEditMajor("Math");
        page.setEditGpa("3.9");
        page.saveEdit();
        page.waitForMajor(id, "Math");
        page.waitForGpa(id, "3.9");

        page.open();
        page.waitForStudentNamed(name);
        assertEquals("Math", page.majorOf(id));
        assertEquals("3.9", page.gpaOf(id));
        assertEquals(name, page.nameOf(id));
    }

    //TC4
    @Test
    void deleteStudent_removesOnlyThatRowAfterReload() {
        StudentListPage page = page();
        String keepName = unique("Keep");
        String deleteName = unique("Delete");
        createStudent(page, keepName, "Art", "3.1");
        long deleteId = createStudent(page, deleteName, "Art", "3.2");
        page.clickDelete(deleteId);
        page.waitForRowGone(deleteId);

        page.open();
        page.waitForStudentNamed(keepName);
        assertFalse(page.hasRowContaining(deleteName));
    }

    //TC11, TC12, TC15
    private void assertGpaAccepted(String typed) {
        StudentListPage page = page();
        long id = createStudent(page, unique("Gpa"), "CS", typed);
        assertEquals(Double.parseDouble(typed), Double.parseDouble(page.gpaOf(id)), 0.0001);
    }

    @Test
    void gpaZero_isAccepted() {
        assertGpaAccepted("0");
    }

    @Test
    void gpaFour_isAccepted() {
        assertGpaAccepted("4.0");
    }

    @Test
    void name255Characters_isAcceptedInFull() {
        StudentListPage page = page();
        String name = padTo(unique("Long"), 255);
        assertEquals(255, name.length());
        long id = createStudent(page, name, "CS", "3.5");
        assertEquals(255, page.nameOf(id).length());
        assertEquals(name, page.nameOf(id));
    }

    //TC13, TC14, TC16, TC19
    private void assertNotAdded(String base, int nameLength, String gpa) {
        StudentListPage page = page();
        String marker = unique("Marker");
        createStudent(page, marker, "CS", "3.5");

        page.submitNewStudent(padTo(base, nameLength), "CS", gpa);
        assertFalse(page.rowContainingAppearsWithin(base, SETTLE), "invalid student appeared in the table");

        page.open();
        page.waitForStudentNamed(marker);      // wait until rows have loaded
        assertFalse(page.hasRowContaining(base), "invalid student was saved");
    }

    @Test
    void gpaMinusPointOne_isNotAdded() {
        assertNotAdded(unique("NegGpa"), 0, "-0.1");
    }

    @Test
    void gpaFourPointOne_isNotAdded() {
        assertNotAdded(unique("HighGpa"), 0, "4.1");
    }

    @Test
    void gpaFive_isNotAdded() {
        assertNotAdded(unique("Five"), 0, "5");
    }

    @Test
    void name256Characters_isNotAdded() {
        assertNotAdded(unique("TooLong"), 256, "3.5");
    }

    //TC18
    @Test
    void cancelEdit_changesNothing_evenAfterReload() {
        StudentListPage page = page();
        String name = unique("Cancel");
        long id = createStudent(page, name, "CS", "3.5");
        page.clickEdit(id);
        page.setEditName(unique("Other"));
        page.setEditMajor("SWE");
        page.setEditGpa("1.5");
        page.cancelEdit();
        assertEquals(name, page.nameOf(id));
        assertEquals("CS", page.majorOf(id));
        assertEquals("3.5", page.gpaOf(id));

        page.open();
        page.waitForStudentNamed(name);
        assertEquals("CS", page.majorOf(id));
        assertEquals("3.5", page.gpaOf(id));
    }

    //TC21
    @Test
    void editStudent_gpaFive_isNotSaved() {
        StudentListPage page = page();
        String name = unique("EditBad");
        long id = createStudent(page, name, "CS", "2.5");
        String before = page.gpaOf(id);
        page.clickEdit(id);
        page.setEditGpa("5");
        page.clickSave();
        assertFalse(page.gpaChangesFromWithin(id, before, SETTLE), "GPA changed to an invalid value");

        page.open();
        page.waitForStudentNamed(name);
        assertEquals(before, page.gpaOf(id));
    }

    //Defects
    private void assertMessageAfterBadAdd(String base, int nameLength, String gpa) {
        StudentListPage page = page();
        page.open();
        page.submitNewStudent(padTo(base, nameLength), "CS", gpa);
        assertTrue(UserMessages.appearsWithin(driver, SETTLE), "no message was shown to the user");
    }

    @Tag("defect")
    @Test
    void gpaFive_showsMessage() { // TC19
        assertMessageAfterBadAdd(unique("MsgFive"), 0, "5");
    }

    @Tag("defect")
    @Test
    void gpaMinusPointOne_showsMessage() { // TC13
        assertMessageAfterBadAdd(unique("MsgNeg"), 0, "-0.1");
    }

    @Tag("defect")
    @Test
    void gpaFourPointOne_showsMessage() { // TC14
        assertMessageAfterBadAdd(unique("MsgHigh"), 0, "4.1");
    }

    @Tag("defect")
    @Test
    void name256Characters_showsMessage() { // TC16
        assertMessageAfterBadAdd(unique("MsgLong"), 256, "3.5");
    }

    @Tag("defect")
    @Test
    void failedAdd_keepsTypedInput() { // TC20
        StudentListPage page = page();
        page.open();
        String name = unique("KeepInput");
        page.submitNewStudent(name, "CS", "5");
        assertFalse(page.formClearedWithin(SETTLE), "the form was cleared after a failed add");
        assertEquals(name, page.nameInputValue());
        assertEquals("CS", page.majorInputValue());
        assertEquals("5", page.gpaInputValue());
    }

    @Tag("defect")
    @Test
    void editStudent_gpaFive_showsMessage() { // TC21
        StudentListPage page = page();
        long id = createStudent(page, unique("EditMsg"), "CS", "2.5");
        page.clickEdit(id);
        page.setEditGpa("5");
        page.clickSave();
        assertTrue(UserMessages.appearsWithin(driver, SETTLE), "no message was shown to the user");
    }
}