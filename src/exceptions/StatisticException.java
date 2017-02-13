package exceptions;

/**
 *
 * @author xvas
 */
public class StatisticException extends Exception {

   public StatisticException() {
      super();
   }

   public StatisticException(String msg) {
      super(msg);
   }

   public StatisticException(StringBuilder msg) {
      super(msg.toString());
   }

   public StatisticException(Throwable ex) {
      super(ex);
   }
   public StatisticException(String msg, Throwable ex) {
      super(ex);
   }
}
