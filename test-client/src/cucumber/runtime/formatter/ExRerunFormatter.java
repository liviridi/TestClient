package cucumber.runtime.formatter;

import java.io.File;


/**
 * Formatter for reporting all failed features and print their locations
 * Failed means: (failed, undefined, pending) test result
 */
public class ExRerunFormatter extends RerunFormatter {
    public ExRerunFormatter(Appendable out) {
		super(out);
	}

	@Override
    public void uri(String uri) {
		if (!uri.startsWith("features"))
            super.uri("features" + File.separatorChar + uri);
		else
			super.uri(uri);
    }
}
