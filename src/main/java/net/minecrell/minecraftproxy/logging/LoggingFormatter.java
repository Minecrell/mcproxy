package net.minecrell.minecraftproxy.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public final class LoggingFormatter extends Formatter {
    private final DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    @Override
    public String format(LogRecord record) {
        StringBuilder formatted = new StringBuilder()
                .append(dateFormat.format(record.getMillis()))
                .append(" [")
                .append(record.getLevel().getLocalizedName())
                .append("] ")
                .append(this.formatMessage(record))
                .append('\n');

        if (record.getThrown() != null) {
            StringWriter writer = new StringWriter();
            record.getThrown().printStackTrace(new PrintWriter(writer));
            formatted.append(writer);
        }

        return formatted.toString();
    }
}
