import { useMemo, useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { StatusBar } from 'expo-status-bar';
import { applyLocale, i18n } from '../src/i18n';
import { palette } from '../src/theme';
import { clearTokens, persistTokens, useHerdCommandAuth } from '../src/auth';

export default function HomeScreen() {
  const [locale, setLocale] = useState<'ar' | 'en'>('ar');
  const { request, promptAsync } = useHerdCommandAuth();
  const copy = useMemo(() => {
    applyLocale(locale);
    return {
      appName: i18n.t('appName'),
      tagline: i18n.t('tagline'),
      home: i18n.t('home'),
      oidcStub: i18n.t('oidcStub'),
      signIn: i18n.t('signIn'),
      language: i18n.t('language'),
    };
  }, [locale]);

  async function onSignIn() {
    const result = await promptAsync();
    if (result.type === 'success' && result.authentication?.accessToken) {
      await persistTokens(result.authentication.accessToken);
    }
  }

  return (
    <View style={styles.screen}>
      <StatusBar style="dark" />
      <Text style={styles.title}>{copy.appName}</Text>
      <Text style={styles.muted}>{copy.tagline}</Text>
      <Text style={styles.body}>{copy.home}</Text>
      <Text style={styles.body}>{copy.oidcStub}</Text>
      <Pressable
        style={[styles.button, !request && styles.disabled]}
        disabled={!request}
        onPress={() => void onSignIn()}
      >
        <Text style={styles.buttonLabel}>{copy.signIn}</Text>
      </Pressable>
      <Pressable
        style={styles.secondary}
        onPress={() => {
          void clearTokens();
          setLocale(locale === 'ar' ? 'en' : 'ar');
        }}
      >
        <Text style={styles.secondaryLabel}>{copy.language}</Text>
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: palette.background,
    padding: 24,
    justifyContent: 'center',
    gap: 12,
  },
  title: {
    color: palette.text,
    fontSize: 32,
    lineHeight: 40,
    fontWeight: '700',
    textAlign: 'left',
  },
  muted: {
    color: palette.muted,
    fontSize: 16,
  },
  body: {
    color: palette.text,
    fontSize: 16,
    lineHeight: 24,
  },
  button: {
    minHeight: 48,
    backgroundColor: palette.brand,
    borderRadius: 12,
    alignItems: 'center',
    justifyContent: 'center',
  },
  disabled: {
    opacity: 0.55,
  },
  buttonLabel: {
    color: '#fff',
    fontWeight: '600',
    fontSize: 16,
  },
  secondary: {
    minHeight: 44,
    alignItems: 'center',
    justifyContent: 'center',
  },
  secondaryLabel: {
    color: palette.brand,
    fontWeight: '600',
  },
});
