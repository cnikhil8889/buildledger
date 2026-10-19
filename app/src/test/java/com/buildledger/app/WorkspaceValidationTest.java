package com.buildledger.app;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class WorkspaceValidationTest {
    private JSONObject valid() throws Exception {
        JSONObject project=new JSONObject().put("id","p1").put("name","Greenfield Homes").put("location","Hyderabad").put("state","Telangana").put("notes","").put("budget",1000000).put("year",2026).put("progress",30).put("type","Residential").put("status","In progress").put("createdAt","2026-10-17").put("requirements",new JSONArray());
        JSONObject buyer=new JSONObject().put("id","b1").put("projectId","p1").put("name","Test Buyer").put("unit","A-101").put("phone","").put("notes","").put("saleValue",800000).put("date","2026-10-17");
        JSONObject expense=new JSONObject().put("id","t1").put("projectId","p1").put("direction","expense").put("category","Steel").put("party","Steel supplier").put("amount",120000).put("paid",75000).put("date","2026-10-17").put("dueDate","").put("method","Bank transfer").put("reference","").put("buyerId","").put("notes","");
        return new JSONObject().put("builderName","Sample Builder").put("city","Hyderabad").put("storage","cloud").put("projects",new JSONArray().put(project)).put("buyers",new JSONArray().put(buyer)).put("payments",new JSONArray().put(expense));
    }
    private void rejected(JSONObject w) throws Exception {assertThrows(Exception.class,()->LedgerStore.validateWorkspace(w));}
    @Test public void completeWorkspaceAccepted() throws Exception {LedgerStore.validateWorkspace(valid());}
    @Test public void paymentAboveBillRejected() throws Exception {JSONObject w=valid();w.getJSONArray("payments").getJSONObject(0).put("paid",120001);rejected(w);}
    @Test public void negativeBudgetRejected() throws Exception {JSONObject w=valid();w.getJSONArray("projects").getJSONObject(0).put("budget",-1);rejected(w);}
    @Test public void extraMoneyPrecisionRejected() throws Exception {JSONObject w=valid();w.getJSONArray("projects").getJSONObject(0).put("budget",1000.001);rejected(w);}
    @Test public void invalidCalendarDayRejected() throws Exception {JSONObject w=valid();w.getJSONArray("payments").getJSONObject(0).put("date","2026-02-31");rejected(w);}
    @Test public void duplicateProjectRejected() throws Exception {JSONObject w=valid();w.getJSONArray("projects").put(w.getJSONArray("projects").getJSONObject(0));rejected(w);}
    @Test public void missingBuyerProjectRejected() throws Exception {JSONObject w=valid();w.getJSONArray("buyers").getJSONObject(0).put("projectId","foreign");rejected(w);}
    @Test public void crossProjectCollectionRejected() throws Exception {JSONObject w=valid();w.getJSONArray("payments").getJSONObject(0).put("direction","receipt").put("buyerId","missing");rejected(w);}
}
