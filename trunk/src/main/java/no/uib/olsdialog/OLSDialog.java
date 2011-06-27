package no.uib.olsdialog;

import com.jgoodies.looks.plastic.PlasticLookAndFeel;
import com.jgoodies.looks.plastic.PlasticXPLookAndFeel;
import com.jgoodies.looks.plastic.theme.SkyKrupp;
import no.uib.olsdialog.util.*;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.renderer.DefaultTableRenderer;
import uk.ac.ebi.ook.web.model.DataHolder;
import uk.ac.ebi.ook.web.services.Query;
import uk.ac.ebi.ook.web.services.QueryService;
import uk.ac.ebi.ook.web.services.QueryServiceLocator;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.xml.rpc.ServiceException;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.util.*;
import java.util.List;

/**
 * A dialog for interacting with the Ontology Lookup Service OLS 
 * (http://www.ebi.ac.uk/ontology-lookup).
 *
 * @author  Harald Barsnes
 * 
 * Created: March 2008
 * Revised: July 2009
 */
public class OLSDialog extends javax.swing.JDialog {

   /**
    * Set to true of debug output is wanted.
    */
    public static final boolean debug = false;
    private String field;
    private String selectedOntology;
    private int modifiedRow = -1;
    private OLSInputable olsInputable;
    private String mappedTerm;
    private Map<String, List<String>> preselectedOntologies;
    private Map<String,String> preselectedNames2Ids;

    /**
     * The search is only performed if a certain amount of characters are inserted.
     */
    private final int MINIMUM_WORD_LENGTH = 3;

    /**
     * The default error message used when connecting to OLS fails.
     */
    private String defaultOlsConnectionFailureErrorMessage =
            "An error occured when trying to contact the OLS. Make sure that\n" +
            "you are online. Also check your firewall (and proxy) settings.\n\n" +
            "See the Troubleshooting section at the OLS Dialog home page\n" +
            "for details: http://ols-dialog.googlecode.com.";
    /**
     * Used for term name searches.
     */
    public static final Integer OLS_DIALOG_TERM_NAME_SEARCH = 0;
    /**
     * Used for term id searches.
     */
    public static final Integer OLS_DIALOG_TERM_ID_SEARCH = 1;
    /**
     * Used for PSI modification mass searches.
     */
    public static final Integer OLS_DIALOG_PSI_MOD_MASS_SEARCH = 2;
    /**
     * Used for ontology browsing.
     */
    public static final Integer OLS_DIALOG_BROWSE_ONTOLOGY = 3;
    private static Query olsConnection;
    private TreeBrowser treeBrowser;
    private String currentlySelectedBrowseOntologyAccessionNumber = null;
    private String currentlySelectedTermNameSearchAccessionNumber = null;
    private String currentlySelectedTermIdSearchAccessionNumber = null;
    private String currentlySelectedMassSearchAccessionNumber = null;
    private String lastSelectedOntology = null;
    private final int MAX_TOOL_TIP_LENGTH = 40;

    private Map<String, String> metadata;

    /**
     * Opens a dialog that lets you search for terms using the OLS.
     * 
     * @param parent the parent JFrame
     * @param olsInputable a reference to the frame using the OLS Dialog
     * @param modal
     * @param field the name of the field to insert the results into
     * @param selectedOntology the name of the ontology to search in, e.g., "GO" or "MOD".
     * @param term the term to search for
     */
    public OLSDialog(JFrame parent, OLSInputable olsInputable, boolean modal, String field,
            String selectedOntology, String term) {
        this(parent, olsInputable, modal, field, selectedOntology, -1, term, null, null, OLS_DIALOG_TERM_NAME_SEARCH, null);
    }

    /**
     * Opens a dialog that lets you search for terms using the OLS.
     *
     * @param parent the parent JFrame
     * @param olsInputable a reference to the frame using the OLS Dialog
     * @param modal
     * @param field the name of the field to insert the results into
     * @param selectedOntology the name of the ontology to search in, e.g., "GO" or "MOD".
     * It also accepts the ontology title, e.g. "PSI Mass Spectrometry Ontology [MS]" or
     * ""PSI Mass Spectrometry Ontology [MS] / source"
     * @param term the term to search for
     * @param preselectedOntologies Default ontologies to display. Key: ontology name, e.g. "MS" or "GO".
     * Value: parent ontologies, e.g. "MS:1000458", "null" (no parent ontology preselected) 
     */
    public OLSDialog(JFrame parent, OLSInputable olsInputable, boolean modal, String field,
            String selectedOntology, String term, Map<String,List<String>> preselectedOntologies) {
        this(parent, olsInputable, modal, field, selectedOntology, -1, term, null, null, OLS_DIALOG_TERM_NAME_SEARCH, preselectedOntologies);
    }

    /**
     * Opens a dialog that lets you search for terms using the OLS.
     *
     * @param parent the parent JDialog
     * @param olsInputable a reference to the frame using the OLS Dialog
     * @param modal
     * @param field the name of the field to insert the results into
     * @param selectedOntology the name of the ontology to search in, e.g., "GO" or "MOD".
     * @param term the term to search for
     */
    public OLSDialog(JDialog parent, OLSInputable olsInputable, boolean modal, String field,
            String selectedOntology, String term) {
        this(parent, olsInputable, modal, field, selectedOntology, -1, term, null, null, OLS_DIALOG_TERM_NAME_SEARCH, null);
    }

    /**
     * Opens a dialog that lets you search for terms using the OLS.
     *
     * @param parent the parent JDialog
     * @param olsInputable a reference to the frame using the OLS Dialog
     * @param modal
     * @param field the name of the field to insert the results into
     * @param selectedOntology the name of the ontology to search in, e.g., "GO" or "MOD".
     * @param term the term to search for
     * @param preselectedOntologies Default ontologies to display. Key: ontology name, e.g. "MS" or "GO".
     * Value: parent ontologies, e.g. "MS:1000458", "null" (no parent ontology preselected)
     */
    public OLSDialog(JDialog parent, OLSInputable olsInputable, boolean modal, String field,
            String selectedOntology, String term, Map<String,List<String>> preselectedOntologies) {
        this(parent, olsInputable, modal, field, selectedOntology, -1, term, null, null, OLS_DIALOG_TERM_NAME_SEARCH, preselectedOntologies);
    }

    /**
     * Opens a dialog that lets you search for terms using the OLS.
     *
     * @param parent the parent JFrame
     * @param olsInputable a reference to the frame using the OLS Dialog
     * @param modal
     * @param field the name of the field to insert the results into
     * @param selectedOntology the name of the ontology to search in, e.g., "GO" or "MOD".
     * @param modifiedRow the row to modify, use -1 if adding a new row
     * @param term the term to search for
     */
    public OLSDialog(JFrame parent, OLSInputable olsInputable, boolean modal, String field,
            String selectedOntology, int modifiedRow, String term) {
        this(parent, olsInputable, modal, field, selectedOntology, modifiedRow, term, null, null, OLS_DIALOG_TERM_NAME_SEARCH, null);
    }

    /**
     * Opens a dialog that lets you search for terms using the OLS.
     *
     * @param parent the parent JFrame
     * @param olsInputable a reference to the frame using the OLS Dialog
     * @param modal
     * @param field the name of the field to insert the results into
     * @param selectedOntology the name of the ontology to search in, e.g., "GO" or "MOD".
     * @param modifiedRow the row to modify, use -1 if adding a new row
     * @param term the term to search for    
     * @param preselectedOntologies Default ontologies to display. Key: ontology name, e.g. "MS" or "GO".
     * Value: parent ontologies, e.g. "MS:1000458", "null" (no parent ontology preselected)
     */
        public OLSDialog(JFrame parent, OLSInputable olsInputable, boolean modal, String field,
            String selectedOntology, int modifiedRow, String term, Map<String,List<String>> preselectedOntologies) {
        this(parent, olsInputable, modal, field, selectedOntology, modifiedRow, term, null, null, OLS_DIALOG_TERM_NAME_SEARCH, preselectedOntologies);
    }

    /**
     * Opens a dialog that lets you search for terms using the OLS.
     *
     * @param parent the parent JDialog
     * @param olsInputable a reference to the frame using the OLS Dialog
     * @param modal
     * @param field the name of the field to insert the results into
     * @param selectedOntology the name of the ontology to search in, e.g., "GO" or "MOD".
     * @param modifiedRow the row to modify, use -1 if adding a new row
     * @param term the term to search for
     */
    public OLSDialog(JDialog parent, OLSInputable olsInputable, boolean modal, String field,
            String selectedOntology, int modifiedRow, String term) {
        this(parent, olsInputable, modal, field, selectedOntology, modifiedRow, term, null, null, OLS_DIALOG_TERM_NAME_SEARCH, null);
    }

    /**
     * Opens a dialog that lets you search for terms using the OLS.
     *
     * @param parent the parent JDialog
     * @param olsInputable a reference to the frame using the OLS Dialog
     * @param modal
     * @param field the name of the field to insert the results into
     * @param selectedOntology the name of the ontology to search in, e.g., "GO" or "MOD".
     * @param modifiedRow the row to modify, use -1 if adding a new row
     * @param term the term to search for
     * @param preselectedOntologies Default ontologies to display. Key: ontology name, e.g. "MS" or "GO".
     * Value: parent ontologies, e.g. "MS:1000458", "null" (no parent ontology preselected)
     */
    public OLSDialog(JDialog parent, OLSInputable olsInputable, boolean modal, String field,
            String selectedOntology, int modifiedRow, String term, Map<String,List<String>> preselectedOntologies) {
        this(parent, olsInputable, modal, field, selectedOntology, modifiedRow, term, null, null, OLS_DIALOG_TERM_NAME_SEARCH, preselectedOntologies);
    }

    /**
     * Opens a dialog that lets you search for terms using the OLS.
     * 
     * @param parent the parent JFrame
     * @param olsInputable a reference to the frame using the OLS Dialog
     * @param modal
     * @param field the name of the field to insert the results into
     * @param selectedOntology the name of the ontology to search in, e.g., "GO" or "MOD".
     * @param modifiedRow the row to modify, use -1 if adding a new row
     * @param term the term to search for
     * @param modificationMass the mass of the modification
     * @param modificationAccuracy the mass accuracy
     * @param searchType one of the following: OLS_DIALOG_TERM_NAME_SEARCH, OLS_DIALOG_TERM_ID_SEARCH,
     *                     OLS_DIALOG_BROWSE_ONTOLOGY or OLS_DIALOG_PSI_MOD_MASS_SEARCH
     */
    public OLSDialog(JFrame parent, OLSInputable olsInputable, boolean modal, String field,
            String selectedOntology, int modifiedRow, String term,
            Double modificationMass, Double modificationAccuracy, Integer searchType, Map<String,List<String>> preselectedOntologies) {
        super(parent, modal);

        this.olsInputable = olsInputable;
        this.field = field;
        this.selectedOntology = selectedOntology;
        this.modifiedRow = modifiedRow;
        this.mappedTerm = term;

        if(preselectedOntologies == null){
            this.preselectedOntologies = new HashMap<String, List<String>>();
        } else {
            this.preselectedOntologies = preselectedOntologies;
        }

        setUpFrame(searchType);

        boolean error = openOlsConnectionAndInsertOntologyNames();

        if (error) {
            this.dispose();
        } else {
            insertValues(modificationMass, modificationAccuracy, searchType);
            this.setLocationRelativeTo(parent);
            this.setVisible(true);
        }
    }
    /**
     * Opens a dialog that lets you search for terms using the OLS.
     *
     * @param parent the parent JFrame
     * @param olsInputable a reference to the frame using the OLS Dialog
     * @param modal
     * @param field the name of the field to insert the results into
     * @param selectedOntology the name of the ontology to search in, e.g., "GO" or "MOD".
     * @param modifiedRow the row to modify, use -1 if adding a new row
     * @param term the term to search for
     * @param modificationMass the mass of the modification
     * @param modificationAccuracy the mass accuracy
     * @param searchType one of the following: OLS_DIALOG_TERM_NAME_SEARCH, OLS_DIALOG_TERM_ID_SEARCH,
     *                     OLS_DIALOG_BROWSE_ONTOLOGY or OLS_DIALOG_PSI_MOD_MASS_SEARCH
     */
    public OLSDialog(JFrame parent, OLSInputable olsInputable, boolean modal, String field,
            String selectedOntology, int modifiedRow, String term,
            Double modificationMass, Double modificationAccuracy, Integer searchType) {
        this(parent, olsInputable, modal, field, selectedOntology, modifiedRow, term, modificationMass, modificationAccuracy, searchType, null);
    }

    /**
     * Opens a dialog that lets you search for terms using the OLS.
     *
     * @param parent the parent JDialog
     * @param olsInputable a reference to the frame using the OLS Dialog
     * @param modal
     * @param field the name of the field to insert the results into
     * @param selectedOntology the name of the ontology to search in, e.g., "GO" or "MOD".
     * @param modifiedRow the row to modify, use -1 if adding a new row
     * @param term the term to search for
     * @param modificationMass the mass of the modification
     * @param modificationAccuracy the mass accuracy
     * @param searchType one of the following: OLS_DIALOG_TERM_NAME_SEARCH, OLS_DIALOG_TERM_ID_SEARCH,
     *                     OLS_DIALOG_BROWSE_ONTOLOGY or OLS_DIALOG_PSI_MOD_MASS_SEARCH
     * @param preselectedOntologies Default ontologies to display. Key: ontology name, e.g. "MS" or "GO".
     * Value: parent ontologies, e.g. "MS:1000458", "null" (no parent ontology preselected)
     */
    public OLSDialog(JDialog parent, OLSInputable olsInputable, boolean modal, String field,
            String selectedOntology, int modifiedRow, String term,
            Double modificationMass, Double modificationAccuracy, Integer searchType, Map<String,List<String>> preselectedOntologies) {
        super(parent, modal);

        this.olsInputable = olsInputable;
        this.field = field;
        this.selectedOntology = selectedOntology;
        this.modifiedRow = modifiedRow;
        this.mappedTerm = term;

        if(preselectedOntologies == null){
            this.preselectedOntologies = new HashMap<String, List<String>>();
        } else {
            this.preselectedOntologies = preselectedOntologies;
        }

        setUpFrame(searchType);

        boolean error = openOlsConnectionAndInsertOntologyNames();

        if (error) {
            this.dispose();
        } else {
            insertValues(modificationMass, modificationAccuracy, searchType);
            this.setLocationRelativeTo(parent);
            this.setVisible(true);
        }
    }
    /**
     * Opens a dialog that lets you search for terms using the OLS.
     *
     * @param parent the parent JDialog
     * @param olsInputable a reference to the frame using the OLS Dialog
     * @param modal
     * @param field the name of the field to insert the results into
     * @param selectedOntology the name of the ontology to search in, e.g., "GO" or "MOD".
     * @param modifiedRow the row to modify, use -1 if adding a new row
     * @param term the term to search for
     * @param modificationMass the mass of the modification
     * @param modificationAccuracy the mass accuracy
     * @param searchType one of the following: OLS_DIALOG_TERM_NAME_SEARCH, OLS_DIALOG_TERM_ID_SEARCH,
     *                     OLS_DIALOG_BROWSE_ONTOLOGY or OLS_DIALOG_PSI_MOD_MASS_SEARCH
     */
    public OLSDialog(JDialog parent, OLSInputable olsInputable, boolean modal, String field,
            String selectedOntology, int modifiedRow, String term,
            Double modificationMass, Double modificationAccuracy, Integer searchType) {
        this(parent, olsInputable, modal, field, selectedOntology, modifiedRow, term, modificationMass, modificationAccuracy, searchType, null);
    }

    /**
     * Inserts the provided values into the corresponding fields.
     */
    private void insertValues(Double modificationMass, Double modificationAccuracy, Integer searchType) {

        if (mappedTerm != null) {
            termNameSearchJTextField.setText(mappedTerm);
            termNameSearchJTextFieldKeyReleased(null);
        }

        if (modificationAccuracy != null) {
            precisionJTextField.setText(modificationAccuracy.toString());
        }

        if (modificationMass != null) {
            modificationMassJTextField.setText(modificationMass.toString());
            modificationMassSearchJButtonActionPerformed(null);
        }

        updateBrowseOntologyView();

        if (searchType == OLS_DIALOG_TERM_NAME_SEARCH) {
            termNameSearchJTextField.requestFocus();
        } else if (searchType == OLS_DIALOG_BROWSE_ONTOLOGY) {
            //updateBrowseOntologyView();
        } else if (searchType == OLS_DIALOG_PSI_MOD_MASS_SEARCH) {
            modificationMassJTextField.requestFocus();
        }
    }

    /**
     * Includes code used by all constructors to set up the frame, e.g., handling column tooltips etc.
     */
    private void setUpFrame(Integer searchType) {
        // set up of the default font size and type
//        java.util.Enumeration keys = UIManager.getDefaults().keys();
//        while (keys.hasMoreElements()) {
//            Object key = keys.nextElement();
//            Object value = UIManager.get(key);
//            if (value instanceof javax.swing.plaf.FontUIResource) {
//                UIManager.put(key, new java.awt.Font("Tahoma", 0, 11));
//            }
//        }

        initComponents();

        setLookAndFeel();

        setTitle("Ontology Lookup Service - (ols-dialog v" + getVersion() + ")");

        // hide the mass search dummy label
        dummyLabelJLabel.setForeground(massSearchJPanel.getBackground());

        // initialize the tree browser
        treeBrowser = new TreeBrowser(this);
        browseJPanel.add(treeBrowser);

        // open the requested search type pane
        searchTypeJTabbedPane.setSelectedIndex(searchType);

        // use combobox renderer that centers the text
        ontologyJComboBox.setRenderer(new MyComboBoxRenderer(null, SwingConstants.CENTER));
        massTypeJComboBox.setRenderer(new MyComboBoxRenderer(null, SwingConstants.CENTER));

        // disable reordring of the columns
        olsResultsTermNameSearchJXTable.getTableHeader().setReorderingAllowed(false);
        olsResultsMassSearchJXTable.getTableHeader().setReorderingAllowed(false);
        olsResultsTermIdSearchJXTable.getTableHeader().setReorderingAllowed(false);
        termDetailsTermNameSearchJXTable.getTableHeader().setReorderingAllowed(false);
        termDetailsMassSearchJXTable.getTableHeader().setReorderingAllowed(false);
        termDetailsBrowseOntologyJXTable.getTableHeader().setReorderingAllowed(false);
        termDetailsTermIdSearchJXTable.getTableHeader().setReorderingAllowed(false);

        // make sure that only one row can be selected at ones
        olsResultsTermNameSearchJXTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        olsResultsMassSearchJXTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        olsResultsTermIdSearchJXTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);

        // show tooltip if content in the value column is longer than 50 characters
        termDetailsTermNameSearchJXTable.getColumn(1).setCellRenderer(new DefaultTableRenderer() {

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected,
                    boolean hasFocus,
                    int row,
                    int column) {
                setTableToolTip(table, value, row, column);
                return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            }
        });

        termDetailsMassSearchJXTable.getColumn(1).setCellRenderer(new DefaultTableRenderer() {

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected,
                    boolean hasFocus,
                    int row,
                    int column) {
                setTableToolTip(table, value, row, column);
                return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            }
        });

        termDetailsBrowseOntologyJXTable.getColumn(1).setCellRenderer(new DefaultTableRenderer() {

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected,
                    boolean hasFocus,
                    int row,
                    int column) {
                setTableToolTip(table, value, row, column);
                return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            }
        });

        // only works for Java 1.6 and newer
//        setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().
//                getResource("/no/uib/olsdialog/icons/ols_transparent_small.GIF")));

    }

    /**
     * Retrieves the version number set in the pom file.
     *
     * @return the version number of the ols-dialog
     */
    public String getVersion() {

        java.util.Properties p = new java.util.Properties();

        try {
            InputStream is = this.getClass().getClassLoader().getResourceAsStream("ols-dialog.properties");
            p.load( is );
        } catch (IOException e) {
            e.printStackTrace();
        }

        return p.getProperty("ols-dialog.version");
    }

    /**
     * Calls OLS webserver and gets root terms of an ontology
     *
     * @return Map of root terms - key is termId, value is termName. Map should not be null.
     */
    public Map<String, String> getOntologyRoots(String ontology) {
        return getOntologyRoots(ontology, null);
    }

    /**
     * Calls OLS webserver and gets root terms of an ontology from a parent term
     * @param ontology
     * @param parentTerm
     * @return
     */
    public Map<String, String> getOntologyRoots(String ontology, String parentTerm) {

        Map<String, String> retrievedValues = new HashMap<String, String>();

        try {
            Map roots;
            if(parentTerm == null){
                roots = olsConnection.getRootTerms(ontology);
            } else {
                roots = olsConnection.getTermChildren(parentTerm, ontology, 1, null);
            }

            if (roots != null) {
                retrievedValues.putAll(roots);
            }

        } catch (RemoteException e) {
            JOptionPane.showMessageDialog(
                    this,
                    defaultOlsConnectionFailureErrorMessage,
                    "OLS Connection Error", JOptionPane.ERROR_MESSAGE);
            Util.writeToErrorLog("Error when trying to access OLS: ");
            e.printStackTrace();
        }

        return retrievedValues;
    }

    /**
     * Clears the meta data section for the selected search type
     *
     * @param searchType the search type to clear the meta data for
     * @param clearSearchResults if true the search results table is cleared
     * @param clearMetaData if true the meta data is cleared
     */
    public void clearData(Integer searchType, boolean clearSearchResults, boolean clearMetaData) {

        JTextPane currentDefinitionsJTextPane = null;
        JXTable currentTermDetailsJXTable = null;
        JScrollPane currentTermDetailsJScrollPane = null;
        JXTable currentSearchResultsJXTable = null;
        JScrollPane currentSearchResultsJScrollPane = null;

        if (searchType == OLS_DIALOG_TERM_NAME_SEARCH) {
            currentlySelectedTermNameSearchAccessionNumber = null;
            currentSearchResultsJXTable = olsResultsTermNameSearchJXTable;
            currentSearchResultsJScrollPane = olsResultsTermNameSearchJScrollPane;
            currentDefinitionsJTextPane = definitionTermNameSearchJTextPane;
            currentTermDetailsJXTable = termDetailsTermNameSearchJXTable;
            currentTermDetailsJScrollPane = termDetailsTermNameSearchJScrollPane;
            viewTermHierarchyTermNameSearchJLabel.setEnabled(false);
        } else if (searchType == OLS_DIALOG_PSI_MOD_MASS_SEARCH) {
            currentlySelectedMassSearchAccessionNumber = null;
            currentSearchResultsJXTable = olsResultsMassSearchJXTable;
            currentSearchResultsJScrollPane = olsResultsMassSearchJScrollPane;
            currentDefinitionsJTextPane = definitionMassSearchJTextPane;
            currentTermDetailsJXTable = termDetailsMassSearchJXTable;
            currentTermDetailsJScrollPane = termDetailsMassSearchJScrollPane;
            viewTermHierarchyMassSearchJLabel.setEnabled(false);
        } else if (searchType == OLS_DIALOG_BROWSE_ONTOLOGY) {
            currentlySelectedBrowseOntologyAccessionNumber = null;
            currentDefinitionsJTextPane = definitionBrowseOntologyJTextPane;
            currentTermDetailsJXTable = termDetailsBrowseOntologyJXTable;
            currentTermDetailsJScrollPane = termDetailsBrowseOntologyJScrollPane;
            viewTermHierarchyBrowseOntologyJLabel.setEnabled(false);
        } else if (searchType == OLS_DIALOG_TERM_ID_SEARCH) {
            currentlySelectedTermIdSearchAccessionNumber = null;
            currentSearchResultsJXTable = olsResultsTermIdSearchJXTable;
            currentSearchResultsJScrollPane = olsResultsTermIdSearchJScrollPane;
            currentDefinitionsJTextPane = definitionTermIdSearchJTextPane;
            currentTermDetailsJXTable = termDetailsTermIdSearchJXTable;
            currentTermDetailsJScrollPane = termDetailsTermIdSearchJScrollPane;
            viewTermHierarchyTermIdSearchJLabel.setEnabled(false);
        }

        if (clearMetaData) {

            currentDefinitionsJTextPane.setText("");

            while (currentTermDetailsJXTable.getRowCount() > 0) {
                ((DefaultTableModel) currentTermDetailsJXTable.getModel()).removeRow(0);
            }

            currentTermDetailsJScrollPane.getVerticalScrollBar().setValue(0);
        }

        if (clearSearchResults) {
            if (searchType != OLS_DIALOG_BROWSE_ONTOLOGY) {

                while (currentSearchResultsJXTable.getRowCount() > 0) {
                    ((DefaultTableModel) currentSearchResultsJXTable.getModel()).removeRow(0);
                }

                currentSearchResultsJScrollPane.getVerticalScrollBar().setValue(0);
            }
        }
    }

    /**
     * Tries to load the children of a given term.
     *
     * @param parent the tree node where to load the terms
     * @param termId the term id to query on
     * @return true if the terms was loaded sucessfully, false otherwise
     */
    public boolean loadChildren(TreeNode parent, String termId) {

        if (termId == null) {
            return false;
        }

        boolean error = false;

        String ontology = getCurrentOntologyLabel();

        //get children from OLS
        Map<String, String> childTerms = null;

        try {
            childTerms = olsConnection.getTermChildren(termId, ontology, 1, null);
        } catch (RemoteException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    defaultOlsConnectionFailureErrorMessage,
                    "OLS Connection Error", JOptionPane.ERROR_MESSAGE);
            Util.writeToErrorLog("Error when trying to access OLS: ");
            ex.printStackTrace();
            error = true;
        }

        if (!error && !childTerms.isEmpty()) {

            // add the nodes to the tree
            for (String tId : childTerms.keySet()) {
                treeBrowser.addNode(tId, childTerms.get(tId));
            }

            return true;

        } else {

            if (debug) {
                System.out.println("no children returned for " + termId);
            }

            return false;
        }
    }

    /**
     * Returns the ontology label extracted from the term id.
     * 
     * @param termId the term id to extract the ontology label from
     * @return the ontology label extracted from the term id, or null if no ontology is found
     */
    private String getOntologyLabelFromTermId(String termId) {

        String ontologyLabel = null;

        if (termId.lastIndexOf(":") != -1) {
            ontologyLabel = termId.substring(0, termId.lastIndexOf(":"));
        } else if (termId.lastIndexOf("_") != -1) { // needed for EFO
            ontologyLabel = termId.substring(0, termId.lastIndexOf("_"));
        } else if (termId.equalsIgnoreCase("No Root Terms Defined!")) {
            ontologyLabel = null;
        } else {
            ontologyLabel = "NEWT";
        }

        return ontologyLabel;
    }

    /**
     * Load metadata for a given termiId.
     *
     * @param termId the term to load meta data for
     * @param searchType the search type where the meta data will be inserted
     */
    public void loadMetaData(String termId, Integer searchType) {

        JTextPane currentDefinitionsJTextPane = null;
        JXTable currentTermDetailsJXTable = null;
        JScrollPane currentTermDetailsJScrollPane = null;

        if (searchType == OLS_DIALOG_TERM_NAME_SEARCH) {
            currentDefinitionsJTextPane = definitionTermNameSearchJTextPane;
            currentTermDetailsJXTable = termDetailsTermNameSearchJXTable;
            currentTermDetailsJScrollPane = termDetailsTermNameSearchJScrollPane;
        } else if (searchType == OLS_DIALOG_PSI_MOD_MASS_SEARCH) {
            currentDefinitionsJTextPane = definitionMassSearchJTextPane;
            currentTermDetailsJXTable = termDetailsMassSearchJXTable;
            currentTermDetailsJScrollPane = termDetailsMassSearchJScrollPane;
        } else if (searchType == OLS_DIALOG_BROWSE_ONTOLOGY) {
            currentDefinitionsJTextPane = definitionBrowseOntologyJTextPane;
            currentTermDetailsJXTable = termDetailsBrowseOntologyJXTable;
            currentTermDetailsJScrollPane = termDetailsBrowseOntologyJScrollPane;
        } else if (searchType == OLS_DIALOG_TERM_ID_SEARCH) {
            currentDefinitionsJTextPane = definitionTermIdSearchJTextPane;
            currentTermDetailsJXTable = termDetailsTermIdSearchJXTable;
            currentTermDetailsJScrollPane = termDetailsTermIdSearchJScrollPane;
        }

        //clear meta data
        clearData(searchType, false, true);

        if (termId == null) {
            return;
        }

        String ontology = getOntologyLabelFromTermId(termId);

        if (searchType == OLS_DIALOG_TERM_NAME_SEARCH) {
            currentlySelectedTermNameSearchAccessionNumber = termId;
            viewTermHierarchyTermNameSearchJLabel.setEnabled(true);
        } else if (searchType == OLS_DIALOG_PSI_MOD_MASS_SEARCH) {
            currentlySelectedMassSearchAccessionNumber = termId;
            viewTermHierarchyMassSearchJLabel.setEnabled(true);
        } else if (searchType == OLS_DIALOG_BROWSE_ONTOLOGY) {
            currentlySelectedBrowseOntologyAccessionNumber = termId;
            viewTermHierarchyBrowseOntologyJLabel.setEnabled(true);
        } else if (searchType == OLS_DIALOG_TERM_ID_SEARCH) {
            currentlySelectedTermIdSearchAccessionNumber = termId;
            viewTermHierarchyTermIdSearchJLabel.setEnabled(true);
        }

        boolean error = false;

        if (ontology != null && ontology.equalsIgnoreCase("NEWT")) {
            currentDefinitionsJTextPane.setText("Retreiving 'Term Details' is disabled for NEWT.");
            currentDefinitionsJTextPane.setCaretPosition(0);
            currentTermDetailsJXTable.setEnabled(false);
            error = true;
        } else {
            currentTermDetailsJXTable.setEnabled(true);
        }

        if (!error) {

            metadata = null;
            Map<String, String> xRefs = null;

            //query OLS
            try {
                metadata = olsConnection.getTermMetadata(termId, ontology);
                xRefs = olsConnection.getTermXrefs(termId, ontology);
            } catch (RemoteException ex) {
                JOptionPane.showMessageDialog(
                        this,
                        defaultOlsConnectionFailureErrorMessage,
                        "OLS Connection Error", JOptionPane.ERROR_MESSAGE);
                Util.writeToErrorLog("Error when trying to access OLS: ");
                ex.printStackTrace();

                if (searchType == OLS_DIALOG_TERM_NAME_SEARCH) {
                    currentlySelectedTermNameSearchAccessionNumber = null;
                } else if (searchType == OLS_DIALOG_PSI_MOD_MASS_SEARCH) {
                    currentlySelectedMassSearchAccessionNumber = null;
                } else if (searchType == OLS_DIALOG_BROWSE_ONTOLOGY) {
                    currentlySelectedBrowseOntologyAccessionNumber = null;
                } else if (searchType == OLS_DIALOG_TERM_ID_SEARCH) {
                    currentlySelectedTermIdSearchAccessionNumber = termId;
                }

                error = true;
            }

            if (!error && !metadata.isEmpty()) {

                // retrieve the terms meta data and insert into the table
                // note that "definition" is handled separatly
                for (Iterator i = metadata.keySet().iterator(); i.hasNext();) {
                    String key = (String) i.next();

                    if (key != null && key.equalsIgnoreCase("definition")) {
                        currentDefinitionsJTextPane.setText("" + metadata.get(key));
                        currentDefinitionsJTextPane.setCaretPosition(0);
                    } else {
                        ((DefaultTableModel) currentTermDetailsJXTable.getModel()).addRow(
                                new Object[]{key, metadata.get(key)});
                    }
                }

                if (currentDefinitionsJTextPane.getText().equalsIgnoreCase("null")) {
                    currentDefinitionsJTextPane.setText("(no definition provided in CV term)");
                }

                // iterate the xrefs and insert them into the table
                for (Iterator i = xRefs.keySet().iterator(); i.hasNext();) {
                    String key = (String) i.next();

                    ((DefaultTableModel) currentTermDetailsJXTable.getModel()).addRow(
                            new Object[]{key, xRefs.get(key)});
                }

                // set the horizontal scroll bar to the top
                currentTermDetailsJScrollPane.getVerticalScrollBar().setValue(0);
            } else {
                if (searchType == OLS_DIALOG_TERM_NAME_SEARCH) {
                    viewTermHierarchyTermNameSearchJLabel.setEnabled(false);
                } else if (searchType == OLS_DIALOG_PSI_MOD_MASS_SEARCH) {
                    viewTermHierarchyMassSearchJLabel.setEnabled(false);
                } else if (searchType == OLS_DIALOG_BROWSE_ONTOLOGY) {
                    viewTermHierarchyBrowseOntologyJLabel.setEnabled(false);
                } else if (searchType == OLS_DIALOG_TERM_ID_SEARCH) {
                    viewTermHierarchyTermIdSearchJLabel.setEnabled(false);
                }
            }
        } else {
            if (ontology != null && ontology.equalsIgnoreCase("NEWT")) {
                if (searchType == OLS_DIALOG_TERM_NAME_SEARCH) {
                    viewTermHierarchyTermNameSearchJLabel.setEnabled(true);
                } else if (searchType == OLS_DIALOG_PSI_MOD_MASS_SEARCH) {
                    viewTermHierarchyMassSearchJLabel.setEnabled(true);
                } else if (searchType == OLS_DIALOG_BROWSE_ONTOLOGY) {
                    viewTermHierarchyBrowseOntologyJLabel.setEnabled(true);
                } else if (searchType == OLS_DIALOG_TERM_ID_SEARCH) {
                    viewTermHierarchyTermIdSearchJLabel.setEnabled(true);
                }
            } else {
                if (searchType == OLS_DIALOG_TERM_NAME_SEARCH) {
                    viewTermHierarchyTermNameSearchJLabel.setEnabled(false);
                } else if (searchType == OLS_DIALOG_PSI_MOD_MASS_SEARCH) {
                    viewTermHierarchyMassSearchJLabel.setEnabled(false);
                } else if (searchType == OLS_DIALOG_BROWSE_ONTOLOGY) {
                    viewTermHierarchyBrowseOntologyJLabel.setEnabled(false);
                } else if (searchType == OLS_DIALOG_TERM_ID_SEARCH) {
                    viewTermHierarchyTermIdSearchJLabel.setEnabled(false);
                }
            }
        }

        if (searchType == OLS_DIALOG_TERM_NAME_SEARCH) {
            insertSelectedJButton.setEnabled(currentlySelectedTermNameSearchAccessionNumber != null);
        } else if (searchType == OLS_DIALOG_PSI_MOD_MASS_SEARCH) {
            insertSelectedJButton.setEnabled(currentlySelectedMassSearchAccessionNumber != null);
        } else if (searchType == OLS_DIALOG_BROWSE_ONTOLOGY) {
            insertSelectedJButton.setEnabled(currentlySelectedBrowseOntologyAccessionNumber != null);
        } else if (searchType == OLS_DIALOG_TERM_ID_SEARCH) {
            insertSelectedJButton.setEnabled(currentlySelectedTermIdSearchAccessionNumber != null);
        }
    }

    /**
     * A helper method for setting the cell tool tips. Included in order to not have to
     * duplicate the code for each table.
     *
     * @param table
     * @param value
     * @param row
     * @param column
     */
    private void setTableToolTip(JTable table, Object value, int row, int column) {
        if (table != null) {
            if (table.getValueAt(row, column) != null) {
                if (column == 1 && table.getValueAt(row, column).toString().length() > MAX_TOOL_TIP_LENGTH) {
                    table.setToolTipText(buildToolTipText("" + value.toString(), MAX_TOOL_TIP_LENGTH));
                } else {
                    table.setToolTipText(null);
                }
            } else {
                table.setToolTipText(null);
            }
        } else {
            table.setToolTipText(null);
        }
    }

    /**
     * Creates a multiple lines tooltip based on the provided text.
     *
     * @param aToolTip the orginal one line tool tip
     * @return the multiple line tooltip as html
     */
    private String buildToolTipText(String aToolTip, int maxToolTipLength) {

        String currentToolTip = "<html>";

        int indexOfLastSpace = 0;
        String currentToolTipLine = "";
        int currentStartIndex = 0;

        for (int i = 0; i < aToolTip.length(); i++) {

            currentToolTipLine += aToolTip.substring(i, i + 1);

            if (aToolTip.substring(i, i + 1).equalsIgnoreCase(" ")) {
                indexOfLastSpace = i;
            }

            if (currentToolTipLine.length() > maxToolTipLength) {
                if (indexOfLastSpace == currentStartIndex) {
                    currentToolTip += aToolTip.substring(currentStartIndex, i + 1) + "-<br>";
                    currentStartIndex = i + 1;
                    indexOfLastSpace = i + 1;
                    currentToolTipLine = "";
                } else {
                    currentToolTip += aToolTip.substring(currentStartIndex, indexOfLastSpace) + "<br>";
                    currentStartIndex = indexOfLastSpace;
                    currentToolTipLine = "";
                    i = currentStartIndex;
                }
            }
        }

        if (currentToolTipLine.length() > 0) {
            currentToolTip += aToolTip.substring(currentStartIndex);
        }

        currentToolTip += "</html>";

        return currentToolTip;
    }

    /**
     * Sets the look and feel of the OLS Dialog.
     *
     * Note that the OLS Dialog has been created with the following look and feel
     * in mind. If using a different look and feel you might need to tweak the GUI
     * to get the best appearance.
     */
    private void setLookAndFeel() {
        try {
            PlasticLookAndFeel.setPlasticTheme(new SkyKrupp());
            UIManager.setLookAndFeel(new PlasticXPLookAndFeel());
            SwingUtilities.updateComponentTreeUI(this);
        } catch (UnsupportedLookAndFeelException e) {
            // ignore exception, i.e. use default look and feel
        }
    }

    /**
     * Opens the OLS connection and retrieves and inserts the ontology names
     * into the ontology combo box.
     * 
     * @return false if an error occured, true otherwise
     */
    public boolean openOlsConnectionAndInsertOntologyNames() {

        boolean error = false;

        Vector ontologyNamesAndKeys = new Vector();

        preselectedNames2Ids = new HashMap<String, String>();

        try {
            QueryService locator = new QueryServiceLocator();
            olsConnection = locator.getOntologyQuery();
            Map map = olsConnection.getOntologyNames();

            String temp = "";
            String tempSuffix = "";

            String ontologyToSelect = "";

            for (Iterator i = map.keySet().iterator(); i.hasNext();) {
                String key = (String) i.next();
                temp = map.get(key) + " [" + key + "]";
                if(preselectedOntologies.size() == 0){
                    ontologyNamesAndKeys.add(temp);
                } else {
                    if(preselectedOntologies.keySet().contains(key.toUpperCase())){
                        if(preselectedOntologies.get(key.toUpperCase()) == null){
                            ontologyNamesAndKeys.add(temp);
                        } else {
                            for(String ontologyTermId:preselectedOntologies.get(key.toUpperCase())){
                                String ontologyTermName = olsConnection.getTermById(ontologyTermId, key);
                                String suffix = ontologyTermName;
                                if(ontologyTermName == null){
                                    suffix = ontologyTermId;
                                } else if(ontologyTermName.length() == 0){
                                    suffix = ontologyTermId;
                                }
                                String ontologyName = temp + " / " + suffix;
                                if (selectedOntology.equalsIgnoreCase(ontologyName)) {
                                    ontologyToSelect = ontologyName;
                                }
                                ontologyNamesAndKeys.add(ontologyName);
                                preselectedNames2Ids.put(suffix,ontologyTermId);
                            }
                        }
                    }
                }

                if (selectedOntology.equalsIgnoreCase(temp) || selectedOntology.equalsIgnoreCase(key)) {
                    ontologyToSelect = temp;
                }
            }
            //check all preselected ontologies have been found in OLS
            if(preselectedOntologies.size() != 0){
                if(preselectedOntologies.size() != ontologyNamesAndKeys.size()){
                    String msg = "Warning: One or more of your preselected ontologies have not been found in OLS";
                    Util.writeToErrorLog(msg);
                }
            }

            // sort the ontologies into alphabetic ascending order
            java.util.Collections.sort(ontologyNamesAndKeys);

            ontologyNamesAndKeys.add(0, "-- Search in All Ontologies available in the OLS registry --");
            if(preselectedOntologies.size() > 1){
                ontologyNamesAndKeys.add(1, "-- Search in these preselected Ontologies --");
            }

            ontologyJComboBox.setModel(new DefaultComboBoxModel(ontologyNamesAndKeys));
            //default selected ontology. Has to be the same name shown in the menu
            ontologyJComboBox.setSelectedItem(ontologyToSelect);

            hideOrShowNewtLinks();

            lastSelectedOntology = (String) ontologyJComboBox.getSelectedItem();
        } catch (RemoteException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    defaultOlsConnectionFailureErrorMessage,
                    "Failed to Contact the OLS", JOptionPane.ERROR_MESSAGE);
            Util.writeToErrorLog("Error when trying to access OLS: ");
            ex.printStackTrace();
            error = true;
        } catch (ServiceException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    defaultOlsConnectionFailureErrorMessage,
                    "Failed to Contact the OLS", JOptionPane.ERROR_MESSAGE);
            Util.writeToErrorLog("Error when trying to access OLS: ");
            ex.printStackTrace();
            error = true;
        }

        return error;
    }

    /**
     * Makes the 'newt species tip' links visible or not visible.
     */
    private void hideOrShowNewtLinks() {

        // note: has to be done like this and not simply by disabling or
        // making invisible, as both of those options have unwanted side effects
        if (getCurrentOntologyLabel().equalsIgnoreCase("NEWT")) {
            newtSpeciesTipsTermNameSearchJLabel.setForeground(Color.BLUE);
            newtSpeciesTipsTermIdSearchJLabel.setForeground(Color.BLUE);
        } else {
            newtSpeciesTipsTermNameSearchJLabel.setForeground(termNameSearchJPanel.getBackground());
            newtSpeciesTipsTermIdSearchJLabel.setForeground(termNameSearchJPanel.getBackground());
        }
    }

    /**
     * Update the ontology tree browser with the roots of the selected ontology.
     */
    private void updateBrowseOntologyView() {

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

        // get selected ontology
        String ontology = getCurrentOntologyLabel();

        // get selected predefined ontology term for selected ontology
        String parentTermName = getCurrentOntologyTermLabel();
        String parentTermId = preselectedNames2Ids.get(parentTermName);

        // set the root to the ontology label
        if(parentTermName != null && parentTermId != null){
            treeBrowser.initialize("[" + parentTermId + "] " + parentTermName);
        } else {
            treeBrowser.initialize(ontology);
        }

        // load root terms
        Map<String, String> rootTerms = getOntologyRoots(ontology, parentTermId);

        // update the tree
        for (String termId : rootTerms.keySet()) {
            treeBrowser.addNode(termId, rootTerms.get(termId));
        }

        // not root terms found
        if (rootTerms.size() == 0) {
            treeBrowser.addNode("No Root Terms Defined!", "");
        }

        // makes sure that all second level non visible nodes are added
        treeBrowser.updateTree();

        // move the horizontal scroll bar value to the top
        treeBrowser.scrollToTop();

        currentlySelectedBrowseOntologyAccessionNumber = null;

        clearData(OLS_DIALOG_BROWSE_ONTOLOGY, true, true);

        if (debug) {
            System.out.println("updated roots");
        }

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }

    /**
     * Adds a second level of non visible nodes. Needed to be able to show folder
     * icons for the current level of nodes.
     *
     * @param termId the term id for the term to add the second level for
     * @param ontology the ontology to get the terms from
     * @param parentNode the node to add the new nodes to
     * @return true if an error occured, false otherwise
     */
    public boolean addSecondLevelOfNodes(String termId, String ontology, DefaultMutableTreeNode parentNode) {

        boolean error = false;

        try {
            // get the next level of nodes
            Map<String, String> secondLevelChildTerms = olsConnection.getTermChildren(termId, ontology, 1, null);

            // add the level of non visible nodes
            for (String tId2 : secondLevelChildTerms.keySet()) {
                treeBrowser.addNode(parentNode, tId2, secondLevelChildTerms.get(tId2), false);
            }

        } catch (RemoteException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    defaultOlsConnectionFailureErrorMessage,
                    "OLS Connection Error", JOptionPane.ERROR_MESSAGE);
            Util.writeToErrorLog("Error when trying to access OLS: ");
            ex.printStackTrace();
            error = true;
        }

        return error;
    }

    /**
     * Returns the currently selected ontology label.
     *
     * @return the currently selected ontology label
     */
    public String getCurrentOntologyLabel() {

        String ontology = ((String) ontologyJComboBox.getSelectedItem());
        //ontology = ontology.substring(ontology.lastIndexOf("[") + 1, ontology.length() - 1);
        if(ontology.lastIndexOf("[") != -1){
            ontology = ontology.substring(ontology.lastIndexOf("[") + 1, ontology.length());
        }
        if(ontology.lastIndexOf("]") != -1){
            ontology = ontology.substring(0, ontology.lastIndexOf("]"));
        }

        return ontology;
    }

    public String getCurrentOntologyTermLabel() {

        String ontologyTerm = ((String) ontologyJComboBox.getSelectedItem());
        //ontology = ontology.substring(ontology.lastIndexOf("[") + 1, ontology.length() - 1);
        if(ontologyTerm.lastIndexOf("/ ") != -1){
            ontologyTerm = ontologyTerm.substring(ontologyTerm.lastIndexOf("/ ") + 2, ontologyTerm.length());
        } else {
            ontologyTerm = null;
        }

        return ontologyTerm;
    }

    /**
     * This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        insertSelectedJButton = new javax.swing.JButton();
        cancelJButton = new javax.swing.JButton();
        helpJButton = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JSeparator();
        aboutJButton = new javax.swing.JButton();
        searchParametersJPanel = new javax.swing.JPanel();
        searchTypeJTabbedPane = new javax.swing.JTabbedPane();
        termNameSearchJPanel = new javax.swing.JPanel();
        jScrollPane4 = new JScrollPane();
        definitionTermNameSearchJTextPane = new JTextPane();
        termDetailsTermNameSearchJScrollPane = new JScrollPane();
        termDetailsTermNameSearchJXTable = new JXTable();
        searchResultsTermNameJLabel = new javax.swing.JLabel();
        selectedTermTermNameJLabel = new javax.swing.JLabel();
        olsResultsTermNameSearchJScrollPane = new JScrollPane();
        olsResultsTermNameSearchJXTable = new JXTable();
        viewTermHierarchyTermNameSearchJLabel = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        termNameJLabel = new javax.swing.JLabel();
        termNameSearchJTextField = new javax.swing.JTextField();
        newtSpeciesTipsTermNameSearchJLabel = new javax.swing.JLabel();
        numberOfTermsTermNameSearchJTextField = new javax.swing.JTextField();
        termIdSearchJPanel = new javax.swing.JPanel();
        jScrollPane6 = new JScrollPane();
        definitionTermIdSearchJTextPane = new JTextPane();
        termDetailsTermIdSearchJScrollPane = new JScrollPane();
        termDetailsTermIdSearchJXTable = new JXTable();
        jLabel11 = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        olsResultsTermIdSearchJScrollPane = new JScrollPane();
        olsResultsTermIdSearchJXTable = new JXTable();
        viewTermHierarchyTermIdSearchJLabel = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jLabel10 = new javax.swing.JLabel();
        termIdSearchJTextField = new javax.swing.JTextField();
        newtSpeciesTipsTermIdSearchJLabel = new javax.swing.JLabel();
        termIdSearchJButton = new javax.swing.JButton();
        massSearchJPanel = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jScrollPane5 = new JScrollPane();
        definitionMassSearchJTextPane = new JTextPane();
        termDetailsMassSearchJScrollPane = new JScrollPane();
        termDetailsMassSearchJXTable = new JXTable();
        olsResultsMassSearchJScrollPane = new JScrollPane();
        olsResultsMassSearchJXTable = new JXTable();
        viewTermHierarchyMassSearchJLabel = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        modificationMassJTextField = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        precisionJTextField = new javax.swing.JTextField();
        jLabel12 = new javax.swing.JLabel();
        massTypeJComboBox = new javax.swing.JComboBox();
        dummyLabelJLabel = new javax.swing.JLabel();
        modificationMassSearchJButton = new javax.swing.JButton();
        browseOntologyJPanel = new javax.swing.JPanel();
        jLabel8 = new javax.swing.JLabel();
        jScrollPane7 = new JScrollPane();
        definitionBrowseOntologyJTextPane = new JTextPane();
        termDetailsBrowseOntologyJScrollPane = new JScrollPane();
        termDetailsBrowseOntologyJXTable = new JXTable();
        browseJPanel = new javax.swing.JPanel();
        viewTermHierarchyBrowseOntologyJLabel = new javax.swing.JLabel();
        ontologyJLabel = new javax.swing.JLabel();
        ontologyJComboBox = new javax.swing.JComboBox();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(" Ontology Lookup Service - (ols-dialog v3.0)");
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        insertSelectedJButton.setText("Use Selected Term");
        insertSelectedJButton.setEnabled(false);
        insertSelectedJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                insertSelectedJButtonActionPerformed(evt);
            }
        });

        cancelJButton.setText("Cancel");
        cancelJButton.setMaximumSize(new java.awt.Dimension(121, 23));
        cancelJButton.setMinimumSize(new java.awt.Dimension(121, 23));
        cancelJButton.setPreferredSize(new java.awt.Dimension(121, 23));
        cancelJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelJButtonActionPerformed(evt);
            }
        });

        helpJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/no/uib/olsdialog/icons/help.GIF"))); // NOI18N
        helpJButton.setToolTipText("Help");
        helpJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                helpJButtonActionPerformed(evt);
            }
        });

        aboutJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/no/uib/olsdialog/icons/ols_transparent_small.GIF"))); // NOI18N
        aboutJButton.setToolTipText("About");
        aboutJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aboutJButtonActionPerformed(evt);
            }
        });

        searchParametersJPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Search Parameters"));

        searchTypeJTabbedPane.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                searchTypeJTabbedPaneStateChanged(evt);
            }
        });

        definitionTermNameSearchJTextPane.setEditable(false);
        jScrollPane4.setViewportView(definitionTermNameSearchJTextPane);

        termDetailsTermNameSearchJXTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Name", "Value"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        termDetailsTermNameSearchJXTable.setOpaque(false);
        termDetailsTermNameSearchJScrollPane.setViewportView(termDetailsTermNameSearchJXTable);

        searchResultsTermNameJLabel.setText("Search Results:");

        selectedTermTermNameJLabel.setText("Selected Term:");

        olsResultsTermNameSearchJXTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Accession", "CV Term"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        olsResultsTermNameSearchJXTable.setOpaque(false);
        olsResultsTermNameSearchJXTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                olsResultsTermNameSearchJXTableMouseClicked(evt);
            }
        });
        olsResultsTermNameSearchJXTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                olsResultsTermNameSearchJXTableKeyReleased(evt);
            }
        });
        olsResultsTermNameSearchJScrollPane.setViewportView(olsResultsTermNameSearchJXTable);

        viewTermHierarchyTermNameSearchJLabel.setForeground(new java.awt.Color(0, 0, 255));
        viewTermHierarchyTermNameSearchJLabel.setText("View Term Hierarchy");
        viewTermHierarchyTermNameSearchJLabel.setEnabled(false);
        viewTermHierarchyTermNameSearchJLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                viewTermHierarchyTermNameSearchJLabelMouseClicked(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                viewTermHierarchyTermNameSearchJLabelMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                viewTermHierarchyTermNameSearchJLabelMouseExited(evt);
            }
        });

        termNameJLabel.setText("Term Name:");

        termNameSearchJTextField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        termNameSearchJTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                termNameSearchJTextFieldKeyReleased(evt);
            }
        });

        newtSpeciesTipsTermNameSearchJLabel.setFont(newtSpeciesTipsTermNameSearchJLabel.getFont().deriveFont(newtSpeciesTipsTermNameSearchJLabel.getFont().getSize()-1f));
        newtSpeciesTipsTermNameSearchJLabel.setForeground(new java.awt.Color(0, 0, 255));
        newtSpeciesTipsTermNameSearchJLabel.setText("NEWT Species Tips");
        newtSpeciesTipsTermNameSearchJLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                newtSpeciesTipsTermNameSearchJLabelMouseClicked(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                newtSpeciesTipsTermNameSearchJLabelMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                newtSpeciesTipsTermNameSearchJLabelMouseExited(evt);
            }
        });

        numberOfTermsTermNameSearchJTextField.setEditable(false);
        numberOfTermsTermNameSearchJTextField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        numberOfTermsTermNameSearchJTextField.setToolTipText("Number of Matching Terms");

        org.jdesktop.layout.GroupLayout jPanel3Layout = new org.jdesktop.layout.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(newtSpeciesTipsTermNameSearchJLabel)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, jPanel3Layout.createSequentialGroup()
                        .add(termNameJLabel)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(termNameSearchJTextField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 470, Short.MAX_VALUE)))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(numberOfTermsTermNameSearchJTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 79, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.CENTER)
                    .add(termNameJLabel)
                    .add(numberOfTermsTermNameSearchJTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 20, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(termNameSearchJTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(newtSpeciesTipsTermNameSearchJLabel))
        );

        org.jdesktop.layout.GroupLayout termNameSearchJPanelLayout = new org.jdesktop.layout.GroupLayout(termNameSearchJPanel);
        termNameSearchJPanel.setLayout(termNameSearchJPanelLayout);
        termNameSearchJPanelLayout.setHorizontalGroup(
            termNameSearchJPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .add(termNameSearchJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(searchResultsTermNameJLabel)
                .addContainerGap(562, Short.MAX_VALUE))
            .add(termNameSearchJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(termNameSearchJPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, olsResultsTermNameSearchJScrollPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 627, Short.MAX_VALUE)
                    .add(termDetailsTermNameSearchJScrollPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 627, Short.MAX_VALUE)
                    .add(termNameSearchJPanelLayout.createSequentialGroup()
                        .add(selectedTermTermNameJLabel)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 457, Short.MAX_VALUE)
                        .add(viewTermHierarchyTermNameSearchJLabel))
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jScrollPane4, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 627, Short.MAX_VALUE))
                .addContainerGap())
        );
        termNameSearchJPanelLayout.setVerticalGroup(
            termNameSearchJPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, termNameSearchJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(searchResultsTermNameJLabel)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(olsResultsTermNameSearchJScrollPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 127, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(termNameSearchJPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(selectedTermTermNameJLabel)
                    .add(viewTermHierarchyTermNameSearchJLabel))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jScrollPane4, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 50, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(termDetailsTermNameSearchJScrollPane, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 77, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        searchTypeJTabbedPane.addTab("Term Name Search", termNameSearchJPanel);

        definitionTermIdSearchJTextPane.setEditable(false);
        jScrollPane6.setViewportView(definitionTermIdSearchJTextPane);

        termDetailsTermIdSearchJXTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Name", "Value"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        termDetailsTermIdSearchJXTable.setOpaque(false);
        termDetailsTermIdSearchJScrollPane.setViewportView(termDetailsTermIdSearchJXTable);

        jLabel11.setText("Search Results:");

        jLabel13.setText("Selected Term:");

        olsResultsTermIdSearchJXTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Accession", "CV Term"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        olsResultsTermIdSearchJXTable.setOpaque(false);
        olsResultsTermIdSearchJXTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                olsResultsTermIdSearchJXTableMouseClicked(evt);
            }
        });
        olsResultsTermIdSearchJXTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                olsResultsTermIdSearchJXTableKeyReleased(evt);
            }
        });
        olsResultsTermIdSearchJScrollPane.setViewportView(olsResultsTermIdSearchJXTable);

        viewTermHierarchyTermIdSearchJLabel.setForeground(new java.awt.Color(0, 0, 255));
        viewTermHierarchyTermIdSearchJLabel.setText("View Term Hierarchy");
        viewTermHierarchyTermIdSearchJLabel.setEnabled(false);
        viewTermHierarchyTermIdSearchJLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                viewTermHierarchyTermIdSearchJLabelMouseClicked(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                viewTermHierarchyTermIdSearchJLabelMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                viewTermHierarchyTermIdSearchJLabelMouseExited(evt);
            }
        });

        jLabel10.setText("Term ID:");
        jLabel10.setPreferredSize(new java.awt.Dimension(58, 14));

        termIdSearchJTextField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        termIdSearchJTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                termIdSearchJTextFieldKeyPressed(evt);
            }
        });

        newtSpeciesTipsTermIdSearchJLabel.setFont(newtSpeciesTipsTermIdSearchJLabel.getFont().deriveFont(newtSpeciesTipsTermIdSearchJLabel.getFont().getSize()-1f));
        newtSpeciesTipsTermIdSearchJLabel.setForeground(new java.awt.Color(0, 0, 255));
        newtSpeciesTipsTermIdSearchJLabel.setText("NEWT Species Tips");
        newtSpeciesTipsTermIdSearchJLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                newtSpeciesTipsTermIdSearchJLabelMouseClicked(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                newtSpeciesTipsTermIdSearchJLabelMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                newtSpeciesTipsTermIdSearchJLabelMouseExited(evt);
            }
        });

        termIdSearchJButton.setText("Search");
        termIdSearchJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                termIdSearchJButtonActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout jPanel2Layout = new org.jdesktop.layout.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(newtSpeciesTipsTermIdSearchJLabel)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, jPanel2Layout.createSequentialGroup()
                        .add(jLabel10, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(termIdSearchJTextField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 469, Short.MAX_VALUE)))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(termIdSearchJButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 80, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel2Layout.createSequentialGroup()
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.CENTER)
                    .add(termIdSearchJTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel10, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(termIdSearchJButton))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(newtSpeciesTipsTermIdSearchJLabel))
        );

        org.jdesktop.layout.GroupLayout termIdSearchJPanelLayout = new org.jdesktop.layout.GroupLayout(termIdSearchJPanel);
        termIdSearchJPanel.setLayout(termIdSearchJPanelLayout);
        termIdSearchJPanelLayout.setHorizontalGroup(
            termIdSearchJPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .add(termIdSearchJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(jLabel11)
                .addContainerGap(562, Short.MAX_VALUE))
            .add(termIdSearchJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(termIdSearchJPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, olsResultsTermIdSearchJScrollPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 627, Short.MAX_VALUE)
                    .add(termDetailsTermIdSearchJScrollPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 627, Short.MAX_VALUE)
                    .add(jScrollPane6, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 627, Short.MAX_VALUE)
                    .add(termIdSearchJPanelLayout.createSequentialGroup()
                        .add(jLabel13)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 457, Short.MAX_VALUE)
                        .add(viewTermHierarchyTermIdSearchJLabel)))
                .addContainerGap())
        );
        termIdSearchJPanelLayout.setVerticalGroup(
            termIdSearchJPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, termIdSearchJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jLabel11)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(olsResultsTermIdSearchJScrollPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 124, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(termIdSearchJPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel13)
                    .add(viewTermHierarchyTermIdSearchJLabel))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jScrollPane6, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 50, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(termDetailsTermIdSearchJScrollPane, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 77, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        searchTypeJTabbedPane.addTab("Term ID Search", termIdSearchJPanel);

        jLabel6.setText("Search Results:");

        jLabel7.setText("Selected Term:");

        definitionMassSearchJTextPane.setEditable(false);
        jScrollPane5.setViewportView(definitionMassSearchJTextPane);

        termDetailsMassSearchJXTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Name", "Value"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        termDetailsMassSearchJXTable.setOpaque(false);
        termDetailsMassSearchJScrollPane.setViewportView(termDetailsMassSearchJXTable);

        olsResultsMassSearchJXTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Accession", "CV Term"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        olsResultsMassSearchJXTable.setOpaque(false);
        olsResultsMassSearchJXTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                olsResultsMassSearchJXTableMouseClicked(evt);
            }
        });
        olsResultsMassSearchJXTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                olsResultsMassSearchJXTableKeyReleased(evt);
            }
        });
        olsResultsMassSearchJScrollPane.setViewportView(olsResultsMassSearchJXTable);

        viewTermHierarchyMassSearchJLabel.setForeground(new java.awt.Color(0, 0, 255));
        viewTermHierarchyMassSearchJLabel.setText("View Term Hierarchy");
        viewTermHierarchyMassSearchJLabel.setEnabled(false);
        viewTermHierarchyMassSearchJLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                viewTermHierarchyMassSearchJLabelMouseClicked(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                viewTermHierarchyMassSearchJLabelMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                viewTermHierarchyMassSearchJLabelMouseExited(evt);
            }
        });

        jLabel4.setText("Mass:");

        modificationMassJTextField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        modificationMassJTextField.setText("0.0");
        modificationMassJTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                modificationMassJTextFieldKeyPressed(evt);
            }
        });

        jLabel5.setText("+-");
        jLabel5.setToolTipText("Mass Accuracy");

        precisionJTextField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        precisionJTextField.setText("0.1");
        precisionJTextField.setToolTipText("Mass Accuracy");
        precisionJTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                precisionJTextFieldKeyPressed(evt);
            }
        });

        jLabel12.setText("Type:");

        massTypeJComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "- Select -", "DiffAvg", "DiffMono", "MassAvg", "MassMono" }));
        massTypeJComboBox.setSelectedIndex(2);
        massTypeJComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                massTypeJComboBoxActionPerformed(evt);
            }
        });

        dummyLabelJLabel.setFont(dummyLabelJLabel.getFont().deriveFont(dummyLabelJLabel.getFont().getSize()-1f));
        dummyLabelJLabel.setText("NEWT Species Tips");

        modificationMassSearchJButton.setText("Search");
        modificationMassSearchJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                modificationMassSearchJButtonActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(dummyLabelJLabel)
                    .add(jPanel1Layout.createSequentialGroup()
                        .add(jLabel4)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(modificationMassJTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 130, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jLabel5)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(precisionJTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 81, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(jLabel12)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(massTypeJComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED, 161, Short.MAX_VALUE)
                .add(modificationMassSearchJButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 80, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(modificationMassSearchJButton)
                    .add(massTypeJComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel12)
                    .add(precisionJTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel5)
                    .add(jLabel4)
                    .add(modificationMassJTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(dummyLabelJLabel))
        );

        org.jdesktop.layout.GroupLayout massSearchJPanelLayout = new org.jdesktop.layout.GroupLayout(massSearchJPanel);
        massSearchJPanel.setLayout(massSearchJPanelLayout);
        massSearchJPanelLayout.setHorizontalGroup(
            massSearchJPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .add(massSearchJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(jLabel6)
                .addContainerGap(562, Short.MAX_VALUE))
            .add(massSearchJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(massSearchJPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jScrollPane5, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 627, Short.MAX_VALUE)
                    .add(olsResultsMassSearchJScrollPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 627, Short.MAX_VALUE)
                    .add(massSearchJPanelLayout.createSequentialGroup()
                        .add(jLabel7)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 457, Short.MAX_VALUE)
                        .add(viewTermHierarchyMassSearchJLabel))
                    .add(termDetailsMassSearchJScrollPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 627, Short.MAX_VALUE))
                .addContainerGap())
        );
        massSearchJPanelLayout.setVerticalGroup(
            massSearchJPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, massSearchJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jLabel6)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(olsResultsMassSearchJScrollPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 124, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(massSearchJPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel7)
                    .add(viewTermHierarchyMassSearchJLabel))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jScrollPane5, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 50, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(termDetailsMassSearchJScrollPane, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 77, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        searchTypeJTabbedPane.addTab("PSI-MOD Mass Search", massSearchJPanel);

        jLabel8.setText("Selected Term:");

        definitionBrowseOntologyJTextPane.setEditable(false);
        jScrollPane7.setViewportView(definitionBrowseOntologyJTextPane);

        termDetailsBrowseOntologyJXTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Name", "Value"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        termDetailsBrowseOntologyJXTable.setOpaque(false);
        termDetailsBrowseOntologyJScrollPane.setViewportView(termDetailsBrowseOntologyJXTable);

        browseJPanel.setLayout(new javax.swing.BoxLayout(browseJPanel, javax.swing.BoxLayout.LINE_AXIS));

        viewTermHierarchyBrowseOntologyJLabel.setForeground(new java.awt.Color(0, 0, 255));
        viewTermHierarchyBrowseOntologyJLabel.setText("View Term Hierarchy");
        viewTermHierarchyBrowseOntologyJLabel.setEnabled(false);
        viewTermHierarchyBrowseOntologyJLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                viewTermHierarchyBrowseOntologyJLabelMouseClicked(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                viewTermHierachyJLabelMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                viewTermHierachyJLabelMouseExited(evt);
            }
        });

        org.jdesktop.layout.GroupLayout browseOntologyJPanelLayout = new org.jdesktop.layout.GroupLayout(browseOntologyJPanel);
        browseOntologyJPanel.setLayout(browseOntologyJPanelLayout);
        browseOntologyJPanelLayout.setHorizontalGroup(
            browseOntologyJPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, browseOntologyJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(browseOntologyJPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, browseJPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 627, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, browseOntologyJPanelLayout.createSequentialGroup()
                        .add(jLabel8)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 457, Short.MAX_VALUE)
                        .add(viewTermHierarchyBrowseOntologyJLabel))
                    .add(jScrollPane7, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 627, Short.MAX_VALUE)
                    .add(termDetailsBrowseOntologyJScrollPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 627, Short.MAX_VALUE))
                .addContainerGap())
        );
        browseOntologyJPanelLayout.setVerticalGroup(
            browseOntologyJPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, browseOntologyJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(browseJPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 203, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(browseOntologyJPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel8)
                    .add(viewTermHierarchyBrowseOntologyJLabel))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jScrollPane7, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 50, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(termDetailsBrowseOntologyJScrollPane, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 77, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        searchTypeJTabbedPane.addTab("Browse Ontology", browseOntologyJPanel);

        ontologyJLabel.setText("Ontology:");

        ontologyJComboBox.setMaximumRowCount(30);
        ontologyJComboBox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                ontologyJComboBoxItemStateChanged(evt);
            }
        });

        org.jdesktop.layout.GroupLayout searchParametersJPanelLayout = new org.jdesktop.layout.GroupLayout(searchParametersJPanel);
        searchParametersJPanel.setLayout(searchParametersJPanelLayout);
        searchParametersJPanelLayout.setHorizontalGroup(
            searchParametersJPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(searchParametersJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(searchParametersJPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(searchTypeJTabbedPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 652, Short.MAX_VALUE)
                    .add(searchParametersJPanelLayout.createSequentialGroup()
                        .add(ontologyJLabel)
                        .add(18, 18, 18)
                        .add(ontologyJComboBox, 0, 586, Short.MAX_VALUE)))
                .addContainerGap())
        );
        searchParametersJPanelLayout.setVerticalGroup(
            searchParametersJPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(searchParametersJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(searchParametersJPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.CENTER)
                    .add(ontologyJLabel)
                    .add(ontologyJComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(32, 32, 32)
                .add(searchTypeJTabbedPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 417, Short.MAX_VALUE)
                .addContainerGap())
        );

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, searchParametersJPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(layout.createSequentialGroup()
                        .add(helpJButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 23, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(aboutJButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 25, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 382, Short.MAX_VALUE)
                        .add(insertSelectedJButton)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(cancelJButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(org.jdesktop.layout.GroupLayout.LEADING, jSeparator1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 684, Short.MAX_VALUE))
                .addContainerGap())
        );

        layout.linkSize(new java.awt.Component[] {cancelJButton, insertSelectedJButton}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .add(searchParametersJPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(18, 18, 18)
                .add(jSeparator1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 10, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(layout.createSequentialGroup()
                        .add(7, 7, 7)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(cancelJButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(insertSelectedJButton)))
                    .add(layout.createSequentialGroup()
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(aboutJButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 24, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(helpJButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 24, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap())
        );

        layout.linkSize(new java.awt.Component[] {cancelJButton, insertSelectedJButton}, org.jdesktop.layout.GroupLayout.VERTICAL);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Updates the search results if the ontology is changed.
     *
     * @param evt
     */
    private void ontologyJComboBoxItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_ontologyJComboBoxItemStateChanged

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

        // insert the ontology label into the term id search field
        if (ontologyJComboBox.getSelectedIndex() != 0 && isPreselectedOption() == false) {
            if (getCurrentOntologyLabel().equalsIgnoreCase("EFO")) {
                termIdSearchJTextField.setText(getCurrentOntologyLabel() + "_");
            } else if (getCurrentOntologyLabel().equalsIgnoreCase("NEWT")) {
                termIdSearchJTextField.setText("");
            } else {
                termIdSearchJTextField.setText(getCurrentOntologyLabel() + ":");
            }
        } else {
            termIdSearchJTextField.setText("");
        }

        if (searchTypeJTabbedPane.getSelectedIndex() != OLS_DIALOG_PSI_MOD_MASS_SEARCH) {

            String currentOntology = (String) ontologyJComboBox.getSelectedItem();

            if (!currentOntology.equalsIgnoreCase(lastSelectedOntology)) {

                lastSelectedOntology = (String) ontologyJComboBox.getSelectedItem();

                currentlySelectedBrowseOntologyAccessionNumber = null;
                currentlySelectedTermNameSearchAccessionNumber = null;
                currentlySelectedTermIdSearchAccessionNumber = null;

                insertSelectedJButton.setEnabled(false);

                // disable the 'browse ontology' tab when 'search in all ontologies' or 'NEWT' is selected or 'search in preselected ontologies'


                if (ontologyJComboBox.getSelectedIndex() == 0 || getCurrentOntologyLabel().equalsIgnoreCase("NEWT") || isPreselectedOption() == true) {
                    searchTypeJTabbedPane.setEnabledAt(OLS_DIALOG_BROWSE_ONTOLOGY, false);

                    // move away from the 'browse ontology' tab if it is disabled and selected
                    if (searchTypeJTabbedPane.getSelectedIndex() == OLS_DIALOG_BROWSE_ONTOLOGY) {
                        searchTypeJTabbedPane.setSelectedIndex(OLS_DIALOG_TERM_NAME_SEARCH);

                        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

                        if (getCurrentOntologyLabel().equalsIgnoreCase("NEWT")) {
                            JOptionPane.showMessageDialog(this, "Browse Ontology is not available for NEWT.",
                                    "Browse Ontology Disabled", JOptionPane.INFORMATION_MESSAGE);
                        } else if (ontologyJComboBox.getSelectedIndex() == 0) {
                            JOptionPane.showMessageDialog(this, "Browse Ontology is not available when searching several ontologies.",
                                    "Browse Ontology Disabled", JOptionPane.INFORMATION_MESSAGE);
                        }

                        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
                    }

                } else {
                    searchTypeJTabbedPane.setEnabledAt(OLS_DIALOG_BROWSE_ONTOLOGY, true);
                }

                // set the focus
                if(searchTypeJTabbedPane.getSelectedIndex() == OLS_DIALOG_TERM_NAME_SEARCH){
                    termNameSearchJTextField.requestFocus();
                } else if(searchTypeJTabbedPane.getSelectedIndex() == OLS_DIALOG_TERM_ID_SEARCH){
                    termIdSearchJTextField.requestFocus();
                }

                // make the 'newt species tip' link visible or not visible
                hideOrShowNewtLinks();

                // update the searches
                termNameSearchJTextFieldKeyReleased(null);
                updateBrowseOntologyView();
                clearData(OLS_DIALOG_TERM_ID_SEARCH, true, true);

                viewTermHierarchyTermNameSearchJLabel.setEnabled(false);
                viewTermHierarchyTermIdSearchJLabel.setEnabled(false);
                viewTermHierarchyBrowseOntologyJLabel.setEnabled(false);
            }
        }

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

    }//GEN-LAST:event_ontologyJComboBoxItemStateChanged

    /**
     * Searches the selected ontology for terms matching the inserted string.
     * The search finds all terms having the current string as a substring.
     * (But seems to be limited somehow, seeing as using two letters, can
     * result in more hits, than using just one of the letters...)
     *
     * @param evt
     */
    private void termNameSearchJTextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_termNameSearchJTextFieldKeyReleased

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        termNameSearchJTextField.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

        insertSelectedJButton.setEnabled(false);
        currentlySelectedTermNameSearchAccessionNumber = null;

        try {
            // clear the old meta data
            clearData(OLS_DIALOG_TERM_NAME_SEARCH, true, true);

            // the search is only performed if a certain amount of characters are inserted
            if (termNameSearchJTextField.getText().length() >= MINIMUM_WORD_LENGTH) {

                // search the selected ontology and find all matching terms
                String ontology = getCurrentOntologyLabel();

                // if 'search in all ontologies' is selected, set the ontology to null
                if (ontologyJComboBox.getSelectedIndex() == 0) {
                    ontology = null;
                }

                Map map = new HashMap();
                if (isPreselectedOption() == true) {
                    // Ontology terms for preselected Ontologies
                    for(String preselectedOntology:preselectedOntologies.keySet()){
                        map.putAll(olsConnection.getTermsByName(termNameSearchJTextField.getText(), preselectedOntology.toUpperCase(), false));
                    }
                } else {
                    map = olsConnection.getTermsByName(termNameSearchJTextField.getText(), ontology, false);
                }

                for (Iterator i = map.keySet().iterator(); i.hasNext();) {
                    String key = (String) i.next();
                    ((DefaultTableModel) olsResultsTermNameSearchJXTable.getModel()).addRow(new Object[]{key, map.get(key)});
                }

                termNameSearchJTextField.requestFocus();

                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                termNameSearchJTextField.setCursor(new java.awt.Cursor(java.awt.Cursor.TEXT_CURSOR));
                numberOfTermsTermNameSearchJTextField.setText("" + map.size());

                // make the first row visible
                if (olsResultsTermNameSearchJXTable.getRowCount() > 0) {
                    olsResultsTermNameSearchJXTable.scrollRectToVisible(olsResultsTermNameSearchJXTable.getCellRect(0, 0, false));
                }

                //No matching terms found
                if (map.isEmpty()) {
                    //JOptionPane.showMessageDialog(this, "No mathcing terms found.");
                }
            } else {
                numberOfTermsTermNameSearchJTextField.setText("-");
            }
        } catch (RemoteException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    defaultOlsConnectionFailureErrorMessage,
                    "OLS Connection Error", JOptionPane.ERROR_MESSAGE);
            Util.writeToErrorLog("Error when trying to access OLS: ");
            ex.printStackTrace();
        }

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        termNameSearchJTextField.setCursor(new java.awt.Cursor(java.awt.Cursor.TEXT_CURSOR));

    }//GEN-LAST:event_termNameSearchJTextFieldKeyReleased

    /**
     * Inserts the selected ontology into the parents text field or table and
     * then closes the dialog.
     *
     * @param evt
     */
    private void insertSelectedJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_insertSelectedJButtonActionPerformed

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

        String termId = null;

        if (searchTypeJTabbedPane.getSelectedIndex() == OLS_DIALOG_TERM_NAME_SEARCH) {
            termId = "" + olsResultsTermNameSearchJXTable.getValueAt(olsResultsTermNameSearchJXTable.getSelectedRow(), 0);
        } else if (searchTypeJTabbedPane.getSelectedIndex() == OLS_DIALOG_BROWSE_ONTOLOGY) {
            termId = currentlySelectedBrowseOntologyAccessionNumber;
        } else if (searchTypeJTabbedPane.getSelectedIndex() == OLS_DIALOG_PSI_MOD_MASS_SEARCH) {
            termId = "" + olsResultsMassSearchJXTable.getValueAt(olsResultsMassSearchJXTable.getSelectedRow(), 0);
        } else if (searchTypeJTabbedPane.getSelectedIndex() == OLS_DIALOG_TERM_ID_SEARCH) {
            termId = "" + olsResultsTermIdSearchJXTable.getValueAt(olsResultsTermIdSearchJXTable.getSelectedRow(), 0);
        }

        String ontologyLong = ((String) ontologyJComboBox.getSelectedItem());
        String ontologyShort = "unknown";

        if (ontologyJComboBox.getSelectedIndex() == 0 || isPreselectedOption() == true) {

            ontologyShort = getOntologyLabelFromTermId(termId);

            if (ontologyShort == null) {
                ontologyShort = "NEWT";
                ontologyLong = "NEWT UniProt Taxonomy Database [NEWT]";
            } else {

                try {
                    Map ontologies = olsConnection.getOntologyNames();
                    ontologyLong = ontologies.get(ontologyShort).toString() + "[" + ontologyShort + "]";
                } catch (RemoteException ex) {
                    JOptionPane.showMessageDialog(
                            this,
                            defaultOlsConnectionFailureErrorMessage,
                            "OLS Connection Error", JOptionPane.ERROR_MESSAGE);
                    Util.writeToErrorLog("Error when trying to access OLS: ");
                    ex.printStackTrace();
                    ontologyLong = "unknown";
                }
            }
        } else {
            ontologyShort = ontologyLong.substring(ontologyLong.lastIndexOf("[") + 1, ontologyLong.length() - 1);
        }

        try {
            String selectedValue = olsConnection.getTermById(termId, ontologyShort);

            //insert the value into the correct text field or table
            if (olsInputable != null) {
                olsInputable.insertOLSResult(field, selectedValue, termId, ontologyShort, ontologyLong, modifiedRow, mappedTerm, metadata);
                this.setVisible(false);
                this.dispose();
            }
        } catch (RemoteException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    defaultOlsConnectionFailureErrorMessage,
                    "OLS Connection Error", JOptionPane.ERROR_MESSAGE);
            Util.writeToErrorLog("Error when trying to access OLS: ");
            ex.printStackTrace();
        }

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_insertSelectedJButtonActionPerformed

    /**
     * Updates the information about the selected CV term
     *
     * @param evt
     * @param searchResultTable
     */
    private void insertTermDetails(java.awt.event.MouseEvent evt, JXTable searchResultTable) {

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

        int row = searchResultTable.getSelectedRow();

        if (row != -1) {
            insertSelectedJButton.setEnabled(true);
        } else {
            insertSelectedJButton.setEnabled(false);
        }

        boolean doSearch = true;

        if (evt != null) {
            if (evt.getClickCount() == 2 && evt.getButton() == java.awt.event.MouseEvent.BUTTON1) {
                insertSelectedJButtonActionPerformed(null);
                doSearch = false;
            }
        }

        // This does not seem to work... The search is always performed. It
        // seems as the first click in the double click results in one event
        // and the second in another. This results in the term details always
        // beeing retrieved...
        if (doSearch) {
            if (row != -1) {

                Integer searchType = null;

                if (searchTypeJTabbedPane.getSelectedIndex() == OLS_DIALOG_TERM_NAME_SEARCH) {
                    currentlySelectedTermNameSearchAccessionNumber = (String) searchResultTable.getValueAt(row, 0);
                    searchType = OLS_DIALOG_TERM_NAME_SEARCH;
                } else if (searchTypeJTabbedPane.getSelectedIndex() == OLS_DIALOG_PSI_MOD_MASS_SEARCH) {
                    currentlySelectedMassSearchAccessionNumber = (String) searchResultTable.getValueAt(row, 0);
                    searchType = OLS_DIALOG_PSI_MOD_MASS_SEARCH;
                } else if (searchTypeJTabbedPane.getSelectedIndex() == OLS_DIALOG_TERM_ID_SEARCH) {
                    currentlySelectedTermIdSearchAccessionNumber = (String) searchResultTable.getValueAt(row, 0);
                    searchType = OLS_DIALOG_TERM_ID_SEARCH;
                }

                String termID = (String) searchResultTable.getValueAt(row, 0);
                loadMetaData(termID, searchType);
            }
        }

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }

    /**
     * Closes the dialog.
     *
     * @param evt
     */
    private void cancelJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelJButtonActionPerformed
        if (olsInputable != null) {
            this.setVisible(false);
            this.dispose();
        } else {
            System.exit(0);
        }
    }//GEN-LAST:event_cancelJButtonActionPerformed

    /**
     * Opens a help frame.
     * 
     * @param evt
     */
    private void helpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_helpJButtonActionPerformed
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new HelpDialog(this, true, getClass().getResource("/no/uib/olsdialog/helpfiles/OLSDialog.html"));
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_helpJButtonActionPerformed

    /**
     * Opens an About frame.
     *
     * @param evt
     */
    private void aboutJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aboutJButtonActionPerformed
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new HelpDialog(this, true, getClass().getResource("/no/uib/olsdialog/helpfiles/AboutOLS.html"));
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_aboutJButtonActionPerformed

    /**
     * @see #cancelJButtonActionPerformed(java.awt.event.ActionEvent)
     */
    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        cancelJButtonActionPerformed(null);
    }//GEN-LAST:event_formWindowClosing

    /**
     * returns an array of DataHolder objects that contain data on MOD entries (termId, termName, massDelta)
     * given a massDeltaType and a range of masses.
     *
     * @param massDeltaType the type of massDelta to query (can be null)
     * @param fromMass the lower mass limit (inclusive, mandatory)
     * @param toMass the higher mass limit (inclusive, mandatory)
     */
    public DataHolder[] getModificationsByMassDelta(String massDeltaType, double fromMass, double toMass) {

        DataHolder[] retval = null;

        try {
            QueryService locator = new QueryServiceLocator();
            Query service = locator.getOntologyQuery();

            retval = service.getTermsByAnnotationData("MOD", massDeltaType, null, fromMass, toMass);

        } catch (ServiceException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    defaultOlsConnectionFailureErrorMessage,
                    "OLS Connection Error", JOptionPane.ERROR_MESSAGE);
            Util.writeToErrorLog("Error when trying to access OLS: ");
            ex.printStackTrace();
        } catch (RemoteException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    defaultOlsConnectionFailureErrorMessage,
                    "OLS Connection Error", JOptionPane.ERROR_MESSAGE);
            Util.writeToErrorLog("Error when trying to access OLS: ");
            ex.printStackTrace();
        }

        return retval;
    }

    /**
     * Tries to find all terms in the selected ontology that include the
     * selected mass term and has a value within the selected boundaries.
     *
     * @param evt
     */
    private void modificationMassSearchJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_modificationMassSearchJButtonActionPerformed
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

        insertSelectedJButton.setEnabled(false);
        viewTermHierarchyMassSearchJLabel.setEnabled(false);

        // clear the old data
        clearData(OLS_DIALOG_PSI_MOD_MASS_SEARCH, true, true);

        boolean error = false;
        double currentModificationMass = 0.0;
        double currentAccuracy = 0.1;

        try {
            currentModificationMass = new Double(modificationMassJTextField.getText()).doubleValue();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null,
                    "The mass is not a number!", "Modification Mass", JOptionPane.INFORMATION_MESSAGE);
            modificationMassJTextField.requestFocus();
            error = true;
        }

        if (!error) {
            try {
                currentAccuracy = new Double(precisionJTextField.getText()).doubleValue();

                if (currentAccuracy < 0) {
                    JOptionPane.showMessageDialog(null,
                            "The precision has to be a positive value.", "Mass Accuracy", JOptionPane.INFORMATION_MESSAGE);
                    precisionJTextField.requestFocus();
                    error = true;
                }

            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(null,
                        "The precision is not a number!", "Mass Accuracy", JOptionPane.INFORMATION_MESSAGE);
                precisionJTextField.requestFocus();
                error = true;
            }
        }

        if (!error) {

            String massType = massTypeJComboBox.getSelectedItem().toString();

            DataHolder[] results = getModificationsByMassDelta(massType,
                    currentModificationMass - currentAccuracy,
                    currentModificationMass + currentAccuracy);

            if (results != null) {
                for (int i = 0; i < results.length; i++) {
                    ((DefaultTableModel) olsResultsMassSearchJXTable.getModel()).addRow(
                            new Object[]{results[i].getTermId(),
                                results[i].getTermName()});
                }
            }

            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

            // make the first row visible
            if (olsResultsMassSearchJXTable.getRowCount() > 0) {
                olsResultsMassSearchJXTable.scrollRectToVisible(olsResultsTermNameSearchJXTable.getCellRect(0, 0, false));
            }
        }

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_modificationMassSearchJButtonActionPerformed

    /**
     * Enables or disables the search button based on the selection in the combo box.
     *
     * @param evt
     */
    private void massTypeJComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_massTypeJComboBoxActionPerformed
        modificationMassSearchJButton.setEnabled(massTypeJComboBox.getSelectedIndex() != 0);
    }//GEN-LAST:event_massTypeJComboBoxActionPerformed

    /**
     * Makes sure that the PSI-MOD ontology is selected when the modification mass
     * search tab is selected.
     *
     * @param evt
     */
    private void searchTypeJTabbedPaneStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_searchTypeJTabbedPaneStateChanged
        if (searchTypeJTabbedPane.getSelectedIndex() == OLS_DIALOG_PSI_MOD_MASS_SEARCH) {
            insertSelectedJButton.setEnabled(currentlySelectedMassSearchAccessionNumber != null);
            lastSelectedOntology = (String) ontologyJComboBox.getSelectedItem();
            ontologyJComboBox.setSelectedItem("Protein Modifications (PSI-MOD) [MOD]");
            ontologyJComboBox.setEnabled(false);
        } else if (searchTypeJTabbedPane.getSelectedIndex() == OLS_DIALOG_BROWSE_ONTOLOGY) {
            insertSelectedJButton.setEnabled(currentlySelectedBrowseOntologyAccessionNumber != null);
            ontologyJComboBox.setSelectedItem(lastSelectedOntology);
            ontologyJComboBox.setEnabled(true);
        } else if (searchTypeJTabbedPane.getSelectedIndex() == OLS_DIALOG_TERM_NAME_SEARCH) {
            insertSelectedJButton.setEnabled(currentlySelectedTermNameSearchAccessionNumber != null);
            ontologyJComboBox.setSelectedItem(lastSelectedOntology);
            ontologyJComboBox.setEnabled(true);
        } else if (searchTypeJTabbedPane.getSelectedIndex() == OLS_DIALOG_TERM_ID_SEARCH) {
            insertSelectedJButton.setEnabled(currentlySelectedTermIdSearchAccessionNumber != null);
            ontologyJComboBox.setSelectedItem(lastSelectedOntology);
            ontologyJComboBox.setEnabled(true);
        }
    }//GEN-LAST:event_searchTypeJTabbedPaneStateChanged

    /**
     *@see #olsResultsTermNameSearchJXTableMouseClicked(java.awt.event.MouseEvent)
     */
    private void olsResultsTermNameSearchJXTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_olsResultsTermNameSearchJXTableKeyReleased
        olsResultsTermNameSearchJXTableMouseClicked(null);
    }//GEN-LAST:event_olsResultsTermNameSearchJXTableKeyReleased

    /**
     * If the user double clicks the selected row is inserted into the parent
     * frame and closes the dialog. A single click retrieves the additional
     * information known about the term and displays it in the "Term Details"
     * frame.
     *
     * @param evt
     */
    private void olsResultsTermNameSearchJXTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_olsResultsTermNameSearchJXTableMouseClicked
        insertTermDetails(evt, olsResultsTermNameSearchJXTable);
    }//GEN-LAST:event_olsResultsTermNameSearchJXTableMouseClicked

    /**
     * @see #olsResultsMassSearchJXTableMouseClicked(java.awt.event.MouseEvent)
     */
    private void olsResultsMassSearchJXTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_olsResultsMassSearchJXTableKeyReleased
        olsResultsMassSearchJXTableMouseClicked(null);
    }//GEN-LAST:event_olsResultsMassSearchJXTableKeyReleased

    /**
     * If the user double clicks the selected row is inserted into the parent
     * frame and closes the dialog. A single click retrieves the additional
     * information known about the term and displays it in the "Term Details"
     * frame.
     *
     * @param evt
     */
    private void olsResultsMassSearchJXTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_olsResultsMassSearchJXTableMouseClicked
        insertTermDetails(evt, olsResultsMassSearchJXTable);
    }//GEN-LAST:event_olsResultsMassSearchJXTableMouseClicked

    /**
     * Changes the cursor to the hand cursor when over the term hierarchy link.
     *
     * @param evt
     */
    private void viewTermHierachyJLabelMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_viewTermHierachyJLabelMouseEntered
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_viewTermHierachyJLabelMouseEntered

    /**
     * Changes the cursor back to the default cursor when leaving the term hierarchy link.
     *
     * @param evt
     */
    private void viewTermHierachyJLabelMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_viewTermHierachyJLabelMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_viewTermHierachyJLabelMouseExited

    /**
     * Opens a new dialog showing the term hierarchy as a graph.
     *
     * @param evt
     */
    private void viewTermHierarchyBrowseOntologyJLabelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_viewTermHierarchyBrowseOntologyJLabelMouseClicked
        viewTermHierarchy();
    }//GEN-LAST:event_viewTermHierarchyBrowseOntologyJLabelMouseClicked

    /**
     * Opens a new dialog showing the term hierarchy as a graph.
     *
     * @param evt
     */
    private void viewTermHierarchyMassSearchJLabelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_viewTermHierarchyMassSearchJLabelMouseClicked
        viewTermHierarchy();
    }//GEN-LAST:event_viewTermHierarchyMassSearchJLabelMouseClicked

    /**
     * Changes the cursor to the hand cursor when over the term hierarchy link.
     *
     * @param evt
     */
    private void viewTermHierarchyMassSearchJLabelMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_viewTermHierarchyMassSearchJLabelMouseEntered
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_viewTermHierarchyMassSearchJLabelMouseEntered

    /**
     * Changes the cursor back to the default cursor when leaving the term hierarchy link.
     *
     * @param evt
     */
    private void viewTermHierarchyMassSearchJLabelMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_viewTermHierarchyMassSearchJLabelMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_viewTermHierarchyMassSearchJLabelMouseExited

    /**
     * Opens a new dialog showing the term hierarchy as a graph.
     *
     * @param evt
     */
    private void viewTermHierarchyTermNameSearchJLabelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_viewTermHierarchyTermNameSearchJLabelMouseClicked
        viewTermHierarchy();
    }//GEN-LAST:event_viewTermHierarchyTermNameSearchJLabelMouseClicked

    /**
     * Changes the cursor to the hand cursor when over the term hierarchy link.
     *
     * @param evt
     */
    private void viewTermHierarchyTermNameSearchJLabelMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_viewTermHierarchyTermNameSearchJLabelMouseEntered
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_viewTermHierarchyTermNameSearchJLabelMouseEntered

    /**
     * Changes the cursor back to the default cursor when leaving the term hierarchy link.
     *
     * @param evt
     */
    private void viewTermHierarchyTermNameSearchJLabelMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_viewTermHierarchyTermNameSearchJLabelMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_viewTermHierarchyTermNameSearchJLabelMouseExited

    /**
     * If the user double clicks the selected row is inserted into the parent
     * frame and closes the dialog. A single click retrieves the additional
     * information known about the term and displays it in the "Term Details"
     * frame.
     *
     * @param evt
     */
    private void olsResultsTermIdSearchJXTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_olsResultsTermIdSearchJXTableMouseClicked
        insertTermDetails(evt, olsResultsTermIdSearchJXTable);
    }//GEN-LAST:event_olsResultsTermIdSearchJXTableMouseClicked

    /**
     * @see #olsResultsTermIdSearchJXTableMouseClicked(java.awt.event.MouseEvent)
     */
    private void olsResultsTermIdSearchJXTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_olsResultsTermIdSearchJXTableKeyReleased
        olsResultsTermIdSearchJXTableMouseClicked(null);
    }//GEN-LAST:event_olsResultsTermIdSearchJXTableKeyReleased

    /**
     * Opens a new dialog showing the term hierarchy as a graph.
     *
     * @param evt
     */
    private void viewTermHierarchyTermIdSearchJLabelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_viewTermHierarchyTermIdSearchJLabelMouseClicked
        viewTermHierarchy();
    }//GEN-LAST:event_viewTermHierarchyTermIdSearchJLabelMouseClicked

    /**
     * Changes the cursor to the hand cursor when over the term hierarchy link.
     *
     * @param evt
     */
    private void viewTermHierarchyTermIdSearchJLabelMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_viewTermHierarchyTermIdSearchJLabelMouseEntered
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_viewTermHierarchyTermIdSearchJLabelMouseEntered

    /**
     * Changes the cursor back to the default cursor when leaving the term hierarchy link.
     *
     * @param evt
     */
    private void viewTermHierarchyTermIdSearchJLabelMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_viewTermHierarchyTermIdSearchJLabelMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_viewTermHierarchyTermIdSearchJLabelMouseExited

    /**
     * Searches for the term matching the inserted accession number and
     * inserts the result into the table.
     *
     * @param evt
     */
    private void termIdSearchJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_termIdSearchJButtonActionPerformed

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        termIdSearchJTextField.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

        insertSelectedJButton.setEnabled(false);
        currentlySelectedTermIdSearchAccessionNumber = null;

        try {

            // clear the old data
            clearData(OLS_DIALOG_TERM_ID_SEARCH, true, true);

            // search the selected ontology and find all matching terms
            String ontology = getCurrentOntologyLabel();

            if (ontologyJComboBox.getSelectedIndex() == 0) {
                ontology = null;
            }

            String currentTermName = "";
            if (this.isPreselectedOption() == true) {
                // Ontology term for preselected Ontologies
                preselectedOntologiesLoop:
                for(String preselectedOntology:preselectedOntologies.keySet()){
                    currentTermName = olsConnection.getTermById(termIdSearchJTextField.getText().trim(), preselectedOntology.toUpperCase());
                    if(currentTermName.length() > 0){
                        break preselectedOntologiesLoop;
                    }
                }
            } else {
                // Ontology term for one ontology or for all OLS (ontology = null).
                currentTermName = olsConnection.getTermById(termIdSearchJTextField.getText().trim(), ontology);
            }

            if (currentTermName == null) {
                JOptionPane.showMessageDialog(this, "No matching terms found.", "No Matching Terms", JOptionPane.INFORMATION_MESSAGE);
                termIdSearchJTextField.requestFocus();
            } else {

                if (currentTermName.equalsIgnoreCase(termIdSearchJTextField.getText().trim())) {
                    JOptionPane.showMessageDialog(this, "No matching terms found.", "No Matching Terms", JOptionPane.INFORMATION_MESSAGE);
                    termIdSearchJTextField.requestFocus();
                } else {
                    ((DefaultTableModel) olsResultsTermIdSearchJXTable.getModel()).addRow(new Object[]{
                                termIdSearchJTextField.getText().trim(), currentTermName});
                }
            }

            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
            termIdSearchJTextField.setCursor(new java.awt.Cursor(java.awt.Cursor.TEXT_CURSOR));

            // make the first row visible
            if (olsResultsTermIdSearchJXTable.getRowCount() > 0) {
                olsResultsTermIdSearchJXTable.scrollRectToVisible(olsResultsTermIdSearchJXTable.getCellRect(0, 0, false));
            }

        } catch (RemoteException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    defaultOlsConnectionFailureErrorMessage,
                    "OLS Connection Error", JOptionPane.ERROR_MESSAGE);
            Util.writeToErrorLog("Error when trying to access OLS: ");
            ex.printStackTrace();
        }

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        termIdSearchJTextField.setCursor(new java.awt.Cursor(java.awt.Cursor.TEXT_CURSOR));

    }//GEN-LAST:event_termIdSearchJButtonActionPerformed

    /**
     * If 'Enter' is pressed and the 'Search' button is enabled, the search is performed.
     *
     * @param evt
     */
    private void modificationMassJTextFieldKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_modificationMassJTextFieldKeyPressed
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            if (modificationMassSearchJButton.isEnabled()) {
                modificationMassSearchJButtonActionPerformed(null);
            }
        }
    }//GEN-LAST:event_modificationMassJTextFieldKeyPressed

    /**
     * @see #modificationMassJTextFieldKeyPressed(java.awt.event.KeyEvent) 
     */
    private void precisionJTextFieldKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_precisionJTextFieldKeyPressed
        modificationMassJTextFieldKeyPressed(evt);
    }//GEN-LAST:event_precisionJTextFieldKeyPressed

    /**
     * If 'Enter' is pressed and the 'Search' button is enabled the search is performed.
     * Also enables or disables the search button when the field contains text or not.
     *
     * @param evt
     */
    private void termIdSearchJTextFieldKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_termIdSearchJTextFieldKeyPressed
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            if (termIdSearchJButton.isEnabled()) {
                termIdSearchJButtonActionPerformed(null);
            }
        } else {
            termIdSearchJButton.setEnabled(termIdSearchJTextField.getText().length() > 0);
        }
    }//GEN-LAST:event_termIdSearchJTextFieldKeyPressed

    /**
     * Opens a dialog displaying the most common species for easy selection.
     *
     * @param evt
     */
    private void newtSpeciesTipsTermNameSearchJLabelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_newtSpeciesTipsTermNameSearchJLabelMouseClicked
        if (newtSpeciesTipsTermNameSearchJLabel.getForeground() == Color.BLUE) {
            new SimpleNewtSelection(this, true);
        }
    }//GEN-LAST:event_newtSpeciesTipsTermNameSearchJLabelMouseClicked

    /**
     * Changes the cursor to the hand cursor when over the term hierarchy link.
     *
     * @param evt
     */
    private void newtSpeciesTipsTermNameSearchJLabelMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_newtSpeciesTipsTermNameSearchJLabelMouseEntered
        if (newtSpeciesTipsTermNameSearchJLabel.getForeground() == Color.BLUE) {
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        }
    }//GEN-LAST:event_newtSpeciesTipsTermNameSearchJLabelMouseEntered

    /**
     * Changes the cursor back to the default cursor when leaving the term hierarchy link.
     *
     * @param evt
     */
    private void newtSpeciesTipsTermNameSearchJLabelMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_newtSpeciesTipsTermNameSearchJLabelMouseExited
        if (newtSpeciesTipsTermNameSearchJLabel.getForeground() == Color.BLUE) {
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        }
    }//GEN-LAST:event_newtSpeciesTipsTermNameSearchJLabelMouseExited

    /**
     * Opens a dialog displaying the most common species for easy selection.
     *
     * @param evt
     */
    private void newtSpeciesTipsTermIdSearchJLabelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_newtSpeciesTipsTermIdSearchJLabelMouseClicked
        if (newtSpeciesTipsTermIdSearchJLabel.getForeground() == Color.BLUE) {
            new SimpleNewtSelection(this, true);
        }
    }//GEN-LAST:event_newtSpeciesTipsTermIdSearchJLabelMouseClicked

    /**
     * Changes the cursor back to the default cursor when leaving the term hierarchy link.
     *
     * @param evt
     */
    private void newtSpeciesTipsTermIdSearchJLabelMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_newtSpeciesTipsTermIdSearchJLabelMouseEntered
        if (newtSpeciesTipsTermIdSearchJLabel.getForeground() == Color.BLUE) {
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        }
    }//GEN-LAST:event_newtSpeciesTipsTermIdSearchJLabelMouseEntered

    /**
     * Changes the cursor back to the default cursor when leaving the term hierarchy link.
     *
     * @param evt
     */
    private void newtSpeciesTipsTermIdSearchJLabelMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_newtSpeciesTipsTermIdSearchJLabelMouseExited
        if (newtSpeciesTipsTermIdSearchJLabel.getForeground() == Color.BLUE) {
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        }
    }//GEN-LAST:event_newtSpeciesTipsTermIdSearchJLabelMouseExited

    /**
     * Opens a new dialog showing the term hierarchy as a graph.
     */
    private void viewTermHierarchy() {

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

        String accession = null;

        if (searchTypeJTabbedPane.getSelectedIndex() == OLS_DIALOG_TERM_NAME_SEARCH) {
            accession = "" + olsResultsTermNameSearchJXTable.getValueAt(olsResultsTermNameSearchJXTable.getSelectedRow(), 0);
        } else if (searchTypeJTabbedPane.getSelectedIndex() == OLS_DIALOG_BROWSE_ONTOLOGY) {
            accession = currentlySelectedBrowseOntologyAccessionNumber;
        } else if (searchTypeJTabbedPane.getSelectedIndex() == OLS_DIALOG_PSI_MOD_MASS_SEARCH) {
            accession = "" + olsResultsMassSearchJXTable.getValueAt(olsResultsMassSearchJXTable.getSelectedRow(), 0);
        } else if (searchTypeJTabbedPane.getSelectedIndex() == OLS_DIALOG_TERM_ID_SEARCH) {
            accession = "" + olsResultsTermIdSearchJXTable.getValueAt(olsResultsTermIdSearchJXTable.getSelectedRow(), 0);
        }

        String selectedValue = "";
        String ontology = getCurrentOntologyLabel();

        try {
            selectedValue = olsConnection.getTermById(accession, ontology);
        } catch (RemoteException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    defaultOlsConnectionFailureErrorMessage,
                    "OLS Connection Error", JOptionPane.ERROR_MESSAGE);
            Util.writeToErrorLog("Error when trying to access OLS: ");
            ex.printStackTrace();
        }

        new TermHierarchyGraphViewer(this, true, accession, selectedValue, ontology);

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }

    private boolean isPreselectedOption(){
        if(preselectedOntologies.size() > 1 && ontologyJComboBox.getSelectedIndex() == 1){
            return true;
        } else {
            return false;
        }
    }

    /**
     * Inserts a NEWT term into the currently opened search tab.
     *
     * @param termName the term name
     * @param termId the terms id
     */
    public void insertNewtSelection(String termName, String termId) {

        if (searchTypeJTabbedPane.getSelectedIndex() == OLS_DIALOG_TERM_NAME_SEARCH) {
            termNameSearchJTextField.setText(termName);
            termNameSearchJTextFieldKeyReleased(null);
        } else if (searchTypeJTabbedPane.getSelectedIndex() == OLS_DIALOG_TERM_ID_SEARCH) {
            termIdSearchJTextField.setText(termId);
            termIdSearchJButtonActionPerformed(null);
        }
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton aboutJButton;
    private javax.swing.JPanel browseJPanel;
    private javax.swing.JPanel browseOntologyJPanel;
    private javax.swing.JButton cancelJButton;
    private javax.swing.JTextPane definitionBrowseOntologyJTextPane;
    private javax.swing.JTextPane definitionMassSearchJTextPane;
    private javax.swing.JTextPane definitionTermIdSearchJTextPane;
    private javax.swing.JTextPane definitionTermNameSearchJTextPane;
    private javax.swing.JLabel dummyLabelJLabel;
    private javax.swing.JButton helpJButton;
    private javax.swing.JButton insertSelectedJButton;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JScrollPane jScrollPane7;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JPanel massSearchJPanel;
    private javax.swing.JComboBox massTypeJComboBox;
    private javax.swing.JTextField modificationMassJTextField;
    private javax.swing.JButton modificationMassSearchJButton;
    private javax.swing.JLabel newtSpeciesTipsTermIdSearchJLabel;
    private javax.swing.JLabel newtSpeciesTipsTermNameSearchJLabel;
    private javax.swing.JTextField numberOfTermsTermNameSearchJTextField;
    private javax.swing.JScrollPane olsResultsMassSearchJScrollPane;
    private org.jdesktop.swingx.JXTable olsResultsMassSearchJXTable;
    private javax.swing.JScrollPane olsResultsTermIdSearchJScrollPane;
    private org.jdesktop.swingx.JXTable olsResultsTermIdSearchJXTable;
    private javax.swing.JScrollPane olsResultsTermNameSearchJScrollPane;
    private org.jdesktop.swingx.JXTable olsResultsTermNameSearchJXTable;
    private javax.swing.JComboBox ontologyJComboBox;
    private javax.swing.JLabel ontologyJLabel;
    private javax.swing.JTextField precisionJTextField;
    private javax.swing.JPanel searchParametersJPanel;
    private javax.swing.JLabel searchResultsTermNameJLabel;
    private javax.swing.JTabbedPane searchTypeJTabbedPane;
    private javax.swing.JLabel selectedTermTermNameJLabel;
    private javax.swing.JScrollPane termDetailsBrowseOntologyJScrollPane;
    private org.jdesktop.swingx.JXTable termDetailsBrowseOntologyJXTable;
    private javax.swing.JScrollPane termDetailsMassSearchJScrollPane;
    private org.jdesktop.swingx.JXTable termDetailsMassSearchJXTable;
    private javax.swing.JScrollPane termDetailsTermIdSearchJScrollPane;
    private org.jdesktop.swingx.JXTable termDetailsTermIdSearchJXTable;
    private javax.swing.JScrollPane termDetailsTermNameSearchJScrollPane;
    private org.jdesktop.swingx.JXTable termDetailsTermNameSearchJXTable;
    private javax.swing.JButton termIdSearchJButton;
    private javax.swing.JPanel termIdSearchJPanel;
    private javax.swing.JTextField termIdSearchJTextField;
    private javax.swing.JLabel termNameJLabel;
    private javax.swing.JPanel termNameSearchJPanel;
    private javax.swing.JTextField termNameSearchJTextField;
    private javax.swing.JLabel viewTermHierarchyBrowseOntologyJLabel;
    private javax.swing.JLabel viewTermHierarchyMassSearchJLabel;
    private javax.swing.JLabel viewTermHierarchyTermIdSearchJLabel;
    private javax.swing.JLabel viewTermHierarchyTermNameSearchJLabel;
    // End of variables declaration//GEN-END:variables
}
