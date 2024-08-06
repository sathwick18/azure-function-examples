package org.azure.functions;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
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
    private static final String REPORT_ID = "b7812ecc-51bb-41ae-8548-ce55455a506a";

    public static void main(String[] args) {
        try {
            log.info("Authenticating with Azure Active Directory");
            String accessToken = authenticateWithAzureAD();

            log.info("Authentication successful i.e., access token is {}", accessToken);
        }
        catch (Exception e){
            e.printStackTrace();
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
