import { randomBytes } from 'node:crypto';
import { existsSync, readFileSync, writeFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';

// Used locally or by the start scripts in a disposable Node container.
const root = fileURLToPath(new URL('../', import.meta.url));
const target = `${root}.env`;
if (!existsSync(target)) {
  let content = readFileSync(`${root}.env.example`, 'utf8');
  for (const key of ['JWT_SECRET', 'INTERNAL_SERVICE_TOKEN', 'MONGO_ROOT_PASSWORD', 'MINIO_ROOT_PASSWORD']) {
    content = content.replace(new RegExp(`^${key}=.*$`, 'm'), `${key}=${randomBytes(32).toString('hex')}`);
  }
  writeFileSync(target, content, { mode: 0o600 });
  console.log('Created local .env with random secrets.');
} else {
  console.log('Using existing .env.');
}
