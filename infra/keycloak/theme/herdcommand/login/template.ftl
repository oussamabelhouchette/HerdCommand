<#import "field.ftl" as field>
<#import "footer.ftl" as loginFooter>
<#macro username>
  <#assign label>
    <#if !realm.loginWithEmailAllowed>${msg("username")}<#elseif !realm.registrationEmailAsUsername>${msg("usernameOrEmail")}<#else>${msg("email")}</#if>
  </#assign>
  <@field.group name="username" label=label>
    <div class="${properties.kcInputGroup}">
      <div class="${properties.kcInputGroupItemClass} ${properties.kcFill}">
        <span class="${properties.kcInputClass} ${properties.kcFormReadOnlyClass}">
          <input id="kc-attempted-username" value="${auth.attemptedUsername}" readonly>
        </span>
      </div>
      <div class="${properties.kcInputGroupItemClass}">
        <button id="reset-login" class="${properties.kcFormPasswordVisibilityButtonClass} kc-login-tooltip" type="button"
              aria-label="${msg('restartLoginTooltip')}" onclick="location.href='${url.loginRestartFlowUrl}'">
            <i class="fa-sync-alt fas" aria-hidden="true"></i>
            <span class="kc-tooltip-text">${msg("restartLoginTooltip")}</span>
        </button>
      </div>
    </div>
  </@field.group>
</#macro>

<#macro registrationLayout bodyClass="" displayInfo=false displayMessage=true displayRequiredFields=false>
<!DOCTYPE html>
<html class="${properties.kcHtmlClass!}" lang="${lang}"<#if realm.internationalizationEnabled> dir="${(locale.rtl)?then('rtl','ltr')}"</#if>>
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>${msg("loginTitle")}</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=IBM+Plex+Sans+Arabic:wght@400;500;600;700&family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
    <#if properties.stylesCommon?has_content>
        <#list properties.stylesCommon?split(' ') as style>
            <link href="${url.resourcesCommonPath}/${style}" rel="stylesheet" />
        </#list>
    </#if>
    <#if properties.styles?has_content>
        <#list properties.styles?split(' ') as style>
            <link href="${url.resourcesPath}/${style}" rel="stylesheet" />
        </#list>
    </#if>
    <script type="importmap">
        {
            "imports": {
                "rfc4648": "${url.resourcesCommonPath}/vendor/rfc4648/rfc4648.js"
            }
        }
    </script>
    <#if properties.scripts?has_content>
        <#list properties.scripts?split(' ') as script>
            <script src="${url.resourcesPath}/${script}" type="text/javascript"></script>
        </#list>
    </#if>
    <#if scripts??>
        <#list scripts as script>
            <script src="${script}" type="text/javascript"></script>
        </#list>
    </#if>
    <script type="module" src="${url.resourcesPath}/js/passwordVisibility.js"></script>
    <script type="module">
        <#outputformat "JavaScript">
        import { startSessionPolling } from ${(url.resourcesPath + "/js/authChecker.js")?c};
        startSessionPolling(${url.ssoLoginInOtherTabsUrl?c});
        </#outputformat>
    </script>
    <#if authenticationSession??>
        <script type="module">
             <#outputformat "JavaScript">
            import { checkAuthSession } from ${(url.resourcesPath + "/js/authChecker.js")?c};
            checkAuthSession(${authenticationSession.authSessionIdHash?c});
            </#outputformat>
        </script>
    </#if>
</head>
<body id="keycloak-bg" class="hc-body ${properties.kcBodyClass!}" data-page-id="login-${pageId}">
<div class="hc-shell">
<aside class="hc-visual" aria-hidden="true">
  <img class="hc-visual-img" src="${url.resourcesPath}/img/login-visual.jpg" alt="" />
  <div class="hc-visual-scrim"></div>
  <div class="hc-visual-copy">
    <p class="hc-visual-badge">${msg("loginVisualBadge")}</p>
    <p class="hc-visual-headline">${msg("loginVisualHeadline")}</p>
    <p class="hc-visual-line">${msg("loginVisualBody")}</p>
  </div>
</aside>
<div class="hc-page">
  <header class="hc-top">
    <#if realm.internationalizationEnabled && locale.supported?size gt 1>
      <nav class="hc-lang" aria-label="${msg("languages")}">
        <svg class="hc-lang-globe" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true">
          <circle cx="12" cy="12" r="9"/>
          <path d="M3 12h18M12 3a14 14 0 0 1 0 18M12 3a14 14 0 0 0 0 18"/>
        </svg>
        <#list locale.supported?sort_by("languageTag")?reverse as l>
          <a class="hc-lang-item<#if l.languageTag == locale.currentLanguageTag> is-active</#if>" href="${l.url}">
            <#if l.languageTag == "en">EN<#elseif l.languageTag == "ar">العربية<#else>${l.label}</#if>
          </a>
        </#list>
      </nav>
    <#else>
      <span></span>
    </#if>
  </header>

  <div class="hc-form-stage">
    <div class="hc-panel">
      <div class="hc-brand">
        <img class="hc-mark" src="${url.resourcesPath}/img/logo.svg" alt="" />
        <p class="hc-wordmark">${msg("loginTitle")}</p>
      </div>

      <main class="hc-card ${properties.kcLoginMain!}">
        <div class="hc-card-head">
          <h1 class="hc-title" id="kc-page-title"><#nested "header"></h1>
          <#if pageId == "login">
            <p class="hc-subtitle">${msg("loginSubtitle")}</p>
          </#if>
        </div>

        <div class="hc-card-body">
            <#if !(auth?has_content && auth.showUsername() && !auth.showResetCredentials())>
                <#if displayRequiredFields>
                    <p class="hc-required"><span>*</span> ${msg("requiredFields")}</p>
                </#if>
            <#else>
                <#nested "show-username">
                <@username />
            </#if>

            <#if displayMessage && message?has_content && (message.type != 'warning' || !isAppInitiatedAction??)>
                <div class="hc-alert hc-alert-${message.type}" role="alert">${kcSanitize(message.summary)?no_esc}</div>
            </#if>

            <#nested "form">

            <#if auth?has_content && auth.showTryAnotherWayLink()>
              <form id="kc-select-try-another-way-form" action="${url.loginAction}" method="post" novalidate="novalidate">
                  <input type="hidden" name="tryAnotherWay" value="on"/>
                  <a id="try-another-way" href="javascript:document.forms['kc-select-try-another-way-form'].requestSubmit()">
                        ${msg("doTryAnotherWay")}
                  </a>
              </form>
            </#if>

            <#nested "socialProviders">

            <#if displayInfo>
                <div id="kc-info" class="hc-info">
                    <#nested "info">
                </div>
            </#if>
        </div>
      </main>
    </div>
  </div>

  <footer class="hc-footer">
    <@loginFooter.content/>
  </footer>
</div>
</div>
</body>
</html>
</#macro>
