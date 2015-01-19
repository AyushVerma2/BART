package pro.dbro.bart.api.xml;

import org.simpleframework.xml.Attribute;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Base object for Bart objects that include
 * Date and Time
 */
public abstract class BartDateTimeObject implements Comparable<BartDateTimeObject> {

    private static final SimpleDateFormat BART_DATE_PARSER = new SimpleDateFormat("MM/dd/yyyy hh:mm a", Locale.US);

    static {
        BART_DATE_PARSER.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));
    }

    private static Date getDateFromBartDateTime(String bartDate, String bartTime) throws ParseException {
        return BART_DATE_PARSER.parse(String.format("%s %s", bartDate, bartTime));
    }

    @Attribute(name = "origTimeMin")
    private String originTime;

    @Attribute(name = "origTimeDate")
    private String originDate;

    @Attribute(name = "destTimeMin")
    private String destTime;

    @Attribute(name = "destTimeDate")
    private String destDate;

    public String getOriginTime() {
        return originTime;
    }

    public String getOriginDate() {
        return originDate;
    }

    public String getDestTime() {
        return destTime;
    }

    public String getDestDate() {
        return destDate;
    }

    @Override
    public int compareTo(BartDateTimeObject other) {
        try {
            return getOriginAsDate().compareTo(other.getOriginAsDate());
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public long getOriginAsRelativeSec() throws ParseException {
        long diffInMs = getOriginAsDate().getTime() - new Date().getTime();
        return TimeUnit.MILLISECONDS.toSeconds(diffInMs);
    }

    public Date getOriginAsDate() throws ParseException {
        return getDateFromBartDateTime(getOriginDate(), getOriginTime());
    }

    public Date getDestAsDate() throws ParseException {
        return getDateFromBartDateTime(getDestDate(), getDestTime());
    }

    public void adjustWithEstimatedOriginDate(Date originDate){
        this.originDate = BART_DATE_PARSER.format(originDate);
        originTime = BART_DATE_PARSER.format(originDate);
    }

}
