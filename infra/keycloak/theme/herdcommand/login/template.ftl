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
<#assign htmlDir = "ltr">
<#if (locale?? && locale.rtl) || (lang!'ar') == 'ar'>
  <#assign htmlDir = "rtl">
</#if>
<html class="${properties.kcHtmlClass!}" lang="${lang}" dir="${htmlDir}">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>${msg("loginTitle")}</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Rubik:wght@400;500;600;700&display=swap" rel="stylesheet">
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
<body id="keycloak-bg" class="${properties.kcBodyClass!}" data-page-id="login-${pageId}">
<main class="login-page">
  <section class="hero" aria-hidden="true">
    <img class="hero-bg" src="${url.resourcesPath}/img/login-visual.png" alt="" />
    <div class="hero-overlay"></div>
    <div class="hero-copy">
      <div class="badge">
        <span>${msg("loginVisualBadge")}</span>
        <img src="${url.resourcesPath}/img/badge-icon.svg" alt="" />
      </div>
      <p class="hero-title">${msg("loginVisualHeadline")?no_esc}</p>
      <p class="hero-description">${msg("loginVisualBody")}</p>
    </div>
  </section>

  <section class="auth-side">
    <div class="top-row">
      <#assign enUrl = "">
      <#assign arUrl = "">
      <#assign currentLang = lang>
      <#if realm.internationalizationEnabled && locale?? && locale.supported?has_content>
        <#assign currentLang = locale.currentLanguageTag>
        <#list locale.supported as l>
          <#if l.languageTag == "en"><#assign enUrl = l.url></#if>
          <#if l.languageTag == "ar"><#assign arUrl = l.url></#if>
        </#list>
      </#if>
      <div class="language-pill<#if currentLang == "en"> is-en</#if>" aria-label="${msg("languages")}">
        <#if enUrl?has_content>
          <a class="en" href="${enUrl}">EN</a>
        <#else>
          <span class="en">EN</span>
        </#if>
        <span class="divider"><img src="${url.resourcesPath}/img/lang-divider.svg" alt="" /></span>
        <#if arUrl?has_content>
          <a class="ar" href="${arUrl}">العربية</a>
        <#else>
          <span class="ar">العربية</span>
        </#if>
        <img class="globe" src="${url.resourcesPath}/img/globe.svg" alt="" />
      </div>
      <div class="top-spacer"></div>
    </div>

    <#nested "form">

    <#if displayInfo>
      <#nested "info">
    </#if>

    <p class="copyright">${msg("loginFooterCopyright")}</p>
  </section>
</main>
<script>
  document.querySelectorAll("[data-hc-password-toggle]").forEach(function (toggle) {
    toggle.addEventListener("click", function () {
      var input = document.getElementById(toggle.getAttribute("data-hc-password-toggle"));
      if (!input) return;
      input.type = input.type === "password" ? "text" : "password";
    });
  });
</script>
</body>
</html>
</#macro>
