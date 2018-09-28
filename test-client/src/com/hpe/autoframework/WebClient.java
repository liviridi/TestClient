package com.hpe.autoframework;

import java.awt.AWTException;
import java.awt.Robot;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.TestException;

public class WebClient {

	private static final String SCREENSHOT_FILENAME = "screenshot%05d.png";
	
	protected WebDriver driver_ = new InternetExplorerDriver();
	
	protected int screenShotIndex_ = 1;
	
	protected WebElement oldPage_;
	
	public WebClient() {
		
	}

	public void init() {
		
		// move mouse to top left
		Robot robot;
		try {
			robot = new Robot();
			robot.mouseMove(0, 0);
		} catch (AWTException e1) {
			e1.printStackTrace();
		}
		
		// maximize window
		driver_.manage().window().maximize();
	}
	
	public WebDriver getWebDriver() {
		return driver_;
	}
	
	public void takeScreenshot() {
		File scrFile = ((TakesScreenshot)driver_).getScreenshotAs(OutputType.FILE);
		try {
			Files.move(FileSystems.getDefault().getPath(scrFile.getPath()), 
					ExProgressFormatter.getEvidenceDirName().resolve(String.format(SCREENSHOT_FILENAME, screenShotIndex_ ++)), 
					StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			throw new TestException("Saving screen shot file failed:" + scrFile.getPath(), e);
		}
	}
	
	public void waitForTitle(long timeout, String title) {
		final String title_ = title;
		(new WebDriverWait(driver_, timeout)).until(new ExpectedCondition<Boolean>() {
            public Boolean apply(WebDriver driver) {
                return driver.getTitle().equals(title_);
            }
        });
	}
	
	public void waitForPageLoad(long timeout) {
		(new WebDriverWait(driver_, timeout)).until(new ExpectedCondition<Boolean>() {
            public Boolean apply(WebDriver driver) {
            	if (driver_.findElement(By.tagName("html")) == oldPage_)
            		return false;
            	oldPage_ = driver_.findElement(By.tagName("html"));
	            return String
		            .valueOf(((JavascriptExecutor)driver_).executeScript("return document.readyState"))
		            .equals("complete");
            }
        });
	}
	
	public void clickLink(String keyword) {
        List<WebElement> anchors = driver_.findElements(By.tagName("a"));
        Iterator<WebElement> i = anchors.iterator();
        while(i.hasNext()) {
            WebElement anchor = i.next();
            if(anchor.getText().contains(keyword)) {
                if (driver_ instanceof InternetExplorerDriver) {
                	anchor.sendKeys(Keys.CONTROL);
                }
           		anchor.click();
                break;
            }
            String href = anchor.getAttribute("href");
            if(href != null && href.contains(keyword)) {
                if (driver_ instanceof InternetExplorerDriver) {
                	anchor.sendKeys(Keys.CONTROL);
                }
                anchor.click();
                break;
            }
        }
    }
	
	public void close() {
		driver_.quit();
		driver_ = null;
	}
}
