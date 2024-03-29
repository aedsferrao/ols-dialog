package no.uib.olsdialog.util;

import no.uib.olsdialog.OLSDialog;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * A simple dialog to retrieve and display the term hierarchy of a given CV
 * term.
 *
 * @author Harald Barsnes
 */
public class TermHierarchyGraphViewer extends javax.swing.JDialog {

    /**
     * The maximum dialog width.
     */
    final int MAXIMUM_DIALOG_WIDTH = 600;
    /**
     * The minimum dialog width.
     */
    final int MINIMUM_DIALOG_WIDTH = 250;
    /**
     * The maximum dialog height.
     */
    final int MAXIMUM_DIALOG_HEIGHT = 600;
    /**
     * The vertical padding.
     */
    final int VERTICAL_PADDING = 40;
    /**
     * The horizontal padding.
     */
    final int HORIZONTAL_PADDING = 40;
    /**
     * The scroll pane for the graph viewer.
     */
    private JScrollPane graphViewerScrollPane;

    /**
     * Creates new TermHierarchyGraphViewer dialog and makes it visible.
     *
     * @param olsDialog a reference to the OLS Dialog
     * @param modal
     * @param termId the accession number of the term to search for
     * @param termName the name of the term to search for
     * @param ontology the ontology to search in
     */
    public TermHierarchyGraphViewer(OLSDialog olsDialog, boolean modal, String termId, String termName, String ontology) {
        super(olsDialog, modal);

        initComponents();

        setTitle("Term Hierarchy: " + termId);

        try {
            // prepare the term details
            termId = termId.replace(":", "%3A");
            termName = termName.replace(" ", "_");
            termName = termName.toLowerCase();

            // create the "search url"
            String url = "http://www.ebi.ac.uk/ontology-lookup/generateSSFiles.do?" + "termId=" + termId
                    + "&termName=" + termName
                    + "&ontologyName=" + ontology
                    + "&graphType=root";

            // read the contents of the returned xml file
            URL mapPage = new URL(url);

            BufferedReader in = new BufferedReader(new InputStreamReader(mapPage.openStream()));

            String inputLine;
            String contents = "";

            while ((inputLine = in.readLine()) != null) {
                contents += inputLine;
            }

            // and extract the image file reference from the xml file
            String imageFile = contents.substring(
                    contents.lastIndexOf("<imgFile>") + "<imgFile>".length(), contents.lastIndexOf("</imgFile>"));

            // create the image file url
            URL imageUrlAsUrl = new URL("http://www.ebi.ac.uk/ontology-lookup/serveImgFile.do?imgFileName=" + imageFile);

            // create the graph image and add it to the scroll pane
            java.awt.Image image = Toolkit.getDefaultToolkit().createImage(imageUrlAsUrl);
            ImageIcon ii = new ImageIcon(image);
            JLabel label = new JLabel(ii);
            label.setBackground(Color.WHITE);
            label.setOpaque(true);

            // add some padding to the size of the label
            int width = ii.getIconWidth() + HORIZONTAL_PADDING;
            int height = ii.getIconHeight() + VERTICAL_PADDING;

            label.setPreferredSize(new Dimension(width, height));

            // add the label to the scroll pane
            graphViewerScrollPane = new JScrollPane(label);
            graphViewerScrollPane.setBackground(Color.WHITE);
            graphViewerScrollPane.setOpaque(true);

            getContentPane().add(graphViewerScrollPane);

            // make sure the dialog does not become too big (or too small)
            if (width > MAXIMUM_DIALOG_WIDTH) {
                width = MAXIMUM_DIALOG_WIDTH;
            } else if (width < MINIMUM_DIALOG_WIDTH) {
                width = MINIMUM_DIALOG_WIDTH;
            }

            if (height > MAXIMUM_DIALOG_HEIGHT) {
                height = MAXIMUM_DIALOG_HEIGHT;
            }

            this.setSize(width + 50, height + 50); // have to add additional size due to possible scroll bars

            // invoke later to give time for the scroll bar to update
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    // move the vertical scroll bar to the bottom to make sure
                    // the selected node is showing (the selected node is always
                    // at the bottom of the graph)
                    graphViewerScrollPane.getVerticalScrollBar().setValue(graphViewerScrollPane.getVerticalScrollBar().getMaximum());
                }
            });

        } catch (MalformedURLException e) {
            JOptionPane.showMessageDialog(
                    olsDialog,
                    "An error occured when opening the term hierarcy.",
                    "Error Opening Term Hierarcy", JOptionPane.ERROR_MESSAGE);
            Util.writeToErrorLog("Error Opening Term Hierarcy: ");
            e.printStackTrace();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(
                    olsDialog,
                    "An error occured when opening the term hierarcy.",
                    "Error Opening Term Hierarcy", JOptionPane.ERROR_MESSAGE);
            Util.writeToErrorLog("Error Opening Term Hierarcy: ");
            e.printStackTrace();
        }

        setLocationRelativeTo(olsDialog);
        setVisible(true);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Term Hierarchy");
        getContentPane().setLayout(new javax.swing.BoxLayout(getContentPane(), javax.swing.BoxLayout.LINE_AXIS));

        pack();
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
