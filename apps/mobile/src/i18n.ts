import { I18n } from 'i18n-js';
import { I18nManager } from 'react-native';

const i18n = new I18n({
  ar: {
    appName: 'هيرد كوماند',
    tagline: 'العمل الميداني أولاً',
    signIn: 'تسجيل الدخول',
    signOut: 'تسجيل الخروج',
    language: 'English',
    home: 'هذه هي قشرة التطبيق المحمول لإصدار الأساس.',
    oidcStub: 'سيتم إكمال تدفق كيكلوك (رمز التفويض + PKCE) على الجهاز.',
  },
  en: {
    appName: 'HerdCommand',
    tagline: 'Field-first operations',
    signIn: 'Sign in',
    signOut: 'Sign out',
    language: 'العربية',
    home: 'This is the mobile application shell for the foundation release.',
    oidcStub: 'Keycloak authorization code + PKCE will complete on a device.',
  },
});

i18n.enableFallback = true;
i18n.defaultLocale = 'ar';
i18n.locale = 'ar';

export function applyLocale(locale: 'ar' | 'en') {
  i18n.locale = locale;
  const rtl = locale === 'ar';
  if (I18nManager.isRTL !== rtl) {
    I18nManager.allowRTL(rtl);
    I18nManager.forceRTL(rtl);
  }
}

export { i18n };
