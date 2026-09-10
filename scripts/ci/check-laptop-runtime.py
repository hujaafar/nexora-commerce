"""Verify applied container limits and detect OOM/restarts after real journeys."""
import json
import subprocess


def docker(*arguments):
    return subprocess.check_output(['docker', *arguments], text=True).strip()


ids = docker('compose', 'ps', '-q').splitlines()
assert len(ids) == 10, f'Expected all 10 application containers, got {len(ids)}'
containers = json.loads(docker('inspect', *ids))
stats = [json.loads(line) for line in docker('stats', '--no-stream', '--format', '{{json .}}', *ids).splitlines()]
report = []
for container in containers:
    config = container['HostConfig']
    state = container['State']
    name = container['Config']['Labels']['com.docker.compose.service']
    assert 0 < config['Memory'] <= 512 * 1024**2, f'{name}: missing/excessive memory limit'
    assert config['MemorySwap'] == config['Memory'], f'{name}: swap is not bounded'
    assert 0 < config['NanoCpus'] <= 1_000_000_000, f'{name}: missing/excessive CPU limit'
    assert config['RestartPolicy']['Name'] == 'no', f'{name}: automatic restart enabled'
    assert config['LogConfig']['Config'].get('max-size') == '5m', f'{name}: logs unbounded'
    assert state['Running'] and not state['OOMKilled'], f'{name}: stopped/OOM killed'
    assert container['RestartCount'] == 0, f'{name}: restarted during acceptance'
    if 'Health' in state:
        assert state['Health']['Status'] == 'healthy', f'{name}: unhealthy'
    report.append({'service': name, 'limitMiB': config['Memory'] // 1024**2,
                   'cpuLimit': config['NanoCpus'] / 1e9, 'oomKilled': state['OOMKilled'],
                   'restarts': container['RestartCount']})
assert sum(item['limitMiB'] for item in report) <= 4096
print(json.dumps({'containers': report, 'snapshot': stats}, indent=2))
