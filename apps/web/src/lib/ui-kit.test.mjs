import assert from 'node:assert/strict';
import test from 'node:test';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const dir = dirname(fileURLToPath(import.meta.url));

function read(relative) {
  return readFileSync(join(dir, relative), 'utf8');
}

const kitFiles = [
  '../components/ui/Dialog.tsx',
  '../components/ui/DataTable.tsx',
  '../components/ui/PageHeader.tsx',
  '../components/ui/FilterBar.tsx',
  '../components/ui/Icons.tsx',
  '../components/ui/ActionButton.tsx',
  '../components/ui/StatGrid.tsx',
  '../components/ui/Callout.tsx',
  '../components/ui/Surface.tsx',
  '../components/ui/Note.tsx',
];

test('shared UI kit has no use client so farm RSC and admin islands can both import it', () => {
  for (const file of kitFiles) {
    assert.doesNotMatch(read(file), /'use client'/, file);
  }
  assert.match(read('../components/ui/Dialog.tsx'), /size\?: 'default' \| 'wide'/);
  assert.match(read('../components/ui/DataTable.tsx'), /tone\?: 'card' \| 'plain'/);
  assert.match(read('../components/ui/FilterBar.tsx'), /function FilterSearch/);
  assert.match(read('../components/ui/FilterBar.tsx'), /function FilterSelect/);
  assert.match(read('../components/ui/StatGrid.tsx'), /href\?: string/);
  assert.match(read('../components/ui/Callout.tsx'), /function Callout/);
  assert.match(read('../components/ui/Surface.tsx'), /function Surface/);
  assert.match(read('../components/ui/Note.tsx'), /function Note/);
});

test('farm and admin screens share header, filters, table, dialog, stats, and actions', () => {
  const screens = {
    animals: read('../components/farm/OwnerAnimals.tsx'),
    farmGroups: read('../components/farm/OwnerGroups.tsx'),
    dashboard: read('../components/farm/FarmDashboard.tsx'),
    notice: read('../components/farm/FarmNotice.tsx'),
    animalSettings: read('../components/admin/AnimalSettings.tsx'),
    breeds: read('../components/admin/BreedManagement.tsx'),
    groups: read('../components/admin/GroupManagement.tsx'),
    statuses: read('../components/admin/StatusManagement.tsx'),
    farms: read('../components/admin/FarmSettings.tsx'),
    wizard: read('../components/admin/FarmCreateWizard.tsx'),
  };

  const farmScreens = ['animals', 'farmGroups', 'dashboard', 'notice'];
  for (const name of farmScreens) {
    const source = screens[name];
    assert.match(source, /from '@\/components\/ui\/PageHeader'|from '@\/components\/ui\/FilterBar'|from '@\/components\/ui\/Dialog'|from '@\/components\/ui\/StatGrid'|from '@\/components\/ui\/DataTable'/, `${name} should use the shared kit`);
    assert.doesNotMatch(source, /className=\{styles\.modalBg\}/, `${name} should not copy a modal backdrop`);
    assert.doesNotMatch(source, /from '\.\/AdminIcons'/, `${name} should import Icons from the kit`);
  }

  for (const name of ['animalSettings', 'breeds', 'groups', 'statuses', 'farms', 'wizard']) {
    const source = screens[name];
    assert.doesNotMatch(source, /from '@\/components\/ui\/(PageHeader|FilterBar|Dialog|StatGrid|DataTable|ActionButton)'/, `${name} must not import farm RSC kit CSS into an admin island`);
    assert.doesNotMatch(source, /className=\{styles\.modalBg\}/, `${name} should not copy a modal backdrop`);
    assert.doesNotMatch(source, /from '\.\/AdminIcons'/, `${name} should import Icons from the kit`);
  }

  assert.match(screens.animals, /PageHeader/);
  assert.match(screens.animals, /FilterBar/);
  assert.match(screens.animals, /DataTable/);
  assert.match(screens.animals, /ActionButton/);
  assert.match(screens.animals, /Callout/);
  assert.match(screens.farmGroups, /PageHeader/);
  assert.match(screens.farmGroups, /FilterBar/);
  assert.match(screens.farmGroups, /DataTable/);
  assert.match(screens.farmGroups, /Callout/);
  assert.match(screens.farmGroups, /Note/);
  assert.match(screens.dashboard, /PageHeader/);
  assert.match(screens.dashboard, /StatGrid/);
  assert.match(screens.dashboard, /Surface/);
  assert.match(screens.dashboard, /listFarmAnimals/);
  assert.match(screens.dashboard, /listGroups/);
  assert.match(screens.animalSettings, /styles\.header/);
  assert.match(screens.animalSettings, /styles\.stats/);
  assert.match(screens.breeds, /styles\.toolbar/);
  assert.match(screens.breeds, /AdminDialog/);
  assert.match(screens.groups, /styles\.toolbar/);
  assert.match(screens.statuses, /styles\.toolbar/);
  assert.match(screens.farms, /styles\.header/);
  assert.match(screens.farms, /styles\.stats/);
  assert.match(screens.farms, /styles\.toolbar/);
  assert.match(screens.farms, /AdminDialog/);
  assert.match(screens.wizard, /size="wide"/);
  assert.match(screens.wizard, /FormFields/);
  assert.match(screens.wizard, /AdminDialog/);
});

test('copied modal wrappers are gone from screen CSS', () => {
  const cssFiles = [
    '../components/farm/OwnerAnimals.module.css',
    '../components/farm/OwnerGroups.module.css',
  ];
  for (const file of cssFiles) {
    const css = read(file);
    assert.doesNotMatch(css, /\.modalBg/, file);
    assert.doesNotMatch(css, /\.tableWrap/, file);
  }
  assert.match(read('../components/admin/FarmSettings.module.css'), /\.tableWrap/);
  assert.match(read('../components/admin/FarmSettings.module.css'), /grid-template-columns: repeat\(4, 1fr\)/);
  assert.match(read('../components/admin/BreedManagement.module.css'), /\.tableWrap/);
  assert.match(read('../components/admin/BreedManagement.module.css'), /grid-template-columns: repeat\(3, 1fr\)/);
});

test('kit CSS locals stay unique so webpack does not clash with admin screen CSS', () => {
  const kitCss = [
    '../components/ui/Dialog.module.css',
    '../components/ui/DataTable.module.css',
    '../components/ui/PageHeader.module.css',
    '../components/ui/FilterBar.module.css',
    '../components/ui/ActionButton.module.css',
    '../components/ui/StatGrid.module.css',
    '../components/ui/Callout.module.css',
    '../components/ui/Surface.module.css',
    '../components/ui/Note.module.css',
  ];
  const clash = /^\.(card|page|actions|note|icon|grid|panel|form|field)\b/m;
  for (const file of kitCss) {
    assert.doesNotMatch(read(file), clash, file);
  }
  assert.match(read('../components/ui/StatGrid.module.css'), /\.statGrid/);
  assert.match(read('../components/ui/DataTable.module.css'), /\.tableCard/);
  assert.match(read('../components/ui/Dialog.module.css'), /\.dialogPanel/);
  assert.match(read('../components/admin/AdminDialog.tsx'), /'use client'/);
});
