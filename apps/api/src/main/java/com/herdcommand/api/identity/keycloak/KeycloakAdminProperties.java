package com.herdcommand.api.identity.keycloak;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "herdcommand.keycloak.admin")
public class KeycloakAdminProperties {

    private String baseUrl = "http://localhost:9090";
    private String realm = "herdcommand";
    private String clientId = "herdcommand-admin";
    private String clientSecret = "";
    private String loginRedirectUri = "http://localhost:3000/login";
    private String publicClientId = "herdcommand";
    private int actionLifespanSeconds = 43200;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getLoginRedirectUri() {
        return loginRedirectUri;
    }

    public void setLoginRedirectUri(String loginRedirectUri) {
        this.loginRedirectUri = loginRedirectUri;
    }

    public String getPublicClientId() {
        return publicClientId;
    }

    public void setPublicClientId(String publicClientId) {
        this.publicClientId = publicClientId;
    }

    public int getActionLifespanSeconds() {
        return actionLifespanSeconds;
    }

    public void setActionLifespanSeconds(int actionLifespanSeconds) {
        this.actionLifespanSeconds = actionLifespanSeconds;
    }

    public boolean hasSecret() {
        return clientSecret != null && !clientSecret.isBlank();
    }

    @Override
    public String toString() {
        return "KeycloakAdminProperties{baseUrl='%s', realm='%s', clientId='%s', secretConfigured=%s}"
                .formatted(baseUrl, realm, clientId, hasSecret());
    }
}
