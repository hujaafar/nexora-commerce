"""Check the consumer POMs and packaged version, without a running Nexus server."""
import os
from pathlib import Path
from zipfile import ZipFile
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[2]
version = os.environ["ARTIFACT_VERSION"]
namespace = {"m": "http://maven.apache.org/POM/4.0.0"}
parent = ET.parse(root / ".flattened-pom.xml").getroot()
assert parent.findtext("m:version", namespaces=namespace) == version
modules = parent.findall("m:modules/m:module", namespace)
assert len(modules) == 6
for entry in modules:
    module = entry.text
    consumer = ET.parse(root / module / ".flattened-pom.xml").getroot()
    assert consumer.findtext("m:parent/m:version", namespaces=namespace) == version, module
    jar = root / module / "target" / f"{module}-{version}.jar"
    with ZipFile(jar) as archive:
        metadata = archive.read(f"META-INF/maven/com.nexora/{module}/pom.properties").decode()
        assert f"version={version}" in metadata.splitlines(), module
        assert "org/springframework/boot/loader/launch/JarLauncher.class" in archive.namelist(), module
    print(f"Verified executable JAR and consumer POM: {module}:{version}")
