package org.adoxx.dashboard.helper;

import java.awt.Choice;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.adoxx.dashboard.helper.utils.Utils;

public class DashboardHelper {

    private static final String defaultDashboardRestHost = "http://127.0.0.1:8080/dashboard/rest";
    private static final String dashboardRestEndpoint = "/datasourceWrapper";
    
    public static String chooseDatasourceType(String lang, String hostName, String previousData) throws Exception{
        final JOptionPane optionPane = new JOptionPane();
        final JPanel panel = new JPanel();
        final Choice datasourcesComboBox = new Choice();
        final JLabel datasourceDescriptionLabel = new JLabel();
        
        if(hostName == null || hostName.equals(""))
            hostName = defaultDashboardRestHost;
        if(hostName.endsWith("/"))
            hostName = hostName.substring(0, hostName.length()-1);
        
        String datasourceModulesConfig = new String(Utils.sendHTTP(hostName + dashboardRestEndpoint + "/getModules", "GET", null, null, true, true).data, "UTF-8");
        JsonObject datasourceModulesConfigJson = Json.createReader(new StringReader(datasourceModulesConfig)).readObject();
        
        final HashMap<String, String> datasourceTypeDesc = new HashMap<String, String>();
        
        for(JsonValue moduleJsonVal : datasourceModulesConfigJson.getJsonArray("moduleList")){
            JsonObject moduleJson = (JsonObject) moduleJsonVal;
            String moduleName = moduleJson.getString("name");
            String moduleDesc = moduleJson.getJsonObject("description").getString(lang);
            datasourceTypeDesc.put(moduleName, moduleDesc);
        }
        int count = 0;
        int selectedIndex = 0;
        for(String datasourceName : datasourceTypeDesc.keySet()){
            datasourcesComboBox.add(datasourceName);
            if(previousData.equals(datasourceName))
                selectedIndex = count;
            count++;
        }
        datasourcesComboBox.select(selectedIndex);
        
        datasourceDescriptionLabel.setText(datasourceTypeDesc.get(datasourcesComboBox.getSelectedItem()));
        
        datasourcesComboBox.addItemListener(new ItemListener(){
            public void itemStateChanged(ItemEvent e){
                datasourceDescriptionLabel.setText(datasourceTypeDesc.get(datasourcesComboBox.getSelectedItem()));
                panel.validate();
                panel.repaint();
            }
        });
        
        optionPane.setMessageType(JOptionPane.PLAIN_MESSAGE);
        
        panel.setLayout(new GridLayout(2, 1));
        
        JButton okButton = new JButton("Ok");
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                optionPane.setValue(JOptionPane.OK_OPTION);
            }
        });
        
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                optionPane.setValue(JOptionPane.CLOSED_OPTION);
            }
        });
        
        panel.add(datasourcesComboBox);
        panel.add(datasourceDescriptionLabel);
        
        optionPane.setMessage(panel);
        optionPane.setOptions(new Object[] { okButton, cancelButton});
        
        JDialog dialog = optionPane.createDialog(null, "Datasource Type selection");
        dialog.setVisible(true);
        
        int retval = (optionPane.getValue() instanceof Integer)?((Integer)optionPane.getValue()).intValue():-1;
        dialog.dispose();
        
        if(retval == JOptionPane.OK_OPTION)
            return (String)datasourcesComboBox.getSelectedItem();
        
        throw new Exception("ABORTED");
    }
    
    public static JsonObject chooseDatasourceConfiguration(String lang, String datasourceType, String hostName, JsonObject previousData) throws Exception{
        final JOptionPane optionPane = new JOptionPane();
        JPanel panel = new JPanel();
        final HashMap<String, JTextField> textFieldMap = new HashMap<String, JTextField>();
        HashMap<String, JLabel> labelMap = new HashMap<String, JLabel>();
        
        if(hostName == null || hostName.equals(""))
            hostName = defaultDashboardRestHost;
        if(hostName.endsWith("/"))
            hostName = hostName.substring(0, hostName.length()-1);
        final String hostNameF = hostName;
        
        String datasourceModulesConfig = new String(Utils.sendHTTP(hostNameF + dashboardRestEndpoint + "/getModules", "GET", null, null, true, true).data, "UTF-8");
        JsonObject datasourceModulesConfigJson = Json.createReader(new StringReader(datasourceModulesConfig)).readObject();
        
        JsonObject configuration = null;
        for(JsonValue moduleJsonVal : datasourceModulesConfigJson.getJsonArray("moduleList")){
            JsonObject moduleJson = (JsonObject) moduleJsonVal;
            if(moduleJson.getString("name").equals(datasourceType)){
                configuration = moduleJson.getJsonObject("configuration");
                break;
            }
        }
        
        if(configuration == null)
            throw new Exception("Impossible to find a Datasource with name " + datasourceType);
        
        panel.setLayout(new GridLayout(configuration.keySet().size(), 2, 1, 1));
        
        for (String key : configuration.keySet()) {
            String description = configuration.getJsonObject(key).getJsonObject("description").getString(lang);
            JLabel lbl = new JLabel(description);
            final JTextField txt = new JTextField();
            txt.setText((previousData.containsKey(key))?previousData.getJsonObject(key).getString("value"):"");
            labelMap.put(key, lbl);
            textFieldMap.put(key, txt);
            
            JPanel panelPair = new JPanel();
            panelPair.setLayout(new GridLayout(2, 1));
            panelPair.add(lbl);
            panelPair.add(txt);
            
            if(configuration.getJsonObject(key).containsKey("moreInfos") && configuration.getJsonObject(key).getJsonObject("moreInfos").containsKey("requireUpload") && configuration.getJsonObject(key).getJsonObject("moreInfos").getBoolean("requireUpload")){
                JButton uploadButton = new JButton("Upload");
                uploadButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        
                        JFileChooser jfc = new JFileChooser();
                        jfc.setDialogTitle("Choose the files to sign");
                        jfc.setMultiSelectionEnabled(false);
                        if(jfc.showOpenDialog(null) != JFileChooser.APPROVE_OPTION)
                            return;
                        File file = jfc.getSelectedFile();
                        try {
                            ArrayList<String[]> htmlHeaderList = new ArrayList<String[]>();
                            htmlHeaderList.add(new String[]{"Content-Type", "application/octet-stream"});
                            String uploadRes = new String(Utils.sendHTTP(hostNameF + dashboardRestEndpoint + "/uploadLocalDatasource?fileName="+file.getName(), "POST", Utils.readFile(file), htmlHeaderList, true, true).data, "UTF-8");
                            txt.setText(uploadRes);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            txt.setText("upload failed: " + ex.getMessage());
                        }
                    }
                });
                panelPair.add(uploadButton);
                panelPair.setLayout(new GridLayout(3, 1));
            }
            
            TitledBorder title = BorderFactory.createTitledBorder(key);
            title.setTitleJustification(TitledBorder.LEFT);
            panelPair.setBorder(title);
            panel.add(panelPair);
        }

        optionPane.setMessageType(JOptionPane.PLAIN_MESSAGE);
        
        JButton okButton = new JButton("Ok");
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                /*
                boolean incorrectFilled = false;
                for(JTextField txt : textFieldMap.values())
                    if(txt.getText().equals("")){
                        incorrectFilled = true;
                        break;
                    }
                if(incorrectFilled)
                    JOptionPane.showMessageDialog(null, "Empty Text field present!", "Error", JOptionPane.ERROR_MESSAGE);
                else
                    optionPane.setValue(JOptionPane.OK_OPTION);
                */
                optionPane.setValue(JOptionPane.OK_OPTION);
            }
        });
        
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                optionPane.setValue(JOptionPane.CLOSED_OPTION);
            }
        });
        
        optionPane.setMessage(panel);
        optionPane.setOptions(new Object[] { okButton, cancelButton});
        
        JDialog dialog = optionPane.createDialog(null, "Datasource Configuration Parameters");
        dialog.setVisible(true);
        
        int retval = (optionPane.getValue() instanceof Integer)?((Integer)optionPane.getValue()).intValue():-1;
        dialog.dispose();
        
        if(retval == JOptionPane.OK_OPTION) {
            JsonObjectBuilder ret = Json.createObjectBuilder();
            for(Entry<String, JTextField> entry : textFieldMap.entrySet())
                ret.add(entry.getKey(), Json.createObjectBuilder().add("value", entry.getValue().getText()));
            
            return ret.build();
        }
        
        throw new Exception("ABORTED");
    }
    
    public static JsonArray chooseDatasourceUserInputs(JsonArray previousData) throws Exception{
        
        final JOptionPane optionPane = new JOptionPane();
        final JPanel panel = new JPanel();
        final ArrayList<JTextField> keyArray = new ArrayList<JTextField>();
        final ArrayList<JTextField> descArray = new ArrayList<JTextField>();
        
        JPanel headP = new JPanel();
        panel.add(headP);
        JButton addButton = new JButton("+");
        headP.add(addButton);
       
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JPanel row = new JPanel();
                JTextField keyTextfield = new JTextField();
                keyArray.add(keyTextfield);
                JTextField descTextfield = new JTextField();
                descArray.add(descTextfield);
                
                row.add(new JLabel("Key : "));
                row.add(keyTextfield);
                row.add(new JLabel("Description : "));
                row.add(descTextfield);
                row.setLayout(new GridLayout(1, 4));
                panel.add(row);
                 
                row.revalidate();
                row.repaint();
            }
        });
        
        for(JsonValue prevV : previousData){
            JsonObject prev = (JsonObject)prevV;
            
            JPanel row = new JPanel();
            JTextField keyTextfield = new JTextField();
            keyTextfield.setText(prev.getString("value"));
            keyArray.add(keyTextfield);
            JTextField descTextfield = new JTextField();
            descTextfield.setText(prev.getString("description"));
            descArray.add(descTextfield);
            
            row.add(new JLabel("Key : "));
            row.add(keyTextfield);
            row.add(new JLabel("Description : "));
            row.add(descTextfield);
            row.setLayout(new GridLayout(1, 4));
            panel.add(row);
        }
        panel.setLayout(new GridLayout(previousData.size() + 5, 1));
        
        TitledBorder title = BorderFactory.createTitledBorder("Click \"+\" to add a new user input");
        title.setTitleJustification(TitledBorder.LEFT);
        panel.setBorder(title);
        
        optionPane.setMessage(panel);  
            
        JButton okButton = new JButton("Ok");
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                
                boolean incorrectFilled = false;
                for(JTextField txt : keyArray)
                    if(txt.getText().equals("")){
                        incorrectFilled = true;
                        break;
                    }
                for(JTextField txt : descArray)
                    if(txt.getText().equals("")){
                        incorrectFilled = true;
                        break;
                    }
                if(incorrectFilled)
                    JOptionPane.showMessageDialog(null, "Empty Text field present!", "Error", JOptionPane.ERROR_MESSAGE);
                else
                    optionPane.setValue(JOptionPane.OK_OPTION);
            }
        });
        
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                optionPane.setValue(JOptionPane.CLOSED_OPTION);
            }
        });
        optionPane.setMessage(panel); 
        
        optionPane.setOptions(new Object[] { okButton, cancelButton});
        JDialog dialog = optionPane.createDialog(null, "Datasource User Inputs");
        dialog.setResizable(true);
        dialog.setVisible(true);
        
        int retval = (optionPane.getValue() instanceof Integer)?((Integer)optionPane.getValue()).intValue():-1;
        dialog.dispose();
        
        if(retval == JOptionPane.OK_OPTION) {           
            JsonArrayBuilder retArray = Json.createArrayBuilder();
            
            for(int i=0;i<keyArray.size();i++){
                retArray.add(Json.createObjectBuilder()
                    .add("value", keyArray.get(i).getText())
                    .add("description", descArray.get(i).getText())
                );
            }
    
            return retArray.build();
        }
        
        throw new Exception("ABORTED");
    }
    
    public static JsonArray chooseKpiFields(JsonArray previousData) throws Exception{
        
        final JOptionPane optionPane = new JOptionPane();
        final JPanel panel = new JPanel();
        final ArrayList<JTextField> nameArray = new ArrayList<JTextField>();
        final ArrayList<JTextField> mUnitArray = new ArrayList<JTextField>();
        
        JPanel headP = new JPanel();
        panel.add(headP);
        JButton addButton = new JButton("+");
        headP.add(addButton);
       
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JPanel row = new JPanel();
                JTextField nameTxt = new JTextField();
                nameArray.add(nameTxt);
                JTextField mUnitTxt = new JTextField();
                mUnitArray.add(mUnitTxt);
                
                row.add(new JLabel("Name : "));
                row.add(nameTxt);
                row.add(new JLabel("Measure Unit : "));
                row.add(mUnitTxt);
                row.setLayout(new GridLayout(1, 4));
                panel.add(row);
                 
                row.revalidate();
                row.repaint();
            }
        });
        
        for(JsonValue prevV : previousData){
            JsonObject prev = (JsonObject)prevV;
            
            JPanel row = new JPanel();
            JTextField nameTxt = new JTextField();
            nameTxt.setText(prev.getString("name"));
            nameArray.add(nameTxt);
            JTextField mUnitTxt = new JTextField();
            mUnitTxt.setText(prev.getString("measureUnit"));
            mUnitArray.add(mUnitTxt);
            
            row.add(new JLabel("Name : "));
            row.add(nameTxt);
            row.add(new JLabel("Measure Unit : "));
            row.add(mUnitTxt);
            row.setLayout(new GridLayout(1, 4));
            panel.add(row);
        }
        panel.setLayout(new GridLayout(previousData.size() + 5, 1));
        
        TitledBorder title = BorderFactory.createTitledBorder("Click \"+\" to add a new kpi field");
        title.setTitleJustification(TitledBorder.LEFT);
        panel.setBorder(title);
        
        optionPane.setMessage(panel);  
            
        JButton okButton = new JButton("Ok");
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                optionPane.setValue(JOptionPane.OK_OPTION);
            }
        });
        
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                optionPane.setValue(JOptionPane.CLOSED_OPTION);
            }
        });
        optionPane.setMessage(panel); 
        
        optionPane.setOptions(new Object[] { okButton, cancelButton});
        JDialog dialog = optionPane.createDialog(null, "Kpi Fields");
        dialog.setResizable(true);
        dialog.setVisible(true);
        
        int retval = (optionPane.getValue() instanceof Integer)?((Integer)optionPane.getValue()).intValue():-1;
        dialog.dispose();
        
        if(retval == JOptionPane.OK_OPTION) {           
            JsonArrayBuilder retArray = Json.createArrayBuilder();
            
            for(int i=0;i<nameArray.size();i++){
                retArray.add(Json.createObjectBuilder()
                    .add("name", nameArray.get(i).getText())
                    .add("measureUnit", mUnitArray.get(i).getText())
                );
            }
    
            return retArray.build();
        }
        
        throw new Exception("ABORTED");
    }
    
    public static JsonObject testDatasource(String datasourceType, JsonObject datasourceConfig, JsonObject userInputs) throws Exception{
        throw new Exception("TODO");
    }
    
    public static JsonObject testAlgorithmKpi(JsonObject model, String kpiId) throws Exception{
        throw new Exception("TODO");
    }
    
    public static JsonObject testAlgorithmGoal(JsonObject model, String goalId) throws Exception{
        throw new Exception("TODO");
    }
}
