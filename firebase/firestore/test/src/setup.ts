import { initializeTestEnvironment, type RulesTestEnvironment } from '@firebase/rules-unit-testing';
import { readFile } from 'fs/promises';
import { existsSync } from 'fs';
import path from 'path';

// Derive emulator host/port from env if provided; fallback to defaults.
const DEFAULT_HOST = '127.0.0.1';
const DEFAULT_PORT = 8080;

function parseEmulatorHost(envVal?: string): { host: string; port: number } {
  if (!envVal) return { host: DEFAULT_HOST, port: DEFAULT_PORT };
  const [host, portStr] = envVal.split(':');
  const port = Number(portStr);
  if (!host || Number.isNaN(port)) return { host: DEFAULT_HOST, port: DEFAULT_PORT };
  return { host, port };
}

function resolveRulesPath(): string {
  // Preferred: rules colocated with firebase/firestore (repo path)
  const colocated = path.resolve(__dirname, '../../firestore.rules');
  if (existsSync(colocated)) return colocated;
  // Fallback: top-level project rules
  const repoRoot = path.resolve(__dirname, '../../../..');
  const topLevel = path.join(repoRoot, 'firestore.rules');
  if (existsSync(topLevel)) return topLevel;
  // As a last resort, throw with both attempted paths for troubleshooting
  throw new Error(`Could not find Firestore rules file. Tried: \n - ${colocated}\n - ${topLevel}`);
}

export async function setupEnvironment(): Promise<RulesTestEnvironment> {
  const { host, port } = parseEmulatorHost(process.env.FIRESTORE_EMULATOR_HOST);
  process.env['FIRESTORE_EMULATOR_HOST'] = `${host}:${port}`;
  process.env['FIREBASE_PROJECT_ID'] = process.env['FIREBASE_PROJECT_ID'] || 'demo-unit-tests';

  // Resolve rules file robustly across local and CI environments
  const rulesPath = resolveRulesPath();

  return await initializeTestEnvironment({
    projectId: process.env['FIREBASE_PROJECT_ID']!,
    firestore: {
      rules: await readFile(rulesPath, 'utf-8'),
      host,
      port,
    },
  });
}

export async function setup() {
  // Initialize testing environment (emulators are managed by firebase emulators:exec during CI)
  return await setupEnvironment();
}
