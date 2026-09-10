import { defineConfig } from 'vitest/config';

// A desktop CI installation shares memory with Nexus and SonarQube. Bound
// worker creation instead of letting host CPU count spawn ten Angular VMs.
export default defineConfig({ test: { maxWorkers: 2 } });
