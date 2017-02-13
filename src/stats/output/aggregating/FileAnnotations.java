package stats.output.aggregating;

/**
 *
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public class FileAnnotations {

   private FileAnnotations() {
   }
   //<editor-fold defaultstate="collapsed" desc="Miscellaneus">
   public final static String IGNORE = "ignore";
   public final static String TIME = "TIME";
   //</editor-fold>
   //<editor-fold defaultstate="collapsed" desc="Basic Annotations">
   public final static String OPEN_BEGIN = "<";
   public final static String OPEN_END = "</";
   public final static String CLOSE = ">";
   //</editor-fold>
   //<editor-fold defaultstate="collapsed" desc="SCENARIO_SETUP">
   public final static String SCENARIO_SETUP = "Scenario Setup";
   public final static String SCENARIO_SETUP_BEGIN = OPEN_BEGIN + SCENARIO_SETUP + CLOSE;
   public final static String SCENARIO_SETUP_END = OPEN_END + SCENARIO_SETUP + CLOSE;
   //</editor-fold>
   //<editor-fold defaultstate="collapsed" desc="REPEAT_DETAILS">
   public final static String REPEAT_DETAILS = "Repeat Details";
   public final static String REPEAT_DETAILS_BEGIN = OPEN_BEGIN + REPEAT_DETAILS + CLOSE;
   public final static String REPEAT_DETAILS_END = OPEN_END + REPEAT_DETAILS + CLOSE;
   //</editor-fold>
}
