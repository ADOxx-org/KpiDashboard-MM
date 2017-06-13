package org.adoxx.dashboard.helper;

import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

public class Main {

    public static void main(String[] args) {
        // TODO Auto-generated method stub
        try{
            if(args.length == 0)
                throw new Exception("A parameter must be provided");
            
            switch(args[0]){
                case "chooseDatasourceType": {
                    if(args.length < 3)
                        throw new Exception("The parameter lang and the hostname must be provided");
                    String lang = args[1];
                    String host = args[2];
                    String previous = (args.length>=4)?args[3]:"";
                    String ret = DashboardHelper.chooseDatasourceType(lang, host, previous);
                    System.out.print(ret);
                    break;
                }
                case "chooseDatasourceConfiguration": {
                    if(args.length < 4)
                        throw new Exception("The parameters lang, datasourceType and hostname must be provided");
                    String lang = args[1];
                    String datasourceType = args[2];
                    String host = args[3];
                    String previous = (args.length>=5)?args[4]:"";
                    if(previous.equals(""))
                        previous = "{}";
                    JsonObject previousJ = Json.createReader(new StringReader(previous)).readObject();
                    String ret = DashboardHelper.chooseDatasourceConfiguration(lang, datasourceType, host, previousJ).toString();
                    System.out.print(ret);
                    break;
                }
                
                case "chooseDatasourceUserInputs": {
                    String previous = (args.length>=2)?args[1]:"";
                    if(previous.equals(""))
                        previous = "[]";
                    JsonArray previousJ = Json.createReader(new StringReader(previous)).readArray();
                    String ret = DashboardHelper.chooseDatasourceUserInputs(previousJ).toString();
                    System.out.print(ret);
                    break;
                }
                
                case "chooseKpiFields": {
                    String previous = (args.length>=2)?args[1]:"";
                    if(previous.equals(""))
                        previous = "[]";
                    JsonArray previousJ = Json.createReader(new StringReader(previous)).readArray();
                    String ret = DashboardHelper.chooseKpiFields(previousJ).toString();
                    System.out.print(ret);
                    break;
                }
                
                default : throw new Exception("Parameter " + args[0] + " not implemented");
            }
            
        }catch(Exception ex){
            ex.printStackTrace();
        }
    }

}
