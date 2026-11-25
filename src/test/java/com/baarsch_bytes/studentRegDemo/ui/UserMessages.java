package com.baarsch_bytes.studentRegDemo.ui;

import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.FluentWait;

import java.time.Duration;

//Detects whether the user was told something
public final class UserMessages {

    private static final By MESSAGE_ELEMENTS = By.cssSelector(
            "[role='alert'], [role='status'], [class*='error'], [class*='alert'], "
                    + "[id*='error'], [id*='message']");

    private UserMessages() {
    }

    //True if a message shows up within the timeout
    public static boolean appearsWithin(WebDriver driver, Duration timeout) {
        try {
            return new FluentWait<>(driver)
                    .withTimeout(timeout)
                    .pollingEvery(Duration.ofMillis(100))
                    .until(d -> isMessageShown(d));
        } catch (TimeoutException e) {
            return false; //nothing appeared
        }
    }

    private static boolean isMessageShown(WebDriver d) {
        try {
            Alert alert = d.switchTo().alert();
            alert.accept();
            return true;
        } catch (NoAlertPresentException noAlert) {
            //no pop-up
        }
        for (WebElement e : d.findElements(MESSAGE_ELEMENTS)) {
            if (e.isDisplayed() && !e.getText().isBlank()) {
                return true;
            }
        }
        return false;
    }
}