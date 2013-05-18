package gui;

import control.DefaultController;
import control.DefaultController.NF;
import control.FDISApp;
import fd.FDep;
import fd.Relation;
import java.awt.Color;
import java.awt.Desktop;
import java.beans.PropertyChangeEvent;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jdesktop.application.Action;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.FrameView;
import org.jdesktop.application.Task;
import org.jdesktop.application.TaskMonitor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.swing.DefaultListModel;
import javax.swing.Timer;
import javax.swing.Icon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

/**
 * The application's main frame.
 * 
 * @author Julian Timpner <j.timpner@tu-bs.de>
 * @version 1.4
 */
public class FDISView extends FrameView implements IView {

    /** The MVC controller. */
    private DefaultController controller = null;
    /** The global <code>logger</code> object. */
    private static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    /** Constructor specifying the owning application and a MVC controller. */
    public FDISView(SingleFrameApplication app, DefaultController controller) {
        super(app);

        this.controller = controller;
        controller.addView(this);

        initComponents();

        enableDisplay(false);

        /* 
         * status bar initialization - message timeout, idle icon and
         * busy animation, etc
         */
        ResourceMap resourceMap = getResourceMap();
        int messageTimeout = resourceMap.getInteger("StatusBar.messageTimeout");
        messageTimer = new Timer(messageTimeout, new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                statusMessageLabel.setText("");
            }
        });
        messageTimer.setRepeats(false);
        int busyAnimationRate =
                resourceMap.getInteger("StatusBar.busyAnimationRate");
        for (int i = 0; i < busyIcons.length; i++) {
            busyIcons[i] = resourceMap.getIcon("StatusBar.busyIcons[" + i + "]");
        }
        busyIconTimer = new Timer(busyAnimationRate, new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                busyIconIndex = (busyIconIndex + 1) % busyIcons.length;
                statusAnimationLabel.setIcon(busyIcons[busyIconIndex]);
            }
        });
        idleIcon = resourceMap.getIcon("StatusBar.idleIcon");
        statusAnimationLabel.setIcon(idleIcon);
        progressBar.setVisible(false);

        // connecting action tasks to status bar via TaskMonitor
        TaskMonitor taskMonitor = new TaskMonitor(getApplication().getContext());
        taskMonitor.addPropertyChangeListener(new java.beans.PropertyChangeListener() {

            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                String propertyName = evt.getPropertyName();
                if ("started".equals(propertyName)) {
                    if (!busyIconTimer.isRunning()) {
                        statusAnimationLabel.setIcon(busyIcons[0]);
                        busyIconIndex = 0;
                        busyIconTimer.start();
                    }
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(true);
                } else if ("done".equals(propertyName)) {
                    busyIconTimer.stop();
                    statusAnimationLabel.setIcon(idleIcon);
                    progressBar.setVisible(false);
                    progressBar.setValue(0);
                } else if ("message".equals(propertyName)) {
                    String text = (String) (evt.getNewValue());
                    statusMessageLabel.setText((text == null) ? "" : text);
                    messageTimer.restart();
                } else if ("progress".equals(propertyName)) {
                    int value = (Integer) (evt.getNewValue());
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(value);
                }
            }
        });
    }

    /**
     * Opens a modal frame with about information.
     */
    @Action
    public void showAboutBox() {
        if (aboutBox == null) {
            JFrame mainFrame = FDISApp.getApplication().getMainFrame();
            aboutBox = new AboutBox(mainFrame);
            aboutBox.setLocationRelativeTo(mainFrame);
        }
        FDISApp.getApplication().show(aboutBox);
    }

    /**
     * Opens a modal frame for editing and connecting to a database server.
     */
    @Action
    public void showServerDialog() {
        if (serverDialog == null) {
            JFrame mainFrame = FDISApp.getApplication().getMainFrame();
            serverDialog = new ServerDialog(mainFrame, controller);
            serverDialog.setLocationRelativeTo(mainFrame);
        }
        FDISApp.getApplication().show(serverDialog);
    }

    /**
     * Opens a modal frame for previewing the results of a specified
     * normalization algorithm.
     */
    @Action
    public void showPreviewDialog() {
        /*
         * Create an dialog for displaying a normalization showPreviewDialog
         * and register it as a view to the controller.
         */
        JFrame mainFrame = FDISApp.getApplication().getMainFrame();

        if (showSteps.isSelected()) {

            nfDialog = new NfDialogConsole(mainFrame, controller);
            nfDialog.setLocationRelativeTo(mainFrame);

            controller.addView((NfDialogConsole) nfDialog);
        } else {
            nfDialog = new NfDialog(mainFrame, controller);
            nfDialog.setLocationRelativeTo(mainFrame);

            controller.addView((NfDialog) nfDialog);
        }

        FDISApp.getApplication().show(nfDialog);

        if (radio2NF.isSelected()) {
            controller.normalize(NF.NF2);
        } else if (radio3NF.isSelected()) {
            controller.normalize(NF.NF3);
        }
    }

    /**
     * This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        mainPanel = new javax.swing.JPanel();
        toolBar = new javax.swing.JToolBar();
        openButton = new javax.swing.JButton();
        committButton = new javax.swing.JButton();
        rollbackButton = new javax.swing.JButton();
        rightPanel = new javax.swing.JPanel();
        manipulationPanel = new javax.swing.JPanel();
        lhsLabel = new javax.swing.JLabel();
        rhsLabel = new javax.swing.JLabel();
        rhsScrollPane = new javax.swing.JScrollPane();
        rhsList = new javax.swing.JList();
        lhsScrollPane = new javax.swing.JScrollPane();
        lhsList = new javax.swing.JList();
        addButton = new javax.swing.JButton();
        clearButton = new javax.swing.JButton();
        removeButton = new javax.swing.JButton();
        nfPanel = new javax.swing.JPanel();
        nfLabel = new javax.swing.JLabel();
        normalizationPanel = new javax.swing.JPanel();
        radio2NF = new javax.swing.JRadioButton();
        radio3NF = new javax.swing.JRadioButton();
        prevButton = new javax.swing.JButton();
        showSteps = new javax.swing.JCheckBox();
        nfPrevPanel = new javax.swing.JPanel();
        nfPrevLabel = new javax.swing.JLabel();
        legendPanel = new javax.swing.JPanel();
        greenLabel = new javax.swing.JLabel();
        greenTextLabel = new javax.swing.JLabel();
        redLabel = new javax.swing.JLabel();
        redTextLabel = new javax.swing.JLabel();
        splitPane = new javax.swing.JSplitPane();
        tableScrollPane = new javax.swing.JScrollPane();
        fdTable = new FDTable();
        treeScrollPane = new javax.swing.JScrollPane();
        schemaTree = new javax.swing.JTree();
        menuBar = new javax.swing.JMenuBar();
        javax.swing.JMenu fileMenu = new javax.swing.JMenu();
        connectMenuItem = new javax.swing.JMenuItem();
        menuSeparator3 = new javax.swing.JSeparator();
        closeMenuItem = new javax.swing.JMenuItem();
        dropCatalogMenuItem = new javax.swing.JMenuItem();
        menuSeparator4 = new javax.swing.JSeparator();
        javax.swing.JMenuItem exitMenuItem = new javax.swing.JMenuItem();
        normalizeMenu = new javax.swing.JMenu();
        commitMenuItem = new javax.swing.JMenuItem();
        rollbackMenuItem = new javax.swing.JMenuItem();
        menuSeparator1 = new javax.swing.JSeparator();
        normalizeMenuItem = new javax.swing.JMenu();
        nf2MenuItem = new javax.swing.JMenuItem();
        nf3MenuItem = new javax.swing.JMenuItem();
        setIntraFKMenuItem = new javax.swing.JCheckBoxMenuItem();
        menuSeparator2 = new javax.swing.JSeparator();
        removeMenuItem = new javax.swing.JMenuItem();
        javax.swing.JMenu helpMenu = new javax.swing.JMenu();
        onlineHelpMenuItem = new javax.swing.JMenuItem();
        javax.swing.JMenuItem aboutMenuItem = new javax.swing.JMenuItem();
        statusPanel = new javax.swing.JPanel();
        javax.swing.JSeparator statusPanelSeparator = new javax.swing.JSeparator();
        statusMessageLabel = new javax.swing.JLabel();
        statusAnimationLabel = new javax.swing.JLabel();
        progressBar = new javax.swing.JProgressBar();
        buttonGroup = new javax.swing.ButtonGroup();

        mainPanel.setName("mainPanel"); // NOI18N

        toolBar.setRollover(true);
        toolBar.setName("toolBar"); // NOI18N

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(control.FDISApp.class).getContext().getActionMap(FDISView.class, this);
        openButton.setAction(actionMap.get("showServerDialog")); // NOI18N
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(control.FDISApp.class).getContext().getResourceMap(FDISView.class);
        openButton.setText(resourceMap.getString("openButton.text")); // NOI18N
        openButton.setFocusable(false);
        openButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        openButton.setName("openButton"); // NOI18N
        openButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        toolBar.add(openButton);

        committButton.setAction(actionMap.get("commit")); // NOI18N
        committButton.setText(resourceMap.getString("committButton.text")); // NOI18N
        committButton.setFocusable(false);
        committButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        committButton.setName("committButton"); // NOI18N
        committButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        toolBar.add(committButton);

        rollbackButton.setAction(actionMap.get("rollback")); // NOI18N
        rollbackButton.setText(resourceMap.getString("rollbackButton.text")); // NOI18N
        rollbackButton.setFocusable(false);
        rollbackButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        rollbackButton.setName("rollbackButton"); // NOI18N
        rollbackButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        toolBar.add(rollbackButton);

        rightPanel.setMaximumSize(new java.awt.Dimension(344, 400));
        rightPanel.setName("rightPanel"); // NOI18N

        manipulationPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("manipulationPanel.border.title"))); // NOI18N
        manipulationPanel.setName("manipulationPanel"); // NOI18N

        lhsLabel.setText(resourceMap.getString("lhsLabel.text")); // NOI18N
        lhsLabel.setName("lhsLabel"); // NOI18N

        rhsLabel.setText(resourceMap.getString("rhsLabel.text")); // NOI18N
        rhsLabel.setName("rhsLabel"); // NOI18N

        rhsScrollPane.setName("rhsScrollPane"); // NOI18N

        rhsList.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "None" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        rhsList.setEnabled(false);
        rhsList.setName("rhsList"); // NOI18N
        rhsScrollPane.setViewportView(rhsList);

        lhsScrollPane.setName("lhsScrollPane"); // NOI18N

        lhsList.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "None" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        lhsList.setEnabled(false);
        lhsList.setName("lhsList"); // NOI18N
        lhsScrollPane.setViewportView(lhsList);

        addButton.setAction(actionMap.get("addFDep")); // NOI18N
        addButton.setText(resourceMap.getString("addButton.text")); // NOI18N
        addButton.setToolTipText(resourceMap.getString("addButton.toolTipText")); // NOI18N
        addButton.setName("addButton"); // NOI18N

        clearButton.setAction(actionMap.get("clearSelection")); // NOI18N
        clearButton.setText(resourceMap.getString("clearButton.text")); // NOI18N
        clearButton.setToolTipText(resourceMap.getString("clearButton.toolTipText")); // NOI18N
        clearButton.setName("clearButton"); // NOI18N

        removeButton.setAction(actionMap.get("removeFDep")); // NOI18N
        removeButton.setText(resourceMap.getString("removeButton.text")); // NOI18N
        removeButton.setToolTipText(resourceMap.getString("removeButton.toolTipText")); // NOI18N
        removeButton.setName("removeButton"); // NOI18N

        javax.swing.GroupLayout manipulationPanelLayout = new javax.swing.GroupLayout(manipulationPanel);
        manipulationPanel.setLayout(manipulationPanelLayout);
        manipulationPanelLayout.setHorizontalGroup(
            manipulationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(manipulationPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(manipulationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(manipulationPanelLayout.createSequentialGroup()
                        .addGroup(manipulationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lhsLabel)
                            .addComponent(lhsScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 104, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(addButton))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(manipulationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(clearButton)
                            .addComponent(rhsLabel)
                            .addComponent(rhsScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 104, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(removeButton))
                .addContainerGap(20, Short.MAX_VALUE))
        );

        manipulationPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {lhsScrollPane, rhsScrollPane});

        manipulationPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {addButton, clearButton, removeButton});

        manipulationPanelLayout.setVerticalGroup(
            manipulationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(manipulationPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(manipulationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lhsLabel)
                    .addComponent(rhsLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(manipulationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lhsScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(rhsScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(manipulationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(clearButton)
                    .addComponent(addButton))
                .addGap(18, 18, 18)
                .addComponent(removeButton))
        );

        nfPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("nfPanel.border.title"))); // NOI18N
        nfPanel.setName("nfPanel"); // NOI18N

        nfLabel.setFont(resourceMap.getFont("nfLabel.font")); // NOI18N
        nfLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        nfLabel.setText(resourceMap.getString("nfLabel.text")); // NOI18N
        nfLabel.setName("nfLabel"); // NOI18N

        javax.swing.GroupLayout nfPanelLayout = new javax.swing.GroupLayout(nfPanel);
        nfPanel.setLayout(nfPanelLayout);
        nfPanelLayout.setHorizontalGroup(
            nfPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(nfPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(nfLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 105, Short.MAX_VALUE)
                .addContainerGap())
        );
        nfPanelLayout.setVerticalGroup(
            nfPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(nfPanelLayout.createSequentialGroup()
                .addComponent(nfLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 34, Short.MAX_VALUE)
                .addContainerGap())
        );

        normalizationPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("normalizationPanel.border.title"))); // NOI18N
        normalizationPanel.setName("normalizationPanel"); // NOI18N

        buttonGroup.add(radio2NF);
        radio2NF.setSelected(true);
        radio2NF.setText(resourceMap.getString("radio2NF.text")); // NOI18N
        radio2NF.setName("radio2NF"); // NOI18N

        buttonGroup.add(radio3NF);
        radio3NF.setText(resourceMap.getString("radio3NF.text")); // NOI18N
        radio3NF.setName("radio3NF"); // NOI18N

        prevButton.setAction(actionMap.get("showPreviewDialog")); // NOI18N
        prevButton.setText(resourceMap.getString("prevButton.text")); // NOI18N
        prevButton.setToolTipText(resourceMap.getString("prevButton.toolTipText")); // NOI18N
        prevButton.setName("prevButton"); // NOI18N

        showSteps.setText(resourceMap.getString("showSteps.text")); // NOI18N
        showSteps.setToolTipText(resourceMap.getString("showSteps.toolTipText")); // NOI18N
        showSteps.setName("showSteps"); // NOI18N

        javax.swing.GroupLayout normalizationPanelLayout = new javax.swing.GroupLayout(normalizationPanel);
        normalizationPanel.setLayout(normalizationPanelLayout);
        normalizationPanelLayout.setHorizontalGroup(
            normalizationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(normalizationPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(normalizationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(radio2NF)
                    .addComponent(radio3NF)
                    .addComponent(showSteps)
                    .addComponent(prevButton))
                .addContainerGap(20, Short.MAX_VALUE))
        );
        normalizationPanelLayout.setVerticalGroup(
            normalizationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(normalizationPanelLayout.createSequentialGroup()
                .addComponent(radio2NF)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(radio3NF)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(showSteps)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(prevButton)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        nfPrevPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("nfPrevPanel.border.title"))); // NOI18N
        nfPrevPanel.setName("nfPrevPanel"); // NOI18N

        nfPrevLabel.setFont(resourceMap.getFont("nfPrevLabel.font")); // NOI18N
        nfPrevLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        nfPrevLabel.setText(resourceMap.getString("nfPrevLabel.text")); // NOI18N
        nfPrevLabel.setName("nfPrevLabel"); // NOI18N

        javax.swing.GroupLayout nfPrevPanelLayout = new javax.swing.GroupLayout(nfPrevPanel);
        nfPrevPanel.setLayout(nfPrevPanelLayout);
        nfPrevPanelLayout.setHorizontalGroup(
            nfPrevPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(nfPrevPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(nfPrevLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 105, Short.MAX_VALUE)
                .addContainerGap())
        );
        nfPrevPanelLayout.setVerticalGroup(
            nfPrevPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(nfPrevPanelLayout.createSequentialGroup()
                .addComponent(nfPrevLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 16, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout rightPanelLayout = new javax.swing.GroupLayout(rightPanel);
        rightPanel.setLayout(rightPanelLayout);
        rightPanelLayout.setHorizontalGroup(
            rightPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, rightPanelLayout.createSequentialGroup()
                .addGroup(rightPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(nfPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(nfPrevPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(normalizationPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addComponent(manipulationPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        rightPanelLayout.setVerticalGroup(
            rightPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(rightPanelLayout.createSequentialGroup()
                .addGroup(rightPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(rightPanelLayout.createSequentialGroup()
                        .addComponent(nfPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(nfPrevPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(normalizationPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(manipulationPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(73, 73, 73))
        );

        legendPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("legendPanel.border.title"))); // NOI18N
        legendPanel.setName("legendPanel"); // NOI18N

        greenLabel.setBackground(resourceMap.getColor("greenLabel.background")); // NOI18N
        greenLabel.setText(resourceMap.getString("greenLabel.text")); // NOI18N
        greenLabel.setName("greenLabel"); // NOI18N
        greenLabel.setOpaque(true);

        greenTextLabel.setText(resourceMap.getString("greenTextLabel.text")); // NOI18N
        greenTextLabel.setName("greenTextLabel"); // NOI18N

        redLabel.setBackground(resourceMap.getColor("redLabel.background")); // NOI18N
        redLabel.setName("redLabel"); // NOI18N
        redLabel.setOpaque(true);

        redTextLabel.setText(resourceMap.getString("redTextLabel.text")); // NOI18N
        redTextLabel.setName("redTextLabel"); // NOI18N

        javax.swing.GroupLayout legendPanelLayout = new javax.swing.GroupLayout(legendPanel);
        legendPanel.setLayout(legendPanelLayout);
        legendPanelLayout.setHorizontalGroup(
            legendPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(legendPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(legendPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(legendPanelLayout.createSequentialGroup()
                        .addComponent(greenLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(greenTextLabel))
                    .addGroup(legendPanelLayout.createSequentialGroup()
                        .addComponent(redLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(redTextLabel)))
                .addContainerGap(124, Short.MAX_VALUE))
        );
        legendPanelLayout.setVerticalGroup(
            legendPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(legendPanelLayout.createSequentialGroup()
                .addComponent(greenLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 14, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(redLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 14, Short.MAX_VALUE))
            .addGroup(legendPanelLayout.createSequentialGroup()
                .addComponent(greenTextLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(redTextLabel))
        );

        splitPane.setName("splitPane"); // NOI18N

        tableScrollPane.setName("tableScrollPane"); // NOI18N

        fdTable.setModel(new FDTableModel());
        ((FDTable)fdTable).setColumnControlVisible(true);
        ((FDTable) fdTable).clearAllColor();
        ((FDTable) fdTable).getColumnExt("ID").setVisible(false);
        ((FDTable) fdTable).getColumnExt("Key").setVisible(false);
        ((FDTable) fdTable).setSortable(false);
        fdTable.setName("fdTable");
        fdTable.getTableHeader().setReorderingAllowed(false);
        tableScrollPane.setViewportView(fdTable);

        splitPane.setRightComponent(tableScrollPane);

        treeScrollPane.setName("treeScrollPane"); // NOI18N

        schemaTree.setName("schemaTree"); // NOI18N
        schemaTree.addTreeSelectionListener(new javax.swing.event.TreeSelectionListener() {
            public void valueChanged(javax.swing.event.TreeSelectionEvent evt) {
                schemaTreeValueChanged(evt);
            }
        });
        treeScrollPane.setViewportView(schemaTree);
        DefaultTreeCellRenderer renderer = new SchemaTreeCellRenderer();
        schemaTree.setCellRenderer(renderer);
        DefaultTreeModel model = (DefaultTreeModel) schemaTree.getModel();
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("No Connection");
        model.setRoot(rootNode);
        model.reload();

        schemaTree.setEnabled(false);
        schemaTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

        splitPane.setLeftComponent(treeScrollPane);

        javax.swing.GroupLayout mainPanelLayout = new javax.swing.GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, mainPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(splitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 577, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(legendPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(rightPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
            .addComponent(toolBar, javax.swing.GroupLayout.DEFAULT_SIZE, 859, Short.MAX_VALUE)
        );
        mainPanelLayout.setVerticalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainPanelLayout.createSequentialGroup()
                .addComponent(toolBar, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(mainPanelLayout.createSequentialGroup()
                        .addComponent(rightPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(legendPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(splitPane, javax.swing.GroupLayout.PREFERRED_SIZE, 536, Short.MAX_VALUE))
                .addContainerGap())
        );

        menuBar.setName("menuBar"); // NOI18N

        fileMenu.setText(resourceMap.getString("fileMenu.text")); // NOI18N
        fileMenu.setName("fileMenu"); // NOI18N
        fileMenu.setMnemonic(KeyEvent.VK_D);

        connectMenuItem.setAction(actionMap.get("showServerDialog")); // NOI18N
        connectMenuItem.setText(resourceMap.getString("connectMenuItem.text")); // NOI18N
        connectMenuItem.setName("connectMenuItem"); // NOI18N
        fileMenu.add(connectMenuItem);

        menuSeparator3.setName("menuSeparator3"); // NOI18N
        fileMenu.add(menuSeparator3);

        closeMenuItem.setAction(actionMap.get("close")); // NOI18N
        closeMenuItem.setText(resourceMap.getString("closeMenuItem.text")); // NOI18N
        closeMenuItem.setEnabled(false);
        closeMenuItem.setName("closeMenuItem"); // NOI18N
        fileMenu.add(closeMenuItem);

        dropCatalogMenuItem.setAction(actionMap.get("dropCatalog")); // NOI18N
        dropCatalogMenuItem.setEnabled(false);
        dropCatalogMenuItem.setName("dropCatalogMenuItem"); // NOI18N
        fileMenu.add(dropCatalogMenuItem);

        menuSeparator4.setName("menuSeparator4"); // NOI18N
        fileMenu.add(menuSeparator4);

        exitMenuItem.setAction(actionMap.get("quit")); // NOI18N
        exitMenuItem.setText(resourceMap.getString("exitMenuItem.text")); // NOI18N
        exitMenuItem.setName("exitMenuItem"); // NOI18N
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        normalizeMenu.setText(resourceMap.getString("normalizeMenu.text")); // NOI18N
        normalizeMenu.setName("normalizeMenu"); // NOI18N

        commitMenuItem.setAction(actionMap.get("commit")); // NOI18N
        commitMenuItem.setText(resourceMap.getString("commitMenuItem.text")); // NOI18N
        commitMenuItem.setName("commitMenuItem"); // NOI18N
        normalizeMenu.add(commitMenuItem);

        rollbackMenuItem.setAction(actionMap.get("rollback")); // NOI18N
        rollbackMenuItem.setText(resourceMap.getString("rollbackMenuItem.text")); // NOI18N
        rollbackMenuItem.setName("rollbackMenuItem"); // NOI18N
        normalizeMenu.add(rollbackMenuItem);

        menuSeparator1.setName("menuSeparator1"); // NOI18N
        normalizeMenu.add(menuSeparator1);

        normalizeMenuItem.setText(resourceMap.getString("normalizeMenuItem.text")); // NOI18N
        normalizeMenuItem.setName("normalizeMenuItem"); // NOI18N

        nf2MenuItem.setAction(actionMap.get("normalize2NF")); // NOI18N
        nf2MenuItem.setText(resourceMap.getString("nf2MenuItem.text")); // NOI18N
        nf2MenuItem.setName("nf2MenuItem"); // NOI18N
        normalizeMenuItem.add(nf2MenuItem);

        nf3MenuItem.setAction(actionMap.get("normalize3NF")); // NOI18N
        nf3MenuItem.setText(resourceMap.getString("nf3MenuItem.text")); // NOI18N
        nf3MenuItem.setName("nf3MenuItem"); // NOI18N
        normalizeMenuItem.add(nf3MenuItem);

        normalizeMenu.add(normalizeMenuItem);

        setIntraFKMenuItem.setAction(actionMap.get("setIntraFK")); // NOI18N
        setIntraFKMenuItem.setSelected(true);
        setIntraFKMenuItem.setName("setIntraFKMenuItem"); // NOI18N
        normalizeMenu.add(setIntraFKMenuItem);

        menuSeparator2.setName("menuSeparator2"); // NOI18N
        normalizeMenu.add(menuSeparator2);

        removeMenuItem.setAction(actionMap.get("removeFDep")); // NOI18N
        removeMenuItem.setText(resourceMap.getString("removeMenuItem.text")); // NOI18N
        removeMenuItem.setName("removeMenuItem"); // NOI18N
        normalizeMenu.add(removeMenuItem);

        menuBar.add(normalizeMenu);

        helpMenu.setAction(actionMap.get("browseOnline")); // NOI18N
        helpMenu.setText(resourceMap.getString("helpMenu.text")); // NOI18N
        helpMenu.setName("helpMenu"); // NOI18N

        onlineHelpMenuItem.setAction(actionMap.get("browseOnline")); // NOI18N
        onlineHelpMenuItem.setText(resourceMap.getString("onlineHelpMenuItem.text")); // NOI18N
        onlineHelpMenuItem.setToolTipText(resourceMap.getString("onlineHelpMenuItem.toolTipText")); // NOI18N
        onlineHelpMenuItem.setName("onlineHelpMenuItem"); // NOI18N
        helpMenu.add(onlineHelpMenuItem);

        aboutMenuItem.setAction(actionMap.get("showAboutBox")); // NOI18N
        aboutMenuItem.setText(resourceMap.getString("aboutMenuItem.text")); // NOI18N
        aboutMenuItem.setName("aboutMenuItem"); // NOI18N
        helpMenu.add(aboutMenuItem);

        menuBar.add(helpMenu);

        statusPanel.setName("statusPanel"); // NOI18N

        statusMessageLabel.setName("statusMessageLabel"); // NOI18N

        statusAnimationLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        statusAnimationLabel.setName("statusAnimationLabel"); // NOI18N

        progressBar.setName("progressBar"); // NOI18N

        javax.swing.GroupLayout statusPanelLayout = new javax.swing.GroupLayout(statusPanel);
        statusPanel.setLayout(statusPanelLayout);
        statusPanelLayout.setHorizontalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(statusPanelSeparator, javax.swing.GroupLayout.DEFAULT_SIZE, 859, Short.MAX_VALUE)
            .addGroup(statusPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(statusMessageLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 689, Short.MAX_VALUE)
                .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(statusAnimationLabel)
                .addContainerGap())
        );
        statusPanelLayout.setVerticalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(statusPanelLayout.createSequentialGroup()
                .addComponent(statusPanelSeparator, javax.swing.GroupLayout.PREFERRED_SIZE, 2, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(statusMessageLabel)
                    .addComponent(statusAnimationLabel)
                    .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(3, 3, 3))
        );

        setComponent(mainPanel);
        setMenuBar(menuBar);
        setStatusBar(statusPanel);
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Handles tree value changes from the tree that displays the database 
     * schema and its relations.
     * 
     * @param evt event created by selecting another node
     */
    private void schemaTreeValueChanged(javax.swing.event.TreeSelectionEvent evt) {//GEN-FIRST:event_schemaTreeValueChanged
        // Get selected node.
        DefaultMutableTreeNode node =
                (DefaultMutableTreeNode) schemaTree.getLastSelectedPathComponent();

        /*
         * If there are changes to the selected relation...
         */
        if (controller.hasChanges()) {

            /* If nothing is selected, this is neccessary, because otherwise
             * this event handler would be called recursively by the
             * clearSelection() method below.
             */
            if (node != null) {
                JFrame mainFrame = FDISApp.getApplication().getMainFrame();
                JOptionPane.showMessageDialog(mainFrame,
                        "There are changes to the selected relation. " +
                        "Please do a commit or rollback before proceeding.",
                        "Confirm Changes",
                        JOptionPane.WARNING_MESSAGE);
            }

            schemaTree.clearSelection();

        /*
         * ...nothing has changed, thus the new selected relation can savely
         * be loaded.
         */
        } else {
            ((FDTable) fdTable).clearAllColor();

            /* If nothing is selected */
            if (node == null) {
                return;
            }

            /* Retrieves the node that was selected. */
            Object nodeInfo = node.getUserObject();

            /* Reacts to the node selection. */

            DefaultTreeModel model = (DefaultTreeModel) schemaTree.getModel();

            boolean isRoot = schemaTree.getModel().getRoot().equals(node);
            boolean isLeaf = model.isLeaf(node);

            if (!isLeaf && !isRoot) {

                Relation table = (Relation) nodeInfo;
                controller.loadRelation(table);

                // Fill lists with the relation's attributes.
                DefaultListModel listModel = new DefaultListModel();

                for (String col : table.getColumns()) {
                    listModel.addElement(col);
                }

                lhsList.setModel(listModel);
                rhsList.setModel(listModel);

                enableDisplay(true);
            } else {
                clearModels();
                enableDisplay(false);
                setCommittable(false);
                setRollbackable(false);
            }
        }
    }//GEN-LAST:event_schemaTreeValueChanged

    /**
     * Returns the enabled state of the 'commit' button.
     *
     * @return true if there are changes to the selected relation
     */
    public boolean isCommittable() {
        return committable;
    }

    /**
     * Sets the enabled state of the 'commit' button. Should be set to
     * <code>true</code>, if there are changes to the selected relation.
     * 
     * @param b enabled
     */
    public void setCommittable(boolean b) {
        boolean old = isCommittable();
        this.committable = b;
        firePropertyChange("committable", old, isCommittable());
    }

    /**
     * Returns the enabled state of the 'rollback' button.
     *
     * @return true if there are changes to the selected relation
     */
    public boolean isRollbackable() {
        return rollbackable;
    }

    /**
     * Sets the enabled state of the 'rollback' button. Should be set to
     * <code>true</code>, if there are changes to the selected relation.
     *
     * @param b enabled
     */
    public void setRollbackable(boolean b) {
        boolean old = isRollbackable();
        this.rollbackable = b;
        firePropertyChange("rollbackable", old, isRollbackable());
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addButton;
    private javax.swing.ButtonGroup buttonGroup;
    private javax.swing.JButton clearButton;
    private javax.swing.JMenuItem closeMenuItem;
    private javax.swing.JMenuItem commitMenuItem;
    private javax.swing.JButton committButton;
    private javax.swing.JMenuItem connectMenuItem;
    private javax.swing.JMenuItem dropCatalogMenuItem;
    private javax.swing.JTable fdTable;
    private javax.swing.JLabel greenLabel;
    private javax.swing.JLabel greenTextLabel;
    private javax.swing.JPanel legendPanel;
    private javax.swing.JLabel lhsLabel;
    private javax.swing.JList lhsList;
    private javax.swing.JScrollPane lhsScrollPane;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JPanel manipulationPanel;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JSeparator menuSeparator1;
    private javax.swing.JSeparator menuSeparator2;
    private javax.swing.JSeparator menuSeparator3;
    private javax.swing.JSeparator menuSeparator4;
    private javax.swing.JMenuItem nf2MenuItem;
    private javax.swing.JMenuItem nf3MenuItem;
    private javax.swing.JLabel nfLabel;
    private javax.swing.JPanel nfPanel;
    private javax.swing.JLabel nfPrevLabel;
    private javax.swing.JPanel nfPrevPanel;
    private javax.swing.JPanel normalizationPanel;
    private javax.swing.JMenu normalizeMenu;
    private javax.swing.JMenu normalizeMenuItem;
    private javax.swing.JMenuItem onlineHelpMenuItem;
    private javax.swing.JButton openButton;
    private javax.swing.JButton prevButton;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JRadioButton radio2NF;
    private javax.swing.JRadioButton radio3NF;
    private javax.swing.JLabel redLabel;
    private javax.swing.JLabel redTextLabel;
    private javax.swing.JButton removeButton;
    private javax.swing.JMenuItem removeMenuItem;
    private javax.swing.JLabel rhsLabel;
    private javax.swing.JList rhsList;
    private javax.swing.JScrollPane rhsScrollPane;
    private javax.swing.JPanel rightPanel;
    private javax.swing.JButton rollbackButton;
    private javax.swing.JMenuItem rollbackMenuItem;
    private javax.swing.JTree schemaTree;
    private javax.swing.JCheckBoxMenuItem setIntraFKMenuItem;
    private javax.swing.JCheckBox showSteps;
    private javax.swing.JSplitPane splitPane;
    private javax.swing.JLabel statusAnimationLabel;
    private javax.swing.JLabel statusMessageLabel;
    private javax.swing.JPanel statusPanel;
    private javax.swing.JScrollPane tableScrollPane;
    private javax.swing.JToolBar toolBar;
    private javax.swing.JScrollPane treeScrollPane;
    // End of variables declaration//GEN-END:variables
    private final Timer messageTimer;
    private final Timer busyIconTimer;
    private final Icon idleIcon;
    private final Icon[] busyIcons = new Icon[15];
    private int busyIconIndex = 0;
    private JDialog aboutBox;
    private JDialog serverDialog;
    private JDialog nfDialog;
    /** The enabled state of the 'commit' button. */
    private boolean committable = false;
    /** The enabled state of the 'rollback' button. */
    private boolean rollbackable = false;

    /**
     * A convenience method for setting the enabled state of serveral
     * GUI components.
     * 
     * @param enabled
     */
    private void enableDisplay(boolean enabled) {

        if (nfLabel.getText().equals("2 NF") ||
                nfLabel.getText().equals("3 NF") ||
                nfLabel.getText().equals("BCNF")) {
            radio2NF.setEnabled(false);
            radio3NF.setSelected(true);
            nf2MenuItem.setEnabled(false);
        } else {
            radio2NF.setEnabled(enabled);
            nf2MenuItem.setEnabled(true);
        }
        if (nfLabel.getText().equals("3 NF") ||
                nfLabel.getText().equals("BCNF")) {
            radio3NF.setEnabled(false);
            prevButton.setEnabled(false);
            showSteps.setEnabled(false);
            normalizeMenuItem.setEnabled(false);
        } else {
            radio3NF.setEnabled(enabled);
            prevButton.setEnabled(enabled);
            showSteps.setEnabled(enabled);
            normalizeMenuItem.setEnabled(enabled);
        }

        fdTable.setEnabled(enabled);
        lhsList.setEnabled(enabled);
        rhsList.setEnabled(enabled);
        nfLabel.setEnabled(enabled);
        nfPrevLabel.setEnabled(enabled);

        addButton.setEnabled(enabled);
        removeButton.setEnabled(enabled);
        clearButton.setEnabled(enabled);

        removeMenuItem.setEnabled(enabled);
    }

    /**
     * A convenience method for clearing the table's and the lists' data models.
     */
    private void clearModels() {
        FDTableModel model = (FDTableModel) fdTable.getModel();
        model.removeAllData();
        model.fireTableDataChanged();

        DefaultListModel listModel = new DefaultListModel();
        listModel.addElement("None");
        lhsList.setModel(listModel);
        rhsList.setModel(listModel);

        nfLabel.setText("-");
    }

    /**
     * The <code>modelPropertyChange</code> method is called by the
     * controller whenever the model reports a state change.
     *
     * @param evt an event created when a property changes.
     */
    public void modelPropertyChange(final PropertyChangeEvent evt) {

        /*
         * A new database schema is loaded in the SchemaManager. Inserts its
         * name into the tree.
         */
        if (evt.getPropertyName().equals(
                DefaultController.ELEMENT_SCHEMA_NAME_PROPERTY)) {

            String newNameValue = evt.getNewValue().toString();
            DefaultMutableTreeNode rootNode = null;

            rootNode = new DefaultMutableTreeNode(newNameValue);
            DefaultTreeModel model = (DefaultTreeModel) schemaTree.getModel();
            model.setRoot(rootNode);

            schemaTree.setModel(model);
            schemaTree.setSelectionRow(0);

            closeMenuItem.setEnabled(true);
            dropCatalogMenuItem.setEnabled(true);
        /*
         * The relations in the loaded schema have changed. Inserts all
         * relations and their attributes as nodes into the tree.
         */
        } else if (evt.getPropertyName().equals(
                DefaultController.ELEMENT_SCHEMA_RELATIONS_PROPERTY)) {
            SortedSet<Relation> newRelations =
                    (SortedSet<Relation>) evt.getNewValue();

            // Prepare Tree
            DefaultMutableTreeNode rootNode = null;
            DefaultMutableTreeNode node = null;
            DefaultMutableTreeNode childNode = null;

            DefaultTreeModel model = (DefaultTreeModel) schemaTree.getModel();
            rootNode = (DefaultMutableTreeNode) model.getRoot();

            for (Relation rel : newRelations) {
                node = new DefaultMutableTreeNode(rel);
                SortedSet<String> columns = rel.getColumns();
                for (String col : columns) {
                    childNode = new DefaultMutableTreeNode(col);
                    node.add(childNode);
                }
                rootNode.add(node);
            }

            TreePath path = new TreePath(rootNode);
            schemaTree.expandPath(path);
            schemaTree.setModel(model);
            schemaTree.setSelectionRow(0);

            schemaTree.setEnabled(true);

        /*
         * FDeps in the currently loaded relation have changed.
         */
        } else if (evt.getPropertyName().equals(
                DefaultController.ELEMENT_RELATION_FDEPS_PROPERTY)) {
            Set<FDep> fdeps = (Set<FDep>) evt.getNewValue();
            FDTableModel model = new FDTableModel();

            for (FDep fd : fdeps) {
                if (fd.getIsKey()) {
                    //model.addRow(fd.getLeftSide());
                    //model.addRow(fd.getLeftSide(), fd.getRightSide());
                    model.addRow(fd.getLeftSide(), fd.getRightSide(),
                            fd.getId(), fd.getIsKey());
                } else {
                    //model.addRow(fd.getLeftSide(), fd.getRightSide());
                    model.addRow(fd.getLeftSide(), fd.getRightSide(),
                            fd.getId(), fd.getIsKey());
                }
            }

            fdTable.setModel(model);
            ((FDTable) fdTable).clearAllColor();
            ((FDTable) fdTable).getColumnExt("ID").setVisible(false);
            ((FDTable) fdTable).getColumnExt("Key").setVisible(false);
            ((FDTable) fdTable).setSortable(false);

        /*
         * Additional FDeps have been specified. Inserts them into the
         * table and marks them.
         */
        } else if (evt.getPropertyName().equals(
                DefaultController.ELEMENT_RELATION_ADDITIONAL_FDEPS_PROPERTY)) {
            FDep fd = (FDep) evt.getNewValue();
            FDTableModel model = (FDTableModel) fdTable.getModel();

            //model.addRow(fd.getLeftSide(), fd.getRightSide());
            model.addRow(fd.getLeftSide(), fd.getRightSide(), -1, false);
            ((FDTable) fdTable).setRowColor(model.getRowCount() - 1, Color.GREEN);
            model.fireTableDataChanged();

            setRollbackable(true);
            setCommittable(true);
        /*
         * The relation's normalform showPreviewDialog (may) has changed. Update the view
         * correpondingly.
         */
        } else if (evt.getPropertyName().equals(
                DefaultController.ELEMENT_RELATION_NF_PREVIEW_PROPERTY)) {

            String nfPrev = (String) evt.getNewValue();
            nfPrevLabel.setText(nfPrev);

        /*
         * The relation's normalform (may) has changed. Update the view
         * correpondingly.
         */
        } else if (evt.getPropertyName().equals(
                DefaultController.ELEMENT_RELATION_NF_PROPERTY)) {
            String nf = (String) evt.getNewValue();

            nfLabel.setText(nf);
        }
    }

    /**
     * Adds an additional functional dependecy to the loaded relation. This
     * will not take effect on the database until a commit.
     */
    @Action
    public void addFDep() {
        boolean isSelectionEmpty = lhsList.isSelectionEmpty() ||
                rhsList.isSelectionEmpty();
        if (!isSelectionEmpty) {
            Object[] leftSelection = lhsList.getSelectedValues();
            Object[] rightSelection = rhsList.getSelectedValues();
            SortedSet<String> lhs = new TreeSet<String>();
            SortedSet<String> rhs = new TreeSet<String>();

            boolean isKey = false;
            //if (rightSelection.length == rhsList.getModel().getSize()) {
            //    isKey = true;
            //}

            List list = Arrays.asList(leftSelection);
            for (Object col : list) {
                lhs.add((String) col);
            }

            list = Arrays.asList(rightSelection);
            for (Object col : list) {
                rhs.add((String) col);
            }

            clearSelection();

            FDep fd = new FDep(isKey);
            fd.setLeftSide(lhs);
            fd.setRightSide(rhs);

            controller.addFdep(fd);
        } else {
            JOptionPane.showMessageDialog(null,
                    "Please specify both a left-hand and a right-hand side.",
                    "FD Incomplete",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * Removes a functional dependecy from the loaded relation. This
     * will not take effect on the database until a commit.
     */
    @Action
    public void removeFDep() {
        String ls = System.getProperty("line.separator");

        int[] rows = fdTable.getSelectedRows();

        if (rows.length != 0) {
            FDTableModel model = (FDTableModel) fdTable.getModel();
            Boolean isKey;
            Integer id;

            for (int i = 0; i < rows.length; i++) {
                isKey = (Boolean) model.getValueAt(rows[i], 3);
                id = (Integer) model.getValueAt(rows[i], 2);

                if (id == -1) {
                    JOptionPane.showMessageDialog(null,
                            "This FD is only temporary and cannot be removed. " +
                            ls +
                            "Please do a rollback instead.",
                            "Temporary FD",
                            JOptionPane.INFORMATION_MESSAGE);
                } else if (isKey) {
                    JOptionPane.showMessageDialog(null,
                            "This FD is implemented as a key constraint and " +
                            "cannot be removed.",
                            "FD cannot be removed",
                            JOptionPane.INFORMATION_MESSAGE);
                } else {
                    controller.removeFDep(id);
                    ((FDTable) fdTable).setRowColor(rows[i], Color.RED);

                    clearSelection();

                    setCommittable(true);
                    setRollbackable(true);
                }
            }
        } else {
            JOptionPane.showMessageDialog(null,
                    "Please select a FD in the table",
                    "Select FD",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * A convenience method for clearing table and list selections.
     */
    @Action
    public void clearSelection() {
        lhsList.clearSelection();
        rhsList.clearSelection();
        fdTable.clearSelection();
    }

    /**
     * Commits changes to the database.
     *
     * @return a background task performing the operation
     */
    @Action(block = Task.BlockingScope.APPLICATION, enabledProperty = "committable")
    public Task commit() {
        return new CommitTask(getApplication());
    }

    private class CommitTask extends org.jdesktop.application.Task<Object, Void> {

        CommitTask(org.jdesktop.application.Application app) {
            // Runs on the EDT.  Copy GUI state that
            // doInBackground() depends on from parameters
            // to CommitTask fields, here.
            super(app);
        }

        @Override
        protected Object doInBackground() {
            // Your Task's code here.  This method runs
            // on a background thread, so don't reference
            // the Swing GUI from here.
            controller.commit();
            return null;  // return your result

        }

        @Override
        protected void succeeded(Object result) {
            // Runs on the EDT.  Update the GUI based on
            // the result computed by doInBackground().
            setCommittable(false);
            clearModels();
            nfPrevLabel.setText("-");
        }
    }

    /**
     * Rolls changes back without any effect on the database.
     *
     * @return a background task performing the operation
     */
    @Action(block = Task.BlockingScope.APPLICATION, enabledProperty = "rollbackable")
    public Task rollback() {
        return new RollbackTask(getApplication());
    }

    private class RollbackTask extends org.jdesktop.application.Task<Object, Void> {

        RollbackTask(org.jdesktop.application.Application app) {
            // Runs on the EDT.  Copy GUI state that
            // doInBackground() depends on from parameters
            // to RollbackTask fields, here.
            super(app);
        }

        @Override
        protected Object doInBackground() {
            // Your Task's code here.  This method runs
            // on a background thread, so don't reference
            // the Swing GUI from here.
            controller.rollback();
            return null;  // return your result
        }

        @Override
        protected void succeeded(Object result) {
            // Runs on the EDT.  Update the GUI based on
            // the result computed by doInBackground().
            setCommittable(false);
            setRollbackable(false);
            nfPrevLabel.setText("-");
        }
    }

    @Action
    public void browseOnline() {
        Desktop desktop = null;
        // Before more Desktop API is used, first check
        // whether the API is supported by this particular
        // virtual machine (VM) on this particular host.
        if (Desktop.isDesktopSupported()) {
            desktop = Desktop.getDesktop();

            URI uri;
            try {
                uri = uri = new URI("http://www.ifis.cs.tu-bs.de");
                desktop.browse(uri);
            } catch (URISyntaxException ex) {
                logger.logp(Level.SEVERE, FDISView.class.getName(),
                        "browseOnline",
                        "Couldn't create URI from String.", ex);
            } catch (IOException ex) {
                logger.logp(Level.SEVERE, FDISView.class.getName(),
                        "browseOnline",
                        "Couldn't open browser.", ex);
            }
        }
    }

    /**
     * A convenience method for selecting 2NF and showing the preview dialog .
     */
    @Action
    public void normalize2NF() {
        radio2NF.setSelected(true);
        showPreviewDialog();
    }

    /**
     * A convenience method for selecting 3NF and showing the preview dialog .
     */
    @Action
    public void normalize3NF() {
        radio3NF.setSelected(true);
        showPreviewDialog();
    }

    /**
     * Closes the active database connection. Discards all changes to the
     * schema or its relations.
     */
    @Action
    public void close() {
        enableDisplay(false);
        controller.closeConnection();
        closeMenuItem.setEnabled(false);
        dropCatalogMenuItem.setEnabled(false);
    }

    /**
     * Drops the fd_catalog relations from the database and closes the current
     * connection.
     */
    @Action
    public void dropCatalog() {
        controller.dropCatalog();
        close();
    }

    /**
     * Determines whether internal foreign keys are generated automatically.
     */
    @Action
    public void setIntraFK() {
        controller.setIntraFkEnabled(setIntraFKMenuItem.isSelected());
    }
}
