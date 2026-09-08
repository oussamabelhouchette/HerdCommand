import { cache } from 'react';
import { requireFarmOwner, type AdminGate } from './require-admin';
import { findOwnerFarm, loadOwnerFarms, type OwnerFarm } from './owner-farms';

export type FarmPortal = {
  gate: AdminGate;
  farms: OwnerFarm[];
  error?: unknown;
};

export type CurrentFarm =
  | { ok: true; farm: OwnerFarm; farms: OwnerFarm[]; token: string }
  | { ok: false; code: 'forbidden' | 'load-error' | 'not-found'; error?: unknown };

export const loadFarmPortal = cache(async (locale: string): Promise<FarmPortal> => {
  const gate = await requireFarmOwner(locale);
  if (!gate.allowed || !gate.session.accessToken) {
    return { gate, farms: [] };
  }
  const loaded = await loadOwnerFarms(gate.session.accessToken, locale);
  return { gate, farms: loaded.farms, error: loaded.error };
});

export const requireCurrentFarm = cache(async (locale: string, farmId: string): Promise<CurrentFarm> => {
  const portal = await loadFarmPortal(locale);
  if (!portal.gate.allowed || !portal.gate.session.accessToken) {
    return { ok: false, code: 'forbidden' };
  }
  if (portal.error) {
    return { ok: false, code: 'load-error', error: portal.error };
  }
  const farm = findOwnerFarm(portal.farms, farmId);
  if (!farm) {
    return { ok: false, code: 'not-found' };
  }
  return { ok: true, farm, farms: portal.farms, token: portal.gate.session.accessToken };
});
