package stats.output.aggregating;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 *
 * @author xvas
 */
public class SetupsGrp {

   private final Set<ResultsFile> _groupMembers;
   /**
    * key: Shortcut of parameter title
    *
    * value: the bean
    */
   private final SortedMap<String, ScenarioParamBean> groupedByScenarioSetup;
   private final ResultsFile _head;
   private final Set<String> _paramsIgnored;

   public SetupsGrp(ResultsFile head, Set<String> paramsIgnored) {
      this._groupMembers = new HashSet<>();
      this._groupMembers.add(head);
      this._head = head;
      this._paramsIgnored = paramsIgnored;

      this.groupedByScenarioSetup = ResultsGroup__groupedByScenarioSetup(head, paramsIgnored);
   }

   /**
    * The setup parameters and their values which are used to for comparing the head setup with candidate setups. If
    * candidate setups are equal to the head, then they are included in this group. Note that the setup parameters
    * extracted from the head are in the form of ScenarioParamBean instances, and that no ScenarioParamBean are returned
    * for parameters included in the paramsIgnored.
    *
    * @param head
    * @param paramsIgnored the parameter to ignore.
    * @return A map of the id (parameter shortcut name) and the beans of the setup parameters and their values which are
    * used to for comparing the head
    *
    * setup with candidate setups.
    */
   private TreeMap<String, ScenarioParamBean> ResultsGroup__groupedByScenarioSetup(ResultsFile head, Set<String> paramsIgnored) {
      TreeMap<String, ScenarioParamBean> scenarioSetupBeans = new TreeMap<>();

      Map<String, ScenarioParamBean> scenarioSetup = head.scenarioSetup();
      for (Map.Entry<String, ScenarioParamBean> entry : scenarioSetup.entrySet()) {
         ScenarioParamBean scenarioParamBean = entry.getValue();

         if (!paramsIgnored.contains(scenarioParamBean.getTitleShort())
               && !paramsIgnored.contains(scenarioParamBean.getTitle_full())) {
            scenarioSetupBeans.put(entry.getKey(), scenarioParamBean);
         }

      }

      return scenarioSetupBeans;
   }

   public boolean tryAdd(ResultsFile rf) {
      if (_head.equalsSetupParams(rf, _paramsIgnored)) {
         _groupMembers.add(rf);
         return true;
      }
      return false;
   }

   /**
    * @return the results files that are members of this group as an unmodifiable set.
    */
   public Set<ResultsFile> members() {
      return Collections.unmodifiableSet(_groupMembers);
   }

   /**
    * @return A string representation of the scenario parameters and their values for this group. Each parameter-value
    * pairs are separated by line feed.
    */
   public String scenarioSetup() {
      StringBuilder stringBuilder = new StringBuilder(180);
      for (Iterator<Map.Entry<String, ScenarioParamBean>> it = groupedByScenarioSetup.entrySet().iterator(); it.hasNext();) {
         Map.Entry<String, ScenarioParamBean> entry = it.next();
         ScenarioParamBean bean = entry.getValue();
//         stringBuilder.append(bean.getTitle_full()).append(',');
         stringBuilder.append(bean.getTitleShort()).append(',');
         stringBuilder.append(bean.getValue());
         if (it.hasNext()) {
            stringBuilder.append('\n');
         }
      }

      return stringBuilder.toString();
   }
}
