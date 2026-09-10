# Stop only Nexora's optional labs; preserve images, volumes, and build history.
$ErrorActionPreference = 'Stop'
foreach ($project in @('nexora-commerce-ci', 'nexora-commerce-quality', 'nexora-artifacts')) {
    $containers = @(docker ps -aq --filter "label=com.docker.compose.project=$project")
    if ($LASTEXITCODE -ne 0) { throw 'Docker must be running and responsive to stop lab containers.' }
    if ($containers.Count -gt 0) {
        docker update --restart=no @containers
        if ($LASTEXITCODE -ne 0) { throw "Could not disable automatic restart for $project." }
        docker stop --time 45 @containers
        if ($LASTEXITCODE -ne 0) { throw "Could not stop all containers in $project." }
    }
}
