/** @type {import('jest').Config} */
module.exports = {
  preset: 'ts-jest',
  testEnvironment: 'node',
  roots: ['<rootDir>/src'],
  testMatch: ['**/*.test.ts'],
  transform: {
    '^.+\\.ts$': 'ts-jest',
  },
  moduleNameMapper: {
    '^\\./wasm$': '<rootDir>/wasm/pkg/frapcode_core.js',
  },
  transformIgnorePatterns: [
    'node_modules/(?!(frapcode_core)/)',
  ],
};
