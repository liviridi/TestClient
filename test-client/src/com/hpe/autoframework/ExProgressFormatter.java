package com.hpe.autoframework;

import gherkin.formatter.Formatter;
import gherkin.formatter.NiceAppendable;
import gherkin.formatter.Reporter;
import gherkin.formatter.ansi.AnsiEscapes;
import gherkin.formatter.model.Background;
import gherkin.formatter.model.Examples;
import gherkin.formatter.model.Feature;
import gherkin.formatter.model.Match;
import gherkin.formatter.model.Result;
import gherkin.formatter.model.Scenario;
import gherkin.formatter.model.ScenarioOutline;
import gherkin.formatter.model.Step;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.TestException;

import cucumber.runtime.formatter.ColorAware;

public class ExProgressFormatter implements Formatter, Reporter, ColorAware {
	
	private final static String EVIDENCE_DIR = "evidence";
	
    @SuppressWarnings("serial")
	private static final Map<String, Character> CHARS = new HashMap<String, Character>() {{
        put("passed", '.');
        put("undefined", 'U');
        put("pending", 'P');
        put("skipped", '-');
        put("failed", 'F');
    }};
    
    @SuppressWarnings("serial")
	private static final Map<String, AnsiEscapes> ANSI_ESCAPES = new HashMap<String, AnsiEscapes>() {{
        put("passed", AnsiEscapes.GREEN);
        put("undefined", AnsiEscapes.YELLOW);
        put("pending", AnsiEscapes.YELLOW);
        put("skipped", AnsiEscapes.CYAN);
        put("failed", AnsiEscapes.RED);
    }};

    private final NiceAppendable out;
    private boolean monochrome = false;
    
    private String scenarioStatus_;
    private int passedSteps_;
    private int undefinedSteps_;
    private int pendingSteps_;
    private int skippedSteps_;
    private int failedSteps_;
    
	/**
	 * Evidence saving directory
	 */
	private String evidenceDir_;

	/**
	 * Evidence directory name
	 */
	private static Path evdDir_;

    public static Path getEvidenceDirName() {
    	if (evdDir_ == null)
    		throw new TestException("No evidence saving directory created");
		return evdDir_;
	}

	public ExProgressFormatter(Appendable appendable) {
        out = new NiceAppendable(appendable);
    }

    @Override
    public void uri(String uri) {
    }

    @Override
    public void feature(Feature feature) {
    	onStart();
    }

    @Override
    public void background(Background background) {
    }

    @Override
    public void scenario(Scenario scenario) {
    }

    @Override
    public void scenarioOutline(ScenarioOutline scenarioOutline) {
    }

    @Override
    public void examples(Examples examples) {
    }

    @Override
    public void step(Step step) {
    }

    @Override
    public void eof() {
    }

    @Override
    public void syntaxError(String state, String event, List<String> legalEvents, String uri, Integer line) {
    }

    @Override
    public void startOfScenarioLifeCycle(Scenario scenario) {
    	out.append(scenario.getName() + ": ");
    	scenarioStatus_ = Result.PASSED;
        passedSteps_ = 0;
        undefinedSteps_ = 0;
        pendingSteps_ = 0;
        skippedSteps_ = 0;
        failedSteps_ = 0;
        startEvidence(scenario);
    }

    @Override
    public void endOfScenarioLifeCycle(Scenario scenario) {
    	int total = passedSteps_ + undefinedSteps_ + pendingSteps_ + skippedSteps_ + failedSteps_;
    	out.append(" " + scenarioStatus_ + "  " + total + " steps (" 
    			+ passedSteps_ + " " + Result.PASSED + "  " 
    			+ failedSteps_ + " " + Result.FAILED + "  " 
    			+ undefinedSteps_ + " " + "undefined" + "  " 
    			+ pendingSteps_ + " " + "pending" + "  " 
    			+ skippedSteps_ + " " + "skipped" 
    			+ ")");
        out.println();
    }

    @Override
    public void done() {
        out.println();
    }

    @Override
    public void close() {
        out.close();
    }

    @Override
    public void result(Result result) {
        if (!monochrome) {
            ANSI_ESCAPES.get(result.getStatus()).appendTo(out);
        }
        out.append(CHARS.get(result.getStatus()));
        if (!monochrome) {
            AnsiEscapes.RESET.appendTo(out);
        }
        
        // set the first non-passed status as the final status of the scenario
        if (scenarioStatus_.equals(Result.PASSED))
        	scenarioStatus_ = result.getStatus();
        String status = result.getStatus();
        if (status.equals(Result.PASSED)) {
        	passedSteps_ ++;
        } else if (status.equals("undefined")) {
        	undefinedSteps_ ++;
        } else if (status.equals(Result.FAILED)) {
            failedSteps_ ++;
        } else if (status.equals("skipped")) {
            skippedSteps_ ++;
        } else {
        	pendingSteps_ ++;
        }
    }

    @Override
    public void before(Match match, Result result) {
        handleHook(match, result, "B");
    }

    @Override
    public void after(Match match, Result result) {
        handleHook(match, result, "A");
    }

    private void handleHook(Match match, Result result, String character) {
        if (result.getStatus().equals(Result.FAILED)) {
            if (!monochrome) {
                ANSI_ESCAPES.get(result.getStatus()).appendTo(out);
            }
            out.append(character);
            if (!monochrome) {
                AnsiEscapes.RESET.appendTo(out);
            }
        }
    }

    @Override
    public void match(Match match) {
    }

    @Override
    public void embedding(String mimeType, byte[] data) {
    }

    @Override
    public void write(String text) {
    }

    @Override
    public void setMonochrome(boolean monochrome) {
        this.monochrome = monochrome;
    }
    
    public void onStart() {
    	
    	DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss.S");
    	evidenceDir_ = "Evidence_" + dateFormat.format(new Date(System.currentTimeMillis()));
    }

	/**
	 * Start evidence
	 */
    private void startEvidence(Scenario scenario) {
    	String scenarioname = escapeScenarioName(scenario.getName());
    	evdDir_ = FileSystems.getDefault().getPath(EVIDENCE_DIR + File.separator + evidenceDir_ + File.separator + scenarioname + "_evidence");
    	
    	// create evidence saving directory
        try {
			Files.createDirectories(evdDir_);
		} catch (IOException exp) {
			throw new TestException("Evidence saving directory create failed", exp);
		}
    }

    private String escapeScenarioName(String scenarioname) {
    	char c;
    	StringBuffer sb = new StringBuffer();
    	int len = scenarioname.length();
    	for (int i = 0; i < len; i ++) {
    		c = scenarioname.charAt(i);
    		if (c >= '\0' && c <= ' ' || c == '/' || c == '\\')
    			sb.append('_');
    		else
    			sb.append(c);
    	}
    	return sb.toString();
    }
}
