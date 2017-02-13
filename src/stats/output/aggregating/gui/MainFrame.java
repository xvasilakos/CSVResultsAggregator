package stats.output.aggregating.gui;

import exceptions.StatisticException;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultListModel;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.ListSelectionModel;
import stats.Constants;
import stats.Statistics;
import static stats.Constants.DEFAULT_STATS_PATH;
import static stats.Constants.MODE__TRANSIENT_STATS;
import static stats.output.aggregating.AggregatorApp.chooseConfidenceInterval;
import static stats.output.aggregating.AggregatorApp.parseResultsFiles;
import static stats.output.aggregating.AggregatorApp.selectFiles;
import stats.output.aggregating.ResultsFile;
import utilities.CommonFunctions;
import static stats.output.aggregating.AggregatorApp.aggrAndPrint;

/**
 *
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public class MainFrame extends javax.swing.JFrame {

    public static final String agg = "agg";
    public static final String trn = "trn";
    public static final String trnAgg = "trnAgg";

    public static void main(String args[]) {
        LOG.setLevel(Level.ALL);
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /*
         * If Nimbus (introduced in Java SE 6) is not available, stay with the
         * default look and feel. For details see
         * http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            LOG.log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            LOG.log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            LOG.log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            LOG.log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                MainFrame frame = new MainFrame();

                Dimension screenDimentions = Toolkit.getDefaultToolkit().getScreenSize();
                int posX = screenDimentions.width - frame.getWidth();
                posX /= 2;
                int posY = screenDimentions.height - frame.getHeight();
                posY /= 2;
                frame.setLocation(posX, posY);
                frame.setVisible(true);
            }
        });
    }

    private String[] loadTitles() {
        Set<String> titles = new TreeSet<>();

        String path = "/" + getClass().getPackage().getName().replace(".", "/") + "/titles_filtered.txt";

        LOG.log(Level.INFO, "Loading titles from resource: \"{0}\".", path);
        InputStream in = getClass().getResourceAsStream(path);
        if (in == null) {
            LOG.log(Level.SEVERE, "Resource loaded from " + path + " is null");
            throw new RuntimeException();
        }

        Scanner scan = new Scanner(in);

        try {

            String nxt;
            int line = 0;
            while (scan.hasNext()) {
                nxt = scan.nextLine().trim();
                line++;

                //<editor-fold defaultstate="collapsed" desc="ignore comments">
                if (nxt.startsWith("/*")) {
                    int line2 = line;
//                    LOG.log(Level.WARNING, "Ignoring: \"{0}\"", nxt);

                    COMMENT:
                    while (scan.hasNext()) {
                        nxt = scan.nextLine().trim();
                        line2++;
                        if (nxt.endsWith("*/")) {
                            if (scan.hasNext()) {
                                nxt = scan.nextLine().trim();
                                break COMMENT;
                            } else {
                                return titles.toArray(new String[titles.size()]);
                            }
                        }
//                        LOG.log(Level.WARNING, "Ignoring: \"{0}\"", nxt);

                    }
                    if (!scan.hasNext()) {
                        throw new IOException("Comment not closed. Comment open in line " + line);
                    }
                    line = line2;
                }
//</editor-fold>

                if (!nxt.trim().equals("")
                        && !nxt.trim().equals(" ")) {
                    titles.add(nxt.trim());
                }
            }
        } catch (Exception ex) {
            String uponIOEX_msg = "Failed to load default properties file: " + path + "\n";
            LOG.log(Level.SEVERE, uponIOEX_msg, ex);
            System.exit(-1000);
        }

        return titles.toArray(new String[titles.size()]);

    }

    public MainFrame() {
        initComponents();
        statTitlesFiler = loadTitles();

        //<editor-fold defaultstate="collapsed" desc="statTitlesFilter__jList.setModel(...);">
        DefaultListModel<String> model = new DefaultListModel();
        for (String nxt__statTitle : statTitlesFiler) {
            model.addElement(nxt__statTitle);
        }
        statTitlesFilter__jList.setModel(model);
        statTitlesFilter__jList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        //</editor-fold>
        //<editor-fold defaultstate="collapsed" desc="ignoredParams__jList.setModel(...);">
        model = new DefaultListModel();
        for (String nxt__ignoredParam : grpingIgnoredParams) {
            model.addElement(nxt__ignoredParam);
        }
        ignoredParams__jList.setModel(model);
        ignoredParams__jList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        //</editor-fold>
    }

    private String[] statTitlesFiler;
//            = new String[]{
//                "G%(MaxPop)",
//                "G%(incremental.EMC)",
//                "G%(incremental.Naive)",
//                "G%(incremental.Oracle)",
//                "G%(rplc.mingain.no_price.EMC_LC_Full)",
//                "G%(rplc.mingain.no_price.EMPC_LC_Full)"
//            };

    private Set<String> grpingIgnoredParams = new TreeSet<>(Arrays.asList(
            new String[]{
                //      "TU",
                //      "gamma", 
                //            "BF", "#SCs", "SCrd"
                "dmdTrc",
                "rpt",
                "sc"
            }));

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        buttonGroup1 = new javax.swing.ButtonGroup();
        buttonGroup2 = new javax.swing.ButtonGroup();
        mainPanel = new javax.swing.JPanel();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        iterativeMUTitlesPanel = new javax.swing.JPanel();
        statTitlesAdd_jButton = new javax.swing.JButton();
        statTitleAdd__jtextfiled = new javax.swing.JTextField();
        jScrollPane1 = new javax.swing.JScrollPane();
        statTitlesFilter__jList = new javax.swing.JList();
        statTitlesRemove_jButton = new javax.swing.JButton();
        autoOutput__jToggleBut = new javax.swing.JToggleButton();
        autoOpenToggleBut = new javax.swing.JToggleButton();
        iterativeMUTitlesPanel1 = new javax.swing.JPanel();
        ignoredParamAdd_jButton = new javax.swing.JButton();
        ignoredParamAdd__jtextfiled = new javax.swing.JTextField();
        jScrollPane3 = new javax.swing.JScrollPane();
        jScrollPane4 = new javax.swing.JScrollPane();
        ignoredParams__jList = new javax.swing.JList();
        ignoredParamRemove_jButton = new javax.swing.JButton();
        aggrRpts_pnl = new javax.swing.JPanel();
        mean__jToggle = new javax.swing.JToggleButton();
        stdev__jToggle = new javax.swing.JToggleButton();
        conf_95__jToggle = new javax.swing.JToggleButton();
        conf_none__jToggle = new javax.swing.JToggleButton();
        conf_aks__jToggle = new javax.swing.JToggleButton();
        jSeparator2 = new javax.swing.JSeparator();
        fileTypes_pnl = new javax.swing.JPanel();
        skipUntilTimeTextField = new javax.swing.JTextField();
        skipUntilTimeLbl = new javax.swing.JLabel();
        aggrRpts__jBut = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        mainPanel.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        mainPanel.setName("mainPanel"); // NOI18N

        iterativeMUTitlesPanel.setBackground(new java.awt.Color(230, 230, 230));
        iterativeMUTitlesPanel.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));

        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("stats/output/aggregating/gui/Bundle"); // NOI18N
        statTitlesAdd_jButton.setText(bundle.getString("MainFrame.statTitlesAdd_jButton.text")); // NOI18N
        statTitlesAdd_jButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                statTitlesAdd_jButtonActionPerformed(evt);
            }
        });

        statTitleAdd__jtextfiled.setText(bundle.getString("MainFrame.statTitleAdd__jtextfiled.text")); // NOI18N
        statTitleAdd__jtextfiled.setToolTipText(bundle.getString("MainFrame.statTitleAdd__jtextfiled.toolTipText")); // NOI18N
        statTitleAdd__jtextfiled.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                statTitleAdd__jtextfiledActionPerformed(evt);
            }
        });

        jScrollPane1.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        jScrollPane1.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        statTitlesFilter__jList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        statTitlesFilter__jList.setToolTipText(bundle.getString("MainFrame.statTitlesFilter__jList.toolTipText")); // NOI18N
        jScrollPane1.setViewportView(statTitlesFilter__jList);

        statTitlesRemove_jButton.setText(bundle.getString("MainFrame.statTitlesRemove_jButton.text")); // NOI18N
        statTitlesRemove_jButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                statTitlesRemove_jButtonActionPerformed(evt);
            }
        });

        autoOutput__jToggleBut.setSelected(true);
        autoOutput__jToggleBut.setText(bundle.getString("MainFrame.autoOutput__jToggleBut.text")); // NOI18N
        autoOutput__jToggleBut.setToolTipText(bundle.getString("MainFrame.autoOutput__jToggleBut.toolTipText")); // NOI18N
        autoOutput__jToggleBut.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                autoOutput__jToggleButActionPerformed(evt);
            }
        });

        autoOpenToggleBut.setSelected(true);
        autoOpenToggleBut.setText(bundle.getString("MainFrame.autoOpenToggleBut.text")); // NOI18N
        autoOpenToggleBut.setToolTipText(bundle.getString("MainFrame.autoOpenToggleBut.toolTipText")); // NOI18N
        autoOpenToggleBut.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                autoOpenToggleButActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout iterativeMUTitlesPanelLayout = new javax.swing.GroupLayout(iterativeMUTitlesPanel);
        iterativeMUTitlesPanel.setLayout(iterativeMUTitlesPanelLayout);
        iterativeMUTitlesPanelLayout.setHorizontalGroup(
            iterativeMUTitlesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(iterativeMUTitlesPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(iterativeMUTitlesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(iterativeMUTitlesPanelLayout.createSequentialGroup()
                        .addComponent(statTitlesAdd_jButton)
                        .addGap(18, 18, 18)
                        .addComponent(statTitleAdd__jtextfiled)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, iterativeMUTitlesPanelLayout.createSequentialGroup()
                        .addComponent(autoOutput__jToggleBut)
                        .addGap(18, 18, 18)
                        .addComponent(autoOpenToggleBut)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 76, Short.MAX_VALUE)
                        .addComponent(statTitlesRemove_jButton))))
            .addGroup(iterativeMUTitlesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(iterativeMUTitlesPanelLayout.createSequentialGroup()
                    .addGap(0, 0, Short.MAX_VALUE)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 426, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGap(0, 0, Short.MAX_VALUE)))
        );
        iterativeMUTitlesPanelLayout.setVerticalGroup(
            iterativeMUTitlesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(iterativeMUTitlesPanelLayout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addGroup(iterativeMUTitlesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(statTitlesAdd_jButton)
                    .addComponent(statTitleAdd__jtextfiled, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 347, Short.MAX_VALUE)
                .addGroup(iterativeMUTitlesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(statTitlesRemove_jButton)
                    .addComponent(autoOutput__jToggleBut)
                    .addComponent(autoOpenToggleBut)))
            .addGroup(iterativeMUTitlesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(iterativeMUTitlesPanelLayout.createSequentialGroup()
                    .addGap(0, 0, Short.MAX_VALUE)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 328, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGap(0, 0, Short.MAX_VALUE)))
        );

        jTabbedPane1.addTab(bundle.getString("MainFrame.iterativeMUTitlesPanel.TabConstraints.tabTitle"), null, iterativeMUTitlesPanel, bundle.getString("MainFrame.iterativeMUTitlesPanel.TabConstraints.tabToolTip")); // NOI18N
        iterativeMUTitlesPanel.getAccessibleContext().setAccessibleName(bundle.getString("MainFrame.iterativeMUTitlesPanel.AccessibleContext.accessibleName")); // NOI18N

        iterativeMUTitlesPanel1.setBackground(new java.awt.Color(230, 230, 230));
        iterativeMUTitlesPanel1.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        iterativeMUTitlesPanel1.setLayout(new java.awt.GridBagLayout());

        ignoredParamAdd_jButton.setText(bundle.getString("MainFrame.ignoredParamAdd_jButton.text")); // NOI18N
        ignoredParamAdd_jButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ignoredParamAdd_jButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(12, 12, 0, 0);
        iterativeMUTitlesPanel1.add(ignoredParamAdd_jButton, gridBagConstraints);

        ignoredParamAdd__jtextfiled.setText(bundle.getString("MainFrame.ignoredParamAdd__jtextfiled.text")); // NOI18N
        ignoredParamAdd__jtextfiled.setToolTipText(bundle.getString("MainFrame.ignoredParamAdd__jtextfiled.toolTipText")); // NOI18N
        ignoredParamAdd__jtextfiled.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ignoredParamAdd__jtextfiledActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.ipadx = 311;
        gridBagConstraints.ipady = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(13, 15, 0, 12);
        iterativeMUTitlesPanel1.add(ignoredParamAdd__jtextfiled, gridBagConstraints);

        ignoredParams__jList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        ignoredParams__jList.setToolTipText(bundle.getString("MainFrame.ignoredParams__jList.toolTipText")); // NOI18N
        jScrollPane4.setViewportView(ignoredParams__jList);

        jScrollPane3.setViewportView(jScrollPane4);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.ipadx = 369;
        gridBagConstraints.ipady = 189;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 12, 0, 12);
        iterativeMUTitlesPanel1.add(jScrollPane3, gridBagConstraints);

        ignoredParamRemove_jButton.setText(bundle.getString("MainFrame.ignoredParamRemove_jButton.text")); // NOI18N
        ignoredParamRemove_jButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ignoredParamRemove_jButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(6, 241, 2, 12);
        iterativeMUTitlesPanel1.add(ignoredParamRemove_jButton, gridBagConstraints);

        jTabbedPane1.addTab(bundle.getString("MainFrame.iterativeMUTitlesPanel1.TabConstraints.tabTitle"), null, iterativeMUTitlesPanel1, bundle.getString("MainFrame.iterativeMUTitlesPanel1.TabConstraints.tabToolTip")); // NOI18N

        aggrRpts_pnl.setBackground(new java.awt.Color(230, 230, 230));
        aggrRpts_pnl.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createTitledBorder(null, bundle.getString("MainFrame.aggrRpts_pnl.border.outsideBorder.title"), javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.ABOVE_TOP), javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED))); // NOI18N

        mean__jToggle.setSelected(true);
        mean__jToggle.setText(bundle.getString("MainFrame.mean__jToggle.text")); // NOI18N
        mean__jToggle.setToolTipText(bundle.getString("MainFrame.mean__jToggle.toolTipText")); // NOI18N
        mean__jToggle.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mean__jToggleActionPerformed(evt);
            }
        });

        stdev__jToggle.setText(bundle.getString("MainFrame.stdev__jToggle.text")); // NOI18N
        stdev__jToggle.setToolTipText(bundle.getString("MainFrame.stdev__jToggle.toolTipText")); // NOI18N
        stdev__jToggle.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stdev__jToggleActionPerformed(evt);
            }
        });

        buttonGroup2.add(conf_95__jToggle);
        conf_95__jToggle.setSelected(true);
        conf_95__jToggle.setText(bundle.getString("MainFrame.conf_95__jToggle.text")); // NOI18N
        conf_95__jToggle.setToolTipText(bundle.getString("MainFrame.conf_95__jToggle.toolTipText")); // NOI18N
        conf_95__jToggle.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                conf_95__jToggleActionPerformed(evt);
            }
        });

        buttonGroup2.add(conf_none__jToggle);
        conf_none__jToggle.setText(bundle.getString("MainFrame.conf_none__jToggle.text")); // NOI18N
        conf_none__jToggle.setToolTipText(bundle.getString("MainFrame.conf_none__jToggle.toolTipText")); // NOI18N
        conf_none__jToggle.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                conf_none__jToggleActionPerformed(evt);
            }
        });

        buttonGroup2.add(conf_aks__jToggle);
        conf_aks__jToggle.setText(bundle.getString("MainFrame.conf_aks__jToggle.text")); // NOI18N
        conf_aks__jToggle.setToolTipText(bundle.getString("MainFrame.conf_aks__jToggle.toolTipText")); // NOI18N
        conf_aks__jToggle.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                conf_aks__jToggleActionPerformed(evt);
            }
        });

        jSeparator2.setOrientation(javax.swing.SwingConstants.VERTICAL);

        javax.swing.GroupLayout aggrRpts_pnlLayout = new javax.swing.GroupLayout(aggrRpts_pnl);
        aggrRpts_pnl.setLayout(aggrRpts_pnlLayout);
        aggrRpts_pnlLayout.setHorizontalGroup(
            aggrRpts_pnlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(aggrRpts_pnlLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(aggrRpts_pnlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(aggrRpts_pnlLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(conf_95__jToggle, javax.swing.GroupLayout.PREFERRED_SIZE, 62, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(aggrRpts_pnlLayout.createSequentialGroup()
                        .addGroup(aggrRpts_pnlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(conf_none__jToggle, javax.swing.GroupLayout.PREFERRED_SIZE, 62, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(conf_aks__jToggle, javax.swing.GroupLayout.PREFERRED_SIZE, 62, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(aggrRpts_pnlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(stdev__jToggle, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 62, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(mean__jToggle, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 62, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        aggrRpts_pnlLayout.setVerticalGroup(
            aggrRpts_pnlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(aggrRpts_pnlLayout.createSequentialGroup()
                .addGroup(aggrRpts_pnlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(aggrRpts_pnlLayout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addComponent(conf_95__jToggle)
                        .addGap(3, 3, 3)
                        .addComponent(conf_none__jToggle)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(conf_aks__jToggle))
                    .addGroup(aggrRpts_pnlLayout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 81, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(aggrRpts_pnlLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(mean__jToggle)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(stdev__jToggle)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        fileTypes_pnl.setBackground(new java.awt.Color(230, 230, 230));
        fileTypes_pnl.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createTitledBorder(null, bundle.getString("MainFrame.fileTypes_pnl.border.outsideBorder.title"), javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.ABOVE_TOP), javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED))); // NOI18N

        buttonGroup1.add(transientStatJRad);
        transientStatJRad.setText(bundle.getString("MainFrame.transientStatJRad.text")); // NOI18N
        transientStatJRad.setMinimumSize(new java.awt.Dimension(50, 50));
        transientStatJRad.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                transientStatJRadActionPerformed(evt);
            }
        });

        buttonGroup1.add(aggrStatJRad);
        aggrStatJRad.setSelected(true);
        aggrStatJRad.setText(bundle.getString("MainFrame.aggrStatJRad.text")); // NOI18N
        aggrStatJRad.setActionCommand(bundle.getString("MainFrame.aggrStatJRad.actionCommand")); // NOI18N
        aggrStatJRad.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aggrStatJRadActionPerformed(evt);
            }
        });

        skipUntilTimeTextField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        skipUntilTimeTextField.setText(bundle.getString("MainFrame.skipUntilTimeTextField.text")); // NOI18N
        skipUntilTimeTextField.setToolTipText(bundle.getString("MainFrame.skipUntilTimeTextField.toolTipText")); // NOI18N
        skipUntilTimeTextField.setMinimumSize(new java.awt.Dimension(50, 50));
        skipUntilTimeTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                skipUntilTimeTextFieldActionPerformed(evt);
            }
        });

        skipUntilTimeLbl.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        skipUntilTimeLbl.setLabelFor(skipUntilTimeTextField);
        skipUntilTimeLbl.setText(bundle.getString("MainFrame.skipUntilTimeLbl.text")); // NOI18N
        skipUntilTimeLbl.setToolTipText(bundle.getString("MainFrame.skipUntilTimeLbl.toolTipText")); // NOI18N
        skipUntilTimeLbl.setEnabled(false);
        skipUntilTimeLbl.setMinimumSize(new java.awt.Dimension(50, 50));

        buttonGroup1.add(aggrtransientStatJRad);
        aggrtransientStatJRad.setText(bundle.getString("MainFrame.aggrtransientStatJRad.text")); // NOI18N
        aggrtransientStatJRad.setMinimumSize(new java.awt.Dimension(50, 50));
        aggrtransientStatJRad.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aggrtransientStatJRadActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout fileTypes_pnlLayout = new javax.swing.GroupLayout(fileTypes_pnl);
        fileTypes_pnl.setLayout(fileTypes_pnlLayout);
        fileTypes_pnlLayout.setHorizontalGroup(
            fileTypes_pnlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(fileTypes_pnlLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(fileTypes_pnlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(fileTypes_pnlLayout.createSequentialGroup()
                        .addComponent(transientStatJRad, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(skipUntilTimeLbl, javax.swing.GroupLayout.PREFERRED_SIZE, 39, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(skipUntilTimeTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(aggrStatJRad, javax.swing.GroupLayout.PREFERRED_SIZE, 118, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(aggrtransientStatJRad, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        fileTypes_pnlLayout.setVerticalGroup(
            fileTypes_pnlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(fileTypes_pnlLayout.createSequentialGroup()
                .addGap(7, 7, 7)
                .addGroup(fileTypes_pnlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(fileTypes_pnlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(skipUntilTimeLbl, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(skipUntilTimeTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(transientStatJRad, javax.swing.GroupLayout.PREFERRED_SIZE, 27, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(aggrtransientStatJRad, javax.swing.GroupLayout.PREFERRED_SIZE, 27, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 11, Short.MAX_VALUE)
                .addComponent(aggrStatJRad)
                .addContainerGap())
        );

        transientStatJRad.getAccessibleContext().setAccessibleName(bundle.getString("MainFrame.transientStatJRad.AccessibleContext.accessibleName")); // NOI18N

        aggrRpts__jBut.setIcon(new javax.swing.ImageIcon(getClass().getResource("/stats/output/aggregating/gui/icons/merge.png"))); // NOI18N
        aggrRpts__jBut.setText(bundle.getString("MainFrame.aggrRpts__jBut.text")); // NOI18N
        aggrRpts__jBut.setToolTipText(bundle.getString("MainFrame.aggrRpts__jBut.toolTipText")); // NOI18N
        aggrRpts__jBut.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        aggrRpts__jBut.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        aggrRpts__jBut.setIconTextGap(1);
        aggrRpts__jBut.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        aggrRpts__jBut.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aggrRpts__jButActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout mainPanelLayout = new javax.swing.GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, mainPanelLayout.createSequentialGroup()
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(mainPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(aggrRpts_pnl, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(fileTypes_pnl, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(mainPanelLayout.createSequentialGroup()
                        .addGap(69, 69, 69)
                        .addComponent(aggrRpts__jBut, javax.swing.GroupLayout.PREFERRED_SIZE, 92, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(10, 10, 10)
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 454, Short.MAX_VALUE)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        mainPanelLayout.setVerticalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jTabbedPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 441, Short.MAX_VALUE)
                    .addGroup(mainPanelLayout.createSequentialGroup()
                        .addGap(4, 4, 4)
                        .addComponent(aggrRpts_pnl, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(fileTypes_pnl, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(aggrRpts__jBut, javax.swing.GroupLayout.PREFERRED_SIZE, 94, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
        );

        aggrRpts_pnl.getAccessibleContext().setAccessibleName(bundle.getString("MainFrame.aggrRpts_pnl.AccessibleContext.accessibleName")); // NOI18N

        getContentPane().add(mainPanel, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents

   private void aggrRpts__jButActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aggrRpts__jButActionPerformed

       List<Thread> workerThreads = new ArrayList();

       final Set selectedTitles = new TreeSet<>(statTitlesFilter__jList.getSelectedValuesList());
       JFileChooser jchosser = selectFiles(
               DEFAULT_STATS_PATH, false, "Aggregate Same Scenario Repeats.", "Aggregate");
       if (jchosser == null) {
           return; // if aborted by user
       }
       File[] userSelectedFiles = jchosser.getSelectedFiles();
       /*pre-parsed results will be groupped together based on the top-most 
       (aka root) common directory*/
       final Map<File, List<File>> mappedParentDir2CSVFilteredFiles = new HashMap<>();

       for (File nxtUsrSelectdFile : userSelectedFiles) {
           // next file or dir must be passed in a list form to method separateResFilesFromResDirs
           List<File> nxtFileOrDirList = new ArrayList<>();

           nxtFileOrDirList.add(nxtUsrSelectdFile);
           filterResFilesOnly(nxtFileOrDirList, mappedParentDir2CSVFilteredFiles);
       }

       // one thread per parent directory ..
       for (final File parentFile : mappedParentDir2CSVFilteredFiles.keySet()) {
           final File[] filteredFiles = mappedParentDir2CSVFilteredFiles.get(parentFile)
                   .toArray(new File[mappedParentDir2CSVFilteredFiles.get(parentFile).size()]);

           Runnable runnable = new Runnable() {
               public void run() {
                   String path = null;

                   String operation;
                   if (aggrStatJRad.isSelected()) {
                       operation = Constants.MODE__AGGREGATES_STATS;
                   } else if (transientStatJRad.isSelected()) {
                       operation = MODE__TRANSIENT_STATS;
                   } else {//if (aggrtransientStatJRad.isSelected()) 
                       operation = Constants.MODE__TRANSIENT_AGGREGATES_STATS;
                   }

                   try {
                       ResultsFile[] resultsFiles
                               = parseResultsFiles(selectedTitles, filteredFiles,
                                       operation,
                                       Integer.parseInt(skipUntilTimeTextField.getText()));

                       Statistics.ConfidenceInterval confidenceInterval = chooseConfidence();

                       path = outputPathCreate();

                       aggrAndPrint(selectedTitles, resultsFiles, path, operation,
                               mean__jToggle.isSelected(), stdev__jToggle.isSelected(),
                               confidenceInterval);

                   } catch (IOException | HeadlessException | StatisticException ex) {
                       LOG.log(Level.SEVERE, null, ex);
                   }
                   //<editor-fold defaultstate="collapsed" desc="inform user about results path. ask to show spreadsheet">
                   String msg = "Repeats aggregated to \"" + path + "\"."
                           + "\nStatistics used: " + CommonFunctions.toString(selectedTitles);

                   if (autoOpenToggleBut.isSelected()) {
                       try {
                           // if not canceled, then launch in excel
                           Desktop.getDesktop().open(new File(path));
                       } catch (IOException ex) {
                           LOG.log(Level.SEVERE, null, ex);
                       }
                   }
//</editor-fold>

                   System.err.println("Finished: " + Thread.currentThread().getName());

               }

               private Statistics.ConfidenceInterval chooseConfidence() throws HeadlessException {
                   Statistics.ConfidenceInterval confidenceInterval;
                   if (conf_aks__jToggle.isSelected()) {
                       confidenceInterval = chooseConfidenceInterval(); // asks user
                   } else if (conf_none__jToggle.isSelected()) {
                       confidenceInterval = Statistics.ConfidenceInterval.NONE;
                   } else {
                       confidenceInterval = Statistics.ConfidenceInterval.PERCENTILE_95;
                   }
                   return confidenceInterval;
               }

               private String outputPathCreate() throws IOException {

                   /*
                    * Constructed after the titles included in statTitlesFilter
                    */
                   String statsIncluded = CommonFunctions.toString("", "", "", "_", selectedTitles);
                   statsIncluded = statsIncluded.length() > 20
                           ? statsIncluded.substring(0, 20)
                           : statsIncluded;

                   StringBuilder aggrResultsFileName = new StringBuilder();

                   aggrResultsFileName.append(aggrtransientStatJRad.isSelected() ? trnAgg
                           : transientStatJRad.isSelected() ? trn
                                   : aggrStatJRad.isSelected() ? agg : null);
                   aggrResultsFileName.append("_").append(statsIncluded);

                   if (!autoOutput__jToggleBut.isSelected()) {
                       String ans
                               = JOptionPane.showInputDialog(
                                       MainFrame.this,
                                       "Chose name for output file",
                                       aggrResultsFileName.toString());
                       if (ans != null) {
                           aggrResultsFileName = new StringBuilder(ans);// asks use
                       } else {
                           return null;
                       }
                   }

                   aggrResultsFileName = new StringBuilder(
                           aggrResultsFileName.toString().replaceAll("<", "_").replaceAll(">", "_")
                   );
                   aggrResultsFileName.append(".csv");

                   String parentPath = parentFile.getParentFile().getCanonicalPath();
                   String path2return = parentPath + "/" + aggrResultsFileName;
                   
                   int i = 1;
                   while (new File(path2return).exists()) {
                       path2return = parentPath + "/" 
                               + ((i<10) ? "0" : "") 
                               + (i++) + "__" 
                               + aggrResultsFileName;
                   }
                   
                   if (i > 1) {
                       LOG.log(Level.WARNING, "Default path cannot be used "
                               + " a file at the same path already exists. "
                               + "Using different file: "
                               + "\"{0}\"", path2return);
                   }
                   return path2return;
               }

           };
           workerThreads.add(new Thread(runnable));
       }

       for (int i = 0; i < workerThreads.size(); i++) {
           workerThreads.get(i).start();
       }

       for (int i = 0; i < workerThreads.size(); i++) {
           try {
               workerThreads.get(i).join();
           } catch (InterruptedException ex) {
               // dont mind
           }
       }

       JOptionPane.showMessageDialog(this,
               "Done", "Processing finished", JOptionPane.INFORMATION_MESSAGE);


   }//GEN-LAST:event_aggrRpts__jButActionPerformed
    private static final Logger LOG = Logger.getLogger(MainFrame.class.getName());

    /**
     * Separates stand-alone result files from directories of result files.
     *
     * @param selectedFiles
     * @param resultsFilesPerDir
     * @param filteredResultFiles parent directory and as key, files to be
     * aggregated as array of values
     * @throws HeadlessException
     */
    private void filterResFilesOnly(List<File> selectedFiles,
            Map<File, List<File>> filteredResultFiles) throws HeadlessException {

        // which ones in case of recursion needed
        List<File> selectedFilesForRecursiveCall = new ArrayList<>();
        // which folders to avoid
        String acceptedFolderPrefix = null;
        String[] rejectedFolderPrefix = null;

        if (aggrStatJRad.isSelected()) {
            acceptedFolderPrefix = agg;
            rejectedFolderPrefix = new String[]{trn, trnAgg};
        } else if (aggrtransientStatJRad.isSelected()) {
            acceptedFolderPrefix = trnAgg;
            rejectedFolderPrefix = new String[]{trn, agg};
        } else if (transientStatJRad.isSelected()) {
            acceptedFolderPrefix = trn;
            rejectedFolderPrefix = new String[]{agg, trnAgg};
        }

        for (File file : selectedFiles) {

            if (file.isFile()) {

                if (file.getName().startsWith(trnAgg)
                        || file.getName().startsWith(trn)
                        || file.getName().startsWith(agg)) {
                    continue; // filter out past-run aggregation results
                }

                if (file.getParentFile().getName().startsWith(acceptedFolderPrefix)
                        && file.getName().endsWith(".csv")) {
                    List<File> resultFiles = filteredResultFiles.get(file.getParentFile());
                    if (resultFiles == null) {
                        resultFiles = new ArrayList<>();
                        filteredResultFiles.put(file.getParentFile(), resultFiles);
                    }
                    resultFiles.add(file);
                }
                continue;
            }

            // otherwise it is a folder...
            if (!file.getName().startsWith(acceptedFolderPrefix)
                    && (file.getName().startsWith(rejectedFolderPrefix[0])
                    || file.getName().startsWith(rejectedFolderPrefix[1]))) {
                continue;//early skip
            }

            File[] subFoldersOrFiles = file.listFiles();

            // check empty directory of files
            if (subFoldersOrFiles.length == 0) {
                try {
                    JOptionPane.showMessageDialog(
                            null, "WARNING!",
                            "Directory " + file.getCanonicalPath()
                            + " is empty and will be ignored",
                            JOptionPane.WARNING_MESSAGE);
                    continue;
                } catch (IOException ex) {
                    LOG.log(Level.SEVERE, null, ex);
                }
            }

            selectedFilesForRecursiveCall.addAll(
                    Arrays.asList(subFoldersOrFiles)
            );
        }

        if (!selectedFilesForRecursiveCall.isEmpty()) {
            // recursive call
            filterResFilesOnly(selectedFilesForRecursiveCall,
                    filteredResultFiles);
        }
    }

   private void statTitlesRemove_jButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_statTitlesRemove_jButtonActionPerformed
       DefaultListModel model = (DefaultListModel) statTitlesFilter__jList.getModel();
       int[] selectedIndices = statTitlesFilter__jList.getSelectedIndices();
       try {
           model.removeRange(selectedIndices[0], selectedIndices[selectedIndices.length - 1]);
       } catch (Exception e) {
           Logger.getLogger(model.getClass().getCanonicalName()).log(Level.WARNING,
                   "While trying to remove statistics title from jList: \n\t thrown {0}\n\t msg: {1}",
                   new Object[]{
                       e.getClass().getCanonicalName(), e.getMessage()
                   });
       }
   }//GEN-LAST:event_statTitlesRemove_jButtonActionPerformed

   private void statTitleAdd__jtextfiledActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_statTitleAdd__jtextfiledActionPerformed
       // TODO add your handling code here:
   }//GEN-LAST:event_statTitleAdd__jtextfiledActionPerformed

   private void statTitlesAdd_jButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_statTitlesAdd_jButtonActionPerformed
       DefaultListModel model = (DefaultListModel) statTitlesFilter__jList.getModel();
       String addTitle = statTitleAdd__jtextfiled.getText();
       model.addElement(addTitle);
   }//GEN-LAST:event_statTitlesAdd_jButtonActionPerformed

   private void ignoredParamAdd_jButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ignoredParamAdd_jButtonActionPerformed
       DefaultListModel model = (DefaultListModel) ignoredParams__jList.getModel();
       String addTitle = ignoredParamAdd__jtextfiled.getText();
       model.addElement(addTitle);
   }//GEN-LAST:event_ignoredParamAdd_jButtonActionPerformed

   private void ignoredParamAdd__jtextfiledActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ignoredParamAdd__jtextfiledActionPerformed
       // TODO add your handling code here:
   }//GEN-LAST:event_ignoredParamAdd__jtextfiledActionPerformed

   private void ignoredParamRemove_jButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ignoredParamRemove_jButtonActionPerformed
       DefaultListModel model = (DefaultListModel) ignoredParams__jList.getModel();
       int[] selectedIndices = ignoredParams__jList.getSelectedIndices();
       try {
           model.removeRange(selectedIndices[0], selectedIndices[selectedIndices.length - 1]);
       } catch (Exception e) {
           Logger.getLogger(model.getClass().getCanonicalName()).log(Level.WARNING, "While trying to remove ignored parameters from jList: \n\t thrown {0}\n\t msg: {1}",
                   new Object[]{
                       e.getClass().getCanonicalName(), e.getMessage()
                   });
       }
   }//GEN-LAST:event_ignoredParamRemove_jButtonActionPerformed

   private void transientStatJRadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_transientStatJRadActionPerformed
       skipUntilTimeTextField.setEnabled(true);
       skipUntilTimeLbl.setEnabled(true);
   }//GEN-LAST:event_transientStatJRadActionPerformed

   private void aggrStatJRadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aggrStatJRadActionPerformed
       skipUntilTimeTextField.setEnabled(false);
       skipUntilTimeLbl.setEnabled(false);
   }//GEN-LAST:event_aggrStatJRadActionPerformed

   private void mean__jToggleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mean__jToggleActionPerformed
       // TODO add your handling code here:
   }//GEN-LAST:event_mean__jToggleActionPerformed

   private void stdev__jToggleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stdev__jToggleActionPerformed
       // TODO add your handling code here:
   }//GEN-LAST:event_stdev__jToggleActionPerformed

   private void conf_95__jToggleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_conf_95__jToggleActionPerformed
       // TODO add your handling code here:
   }//GEN-LAST:event_conf_95__jToggleActionPerformed

   private void conf_aks__jToggleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_conf_aks__jToggleActionPerformed
       // TODO add your handling code here:
   }//GEN-LAST:event_conf_aks__jToggleActionPerformed

   private void conf_none__jToggleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_conf_none__jToggleActionPerformed
       // TODO add your handling code here:
   }//GEN-LAST:event_conf_none__jToggleActionPerformed

   private void autoOutput__jToggleButActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_autoOutput__jToggleButActionPerformed
       // TODO add your handling code here:
   }//GEN-LAST:event_autoOutput__jToggleButActionPerformed

   private void autoOpenToggleButActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_autoOpenToggleButActionPerformed
       // TODO add your handling code here:
   }//GEN-LAST:event_autoOpenToggleButActionPerformed

    private void skipUntilTimeTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_skipUntilTimeTextFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_skipUntilTimeTextFieldActionPerformed

    private void aggrtransientStatJRadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aggrtransientStatJRadActionPerformed
        skipUntilTimeTextField.setEnabled(true);
        skipUntilTimeLbl.setEnabled(true);
    }//GEN-LAST:event_aggrtransientStatJRadActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton aggrRpts__jBut;
    private javax.swing.JPanel aggrRpts_pnl;
    public final javax.swing.JRadioButton aggrStatJRad = new javax.swing.JRadioButton();
    public final javax.swing.JRadioButton aggrtransientStatJRad = new javax.swing.JRadioButton();
    private javax.swing.JToggleButton autoOpenToggleBut;
    private javax.swing.JToggleButton autoOutput__jToggleBut;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.ButtonGroup buttonGroup2;
    private javax.swing.JToggleButton conf_95__jToggle;
    private javax.swing.JToggleButton conf_aks__jToggle;
    private javax.swing.JToggleButton conf_none__jToggle;
    private javax.swing.JPanel fileTypes_pnl;
    private javax.swing.JTextField ignoredParamAdd__jtextfiled;
    private javax.swing.JButton ignoredParamAdd_jButton;
    private javax.swing.JButton ignoredParamRemove_jButton;
    private javax.swing.JList ignoredParams__jList;
    private javax.swing.JPanel iterativeMUTitlesPanel;
    private javax.swing.JPanel iterativeMUTitlesPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JToggleButton mean__jToggle;
    private javax.swing.JLabel skipUntilTimeLbl;
    private javax.swing.JTextField skipUntilTimeTextField;
    private javax.swing.JTextField statTitleAdd__jtextfiled;
    private javax.swing.JButton statTitlesAdd_jButton;
    private javax.swing.JList statTitlesFilter__jList;
    private javax.swing.JButton statTitlesRemove_jButton;
    private javax.swing.JToggleButton stdev__jToggle;
    public final javax.swing.JRadioButton transientStatJRad = new javax.swing.JRadioButton();
    // End of variables declaration//GEN-END:variables
}
