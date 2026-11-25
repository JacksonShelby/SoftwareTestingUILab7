package com.baarsch_bytes.studentRegDemo.ui.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.TimeoutException;
import java.time.Duration;
import org.openqa.selenium.Keys;
import org.openqa.selenium.StaleElementReferenceException;

public class CoursesPage {

    private final WebDriver driver;
    private final WebDriverWait wait;
    private final String baseUrl;

    private static final By FORM = By.id("new-course-fields");
    private static final By NAME_INPUT = By.id("new-course-name");
    private static final By INSTRUCTOR_INPUT = By.id("new-course-instructor");
    private static final By MAX_SIZE_INPUT = By.id("new-course-max-size");
    private static final By ROOM_INPUT = By.id("new-course-room");
    private static final By ADD_BUTTON = By.cssSelector("#new-course-fields button");

    public CoursesPage(WebDriver driver, WebDriverWait wait, String baseUrl) {
        this.driver = driver;
        this.wait = wait;
        this.baseUrl = baseUrl;
    }

    //Opens the Courses page and waits for the add-course form
    public CoursesPage open() {
        driver.get(baseUrl + "/");   // change if the Courses page has another path
        wait.until(ExpectedConditions.visibilityOfElementLocated(FORM));
        return this;
    }

    //Fills in the form and clicks Add Course. Doesn't wait for the result
    public void submitNewCourse(String name, String instructor, String maxSize, String room) {
        type(NAME_INPUT, name);
        type(INSTRUCTOR_INPUT, instructor);
        type(MAX_SIZE_INPUT, maxSize);
        type(ROOM_INPUT, room);
        driver.findElement(ADD_BUTTON).click();
    }

    //Waits for the course row to appear and returns its database id
    public long waitForCourseNamed(String name) {
        By nameCell = By.xpath("//td[starts-with(@id,'course-name-') and normalize-space(.)="
                + xpathLiteral(name) + "]");
        WebElement cell = wait.until(ExpectedConditions.visibilityOfElementLocated(nameCell));
        return Long.parseLong(cell.getDomAttribute("id").substring("course-name-".length()));
    }

    public String nameOf(long id)       { return text("course-name-" + id); }
    public String instructorOf(long id) { return text("course-instructor-" + id); }
    public String maxSizeOf(long id)    { return text("course-max-size-" + id); }
    public String roomOf(long id)       { return text("course-room-" + id); }

    //The id is misleading: this cell holds the enrolled COUNT
    public String enrolledCountOf(long id) { return text("course-roster-" + id); }

    //The roster-names cell has no id, so use the cell after the count cell
    public String rosterOf(long id) {
        return driver.findElement(By.xpath(
                "//td[@id='course-roster-" + id + "']/following-sibling::td[1]")).getText();
    }

    //Selects the student by database id in this course's row, then clicks Add Student
    public void addStudent(long courseId, long studentId) {
        By option = By.xpath("//tr[@id='course-row-" + courseId + "']//option[@value='" + studentId + "']");
        wait.until(ExpectedConditions.presenceOfElementLocated(option));
        WebElement row = driver.findElement(By.id("course-row-" + courseId));
        new Select(row.findElement(By.id("select-student"))).selectByValue(String.valueOf(studentId));
        row.findElement(By.xpath(".//button[normalize-space()='Add Student']")).click();
    }

    public void clickEdit(long id) {
        driver.findElement(By.xpath("//tr[@id='course-row-" + id
                + "']//button[normalize-space()='Edit']")).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("edit-course-name")));
    }

    public void setEditFields(String name, String instructor, String maxSize, String room) {
        type(By.id("edit-course-name"), name);
        type(By.id("edit-course-instructor"), instructor);
        type(By.id("edit-course-max-size"), maxSize);
        type(By.id("edit-course-room"), room);
    }

    public void saveEdit() {
        driver.findElement(By.id("edit-course-save-button")).click();
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("edit-course-save-button")));
    }

    public void cancelEdit() {
        driver.findElement(By.id("edit-course-cancel-button")).click();
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("edit-course-cancel-button")));
    }

    //Only works while the edit view is open. Does not click Save
    public void removeStudent(long studentId) {
        new Select(driver.findElement(By.id("remove-student-select")))
                .selectByValue(String.valueOf(studentId));
        driver.findElement(By.id("remove-student-button")).click();
    }

    public void waitForEnrolledCount(long id, String expected) {
        wait.until(ExpectedConditions.textToBe(By.id("course-roster-" + id), expected));
    }

    public void clickDelete(long id) {
        driver.findElement(By.xpath("//tr[@id='course-row-" + id
                + "']//button[normalize-space()='Delete']")).click();
    }

    public void waitForRowGone(long id) {
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("course-row-" + id)));
    }

    private String text(String id) {
        return driver.findElement(By.id(id)).getText();
    }

    //Types into an input and retries until the field really holds the value
    private void type(By locator, String value) {
        wait.ignoring(StaleElementReferenceException.class).until(d -> {
            WebElement input = d.findElement(locator);
            input.click();
            input.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.BACK_SPACE);
            input.sendKeys(value);
            return value.equals(input.getDomProperty("value"));
        });
    }

    private static String xpathLiteral(String text) {
        if (!text.contains("'")) {
            return "'" + text + "'";
        }
        return "concat('" + text.replace("'", "', \"'\", '") + "')";
    }

    //True if the enrolled count changes from `original` within the timeout
    public boolean enrolledCountChangesFromWithin(long id, String original, Duration timeout) {
        try {
            new WebDriverWait(driver, timeout).until(ExpectedConditions.not(
                    ExpectedConditions.textToBe(By.id("course-roster-" + id), original)));
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    //True if all four add-course inputs become empty within the timeout
    public boolean formClearedWithin(Duration timeout) {
        try {
            new WebDriverWait(driver, timeout).until(d ->
                    "".equals(d.findElement(NAME_INPUT).getDomProperty("value"))
                            && "".equals(d.findElement(INSTRUCTOR_INPUT).getDomProperty("value"))
                            && "".equals(d.findElement(MAX_SIZE_INPUT).getDomProperty("value"))
                            && "".equals(d.findElement(ROOM_INPUT).getDomProperty("value")));
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }
}
