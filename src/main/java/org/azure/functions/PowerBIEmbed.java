package org.azure.functions;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class PowerBIEmbed {


    // Static Values
    private static final String TENANT_ID = "f0e895f6-7ad5-4f09-907c-b52f2bbefe34";
    private static final String AUTHORITY_URL = "https://login.microsoftonline.com/"+TENANT_ID+"/oauth2/v2.0/token";
    private static final String SCOPE = "https://analysis.windows.net/powerbi/api/.default";
    private static final String CLIENT_ID = "b40f0ed2-f584-4852-b7f9-e82230bb68cc";
    private static final String CLIENT_SECRET = "bH-8Q~Wo93CaGw4aMPeZYoORFPapxjwOG4S-GbAb";

    private static final String GROUP_ID = "0a42eae1-4c8d-4dac-9cfe-4dfad8778be2"; // Workspace ID
    private static final String REPORT_ID = "5ff711a9-55b8-493b-b480-b3a6dd9401d4";

    public static void main(String[] args) {
        try {
            log.info("Authenticating with Azure Active Directory");
            String accessToken = authenticateWithAzureAD();

            log.info("Authentication successful i.e., access token is {}", accessToken);

            // Get Power BI Report Embed URL and Token
            JsonObject embedContent = getPowerBiReport(accessToken);

            // Print JSON Response
            log.info("Final JSON response {}",new Gson().toJson(embedContent));
        }
        catch (Exception e){
            log.error("Exception Occured {}", e.getMessage());
        }
    }


    private static JsonObject getPowerBiReport(String accessToken) throws Exception {

        try(CloseableHttpClient httpClient = HttpClients.createDefault()){
            String url = String.format("https://api.powerbi.com/v1.0/myorg/groups/%s/reports/%s", GROUP_ID, REPORT_ID);
            HttpGet getReport = new HttpGet(url);
            getReport.setHeader("Authorization", "Bearer " + accessToken);

            /**
             * Ensure
             *
             * 1. Ensure Correct API Permissions
             *
             * Make sure the service principal has the following API permissions:
             *
             * 	•	Power BI Service API: Report.Read.All and Report.Write.All
             *
             * These permissions need to be granted and then admin consent must be provided for them.
             *
             * 2. Enable Service Principal Access in Power BI
             *
             * You need to enable the settings in the Power BI admin portal to allow service principal access:
             *
             * 	1.	Allow service principals to use Power BI APIs:
             * 	•	Go to the Power BI admin portal.
             * 	•	Under “Tenant settings,” enable the setting for “Allow service principals to use Power BI APIs” for your specific security group.
             * 	2.	Service principals can access admin APIs used for updates:
             * 	•	Enable this setting under the “Admin API settings” section for your specific security group.
             *
             * 3. Configure Security Groups in Azure AD
             *
             * Ensure the service principal is part of the security group specified in the Power BI admin portal settings.
             *
             * 4. Verify Permissions in Azure Portal
             *
             * Make sure the service principal has been granted the required API permissions in the Azure portal:
             *
             * 	1.	Go to the Azure portal.
             * 	2.	Navigate to your Azure AD tenant.
             * 	3.	Select “App registrations” and find your service principal (application).
             * 	4.	Under “API permissions,” ensure the Power BI Service API permissions (Report.Read.All, Report.Write.All) are listed.
             * 	5.	Click “Grant admin consent” to apply the permissions.
             *
             * 5. Ensure the Service Principal Has Access to the Power BI Workspace
             *
             * Ensure the service principal has access to the specific Power BI workspace:
             *
             * 	1.	Go to the Power BI service (app.powerbi.com).
             * 	2.	Navigate to the workspace where your reports are located.
             * 	3.	Add the service principal as an admin, member, or contributor to the workspace.
             */
            HttpResponse response = httpClient.execute(getReport);
            log.info("Response Code {}", response.getStatusLine().getStatusCode());
            String reportResponseBody = EntityUtils.toString(response.getEntity());
            JsonObject reportJson = new Gson().fromJson(reportResponseBody, JsonObject.class);
            String embedUrl = reportJson.get("embedUrl").getAsString();
            log.info("Embed URL {}", embedUrl);

            log.info("Generating Embed Token");
            // Generate Embed Token
            HttpPost postToken = new HttpPost("https://api.powerbi.com/v1.0/myorg/groups/" + GROUP_ID + "/reports/" + REPORT_ID + "/GenerateToken");
            postToken.setHeader("Authorization", "Bearer " + accessToken);
            postToken.setHeader("Content-Type", "application/json");
            postToken.setEntity(new StringEntity("{\"accessLevel\": \"view\"}"));

            HttpResponse tokenResponse = httpClient.execute(postToken);
            String tokenResponseBody = EntityUtils.toString(tokenResponse.getEntity());
            JsonObject tokenJson = new Gson().fromJson(tokenResponseBody, JsonObject.class);
            String embedToken = tokenJson.get("token").getAsString();

            log.info("Embed Token {}", embedToken);

            // Create Embed Content JSON
            JsonObject embedContent = new JsonObject();
            embedContent.addProperty("EmbedToken", embedToken);
            embedContent.addProperty("EmbedUrl", embedUrl);
            embedContent.addProperty("ReportId", REPORT_ID);

            return embedContent;
        }
    }

    private static String authenticateWithAzureAD() throws Exception {
        // RAW HTTP Request
//        try(CloseableHttpClient httpClient = HttpClients.createDefault()){
//            HttpPost post = new HttpPost(AUTHORITY_URL);
//            post.setHeader("Content-Type", "application/x-www-form-urlencoded");
//            String body = "client_id=" + CLIENT_ID
//                    + "&client_secret=" + CLIENT_SECRET
//                    + "&grant_type=client_credentials"
//                    + "&scope=" + SCOPE;
//            post.setEntity(new StringEntity(body));
//
//            HttpResponse response = httpClient.execute(post);
//            int statusCode = response.getStatusLine().getStatusCode();
//            if(statusCode < 200 || statusCode > 299){
//                throw new RuntimeException("Failed : HTTP error code : "
//                        + response.getStatusLine().getStatusCode());
//            }
//            String responseBody = EntityUtils.toString(response.getEntity());
//            log.info("Response Body : " + responseBody);
//            System.out.println(responseBody);
//
//            // Parse JSON Response
//            return new Gson().fromJson(responseBody, JsonObject.class).get("access_token").getAsString();
//        }
        ClientSecretCredential credential = new ClientSecretCredentialBuilder()
            .clientId(CLIENT_ID)
            .clientSecret(CLIENT_SECRET)
            .tenantId(TENANT_ID)
            .build();
        TokenRequestContext tokenRequestContext = new TokenRequestContext().addScopes(SCOPE);
        AccessToken accessToken = credential.getToken(tokenRequestContext).block();

        log.info("Access Token: {}", accessToken);

        if(Objects.isNull(accessToken)) {
            throw new RuntimeException("Failed to get access token");
        }
        return accessToken.getToken();
    }
}
