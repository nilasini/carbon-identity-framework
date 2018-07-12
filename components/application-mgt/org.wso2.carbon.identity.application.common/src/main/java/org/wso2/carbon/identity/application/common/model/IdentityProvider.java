/*
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.application.common.model;

import org.apache.axiom.om.OMElement;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.util.IdentityApplicationManagementUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class IdentityProvider implements Serializable {

    private static final long serialVersionUID = 2199048941051702943L;

    private static final Log log = LogFactory.getLog(IdentityProvider.class);
    private static final String FILE_ELEMENT_IDENTITY_PROVIDER_NAME = "IdentityProviderName";
    private static final String FILE_ELEMENT_IDENTITY_PROVIDER_DESCRIPTION = "IdentityProviderDescription";
    private static final String FILE_ELEMENT_ALIAS = "Alias";
    private static final String FILE_ELEMENT_DISPLAY_NAME = "DisplayName";
    private static final String FILE_ELEMENT_IS_PRIMARY = "IsPrimary";
    private static final String FILE_ELEMENT_IS_ENABLED = "IsEnabled";
    private static final String FILE_ELEMENT_IS_FEDERATION_HUB = "IsFederationHub";
    private static final String FILE_ELEMENT_HOME_REALM_ID = "HomeRealmId";
    private static final String FILE_ELEMENT_PROVISIONING_ROLE = "ProvisioningRole";
    private static final String FILE_ELEMENT_FEDERATED_AUTHENTICATOR_CONFIGS = "FederatedAuthenticatorConfigs";
    private static final String FILE_ELEMENT_DEFAULT_AUTHENTICATOR_CONFIG = "DefaultAuthenticatorConfig";
    private static final String FILE_ELEMENT_PROVISIONING_CONNECTOR_CONFIGS = "ProvisioningConnectorConfigs";
    private static final String FILE_ELEMENT_DEFAULT_PROVISIONING_CONNECTOR_CONFIG =
            "DefaultProvisioningConnectorConfig";
    private static final String FILE_ELEMENT_CLAIM_CONFIG = "ClaimConfig";
    private static final String FILE_ELEMENT_CERTIFICATE = "Certificate";
    private static final String FILE_ELEMENT_PERMISSION_AND_ROLE_CONFIG = "PermissionAndRoleConfig";
    private static final String FILE_ELEMENT_JUST_IN_TIME_PROVISIONING_CONFIG = "JustInTimeProvisioningConfig";
    private final String THUMB_PRINT = "thumbPrint";
    private final String CERT_VALUE = "certValue";
    private final String JSON_ARRAY_IDENTIFIER = "[";
    private final String EMPTY_JSON_ARRAY = "[]";

    private String id;
    private String identityProviderName;
    private String identityProviderDescription;
    private String alias;
    private boolean primary;
    private boolean federationHub;
    private String homeRealmId;
    private String provisioningRole;
    private String displayName;
    private boolean enable;
    private FederatedAuthenticatorConfig[] federatedAuthenticatorConfigs = new FederatedAuthenticatorConfig[0];
    private FederatedAuthenticatorConfig defaultAuthenticatorConfig;
    private ProvisioningConnectorConfig[] provisioningConnectorConfigs = new ProvisioningConnectorConfig[0];
    private ProvisioningConnectorConfig defaultProvisioningConnectorConfig;
    private ClaimConfig claimConfig;
    private String certificate;
    private PermissionsAndRoleConfig permissionAndRoleConfig;
    private JustInTimeProvisioningConfig justInTimeProvisioningConfig;
    private IdentityProviderProperty []idpProperties = new IdentityProviderProperty[0];
    private CertificateInfo[] certificateInfoArray = new CertificateInfo[0];

    public static IdentityProvider build(OMElement identityProviderOM) {
        IdentityProvider identityProvider = new IdentityProvider();

        Iterator<?> iter = identityProviderOM.getChildElements();
        String defaultAuthenticatorConfigName = null;
        String defaultProvisioningConfigName = null;
        
        while (iter.hasNext()) {
            OMElement element = (OMElement) (iter.next());
            String elementName = element.getLocalName();

            if (elementName.equals(FILE_ELEMENT_IDENTITY_PROVIDER_NAME)) {
                if (element.getText() != null) {
                    identityProvider.setIdentityProviderName(element.getText());
                } else {
                    log.error("Identity provider not loaded from the file system. Identity provider name must be " +
                            "not null.");
                    return null;
                }
            } else if (FILE_ELEMENT_IDENTITY_PROVIDER_DESCRIPTION.equals(elementName)) {
                identityProvider.setIdentityProviderDescription(element.getText());
            } else if (FILE_ELEMENT_ALIAS.equals(elementName)) {
                identityProvider.setAlias(element.getText());
            } else if (FILE_ELEMENT_DISPLAY_NAME.equals(elementName)) {
                identityProvider.setDisplayName(element.getText());
            } else if (FILE_ELEMENT_IS_PRIMARY.equals(elementName)) {
                if (element.getText() != null && element.getText().trim().length() > 0) {
                    identityProvider.setPrimary(Boolean.parseBoolean(element.getText()));
                }
            } else if (FILE_ELEMENT_IS_ENABLED.equals(elementName)) {
                if (element.getText() != null && element.getText().trim().length() > 0) {
                    identityProvider.setEnable((Boolean.parseBoolean(element.getText())));
                }
            } else if (FILE_ELEMENT_IS_FEDERATION_HUB.equals(elementName)) {
                if (element.getText() != null && element.getText().trim().length() > 0) {
                    identityProvider.setFederationHub(Boolean.parseBoolean(element.getText()));
                }
            } else if (FILE_ELEMENT_HOME_REALM_ID.equals(elementName)) {
                identityProvider.setHomeRealmId(element.getText());
            } else if (FILE_ELEMENT_PROVISIONING_ROLE.equals(elementName)) {
                identityProvider.setProvisioningRole(element.getText());
            } else if (FILE_ELEMENT_FEDERATED_AUTHENTICATOR_CONFIGS.equals(elementName)) {

                Iterator<?> federatedAuthenticatorConfigsIter = element.getChildElements();

                if (federatedAuthenticatorConfigsIter == null) {
                    continue;
                }

                List<FederatedAuthenticatorConfig> federatedAuthenticatorConfigsArrList;
                federatedAuthenticatorConfigsArrList = new ArrayList<FederatedAuthenticatorConfig>();

                while (federatedAuthenticatorConfigsIter.hasNext()) {
                    OMElement federatedAuthenticatorConfigsElement = (OMElement) (federatedAuthenticatorConfigsIter
                            .next());
                    FederatedAuthenticatorConfig fedAuthConfig;
                    fedAuthConfig = FederatedAuthenticatorConfig
                            .build(federatedAuthenticatorConfigsElement);
                    if (fedAuthConfig != null) {
                        federatedAuthenticatorConfigsArrList.add(fedAuthConfig);
                    }
                }

                if (federatedAuthenticatorConfigsArrList.size() > 0) {
                    FederatedAuthenticatorConfig[] federatedAuthenticatorConfigsArr;
                    federatedAuthenticatorConfigsArr = federatedAuthenticatorConfigsArrList
                            .toArray(new FederatedAuthenticatorConfig[0]);
                    identityProvider
                            .setFederatedAuthenticatorConfigs(federatedAuthenticatorConfigsArr);
                }
            } else if (FILE_ELEMENT_DEFAULT_AUTHENTICATOR_CONFIG.equals(elementName)) {
                defaultAuthenticatorConfigName = element.getText();
            } else if (FILE_ELEMENT_PROVISIONING_CONNECTOR_CONFIGS.equals(elementName)) {

                Iterator<?> provisioningConnectorConfigsIter = element.getChildElements();

                if (provisioningConnectorConfigsIter == null) {
                    continue;
                }

                List<ProvisioningConnectorConfig> provisioningConnectorConfigsArrList;
                provisioningConnectorConfigsArrList = new ArrayList<ProvisioningConnectorConfig>();

                while (provisioningConnectorConfigsIter.hasNext()) {
                    OMElement provisioningConnectorConfigsElement = (OMElement) (provisioningConnectorConfigsIter
                            .next());
                    ProvisioningConnectorConfig proConConfig = null;
                    try {
                        proConConfig = ProvisioningConnectorConfig
                                .build(provisioningConnectorConfigsElement);
                    } catch (IdentityApplicationManagementException e) {
                        log.error("Error while building provisioningConnectorConfig for IDP " + identityProvider
                                .getIdentityProviderName() + ". Cause : " + e.getMessage() + ". Building rest of the " +
                                "IDP configs");
                    }
                    if (proConConfig != null) {
                        provisioningConnectorConfigsArrList.add(proConConfig);
                    }
                }

                if (CollectionUtils.isNotEmpty(provisioningConnectorConfigsArrList)) {
                    ProvisioningConnectorConfig[] provisioningConnectorConfigsArr;
                    provisioningConnectorConfigsArr = provisioningConnectorConfigsArrList
                            .toArray(new ProvisioningConnectorConfig[0]);
                    identityProvider
                            .setProvisioningConnectorConfigs(provisioningConnectorConfigsArr);
                }
            } else if (FILE_ELEMENT_DEFAULT_PROVISIONING_CONNECTOR_CONFIG.equals(elementName)) {
                defaultProvisioningConfigName = element.getText();
            } else if (FILE_ELEMENT_CLAIM_CONFIG.equals(elementName)) {
                identityProvider.setClaimConfig(ClaimConfig.build(element));
            } else if (FILE_ELEMENT_CERTIFICATE.equals(elementName)) {
                identityProvider.setCertificate(element.getText());
            } else if (FILE_ELEMENT_PERMISSION_AND_ROLE_CONFIG.equals(elementName)) {
                identityProvider
                        .setPermissionAndRoleConfig(PermissionsAndRoleConfig.build(element));
            } else if (FILE_ELEMENT_JUST_IN_TIME_PROVISIONING_CONFIG.equals(elementName)) {
                identityProvider.setJustInTimeProvisioningConfig(JustInTimeProvisioningConfig
                        .build(element));
            }

        }
        FederatedAuthenticatorConfig[] federatedAuthenticatorConfigs = identityProvider
                .getFederatedAuthenticatorConfigs();
        boolean foundDefaultAuthenticator = false;
        for (int i = 0; i < federatedAuthenticatorConfigs.length; i++) {
            if (StringUtils.equals(defaultAuthenticatorConfigName, federatedAuthenticatorConfigs[i].getName())) {
                identityProvider.setDefaultAuthenticatorConfig(federatedAuthenticatorConfigs[i]);
                foundDefaultAuthenticator = true;
                break;
            }
        }
        if ((!foundDefaultAuthenticator && federatedAuthenticatorConfigs.length > 0) || (federatedAuthenticatorConfigs
                .length == 0 && StringUtils.isNotBlank(defaultAuthenticatorConfigName))) {
            log.warn("No matching federated authentication config found with default authentication config name :  "
                    + defaultAuthenticatorConfigName + " in identity provider : " + identityProvider.displayName + ".");
            return null;
        }

        ProvisioningConnectorConfig[] provisioningConnectorConfigs = identityProvider
                .getProvisioningConnectorConfigs();
        boolean foundDefaultProvisioningConfig = false;
        for (int i = 0; i < provisioningConnectorConfigs.length; i++) {
            if (StringUtils.equals(defaultProvisioningConfigName, provisioningConnectorConfigs[i].getName())) {
                identityProvider.setDefaultProvisioningConnectorConfig(provisioningConnectorConfigs[i]);
                foundDefaultProvisioningConfig = true;
                break;
            }
        }
        if ((!foundDefaultProvisioningConfig && provisioningConnectorConfigs.length > 0) ||
                (provisioningConnectorConfigs.length == 0 && StringUtils.isNotBlank(defaultProvisioningConfigName))) {
            log.warn("No matching provisioning config found with default provisioning config name :  "
                    + defaultProvisioningConfigName + " in identity provider : " + identityProvider .displayName +
                    ".");
            identityProvider = null;
        }

        return identityProvider;
    }

    /**
     * @return
     */
    public FederatedAuthenticatorConfig[] getFederatedAuthenticatorConfigs() {
        return federatedAuthenticatorConfigs;
    }

    /**
     * @param federatedAuthenticatorConfigs
     */
    public void setFederatedAuthenticatorConfigs(
            FederatedAuthenticatorConfig[] federatedAuthenticatorConfigs) {

        if (federatedAuthenticatorConfigs == null) {
            return;
        }
        Set<FederatedAuthenticatorConfig> propertySet =
                new HashSet<FederatedAuthenticatorConfig>(Arrays.asList(federatedAuthenticatorConfigs));
        this.federatedAuthenticatorConfigs = propertySet.toArray(new FederatedAuthenticatorConfig[propertySet.size()]);
    }

    /**
     * @return
     */
    public FederatedAuthenticatorConfig getDefaultAuthenticatorConfig() {
        return defaultAuthenticatorConfig;
    }

    /**
     * @param defaultAuthenticatorConfig
     */
    public void setDefaultAuthenticatorConfig(
            FederatedAuthenticatorConfig defaultAuthenticatorConfig) {
        this.defaultAuthenticatorConfig = defaultAuthenticatorConfig;
    }

    /**
     * @return
     */
    public String getIdentityProviderName() {
        return identityProviderName;
    }

    /**
     * @param identityProviderName
     */
    public void setIdentityProviderName(String identityProviderName) {
        this.identityProviderName = identityProviderName;
    }

    /**
     * @return
     */
    public String getIdentityProviderDescription() {
        return identityProviderDescription;
    }

    /**
     * @param identityProviderDescription
     */
    public void setIdentityProviderDescription(String identityProviderDescription) {
        this.identityProviderDescription = identityProviderDescription;
    }

    /**
     * @return
     */
    public ProvisioningConnectorConfig getDefaultProvisioningConnectorConfig() {
        return defaultProvisioningConnectorConfig;
    }

    /**
     * @param defaultProvisioningConnectorConfig
     */
    public void setDefaultProvisioningConnectorConfig(
            ProvisioningConnectorConfig defaultProvisioningConnectorConfig) {
        this.defaultProvisioningConnectorConfig = defaultProvisioningConnectorConfig;
    }

    /**
     * @return
     */
    public ProvisioningConnectorConfig[] getProvisioningConnectorConfigs() {
        return provisioningConnectorConfigs;
    }

    /**
     * @param provisioningConnectorConfigs
     */
    public void setProvisioningConnectorConfigs(
            ProvisioningConnectorConfig[] provisioningConnectorConfigs) {
        if (provisioningConnectorConfigs == null) {
            return;
        }
        Set<ProvisioningConnectorConfig> propertySet =
                new HashSet<ProvisioningConnectorConfig>(Arrays.asList(provisioningConnectorConfigs));
        this.provisioningConnectorConfigs = propertySet.toArray(new ProvisioningConnectorConfig[propertySet.size()]);
    }

    /**
     * @return
     */
    public boolean isPrimary() {
        return primary;
    }

    /**
     * @param primary
     */
    public void setPrimary(boolean primary) {
        this.primary = primary;
    }

    /**
     * @return
     */
    public String getAlias() {
        return alias;
    }

    /**
     * @param alias
     */
    public void setAlias(String alias) {
        this.alias = alias;
    }

    /**
     * Get certificate
     * @return if certificate is in JSON array then return only first element.
     */
    public String getCertificate() {

        // Check whether certificate is in json format.
        if (StringUtils.isNotBlank(certificate) && certificate.startsWith(JSON_ARRAY_IDENTIFIER)) {
            if (!certificate.equals(EMPTY_JSON_ARRAY)) {
                certificate = ((JSONObject) (new JSONArray(certificate).get(0))).getString(CERT_VALUE);
            } else {
                // If certificate is an empty json array, then return empty value.
                certificate = "";
            }
        }
        return certificate;
    }

    /**
     * @param certificate
     */
    public void setCertificate(String certificate) {

        setCertificateInfoArray(certificate);
        this.certificate = certificate;
    }

    /**
     * Get certificate info array
     * @return CertificateInfo array
     */
    public CertificateInfo[] getCertificateInfoArray() {

        return certificateInfoArray;
    }

    /**
     * Set certificateInfoArray
     *
     * @param certificateValue array which contains certificate info. Here Certificate info contains thumbPrint
     *                             and certificate value as attributes.
     */
    private void setCertificateInfoArray(String certificateValue) {

        try {
            if (StringUtils.isNotBlank(certificateValue) && !certificateValue.equals(EMPTY_JSON_ARRAY)) {
                certificateValue = certificateValue.trim();
                try {
                    this.certificateInfoArray = handleJsonFormatCertificate(certificateValue);
                } catch (JSONException e) {
                    // Handle plain text certificate for file based configuration.
                    if (certificateValue.startsWith(IdentityUtil.PEM_BEGIN_CERTFICATE)) {
                        this.certificateInfoArray = handlePlainTextCertificate(certificateValue);
                    } else {
                        // Handle encoded certificate values. While uploading through UI.
                        this.certificateInfoArray = handleEncodedCertificate(certificateValue);
                    }
                }
            }
        } catch (NoSuchAlgorithmException e) {
            log.error("Error while generating thumbPrint. Unsupported hash algorithm. ", e);
        }
    }

    /**
     * handle the certificates which is in json format.
     * @param certificateValue
     * @return array of certificate value and thumbPrint of each certificates.
     * @throws NoSuchAlgorithmException
     */
    private CertificateInfo[] handleJsonFormatCertificate(String certificateValue) throws NoSuchAlgorithmException {

        JSONArray jsonCertificateInfoArray = new JSONArray(certificateValue);
        int lengthOfJsonArray = jsonCertificateInfoArray.length();
        if (lengthOfJsonArray > 1 && log.isDebugEnabled()) {
            log.debug(lengthOfJsonArray + " certificates have been found");
        }
        List<CertificateInfo> certificateInfos = new ArrayList<>();
        for (int i = 0; i < lengthOfJsonArray; i++) {
            JSONObject jsonCertificateInfoObject = (JSONObject) jsonCertificateInfoArray.get(i);
            String thumbPrint = jsonCertificateInfoObject.getString(THUMB_PRINT);

            CertificateInfo certificateInfo = new CertificateInfo();
            certificateInfo.setThumbPrint(thumbPrint);
            if (log.isDebugEnabled()) {
                log.debug("Handling json format certificate. ThumbPrint of the certificate is: " + thumbPrint);
            }
            certificateInfo.setCertValue(jsonCertificateInfoObject.getString(CERT_VALUE));
            certificateInfos.add(certificateInfo);
        }
        return certificateInfos.toArray(new CertificateInfo[lengthOfJsonArray]);
    }

    /**
     * handle the certificate which is in encoded format.
     *
     * @param certificateValue
     * @return array of certificate value and thumbPrint of each certificates.
     * @throws NoSuchAlgorithmException
     */
    private CertificateInfo[] handleEncodedCertificate(String certificateValue) throws NoSuchAlgorithmException {

        if (log.isDebugEnabled()) {
            log.debug("Handling encoded certificates: " + certificateValue);
        }
        String decodedCertificate;
        try {
            decodedCertificate = new String(Base64.getDecoder().decode(certificateValue));
        } catch (IllegalArgumentException ex) {
            // TODO Need to handle the exception handling in proper way.
            return generateOneTimeEncodedCertificateInfo(certificateValue);
        }
        // Handle certificates which are one time encoded but doesn't have BEGIN and END statement
        if (StringUtils.isNotBlank(decodedCertificate) && !decodedCertificate.startsWith(IdentityUtil.PEM_BEGIN_CERTFICATE)) {
            return generateOneTimeEncodedCertificateInfo(certificateValue);
        } else {
            return createEncodedCertificateInfo(decodedCertificate);
        }
    }

    private CertificateInfo[] generateOneTimeEncodedCertificateInfo(String certificateValue) throws NoSuchAlgorithmException {

        String encodedCertVal = Base64.getEncoder().encodeToString(certificateValue.getBytes());
        String thumbPrint = IdentityApplicationManagementUtil.generateThumbPrint(encodedCertVal);
        List<CertificateInfo> certificateInfoList = new ArrayList<>();
        CertificateInfo certificateInfo = new CertificateInfo();
        certificateInfo.setThumbPrint(thumbPrint);
        certificateInfo.setCertValue(certificateValue);
        certificateInfoList.add(certificateInfo);
        return certificateInfoList.toArray(new CertificateInfo[1]);
    }

    /**
     * handle the certificate which is in plain text format.
     *
     * @param certificateValue
     * @return array of certificate value and thumbPrint of each certificates.
     * @throws NoSuchAlgorithmException
     */
    private CertificateInfo[] handlePlainTextCertificate(String certificateValue) throws NoSuchAlgorithmException {

        if (log.isDebugEnabled()) {
            log.debug("Handling plain text certificate: " + certificateValue);
        }
        return createDecodedCertificateInfo(certificateValue);
    }

    /**
     * Create certificate info for encoded certificates.
     * @param decodedCertificate
     * @return
     * @throws NoSuchAlgorithmException
     */
    private CertificateInfo[] createEncodedCertificateInfo(String decodedCertificate) throws
            NoSuchAlgorithmException {

        int numberOfCertificates = StringUtils.countMatches(decodedCertificate, IdentityUtil.PEM_BEGIN_CERTFICATE);
        if (numberOfCertificates == 0) {
            log.error("Uploaded certificate doesn't have " + IdentityUtil.PEM_BEGIN_CERTFICATE + " and " +
                    IdentityUtil.PEM_END_CERTIFICATE);
        } else {
            if (log.isDebugEnabled()) {
                log.debug(numberOfCertificates + " certificates have been found. ");
            }
        }
        List<CertificateInfo> certificateInfoArrayList = new ArrayList<>();
        for (int ordinal = 1; ordinal <= numberOfCertificates; ordinal++) {
            String certificateVal;
            certificateVal = Base64.getEncoder().encodeToString(IdentityApplicationManagementUtil.extractCertificate
                    (decodedCertificate, ordinal).getBytes(StandardCharsets.UTF_8));
            CertificateInfo certificateInfo = new CertificateInfo();
            String thumbPrint = IdentityApplicationManagementUtil.generateThumbPrint(certificateVal);
            if (log.isDebugEnabled()) {
                log.debug("ThumbPrint of the certificate is: " + thumbPrint);
            }
            certificateInfo.setThumbPrint(thumbPrint);
            certificateInfo.setCertValue(certificateVal);
            certificateInfoArrayList.add(certificateInfo);
        }
        return certificateInfoArrayList.toArray(new CertificateInfo[numberOfCertificates]);
    }

    /**
     * Create certificate info for decoded certificates.
     * @param decodedCertificate
     * @return
     * @throws NoSuchAlgorithmException
     */
    private CertificateInfo[] createDecodedCertificateInfo(String decodedCertificate) throws
            NoSuchAlgorithmException {

        int numberOfCertificates = StringUtils.countMatches(decodedCertificate, IdentityUtil.PEM_BEGIN_CERTFICATE);
        if (numberOfCertificates == 0) {
            log.error("Uploaded certificate doesn't have " + IdentityUtil.PEM_BEGIN_CERTFICATE + " and " +
                    IdentityUtil.PEM_END_CERTIFICATE);
        } else {
            if (log.isDebugEnabled()) {
                log.debug(numberOfCertificates + " certificates have been found. ");
            }
        }
        List<CertificateInfo> certificateInfoArrayList = new ArrayList<>();
        for (int ordinal = 1; ordinal <= numberOfCertificates; ordinal++) {
            String certificateVal;
            certificateVal = IdentityApplicationManagementUtil.extractCertificate(decodedCertificate, ordinal);
            CertificateInfo certificateInfo = new CertificateInfo();
            String thumbPrint = IdentityApplicationManagementUtil.generateThumbPrint(certificateVal);
            if (log.isDebugEnabled()) {
                log.debug("ThumbPrint of the certificate is: " + thumbPrint);
            }
            certificateInfo.setThumbPrint(thumbPrint);
            certificateInfo.setCertValue(certificateVal);
            certificateInfoArrayList.add(certificateInfo);
        }
        return certificateInfoArrayList.toArray(new CertificateInfo[numberOfCertificates]);
    }
    /**
     * @return
     */
    public ClaimConfig getClaimConfig() {
        return claimConfig;
    }

    /**
     * @param claimConfig
     */
    public void setClaimConfig(ClaimConfig claimConfig) {
        this.claimConfig = claimConfig;
    }

    /**
     * @return
     */
    public PermissionsAndRoleConfig getPermissionAndRoleConfig() {
        return permissionAndRoleConfig;
    }

    /**
     * @param permissionAndRoleConfig
     */
    public void setPermissionAndRoleConfig(PermissionsAndRoleConfig permissionAndRoleConfig) {
        this.permissionAndRoleConfig = permissionAndRoleConfig;
    }

    /**
     * @return
     */
    public String getHomeRealmId() {
        return homeRealmId;
    }

    /**
     * @param homeRealmId
     */
    public void setHomeRealmId(String homeRealmId) {
        this.homeRealmId = homeRealmId;
    }

    /**
     * @return
     */
    public JustInTimeProvisioningConfig getJustInTimeProvisioningConfig() {
        return justInTimeProvisioningConfig;
    }

    /**
     * @param justInTimeProvisioningConfig
     */
    public void setJustInTimeProvisioningConfig(
            JustInTimeProvisioningConfig justInTimeProvisioningConfig) {
        this.justInTimeProvisioningConfig = justInTimeProvisioningConfig;
    }

    /**
     * This represents a federation hub identity provider.
     *
     * @return
     */
    public boolean isFederationHub() {
        return federationHub;
    }

    /**
     * @param federationHub
     */
    public void setFederationHub(boolean federationHub) {
        this.federationHub = federationHub;
    }

    /**
     * This represents a provisioning role of identity provider.
     *
     * @return
     */
    public String getProvisioningRole() {
        return provisioningRole;
    }

    /**
     * @param provisioningRole
     */
    public void setProvisioningRole(String provisioningRole) {
        this.provisioningRole = provisioningRole;
    }

    /**
     * This represents whether the idp enable.
     *
     * @return
     */
    public boolean isEnable() {
        return enable;
    }

    /**
     * @param enable
     */
    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    /**
     * This represents a display name of identity provider.
     *
     * @return
     */
    public String getDisplayName() {
        return displayName;
    }

    /*
     * <IdentityProvider> <IdentityProviderName></IdentityProviderName>
     * <IdentityProviderDescription></IdentityProviderDescription> <Alias></Alias>
     * <IsPrimary></IsPrimary> <IsFederationHub></IsFederationHub><HomeRealmId></HomeRealmId>
     * <ProvisioningRole></ProvisioningRole>
     * <FederatedAuthenticatorConfigs></FederatedAuthenticatorConfigs>
     * <DefaultAuthenticatorConfig></DefaultAuthenticatorConfig>
     * <ProvisioningConnectorConfigs></ProvisioningConnectorConfigs>
     * <DefaultProvisioningConnectorConfig></DefaultProvisioningConnectorConfig>
     * <ClaimConfig></ClaimConfig> <Certificate></Certificate>
     * <PermissionAndRoleConfig></PermissionAndRoleConfig>
     * <JustInTimeProvisioningConfig></JustInTimeProvisioningConfig> </IdentityProvider>
     */

    /**
     * @param displayName
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IdentityProvider)) return false;

        IdentityProvider that = (IdentityProvider) o;

        if (identityProviderName != null ? !identityProviderName.equals(that.identityProviderName) :
                that.identityProviderName != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return identityProviderName != null ? identityProviderName.hashCode() : 0;
    }

    /**
     * Get IDP properties
     * @return
     */
    public IdentityProviderProperty[] getIdpProperties() {
        return idpProperties;
    }

    /**
     * Set IDP Properties
     * @param idpProperties
     */
    public void setIdpProperties(IdentityProviderProperty []idpProperties) {
        this.idpProperties = idpProperties;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return this.id;
    }
}
