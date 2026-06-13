# Test documentation

The active test documentation was moved to:

```text
docs/TESTS.md
```

Run from the project root:

```bash
mvn test
```

Useful targeted checks:

```bash
mvn test -Dtest=VisualizationModeRegistryTest
mvn test -Dtest=VisualizationModesSmokeTest
mvn test -Dtest=RNProviderIntegrationTest
```

This file is kept only as a pointer for developers browsing the `src/test` tree.
