<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('username','password') displayInfo=false; section>
    <#if section = "header">
        ${msg("loginAccountTitle")}
    <#elseif section = "form">
        <#if !realm.loginWithEmailAllowed>
            <#assign identityLabel = msg("username")>
        <#elseif !realm.registrationEmailAsUsername>
            <#assign identityLabel = msg("usernameOrEmail")>
        <#else>
            <#assign identityLabel = msg("email")>
        </#if>
        <form id="kc-form-login" class="login-card" onsubmit="login.disabled = true; return true;" action="${url.loginAction}" method="post" novalidate="novalidate">
            <div class="card-heading">
                <div class="brand-mark">
                    <img src="${url.resourcesPath}/img/brand-mark.svg" alt="" />
                </div>
                <h1 class="card-title" id="kc-page-title">${msg("loginAccountTitle")}</h1>
                <p class="card-subtitle">${msg("loginSubtitle")}</p>
            </div>

            <#if message?has_content && (message.type != 'warning' || !isAppInitiatedAction??)>
                <div class="hc-alert hc-alert-${message.type}" role="alert">${kcSanitize(message.summary)?no_esc}</div>
            </#if>

            <div class="fields">
                <#if !usernameHidden??>
                <label class="field-group">
                    <span class="field-label">${identityLabel}</span>
                    <span class="input-shell">
                        <input id="username" name="username" type="text" inputmode="email" autocomplete="username" placeholder="example@farm.com" value="${(login.username!'')}" aria-label="${identityLabel}" autofocus />
                        <img class="input-icon-right" src="${url.resourcesPath}/img/icon-mail.svg" alt="" />
                    </span>
                </label>
                </#if>

                <div class="field-group">
                    <div class="password-label-row">
                        <#if realm.resetPasswordAllowed>
                            <a class="forgot" href="${url.loginResetCredentialsUrl}">${msg("doForgotPassword")}</a>
                        <#else>
                            <span class="forgot">${msg("doForgotPassword")}</span>
                        </#if>
                        <span class="label">${msg("password")}</span>
                    </div>
                    <label class="input-shell">
                        <img id="password-show-password" class="input-icon-left" src="${url.resourcesPath}/img/icon-eye.svg" alt="" role="button" tabindex="0" data-hc-password-toggle="password" aria-label="${msg('showPassword')}" />
                        <input id="password" name="password" type="password" autocomplete="current-password" aria-label="${msg("password")}" <#if usernameHidden??>autofocus</#if> />
                        <img class="input-icon-right" src="${url.resourcesPath}/img/icon-lock.svg" alt="" />
                    </label>
                </div>
            </div>

            <input type="hidden" id="id-hidden-input" name="credentialId" <#if auth.selectedCredential?has_content>value="${auth.selectedCredential}"</#if>/>

            <div class="actions">
                <button class="submit-btn" name="login" id="kc-login" type="submit">${msg("doLogIn")}</button>
                <div class="signup-line">
                    <span class="question">${msg("noAccount")}</span>
                    <#if realm.registrationAllowed>
                        <a class="link" href="${url.registrationUrl}">${msg("doRegister")}</a>
                    <#else>
                        <span class="link">${msg("doRegister")}</span>
                    </#if>
                </div>
            </div>
        </form>
    </#if>
</@layout.registrationLayout>
